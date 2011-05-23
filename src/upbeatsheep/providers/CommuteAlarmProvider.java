package upbeatsheep.providers;

import java.util.HashMap;

import upbeatsheep.providers.CommuteAlarm.Alarms;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;


/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class CommuteAlarmProvider extends ContentProvider {

    private static final String TAG = "UpbeatSheep Content Provider";

    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 4;
    private static final String ALARMS_TABLE_NAME = "alarms";

    private static HashMap<String, String> sAlarmsProjectionMap;
    private static HashMap<String, String> sLiveFolderProjectionMap;

    private static final int ALARMS = 1;
    private static final int ALARM_ID = 2;
    private static final int LIVE_FOLDER_ALARMS = 3;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ALARMS_TABLE_NAME + " ("
                    + Alarms._ID + " INTEGER PRIMARY KEY,"
                    + Alarms.PLACE + " TEXT,"
                    + Alarms.LATITUDEE6 + " INTEGER,"
                    + Alarms.LONGITUDEE6 + " INTEGER,"
                    + Alarms.RADIUS + " INTEGER,"
                    + Alarms.STATUS + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(ALARMS_TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
        case ALARMS:
            qb.setProjectionMap(sAlarmsProjectionMap);
            break;

        case ALARM_ID:
            qb.setProjectionMap(sAlarmsProjectionMap);
            qb.appendWhere(Alarms._ID + "=" + uri.getPathSegments().get(1));
            break;

        case LIVE_FOLDER_ALARMS:
            qb.setProjectionMap(sLiveFolderProjectionMap);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = CommuteAlarm.Alarms.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case ALARMS:
        case LIVE_FOLDER_ALARMS:
            return Alarms.CONTENT_TYPE;

        case ALARM_ID:
            return Alarms.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != ALARMS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(CommuteAlarm.Alarms.LONGITUDEE6) == false) {
            values.put(CommuteAlarm.Alarms.LONGITUDEE6, now);
        }

        if (values.containsKey(CommuteAlarm.Alarms.RADIUS) == false) {
            values.put(CommuteAlarm.Alarms.RADIUS, now);
        }

        if (values.containsKey(CommuteAlarm.Alarms.PLACE) == false) {
            Resources r = Resources.getSystem();
            values.put(CommuteAlarm.Alarms.PLACE, r.getString(android.R.string.untitled));
        }

        if (values.containsKey(CommuteAlarm.Alarms.LATITUDEE6) == false) {
            values.put(CommuteAlarm.Alarms.LATITUDEE6, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(ALARMS_TABLE_NAME, Alarms.LATITUDEE6, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(CommuteAlarm.Alarms.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case ALARMS:
            count = db.delete(ALARMS_TABLE_NAME, where, whereArgs);
            break;

        case ALARM_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(ALARMS_TABLE_NAME, Alarms._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case ALARMS:
            count = db.update(ALARMS_TABLE_NAME, values, where, whereArgs);
            break;

        case ALARM_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(ALARMS_TABLE_NAME, values, Alarms._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(CommuteAlarm.AUTHORITY, "alarms", ALARMS);
        sUriMatcher.addURI(CommuteAlarm.AUTHORITY, "alarms/#", ALARM_ID);
        sUriMatcher.addURI(CommuteAlarm.AUTHORITY, "live_folders/alarms", LIVE_FOLDER_ALARMS);

        sAlarmsProjectionMap = new HashMap<String, String>();
        sAlarmsProjectionMap.put(Alarms._ID, Alarms._ID);
        sAlarmsProjectionMap.put(Alarms.PLACE, Alarms.PLACE);
        sAlarmsProjectionMap.put(Alarms.LATITUDEE6, Alarms.LATITUDEE6);
        sAlarmsProjectionMap.put(Alarms.LONGITUDEE6, Alarms.LONGITUDEE6);
        sAlarmsProjectionMap.put(Alarms.RADIUS, Alarms.RADIUS);
        sAlarmsProjectionMap.put(Alarms.STATUS, Alarms.STATUS);

        // Support for Live Folders.
        sLiveFolderProjectionMap = new HashMap<String, String>();
        sLiveFolderProjectionMap.put(LiveFolders._ID, Alarms._ID + " AS " +
                LiveFolders._ID);
        sLiveFolderProjectionMap.put(LiveFolders.NAME, Alarms.PLACE + " AS " +
                LiveFolders.NAME);
        // Add more columns here for more robust Live Folders.
    }
}
