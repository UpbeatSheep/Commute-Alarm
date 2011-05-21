package upbeatsheep.CommuteAlarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;

public class Splash extends Activity {

	protected int _splashTime = 3000;
	protected Handler _exitHandler = null;
	protected Runnable _exitRunnable = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_screen);
		
		Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
        }

		// Runnable exiting the splash screen and launching the menu
		_exitRunnable = new Runnable() {
			public void run() {
				exitSplash();
			}
		};
		// Run the exitRunnable in in _splashTime ms
		_exitHandler = new Handler();
		_exitHandler.postDelayed(_exitRunnable, _splashTime);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// Remove the exitRunnable callback from the handler queue
			_exitHandler.removeCallbacks(_exitRunnable);
			// Run the exit code manually
			exitSplash();
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		_exitHandler.removeCallbacks(_exitRunnable);
		super.onBackPressed();
	}

	private void exitSplash() {
		Intent i = new Intent(Intent.ACTION_VIEW, getIntent().getData());
		i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(i);
		overridePendingTransition(R.anim.hold,
				R.anim.fadeout);
		finish();
	}
}
