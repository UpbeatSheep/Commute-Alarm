package upbeatsheep.CommuteAlarm;

import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class Alarm extends MapActivity {

	MapView map;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_details);
		
		GeoPoint location = null;
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			location = new GeoPoint(extras.getInt("lat"), extras.getInt("lon"));
		}
		
		map = (MapView) findViewById(R.id.mapview);
		
		MapController controller = map.getController();
		controller.setZoom(16);
		controller.animateTo(location);
		
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
