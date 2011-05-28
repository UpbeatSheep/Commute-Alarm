package upbeatsheep.CommuteAlarm;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import upbeatsheep.providers.CommuteAlarm;
import upbeatsheep.utils.HTTPClient;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class LocationPicker extends ListActivity {

	final static private String API_URL = "https://maps.googleapis.com/maps/api/";
	
	final static private int DEFAULT_RADIUS = 5000;
	
	final static private String APIKey = "AIzaSyDM8AWFbVUXq58Uf-1_c_oUZYshTu-sYXA";
	final static private String RADIUS = "1";
	final static private String NAME = "rail+station";
	
	final static private String GEOCODE_URL = "geocode/json?sensor=false&region=gb&address=";
	final static private String PLACES_URL = "place/search/json?sensor=false&key=" + APIKey + "&radius=" + RADIUS + "&name=" + NAME + "&location=";
	
	private Uri mUri;
	private JSONArray results;
	final static public String TAG = "UpbeatSheep";
	ListView lv;
	Context mContext;
	View footer;
	JSONArrayAdapter adapter;
	
	ListView list;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		mContext = this;
		
		
		
		final Intent intent = getIntent();

		String input = null;
		setContentView(R.layout.location_picker);
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
			
			
			
			adapter = new JSONArrayAdapter(mContext,results);
			
			setListAdapter(adapter);
			
			//View footer = getLayoutInflater().inflate(R.layout.list_footer, list, false);
			//list.addView(footer);
			new GeocodeInput().execute(input);

			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

		} else {
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}
		
	}
	
	private class GeocodeInput extends AsyncTask<String, Integer, JSONArray> {

		@Override
		protected JSONArray doInBackground(String... input) {
			HTTPClient client = new HTTPClient(mContext);
			
			try {
				
			JSONObject geocodedResult = client.getJSON(API_URL + GEOCODE_URL + URLEncoder.encode(input[0]));
			
			double lat = geocodedResult.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
			double lon = geocodedResult.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
			
			Log.i(TAG, "Location geocoded: " + lat + lon);
			JSONArray jsons = new JSONArray();
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
						jsons.put(cityResult);
					}
				} else {
					Log.e(TAG, "Error! Geocoding API Status: " + geocodedResult.getString("status"));
				}
				return jsons;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
			
			
		}

		@Override
		protected void onPostExecute(JSONArray result) {
			super.onPostExecute(result);
			if(result!=null){
				for(int i = 0;i< result.length();i++){
				
					try {
						results.put(result.getJSONObject(i));
					
						adapter.notifyDataSetChanged();
						Location location = new Location("Geocoding API");
					
						location.setLatitude(result.getJSONObject(i).getDouble("lat"));
						location.setLongitude(result.getJSONObject(i).getDouble("lon"));
					
						new GetPlaces().execute(location);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			
			}
				
				
			setProgressBarIndeterminateVisibility(false);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			setProgressBarIndeterminateVisibility(true);
		}
	 }
	
	private class GetPlaces extends AsyncTask<Location, JSONObject, JSONArray> {

		@Override
		protected JSONArray doInBackground(Location... input) {
			HTTPClient client = new HTTPClient(mContext);
			
			try {
				
			
			double lat = input[0].getLatitude();
			double lon = input[0].getLongitude();
			
			Log.i(TAG, "Location geocoded: " + lat + lon);
			JSONArray jsons = new JSONArray();
			JSONObject placeResult = client.getJSON(API_URL + PLACES_URL + lat + "," + lon);
			if (placeResult.getString("status").contentEquals("OK")){
				for (int i = 0; i < placeResult.getJSONArray("results").length(); i++){
					lat = placeResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
					lon = placeResult.getJSONArray("results").getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
					JSONObject placesResult = new JSONObject();
					placesResult.put("name", placeResult.getJSONArray("results").getJSONObject(i).getString("name"));
					placesResult.put("lat", lat);
					placesResult.put("lon", lon);
					placesResult.put("radius", DEFAULT_RADIUS);
					jsons.put(placesResult);
				}
			} else {
				Log.e(TAG, "Error! Places API Status: " + placeResult.getString("status"));
			}
			
			return jsons;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return null;
		}

		@Override
		protected void onPostExecute(JSONArray result) {
			super.onPostExecute(result);
			
				if(result!=null){
					for(int i = 0;i< result.length();i++){
					
						try {
							results.put(result.getJSONObject(i));
							adapter.notifyDataSetChanged();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					setProgressBarIndeterminateVisibility(false);
				}
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			setProgressBarIndeterminateVisibility(true);
		}
	 }

	protected void onListItemClick(ListView l, View v, int position, long id) {
			try {
				JSONObject result = results.getJSONObject(position);
				
				ContentValues values = new ContentValues();
				
				values.put(CommuteAlarm.Alarms.PLACE, result.getString("name"));
				values.put(CommuteAlarm.Alarms.LATITUDEE6, (int)(result.getDouble("lat")*1E6));
				values.put(CommuteAlarm.Alarms.LONGITUDEE6, (int)(result.getDouble("lon")*1E6));
				values.put(CommuteAlarm.Alarms.RADIUS, result.getInt("radius"));
				values.put(CommuteAlarm.Alarms.STATUS, Alarm.ALARM_STATUS_ACTIVE);
				
				mUri = getContentResolver().insert(getIntent().getData(), values);
				
				startService(new Intent(LocationPicker.this,
		                AlarmService.class));
				
				startActivity(new Intent(Intent.ACTION_EDIT, mUri));
				finish();
			} catch (JSONException e) {
				e.printStackTrace();
		}	    
	}
	
	
	
}
