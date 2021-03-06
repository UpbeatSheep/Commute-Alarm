package upbeatsheep.CommuteAlarm;

import java.io.IOException;

import upbeatsheep.providers.CommuteAlarm;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class Alarms extends ListActivity {

	private static final String TAG = "UpbeatSheep";
	Button addAlarm;
	EditText destinationInput;
	Context mContext = this;
	TextView yourAlarmHelp;
	Cursor mCursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "Started Alarms activity");
		 
		setContentView(R.layout.main);
		startService(new Intent(Alarms.this, AlarmService.class));

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
		}

		initialiseWidgets();

		setUpOnClickListeners();

		Log.v(TAG, "Fetching alarms from the database");
		mCursor = managedQuery(getIntent().getData(), null,
				CommuteAlarm.Alarms.STATUS + " = 1", null,
				CommuteAlarm.Alarms.DEFAULT_SORT_ORDER);

		Log.v(TAG,
				"Defining an adapter for the list view in the Alarms activity");
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, mCursor,
				new String[] { CommuteAlarm.Alarms.PLACE },
				new int[] { android.R.id.text1 });

		Log.v(TAG, "Setting the list adapter for the Alarms activity");
		setListAdapter(adapter);
		
		startManagingCursor(mCursor);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCursor != null) {
			if (mCursor.getCount() < 1) {
				yourAlarmHelp
						.setText("You don't currently have any alarms set. Use the Add button above to set your destination.");
			} else {
				yourAlarmHelp
						.setText("You currently have Commute Alarm set to alert you when you reach the following locations:");
			}
		}
	}

	private void initialiseWidgets() {
		Log.v(TAG, "Initialising widgets in Alarms activity");
		yourAlarmHelp = (TextView) findViewById(R.id.your_alarm_help);
		addAlarm = (Button) findViewById(R.id.btn_add_alarm);
		destinationInput = (EditText) findViewById(R.id.txt_destination);
	}

	private void setUpOnClickListeners() {
		Log.v(TAG, "Setting up OnClickListeners in Alarms activity");
		addAlarm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(Intent.ACTION_INSERT, getIntent()
						.getData());
				i.putExtra("destinationInput", destinationInput.getText()
						.toString());
				Log.v(TAG, "Starting activity for "
						+ getIntent().getData().toString() + " in insert mode");
				startActivity(i);
			}
		});
	}
	
	

	@Override
	protected void onDestroy() {
		
		super.onDestroy();		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

		Log.v(TAG, "Starting activity for " + uri.toString() + " in edit mode");
		startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}
}