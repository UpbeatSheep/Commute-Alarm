package upbeatsheep.CommuteAlarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class AlarmService extends Service {

	private static final String TAG = "UpbeatSheep";

	private Uri mUri;
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);

		locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 10000, 0,
					locationListener);
		Log.i(TAG, "Waiting for location updates...");
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG, "Setting up service...");
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		
		Intent intent = new Intent();
		intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
		mUri = intent.getData();
		
		super.onCreate();
	}
	
	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location myLocation) {
			Log.i(TAG, "Found location!");
			Intent i = new Intent(Alarm.ACTION_NOTIFY, mUri);
			i.putExtra("myLocation", myLocation);
			Log.i(TAG, "Alerting reciever of our new location");
			sendBroadcast(i);
			stopSelf();
		}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Service shutting down...");
		locationManager.removeUpdates(locationListener);
		Log.i(TAG, "Stopped checking for location updates.");
		super.onDestroy();
	}
	
	public class LocalBinder extends Binder {
		AlarmService getService() {
			return AlarmService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

}