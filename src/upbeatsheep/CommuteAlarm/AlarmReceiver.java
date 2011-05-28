package upbeatsheep.CommuteAlarm;

import java.util.Calendar;
import java.util.HashMap;

import upbeatsheep.providers.CommuteAlarm;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "UpbeatSheep";
	private Context mContext;
	private Uri mUri;
	
	private NotificationManager notificationManager;
	private AlarmManager alarmManager;
	private PendingIntent pendingIntent;
	
	private HashMap<Integer, Notification> notificationList = new HashMap<Integer, Notification>();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.i(TAG, "Recieved broadcast!");
		
		mContext = context;
		mUri = intent.getData();
		
		notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

		Intent i = new Intent(mContext, AlarmService.class);
		
		pendingIntent = PendingIntent.getService(mContext, 192837, i, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Log.i(TAG, "Cancelling scheduled location updates");
		alarmManager.cancel(pendingIntent);
		
		Location myLocation = null;
		
		if (intent.getExtras()!=null){
			myLocation = (Location) intent.getExtras().get("myLocation");
		}
		
		if(myLocation != null){
			Log.i(TAG, "Checking alarms in the database");
			checkAlarms(myLocation);
		}
	}
	
	private void checkAlarms(Location myLocation){
		Cursor mCursor = mContext.getContentResolver().query(mUri, null,
				null, null, null);
		
		if (mCursor.moveToFirst()) {

			int activeAlarms = 0;
			float lowestDistance = 0;

			do {
				int alarmLatitudeE6 = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.LATITUDEE6));
				int alarmLongitudeE6 = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.LONGITUDEE6));
				String alarmName = mCursor.getString(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.PLACE));
				int alarmRadius = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.RADIUS));
				int alarmId = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms._ID));
				int alarmStatus = mCursor.getInt(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.STATUS));

				double latitude = (double) (alarmLatitudeE6 / 1E6);
				double longitude = (double) (alarmLongitudeE6 / 1E6);

				Location alarmLocation = new Location("Geocoded");

				alarmLocation.setLatitude(latitude);
				alarmLocation.setLongitude(longitude);

				float alarmDistance = myLocation.distanceTo(alarmLocation);
				
				if (alarmStatus == Alarm.ALARM_STATUS_ACTIVE){
					activeAlarms += 1;
					
					
					if (lowestDistance == 0){
						lowestDistance = alarmDistance;
					} else if (alarmDistance < lowestDistance){
						Log.i(TAG, alarmName + " is lower than the lowest recorded distance at "+ alarmDistance);
						lowestDistance = alarmDistance;
					}
					Log.i(TAG, "Lowest Distance: " + lowestDistance);
					
					if (alarmDistance < alarmRadius){
						arrived(alarmId);
					} else {
						showAlarmNotification(alarmId, alarmName, alarmDistance, estimatedArrivalTime(alarmDistance,myLocation.getSpeed()));
					}
				}
				
			} while (mCursor.moveToNext());
			
			if(activeAlarms > 0){
				Log.i(TAG, "Found " + activeAlarms + " active alarms");
				Log.i(TAG, "Scheduling another location update");
				alarmManager.set(AlarmManager.RTC_WAKEUP, nextLocationUpdateTime(lowestDistance), pendingIntent);
			} else {
				Log.i(TAG, "No active alarms found");
			}
		}
		mCursor.close();
	}
	
	private long nextLocationUpdateTime(float distance){
		Log.i(TAG, "The lowest alarm distance is " + distance + "m");
		Calendar currentTime = Calendar.getInstance();
		
		Log.i(TAG, "Current time: " + currentTime.getTime().toGMTString());
		
		if(distance < 25000){
			currentTime.add(Calendar.SECOND, 10);
			Log.i(TAG, "Adding 10 seconds");
		} else if(distance < 50000){
			currentTime.add(Calendar.MINUTE, 1);
			Log.i(TAG, "Adding 1 minute");
		} else if (distance < 100000){
			currentTime.add(Calendar.MINUTE, 5);
			Log.i(TAG, "Adding 5 minutes");
		} else if (distance < 200000){
			currentTime.add(Calendar.MINUTE, 15);
			Log.i(TAG, "Adding 15 minutes");
		} else {
			currentTime.add(Calendar.MINUTE, 20);
			Log.i(TAG, "Adding 20 minutes");
		}
		
		Log.i(TAG, "Setting the next update time to " + currentTime.getTime().toGMTString());
		return currentTime.getTimeInMillis();
	}
	
	private long estimatedArrivalTime(float distance, float speed){
		return 0;
	}
	
	private void showAlarmNotification(int alarmId, String alarmName,
			float alarmDistance, long estimatedArrivalTime) {
		
		Uri uri = ContentUris.withAppendedId(mUri, alarmId);
		
		Notification notification = new Notification(R.drawable.notify, alarmName,
				System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				new Intent(Intent.ACTION_EDIT, uri), 0);

		notification.setLatestEventInfo(mContext,
				alarmName, alarmDistance + "m to go", contentIntent);
		
		notificationManager.notify(alarmId, notification);
	}
	
	private void arrived(int id){
		Log.i(TAG, "Arrived!");
	}
}