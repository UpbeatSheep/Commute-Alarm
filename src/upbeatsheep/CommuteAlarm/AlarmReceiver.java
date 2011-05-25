package upbeatsheep.CommuteAlarm;

import upbeatsheep.providers.CommuteAlarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "UpbeatSheep";
	private static final int NOTIFICATION_ID = 1000;

	private Uri mUri;

	private Context mContext;
	NotificationManager mNotificationManager;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "AlarmReceiever recieved request for notification for " + intent.getData());
		
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.notify;
		long when = System.currentTimeMillis();

		Notification notification = new Notification();

		notification.icon = icon;
		notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.flags |= Notification.FLAG_INSISTENT;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		CharSequence contentTitle = "You've Arrived";
		CharSequence contentText = "Jazz is fun to listen to.";
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
