package upbeatsheep.CommuteAlarm;

import java.io.IOException;

import upbeatsheep.providers.CommuteAlarm;
import upbeatsheep.utils.AlarmOverlay;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
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

	final static public String STATE_INSERT = "insert";
	final static public String STATE_DELETE = "delete";
	private static final int DELETE_DIALOG = 0;
	private static final String[] ALARM_STATUS = new String[] { "Inactive",
			"Active", "Broken" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		final Intent intent = getIntent();

		final String action = intent.getAction();
		Log.i(TAG, intent.getAction());
		if (Intent.ACTION_EDIT.equals(action)) {
			mUri = intent.getData();
			setLocalVariables();

		} else if (Intent.ACTION_DELETE.equals(action)) {
			mUri = intent.getData();
			state = STATE_DELETE;
			showDialog(DELETE_DIALOG);
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
                LocalService.class));
		setLocalVariables();
		mTitle.setText(alarmName);

		String status = ALARM_STATUS[alarmStatus];
		mStatus.setText("This alarm is " + status.toLowerCase());
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
	
	private void stopSound(){
		if (mMediaPlayer != null){
			mMediaPlayer.stop();
		}
		if(v != null){
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
			showDialog(DELETE_DIALOG);
		} else {
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}
	}
	
	

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id){
		case DELETE_DIALOG:
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); 
			 mMediaPlayer = new MediaPlayer();
			 try {
				mMediaPlayer.setDataSource(this, alert);
				final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				 if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
					 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
					 mMediaPlayer.setLooping(true);
					 mMediaPlayer.prepare();
					 mMediaPlayer.start();
				  }
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

			// 1. Vibrate for 1000 milliseconds
			long milliseconds = 1000000000;
			v.vibrate(milliseconds);
		}
		super.onPrepareDialog(id, dialog);
	}
	
	Vibrator v;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_DIALOG:
			return new AlertDialog.Builder(Alarm.this)
					.setIcon(R.drawable.dialog)
					.setTitle(R.string.delete_dialog_title)
					.setMessage(R.string.delete_dialog_message)
					.setCancelable(false)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									
									deleteAlarm();
								}
							})
					.create();
		}
		return null;
	}

	@Override
	protected void onPause() {
		super.onPause();

		stopSound();
		
		manager.removeUpdates(listener);

		

		if (state == STATE_DELETE) {
			deleteAlarm();
		}

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

		stopSound();
		
		ContentValues values = new ContentValues();
		values.put(CommuteAlarm.Alarms.STATUS, 3);
    	Log.i(TAG, "Inserting " + values.toString() + " into " + mUri.toString());
    	getContentResolver().update(mUri, values, null, null);

		finish();
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
	protected boolean isRouteDisplayed() {
		return false;
	}

}
