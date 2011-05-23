package upbeatsheep.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public final class CommuteAlarm {
    public static final String AUTHORITY = "upbeatsheep.providers.CommuteAlarm";

    // This class cannot be instantiated
    private CommuteAlarm() {}
    
    /**
     * Notes table
     */
    public static final class Alarms implements BaseColumns {
        // This class cannot be instantiated
        private Alarms() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/alarms");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upbeatsheep.alarms";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upbeatsheep.alarms";

   

        /**
         * The title of the note
         * <P>Type: TEXT</P>
         */
        public static final String PLACE = "place";

        /**
         * The note itself
         * <P>Type: TEXT</P>
         */
        public static final String LATITUDEE6 = "lat";

        /**
         * The timestamp for when the note was created
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String LONGITUDEE6 = "long";

        /**
         * The timestamp for when the note was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String RADIUS = "radius";
        
        public static final String STATUS = "temp";
        
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = PLACE + " DESC";
    }
}

