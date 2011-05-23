package upbeatsheep.CommuteAlarm;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import upbeatsheep.providers.CommuteAlarm;
import upbeatsheep.utils.HTTPClient;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class LocationPicker extends ListActivity {

	final static private String API_URL = "https://maps.googleapis.com/maps/api/";
	
	final static private int DEFAULT_RADIUS = 5000;
	
	final static private String APIKey = "AIzaSyDM8AWFbVUXq58Uf-1_c_oUZYshTu-sYXA";
	final static private String RADIUS = "1";
	final static private String NAME = "rail+station";
	
	final static private String GEOCODE_URL = "geocode/json?sensor=false&address=";
	final static private String PLACES_URL = "place/search/json?sensor=false&key=" + APIKey + "&radius=" + RADIUS + "&name=" + NAME + "&location=";
	
	private Uri mUri;
	private JSONArray results;
	final static public String TAG = "UpbeatSheep";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Intent intent = getIntent();

		String input = null;
		
		results = new JSONArray();
		
		final String action = intent.getAction();
		if (Intent.ACTION_INSERT.equals(action)) {

			input = intent.getExtras().getString("destinationInput");

			mUri = intent.getData();

			if (mUri == null) {
				Log.e(TAG, "Failed to insert new alarm into "
						+ getIntent().getData());
				finish();
				return;
			}

			setProgressBarIndeterminateVisibility(true);
			
			try {
				getJSONResults(input);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

		} else {
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}
		
	}
	
	private void getJSONResults(String input) throws JSONException{
		HTTPClient client = new HTTPClient(this);
		
		JSONObject geocodedResult = client.getJSON(API_URL + GEOCODE_URL + URLEncoder.encode(input));
		
		double lat = geocodedResult.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
		double lon = geocodedResult.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
		
		Log.i(TAG, "Location geocoded: " + lat + lon);
		
		JSONObject placeResult = client.getJSON(API_URL + PLACES_URL + lat + "," + lon);
		
		int index = -1;
		
		if (geocodedResult.getString("status").contentEquals("OK")){
			for (int i = 0; i < geocodedResult.getJSONArray("results").length(); i++){
				lat = geocodedResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
				lon = geocodedResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
				JSONObject cityResult = new JSONObject();
				cityResult.put("name", geocodedResult.getJSONArray("results").getJSONObject(i).getString("formatted_address"));
				cityResult.put("lat", lat);
				cityResult.put("lon", lon);
				cityResult.put("radius", DEFAULT_RADIUS);
				index +=1;
				results.put(index, cityResult);
			}
		} else {
			Log.e(TAG, "Error! Geocoding API Status: " + geocodedResult.getString("status"));
		}
		
		if (placeResult.getString("status").contentEquals("OK")){
			for (int i = 0; i < placeResult.getJSONArray("results").length(); i++){
				lat = placeResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
				lon = placeResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
				JSONObject placesResult = new JSONObject();
				placesResult.put("name", placeResult.getJSONArray("results").getJSONObject(i).getString("name"));
				placesResult.put("lat", lat);
				placesResult.put("lon", lon);
				placesResult.put("radius", DEFAULT_RADIUS);
				index +=1;
				results.put(index, placesResult);
			}
		} else {
			Log.e(TAG, "Error! Places API Status: " + placeResult.getString("status"));
		}
		
		JSONArrayAdapter adapter = new JSONArrayAdapter(this,results);
		
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
			try {
				JSONObject result = results.getJSONObject(position);
				
				ContentValues values = new ContentValues();
				
				values.put(CommuteAlarm.Alarms.PLACE, result.getString("name"));
				values.put(CommuteAlarm.Alarms.LATITUDEE6, (int)(result.getDouble("lat")*1E6));
				values.put(CommuteAlarm.Alarms.LONGITUDEE6, (int)(result.getDouble("lon")*1E6));
				values.put(CommuteAlarm.Alarms.RADIUS, result.getInt("radius"));
				values.put(CommuteAlarm.Alarms.STATUS, 1);
				
				mUri = getContentResolver().insert(getIntent().getData(), values);
				
				startService(new Intent(LocationPicker.this,
		                LocalService.class));
				
				startActivity(new Intent(Intent.ACTION_EDIT, mUri));
				finish();
			} catch (JSONException e) {
				e.printStackTrace();
		}	    
	}
	
	
	
}
