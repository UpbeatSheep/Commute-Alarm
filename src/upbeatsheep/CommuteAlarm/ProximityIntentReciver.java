package upbeatsheep.CommuteAlarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

public class ProximityIntentReciver extends BroadcastReceiver
{
	private static final String TAG = "UpbeatSheep";
	    private static final int NOTIFICATION_ID = 1000;

	    private Uri mUri;
	    
	    private Context mContext;
	    
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	
	    	
	        mContext = context;
	    	
	    	if (intent.getExtras()!= null){
	    	  		mUri = ContentUris.withAppendedId(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI, intent.getExtras().getInt("muri"));
	    	}
	    	
	        String key = LocationManager.KEY_PROXIMITY_ENTERING;
	        
	        Boolean entering = intent.getBooleanExtra(key, false);
	        
	        if (entering) {
	            Log.d(TAG, "entering");
	        }
	        else {
	            Log.d(TAG, "exiting");
	        }
	        
	        NotificationManager notificationManager = 
	            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        
	        intent = new Intent(Intent.ACTION_DELETE, mUri);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 4, intent, Intent.FLAG_ACTIVITY_CLEAR_TASK);    
	        
			try {
				pendingIntent.send();
			} catch (CanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	        Notification notification = createNotification();
	        notification.setLatestEventInfo(context, 
	            "Proximity Alert!", "You are near your point of interest.", pendingIntent);
	        
	        notificationManager.notify(NOTIFICATION_ID, notification);
	        
	    }
	    
	    private Notification createNotification() {
	        Notification notification = new Notification();
	        
	        notification.icon = R.drawable.icon;
	        notification.when = System.currentTimeMillis();
	        
	        notification.flags |= Notification.FLAG_AUTO_CANCEL;
	        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
	        
	        notification.defaults |= Notification.DEFAULT_VIBRATE;
	        notification.defaults |= Notification.DEFAULT_LIGHTS;
	        
	        notification.ledARGB = Color.WHITE;
	        notification.ledOnMS = 1500;
	        notification.ledOffMS = 1500;
	        
	        
	        
	        return notification;
	    }
	    
	
}
