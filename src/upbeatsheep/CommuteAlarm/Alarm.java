package upbeatsheep.CommuteAlarm;

import upbeatsheep.providers.CommuteAlarm;
import upbeatsheep.utils.AlarmOverlay;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class Alarm extends MapActivity {

	private static final String TAG = "UpbeatSheep";
	public int alarmLatitudeE6 = 0;
	public int alarmLongitudeE6 = 0;
	public int alarmId = 0;
	private String alarmName = "Somewhere...";
	AlarmOverlay alarmOverlay;
	private int alarmRadius = 5000;
	LocationListener listener;
	SeekBar mAlarmRadius;
	TextView mStatus;
	LocationManager manager;
	private Cursor mCursor;
	MapView mMap;
	TextView mTitle;
	private Uri mUri;
	public int myLatitude = 0;
	public int myLongitude = 0;
	MyLocationOverlay myLocation;
	MediaPlayer mMediaPlayer;

	private String state = "insert";
	private int alarmStatus = 0;

	 public static final String ACTION_NOTIFY = "upbeatsheep.CommuteAlarm.intent.action.NOTIFY";

	final static public String STATE_INSERT = "insert";
	final static public String STATE_DELETE = "delete";
	final static int DIALOG_DELETE = 0;
	final static int ALARM_STATUS_ACTIVE = 1;
	final static int ALARM_STATUS_ARRIVED = 0;
	final static int ALARM_STATUS_DELETED = 2;
	final static int ALARM_STATUS_OTHER = 3;

	PowerManager pm;
	PowerManager.WakeLock wl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		
		
		
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");

		final Intent intent = getIntent();

		final String action = intent.getAction();
		if (Intent.ACTION_EDIT.equals(action)) {
			mUri = intent.getData();
			setLocalVariables();

		} else if (Intent.ACTION_DELETE.equals(action)) {
			mUri = intent.getData();
			state = STATE_DELETE;
			deleteAlarm();
		} else {
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}

		setContentView(R.layout.alarm_details);

		mMap = (MapView) findViewById(R.id.mapview);
		mTitle = (TextView) findViewById(R.id.placename);
		mStatus = (TextView) findViewById(R.id.status);
		mAlarmRadius = (SeekBar) findViewById(R.id.radius_seekBar);

		mAlarmRadius.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				alarmRadius = progress * 250;
				if (alarmOverlay != null) {
					alarmOverlay.setMeters(alarmRadius);
					mMap.invalidate();
				}
				ContentValues values = new ContentValues();
				values.put(CommuteAlarm.Alarms.RADIUS, alarmRadius);

				getContentResolver().update(mUri, values, null, null);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

		});
	}

	private void setLocalVariables() {
		mCursor = managedQuery(mUri, null, null, null, null);

		if (mCursor != null) {
			// Make sure we are at the one and only row in the cursor.
			if (mCursor.moveToFirst()) {

				alarmLatitudeE6 = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.LATITUDEE6));
				alarmLongitudeE6 = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.LONGITUDEE6));
				alarmName = mCursor.getString(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.PLACE));
				alarmRadius = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.RADIUS));
				alarmId = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms._ID));
				alarmStatus = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.STATUS));
			}
		} else {
			// something has gone wrong
			Log.e(TAG, "Nothing found in the cursor!");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		
		startService(new Intent(Alarm.this,
                AlarmService.class));
		setLocalVariables();
		mTitle.setText(alarmName);

		switch(alarmStatus){
		case ALARM_STATUS_ARRIVED:
			mStatus.setText("You have arrived!");
			showDialog(DIALOG_DELETE);
			break;
		case ALARM_STATUS_ACTIVE:
			mStatus.setText("Your alarm is all set. Use the slider below to change the radius.");
			break;
		case ALARM_STATUS_DELETED:
			mStatus.setText("This alarm has been deleted and is inactive.");
			break;
		case ALARM_STATUS_OTHER:
			mStatus.setText("Something has gone wrong, sorry!");
			break;
			default:
				mStatus.setText("Something has gone wrong, sorry!");
				break;
		}
		setUpMap(new GeoPoint(alarmLatitudeE6, alarmLongitudeE6));
		mAlarmRadius.setProgress(Math.round(alarmRadius / 250));
		
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		listener = new MyLocationListener();

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					listener);
		} else {
			manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
					0, listener);
		}
	}

	private void stopSound() {
		if (mMediaPlayer != null) {
			if(mMediaPlayer.isPlaying()){
				mMediaPlayer.stop();
				mMediaPlayer.release();
			}
		}
		if (v != null) {
			v.cancel();
		}

	}

	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);

		final String action = newIntent.getAction();
		if (Intent.ACTION_DELETE.equals(action)) {
			mUri = newIntent.getData();
			setLocalVariables();
			state = STATE_DELETE;
			deleteAlarm();
		} else {
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_DELETE:
			
		}
		super.onPrepareDialog(id, dialog);
	}

	Vibrator v;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE:
			return new AlertDialog.Builder(Alarm.this)
					.setIcon(R.drawable.dialog)
					.setTitle(R.string.delete_dialog_title)
					.setMessage(R.string.delete_dialog_message)
					.setCancelable(false)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
									mNotificationManager.cancel(alarmId);
									finish();
								}
							}).create();
		}
		return null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(wl.isHeld()){
		wl.release();
		}
		
		manager.removeUpdates(listener);

		myLocation.disableCompass();
		myLocation.disableMyLocation();
	}

	private void setUpMap(GeoPoint geoPoint) {
		mMap.getOverlays().clear();

		MapController controller = mMap.getController();
		controller.animateTo(geoPoint);

		alarmOverlay = new AlarmOverlay(getBaseContext(), geoPoint, mMap);
		alarmOverlay.setMeters(alarmRadius);

		myLocation = new MyLocationOverlay(getBaseContext(), mMap);

		mMap.getOverlays().add(alarmOverlay);
		mMap.getOverlays().add(myLocation);

		myLocation.enableCompass();
		myLocation.enableMyLocation();

	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {

			int tempLat = (int) (location.getLatitude() * 1E6);
			int tempLon = (int) (location.getLongitude() * 1E6);

			int latDifference = tempLat - myLatitude;
			int lonDifference = tempLon - myLongitude;

			if (lonDifference > 1000 || lonDifference < -1000
					|| latDifference > 1000 || latDifference < -1000) {

				myLatitude = tempLat;
				myLongitude = tempLon;

				mMap.getController()
						.zoomToSpan(
								(alarmLatitudeE6 > myLatitude ? alarmLatitudeE6
										- myLatitude : myLatitude
										- alarmLatitudeE6),
								(alarmLongitudeE6 > myLongitude ? alarmLongitudeE6
										- myLongitude
										: myLongitude - alarmLongitudeE6));

				mMap.getController()
						.animateTo(
								new GeoPoint(
										alarmLatitudeE6
												- ((alarmLatitudeE6 - myLatitude) / 2),
										alarmLongitudeE6
												- ((alarmLongitudeE6 - myLongitude) / 2)));
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	private final void deleteAlarm() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}

		ContentValues values = new ContentValues();
		values.put(CommuteAlarm.Alarms.STATUS, ALARM_STATUS_DELETED);
		getContentResolver().update(mUri, values, null, null);

		showDialog(DIALOG_DELETE);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.alarms_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.delete:
			deleteAlarm();
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
