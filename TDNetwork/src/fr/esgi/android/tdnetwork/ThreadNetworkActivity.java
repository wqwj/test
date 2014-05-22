package fr.esgi.android.tdnetwork;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TD Network (ESGI 2013-2014)
 * @author odenier
 * 
 */
public class ThreadNetworkActivity extends Activity {
	private static final String TAG = "ThreadNetworkActivity";

	public static final String MY_WEBSITE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";;
	public static final String ESGI_LOGO = "http://openweathermap.org/img/w/";


	TextView sourceCode;
	static ImageView remoteImage;

	// Handler static to avoid memory leak
	static Handler sHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			if (msg != null) {
				// remoteImage must be static because into a static Handler !!
				remoteImage.setImageBitmap((Bitmap) msg.obj);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_thread_network);

		sourceCode = (TextView) findViewById(R.id.sourceCode);
		sourceCode.setMovementMethod(new ScrollingMovementMethod()); // allows vertical scroll on Textview

		remoteImage = (ImageView) findViewById(R.id.remoteImage);
	}

	/**
	 * Method to check whether my device has or not a network connection
	 * Need permission : android.permission.ACCESS_NETWORK_STATE
	 * @return True if device is connected to network and false else
	 */
	private boolean checkDeviceConnected() {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Need permission : android.permission.ACCESS_NETWORK_STATE
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		return (networkInfo != null && networkInfo.isConnected());
	}

	/**
	 * Method used to request Web Page
	 * @param view
	 */
	public void httpRequest(View view) {
		sourceCode.setText("");

		if (checkDeviceConnected()) {
			// Checks network and launchs http request only if device connected
			new DownloadUrlTask().execute(MY_WEBSITE_URL+"Rome");
		}
		else {
			// display error
			Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * AsyncTask to do asynchronous http request
	 * @author odenier
	 * 
	 */
	class DownloadUrlTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			String result = null;

			if (params != null && params.length > 0) {
				try {
					result = downloadUrl(params[0]);
				}
				catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}
			}
			return result;
		}

		private String downloadUrl(String myUrl) throws IOException {
			String contentAsString = null;
			HttpURLConnection conn = null;
			InputStream is = null;
			// Only display the first 1000 characters of the retrieved web page content.
			int len = 1000;

			try {
				URL url = new URL(myUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000 /* milliseconds */);
				conn.setConnectTimeout(15000 /* milliseconds */);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);

				// Starts the query
				conn.connect();
				int response = conn.getResponseCode();
				Log.d(TAG, "The response is: " + response);
				is = conn.getInputStream();

				// Convert the InputStream into a String
				contentAsString = readIt(is, len);
			}
			finally {
				// Makes sure that the InputStream is closed after the app is finished using it.
				if (is != null)
					is.close();
				if (conn != null)
					conn.disconnect();
			}

			return contentAsString;
		}

		/**
		 * Reads an InputStream and converts it to a String.
		 * @param stream
		 * @param len
		 * @return
		 * @throws IOException
		 * @throws UnsupportedEncodingException
		 */
		public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
			Reader reader = null;
			reader = new InputStreamReader(stream, "UTF-8");
			char[] buffer = new char[len];
			reader.read(buffer);
			reader.close();
			return new String(buffer);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			sourceCode.setText(result); // GOOD
		}
	}

	/**
	 * Method used to request a remote image from an URL
	 * @param view
	 */
	public void loadRemoteImage(View v) {
		remoteImage.setImageBitmap(null);

		new Thread(new Runnable() {
			public void run() {
				final Bitmap bitmap = loadRemoteImage(ESGI_LOGO);

				// CRASH android.view.ViewRoot$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
//				remoteImage.setImageBitmap(bitmap);

				// Solution 1 : GOOD
//				ThreadNetworkActivity.this.runOnUiThread(new Runnable() {
//					public void run() {
//						remoteImage.setImageBitmap(bitmap);
//					}
//				});

				// Solution 2 : GOOD
				Message msg = sHandler.obtainMessage();
				msg.obj = bitmap;
				sHandler.sendMessage(msg);

				// Solution 3 : GOOD
//				remoteImage.post(new Runnable() {
//					public void run() {
//						remoteImage.setImageBitmap(bitmap);
//					}
//				});
			}
		}).start();
	}

	/**
	 * Method used to get a Bitmap Image from an URL
	 * @param imageURL
	 *            image's URL
	 * @return A Bitmap Image
	 */
	private Bitmap loadRemoteImage(String imageURL) {
		Bitmap bitmap = null;

		if (checkDeviceConnected() && imageURL != null) {
			HttpURLConnection conn = null;
			InputStream is = null;

			try {
				URL myImageUrl = new URL(imageURL);
				conn = (HttpURLConnection) myImageUrl.openConnection();
				conn.connect();
				is = conn.getInputStream();

				bitmap = BitmapFactory.decodeStream(is);
			}
			catch (MalformedURLException e) {
				Log.e(TAG, e.getMessage());
			}
			catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			finally {
				try {
					if (is != null)
						is.close();
				}
				catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}
				if (conn != null)
					conn.disconnect();
			}
		}
		return bitmap;
	}
 
}
