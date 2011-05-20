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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.maps.GeoPoint;

public class LocationAlarms extends Activity {
	
	Button addAlarm;
	EditText destinationInput;
	Context mContext = this;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        initialiseWidgets();
        
        setUpOnClickListeners();
      
    }
    
    private void initialiseWidgets(){
    	addAlarm =  (Button) findViewById(R.id.btn_add_alarm);
    	destinationInput = (EditText) findViewById(R.id.txt_destination);
    }
    
    private void setUpOnClickListeners(){
    	addAlarm.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				//TODO: Thread this!
				GeoPoint inputLocation = geocode(destinationInput.getText().toString());
				getPlaces(inputLocation, "Rail Station", "");
				getPlaces(inputLocation, "Bus Stop", "");
				
				//TODO: Show dialog of choices
				
				Intent i = new Intent(mContext, LocationAlarm.class);
				startActivity(i);
			}
		});
    }
    
    public static GeoPoint geocode(String address) {
		//TODO: Turn string address into a geopoint
    	return null;
	}

    public JSONObject getPlaces(GeoPoint location, String name, String type){
    	//TODO: Return a JSONObject (or something else?) of the above parameters
    	return null;
    }
    
}