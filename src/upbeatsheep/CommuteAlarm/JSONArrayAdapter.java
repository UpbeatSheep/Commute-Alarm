package upbeatsheep.CommuteAlarm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class JSONArrayAdapter extends BaseAdapter {

	private JSONArray items;
	private LayoutInflater mInflater;
	
	public JSONArrayAdapter(Context context, JSONArray items) {
		super();
		this.items = items;
		 mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return items.length();
	}

	@Override
	public JSONObject getItem(int position) {
		try {
			return items.getJSONObject(position);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
	    View row = mInflater.inflate(android.R.layout.simple_list_item_1, null);

	    TextView name = (TextView) row.findViewById(android.R.id.text1);
	    try {
			name.setText(items.getJSONObject(position).getString("name"));
		} catch (JSONException e) {
			e.printStackTrace();
			name.setText("Error");
		}
	    
	    return row;
	}

}
