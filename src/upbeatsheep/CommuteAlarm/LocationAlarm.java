package upbeatsheep.CommuteAlarm;

import android.os.Bundle;

import com.google.android.maps.MapActivity;

public class LocationAlarm extends MapActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_details);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
