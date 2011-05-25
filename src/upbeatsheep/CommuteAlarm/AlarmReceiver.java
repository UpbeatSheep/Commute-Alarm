package upbeatsheep.CommuteAlarm;

import upbeatsheep.providers.CommuteAlarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "UpbeatSheep";

	NotificationManager mNotificationManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "AlarmReceiever recieved request for notification for " + intent.getData());
		
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Cursor mCursor = context.getContentResolver().query(intent.getData(), null,
				null, null, null);

		String alarmName = "Commute Alarm";
		
		if (mCursor != null) {
			if (mCursor.moveToFirst()) {
				alarmName = mCursor.getString(mCursor
						.getColumnIndex(CommuteAlarm.Alarms.PLACE));
			}
		}
		
		int icon = R.drawable.notify;

		Notification notification = new Notification();

		notification.icon = icon;
		notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.flags |= Notification.FLAG_INSISTENT;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		CharSequence contentTitle = alarmName;
		CharSequence contentText = "You have arrived!";
		Intent notificationIntent = new Intent(Intent.ACTION_DELETE, intent.getData());
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		Log.i(TAG, "Displaying Notification");

		mNotificationManager.notify(Integer.parseInt(intent.getData().getLastPathSegment()), notification);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(notificationIntent);
	}
	
	
}
