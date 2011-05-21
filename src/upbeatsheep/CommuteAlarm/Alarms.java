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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.maps.GeoPoint;

public class Alarms extends Activity {
	
	Button addAlarm;
	EditText destinationInput;
	Context mContext = this;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        Intent i = new Intent(this, Splash.class);
		startActivity(i);
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
				setProgressBarIndeterminateVisibility(true); 
				
				//TODO: Thread this!
				GeoPoint inputLocation = geocode(destinationInput.getText().toString());
				getPlaces(inputLocation, "Rail Station", "");
				getPlaces(inputLocation, "Bus Stop", "");
				
				//TODO: Show dialog of choices
				
				Intent i = new Intent(mContext, Alarm.class);
				i.putExtra("lat", inputLocation.getLatitudeE6());
				i.putExtra("lon", inputLocation.getLongitudeE6());
				setProgressBarIndeterminateVisibility(false); 
				startActivity(i);
			}
		});
    }
    
    public static GeoPoint geocode(String address) {
    	HttpGet httpGet = new HttpGet("http://maps.google.com/maps/api/geocode/json?address=" 
    			+ URLEncoder.encode(address)
				+ "&sensor=false");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		Log.i("UpbeatSheep", "Getting Location...");
		
		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Double lon = new Double(0);
		Log.i("UpbeatSheep", "Longitude: " + lon);
		Double lat = new Double(0);
		Log.i("UpbeatSheep", "Latitude: " + lat);

		try {

			lon = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");

			lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");
			
			return new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));

		} catch (JSONException e) {
			e.printStackTrace();
			return new GeoPoint((int) (50.3703805 * 1E6), (int) (-4.1426530 * 1E6));
		}
	}

    public JSONObject getPlaces(GeoPoint location, String name, String type){
    	//TODO: Return a JSONObject (or something else?) of the above parameters
    	return null;
    }
    
}