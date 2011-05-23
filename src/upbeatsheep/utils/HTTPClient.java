package upbeatsheep.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

public class HTTPClient {

	private static final String TAG = "UpbeatSheep";
	String auth = null;
	DefaultHttpClient httpClient;
	HttpGet httpGet = null;

	HttpPost httpPost = null;
	HttpContext localContext;
	HttpResponse response = null;

	//default image - this is downloaded if an image cannot be found
	final static String DEFAULT_IMAGE_URL = "http://senshot.com/files/2011/02/blueprint.jpg";

	Context mContext;

	private final HashMap<String, Bitmap> bitmapMap;

	private String ret;

	public HTTPClient(Context context) {
		HttpParams myParams = new BasicHttpParams();

		mContext = context;

		bitmapMap = new HashMap<String, Bitmap>();

		HttpConnectionParams.setConnectionTimeout(myParams, 100000);
		HttpConnectionParams.setSoTimeout(myParams, 100000);
		httpClient = new DefaultHttpClient(myParams);
		localContext = new BasicHttpContext();
	}

	public Bitmap fetchBitmap(String urlString) {
		if (bitmapMap.containsKey(urlString)) {
			return bitmapMap.get(urlString);
		}

		Log.d(TAG, "Downloading Image: " + urlString);

		try {
			InputStream is = fetch(urlString);
			Bitmap image = BitmapFactory.decodeStream(is);
			bitmapMap.put(urlString, image);
			return image;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void fetchBitmapOnThread(final String url, final ImageView imageView) {
		if (url != null) {
			if (bitmapMap.containsKey(url)) {
				Log.v(TAG, "Reading " + url + " from memory");
				imageView.setImageBitmap(bitmapMap.get(url));
			} else if (cacheExists(url)) {
				
				String hash = String.valueOf(url.hashCode());
				File file = new File(mContext.getCacheDir(),
						hash);
				if (!file.exists()){
					Log.v(TAG, "Reading " + url + " from internal cache");
					file = new File(mContext.getCacheDir(),
							hash);
				} else {
					Log.v(TAG, "Reading " + url + " from external cache");
				}
				Bitmap image = BitmapFactory.decodeFile(file.toString());
				bitmapMap.put(url, image);
				imageView.setImageBitmap(image);
			} else {
				Log.i(TAG, "Reading " + url + " from network");
				final Handler handler = new Handler() {

					@Override
					public void handleMessage(Message msg) {
						imageView.setImageBitmap((Bitmap) msg.obj);
					}
				};

				Thread thread = new Thread() {
					@Override
					public void run() {
						// TODO: need to set a pending image...
						Bitmap image = fetchBitmap(url);
						cacheDrawable(image, url);
						Message message = handler.obtainMessage(1, image);
						handler.sendMessage(message);
					}
				};
				thread.start();

			}
		} else {
			fetchBitmapOnThread(DEFAULT_IMAGE_URL, imageView);
		}
	}

	private void cacheDrawable(Bitmap image, String url) {
		if (image != null) {
			String hash = String.valueOf(url
					.hashCode());
			File localStorageDirectory = new File(
					mContext.getCacheDir(),hash);
			if(!localStorageDirectory.isDirectory()){
				localStorageDirectory = new File(
						mContext.getCacheDir(),hash);
			}
			try {
				image.compress(Bitmap.CompressFormat.PNG, 90,
						new FileOutputStream(localStorageDirectory));
				Log.i(TAG, "Saved to: " + localStorageDirectory);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private Boolean cacheExists(String url) {

		if (url != null) {
			String hash = String.valueOf(url.hashCode());
			File file = new File(mContext.getCacheDir(), hash);
			if (!file.exists()){
				file = new File(mContext.getCacheDir(), hash);
				return file.exists();
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private InputStream fetch(String urlString) throws MalformedURLException,
			IOException {
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}

	public void abort() {
		try {
			if (httpClient != null) {
				System.out.println("Abort.");
				httpPost.abort();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clearCookies() {
		httpClient.getCookieStore().clear();
	}

	public InputStream getHttpStream(String urlString) throws IOException {
		InputStream in = null;
		int response = -1;

		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();

		if (!(conn instanceof HttpURLConnection))
			throw new IOException("Not an HTTP connection");

		try {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			httpConn.connect();

			response = httpConn.getResponseCode();

			if (response == HttpURLConnection.HTTP_OK) {
				in = httpConn.getInputStream();
			}
		} catch (Exception e) {
			throw new IOException("Error connecting");
		}

		return in;
	}

	public JSONObject getJSON(String url) throws JSONException {
		return new JSONObject(get(url));
	}

	public String get(String url) {
		httpGet = new HttpGet(url);
		Log.d(TAG, "GET " + url);
		try {
			response = httpClient.execute(httpGet);
			return EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			e.printStackTrace();
			return "Error " + e.getCause();
		}
	}

	public String sendPost(String url, List<NameValuePair> data) {
		return sendPost(url, data, null);
	}

	public String sendPost(String url, List<NameValuePair> data,
			String contentType) {

		ret = null;

		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.RFC_2109);

		httpPost = new HttpPost(url);
		response = null;

		StringEntity tmp = null;

		Log.d("ACME", "Setting httpPost headers");

		httpPost.setHeader(
				"Accept",
				"text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");

		if (contentType != null) {
			httpPost.setHeader("Content-Type", contentType);
		} else {
			httpPost.setHeader("Content-Type",
					"application/x-www-form-urlencoded");
		}

		if (data != null) {
			try {
				tmp = new UrlEncodedFormEntity(data);
			} catch (UnsupportedEncodingException e) {
				Log.e("ACME", "HttpUtils : UnsupportedEncodingException : " + e);
			}

			httpPost.setEntity(tmp);
		}

		Log.d("ACME", url + "?" + data);

		try {
			response = httpClient.execute(httpPost, localContext);

			if (response != null) {
				ret = EntityUtils.toString(response.getEntity());
			}
		} catch (Exception e) {
			Log.e(TAG, "HttpUtils: " + e);
		}

		Log.d(TAG, "Returning value:" + ret);

		return ret;
	}
}
