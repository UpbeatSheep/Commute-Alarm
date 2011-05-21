package upbeatsheep.CommuteAlarm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import upbeatsheep.providers.CommuteAlarm;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

import com.google.android.maps.GeoPoint;

public class Alarms extends ListActivity {
	
	Button addAlarm;
	EditText destinationInput;
	Context mContext = this;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
		
		Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(upbeatsheep.providers.CommuteAlarm.Alarms.CONTENT_URI);
        }
		
        initialiseWidgets();
        
        setUpOnClickListeners();
        
        Log.i("UpbeatSheep", getIntent().getData().toString());
        Cursor cursor = managedQuery(getIntent().getData(), null, null, null,
                CommuteAlarm.Alarms.DEFAULT_SORT_ORDER);

       SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
            new String[] { CommuteAlarm.Alarms.PLACE }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
      
    }
    
    private void initialiseWidgets(){
    	addAlarm =  (Button) findViewById(R.id.btn_add_alarm);
    	destinationInput = (EditText) findViewById(R.id.txt_destination);
    }
    
    private void setUpOnClickListeners(){
    	addAlarm.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(Intent.ACTION_INSERT, getIntent().getData());
				i.putExtra("destinationInput", destinationInput.getText().toString());
				setProgressBarIndeterminateVisibility(false); 
				startActivity(i);
			}
		});
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}