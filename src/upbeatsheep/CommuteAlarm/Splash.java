package upbeatsheep.CommuteAlarm;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		finish();
		setContentView(R.layout.splash_screen);
	}
}
