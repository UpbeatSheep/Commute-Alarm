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
import upbeatsheep.utils.AlarmOverlay;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class Alarm extends MapActivity {

	private static final String TAG = "UpbeatSheep";
	MapView mMap;
	TextView mTitle;
	private Uri mUri;
	private Cursor mCursor;
	
	private String mPlaceName = "Somewhere...";
	private int mLatitude;
	private int mLongitude;
	private int mRadius = 5;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
				
		final Intent intent = getIntent();

		String input = null;
		
        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mUri = intent.getData();
            
        } else if (Intent.ACTION_INSERT.equals(action)) {
        	
        	input = intent.getExtras().getString("destinationInput");
        	
            mUri = getContentResolver().insert(intent.getData(), null);

            if (mUri == null) {
                Log.e(TAG, "Failed to insert new alarm into " + getIntent().getData());
                finish();
                return;
            }
            
            setProgressBarIndeterminateVisibility(true); 
    		//TODO: Thread this!
    		GeoPoint inputLocation = geocode(input);
    		getPlaces(inputLocation, "Rail Station", "");
    		getPlaces(inputLocation, "Bus Stop", "");
    		setProgressBarIndeterminateVisibility(false);
    		
    		mPlaceName = input; //TODO: Make this something else...
    		mLatitude = inputLocation.getLatitudeE6();
    		mLongitude = inputLocation.getLongitudeE6();
    		mRadius = 5; //TODO: Actually set this to something
    		
    		ContentValues values = new ContentValues();
    		values.put(CommuteAlarm.Alarms.PLACE, mPlaceName);
    		values.put(CommuteAlarm.Alarms.LATITUDE, mLatitude);
    		values.put(CommuteAlarm.Alarms.LONGITUDE, mLongitude);
    		values.put(CommuteAlarm.Alarms.RADIUS, mRadius);
    		
    		getContentResolver().update(mUri, values, null, null);

            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }
		
        setContentView(R.layout.alarm_details);
        
        mMap = (MapView) findViewById(R.id.mapview);
        mTitle = (TextView) findViewById(R.id.placename);
        SeekBar mAlarmRadius = (SeekBar) findViewById(R.id.radius_seekBar);

        mAlarmRadius.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress,
        			boolean fromUser) {
        		mRadius = progress;
        		
        	}

        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar) {
        		// TODO Auto-generated method stub
        		
        	}

        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar) {
        		// TODO Auto-generated method stub
        		
        	}
                	
                	
                });
        
        mCursor = managedQuery(mUri, null, null, null, null);
				
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		ContentValues values = new ContentValues();
		values.put(CommuteAlarm.Alarms.PLACE, mPlaceName);
		values.put(CommuteAlarm.Alarms.LATITUDE, mLatitude);
		values.put(CommuteAlarm.Alarms.LONGITUDE, mLongitude);
		values.put(CommuteAlarm.Alarms.RADIUS, mRadius);
		
		getContentResolver().update(mUri, values, null, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		
		
		if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();

            String placename = mCursor.getString(mCursor.getColumnIndex(CommuteAlarm.Alarms.PLACE));
            mTitle.setText(placename);
            setUpMap(new GeoPoint(mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.LATITUDE)), mCursor.getInt(mCursor.getColumnIndex(CommuteAlarm.Alarms.LONGITUDE))));

        } else {
            //something has gone wrong
        	Log.e(TAG, "Nothing found in the cursor!");
        }
	}
	
	private void setUpMap(GeoPoint geoPoint) {
		MapController controller = mMap.getController();
		controller.animateTo(geoPoint);
		controller.setZoom(10);
		
		AlarmOverlay alarmOverlay = new AlarmOverlay(getBaseContext(), geoPoint, mMap);
		alarmOverlay.setMeters(mRadius * 1000);
		
		MyLocationOverlay myLocation = new MyLocationOverlay(getBaseContext(), mMap);
		
		mMap.getOverlays().add(alarmOverlay);
		mMap.getOverlays().add(myLocation);
		
		myLocation.enableCompass();
		myLocation.enableMyLocation();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.alarms_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        	case R.id.delete:
        		deleteAlarm();
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
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
		Double lat = new Double(0);

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

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private final void deleteAlarm() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
        }
        finish();
    }

}
