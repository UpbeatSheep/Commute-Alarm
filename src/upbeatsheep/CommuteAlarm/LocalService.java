/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package upbeatsheep.CommuteAlarm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import upbeatsheep.providers.CommuteAlarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application.  The {@link LocalServiceController}
 * and {@link LocalServiceBinding} classes show how to interact with the
 * service.
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class LocalService extends Service {
    private static final String TAG = "UpbeatSheep";

	private NotificationManager mNM;

    private Uri mUri;
    
    private Cursor mCursor;
    HashMap<Integer, Notification> notificationList = new HashMap<Integer, Notification>();
    
    Context mContext;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }
    LocationManager manager;
    LocationListener listener;
    @Override
    public void onCreate() {
    	mContext = this;
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
        mUri = intent.getData();
        Log.i(TAG, "Setting up LocationManager");
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAG, "Setting up LocationListener");
        listener = new MyLocationListener();
        Log.i(TAG, "Requesting Location Updates");
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					listener);
		} else {
			manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					listener);
		}
    }
    
    int myLatitude = 0;
    int myLongitude = 0;
    
    private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location myLocation) {
			Log.i(TAG, "Location Changed");

			Log.i(TAG, "Your Location: " + myLocation.getLatitude() + ", " + myLocation.getLongitude());
				mCursor = getBaseContext().getContentResolver().query(mUri, null, null, null, null);

				mCursor.moveToPosition(3);
				
			    
				
				if (mCursor.moveToFirst()) {
					Log.i(TAG, "Reading from the database successful");
		        	
					int activeAlarms=0;
					
		        	do {
		        		
		        		Log.i(TAG, "Database Entry: " + mCursor.getString(mCursor.getColumnIndex(CommuteAlarm.Alarms.PLACE)));
		        		int lat = mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.LATITUDEE6));
		        		int lon = mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.LONGITUDEE6));
		        		
		        		int status = mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.STATUS));
		        		
		        		String name = mCursor.getString(mCursor.getColumnIndex(CommuteAlarm.Alarms.PLACE));
		        		int id = mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms._ID));
		        		int radius = mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.RADIUS));
		        		
		        		double latitude = (double)(lat/1E6);
		        		double longitude = (double)(lon/1E6);

		        		Location alarmLocation = new Location("Geocoded");
		        		
		        		alarmLocation.setLatitude(latitude);
		        		alarmLocation.setLongitude(longitude);
	        		
		        		if (status == 1){
		        			activeAlarms +=1;
		        		}
	        			
		        		Log.i(TAG, "Alarm Location: " + latitude + ", " + longitude + ". Distance: " + alarmLocation.distanceTo(myLocation) + "m.");
		        		if((alarmLocation.distanceTo(myLocation)) < radius && (status == 1)){
		        			Log.i(TAG, mUri.toString());
		        			mUri = Uri.withAppendedPath(mUri, String.valueOf(id));
		        			Log.i(TAG, mUri.toString());
		        			mNM.cancel(id);
		        			arrivedAtLocation();
		        		} 
		        		if (status != 1) {
		        			notificationList.remove(id);
		        			mNM.cancel(id);
		        		} else {
		        			if (notificationList.containsKey(id)){
			        			Uri uri = ContentUris.withAppendedId(mUri, id);
			        	        
			        	        // The PendingIntent to launch our activity if the user selects this notification
			        	        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
			        	                new Intent(Intent.ACTION_EDIT, uri), 0);
			        	        
			        			notificationList.get(id).setLatestEventInfo(getBaseContext(), getText(R.string.local_service_label),
			        					alarmLocation.distanceTo(myLocation) + "m to " + name, contentIntent);
			        		} else {
			        			notificationList.put(id, showNotification(alarmLocation.distanceTo(myLocation) + "m to " + name, id));
			        			mNM.notify(id, notificationList.get(id));
			        		}
		        		}
		        	} while (mCursor.moveToNext());
		        	
		        	if (activeAlarms == 0){
		        		stopSelf();
		        	}
				} else {
					stopSelf();
				}
			
		}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	}
    
    private void arrivedAtLocation(){
    	ContentValues values = new ContentValues();
    	
    	values.put(CommuteAlarm.Alarms.STATUS, 0);
    	Log.i(TAG, "Inserting " + values.toString() + " into " + mUri.toString());
    	getContentResolver().update(mUri, values, null, null);
    	Log.i(TAG, mUri.toString());
    	Intent i = new Intent(Intent.ACTION_DELETE, mUri);
    	Log.i(TAG, mUri.toString());
    	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(i);
    	Intent intent = new Intent();
        intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
        mUri = intent.getData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.local_service_started);
        Log.i(TAG, "Shutting down service");
        manager.removeUpdates(listener);
        mNM.cancelAll();
        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    /**
     * Show a notification while this service is running.
     */
    private Notification showNotification(String text, int id) {
    	
    	
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.notify, text,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        Uri uri = ContentUris.withAppendedId(mUri, id);
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(Intent.ACTION_EDIT, uri), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(mContext, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        
		return notification;
    }
}

