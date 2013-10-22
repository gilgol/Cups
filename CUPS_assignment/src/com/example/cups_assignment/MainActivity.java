package com.example.cups_assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends Activity {
	private List<HashMap<String, String>> coffeeList;

	private ListView listView;
	private MyListAdapter adapter;

	private AsyncHttpClient client;

	private double currLat, currLng;

	private final String KEY_NAME = "name";
	private final String KEY_ADDRESS = "address";
	private final String KEY_DISTANCE = "distance";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//if you want to lock screen for always Portrait mode    
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  

		init();
	}

	private void init() {
		//initialize current latitude and longitude a
		getLocation();

		//Initialize variables
		coffeeList = new ArrayList<HashMap<String, String>>();

		listView = (ListView) findViewById(R.id.listView1);
		//httpClient initialization
		client = new AsyncHttpClient();
		client.get("http://cupstelaviv.com/venues", new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				//if you are here that means something came back from the GET request... 
				populateCoffeeList(response);
				//we got the coffeeList- lets populate the ListView
				pupulateListView();
			}
		});		
	}
	/**
	 * initialize current latitude and longitude using Location services
	 */
	private void getLocation() {
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, new LocationListener() {
			public void onLocationChanged(Location location) {
				currLng = location.getLongitude();
				currLat = location.getLatitude();
			}

			@Override
			public void onProviderDisabled(String provider) {}
			@Override
			public void onProviderEnabled(String provider) {}
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		}
				);
	}

	/**
	 * parse the response and populate the coffeeList...
	 * @param response
	 */
	private void populateCoffeeList(String response) {
		try {
			//turn response into JSONObject (response is a JSON as string)
			JSONObject jsonObj = new JSONObject(response);

			//making a way to iterate all the JSONObects populating the response
			Iterator<?> iter = jsonObj.keys();
			while (iter.hasNext()) {

				String key = (String) iter.next();
				try {
					JSONObject value = (JSONObject) jsonObj.get(key);

					double lat, lng, distance;
					lat = Double.valueOf(value.getString("lat"));
					lng = Double.valueOf(value.getString("lng"));


					// euclidean distance from current location and the coffee...
					//eucleadDistance = Math.sqrt(Math.pow((lat - currLat) ,2) + Math.pow((lng - currLng),2));

					distance = distFrom(currLat, currLng, lat,lng);

					HashMap<String, String> oneCoffee = new HashMap<String, String>();
					oneCoffee.put(KEY_NAME, value.getString(KEY_NAME));
					oneCoffee.put(KEY_ADDRESS, value.getString(KEY_ADDRESS));
					oneCoffee.put(KEY_DISTANCE, String.valueOf(distance));

					coffeeList.add(oneCoffee);
				} catch (JSONException e) {
					// Something went wrong!
				}
			}//end of while
			sortListByDistance();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 	based on http://en.wikipedia.org/wiki/Haversine_formula 
	 * @param lat1 of current location 
	 * @param lng1 of current location
	 * @param lat2 of coffee location
	 * @param lng2 of coffee location
	 * @return the distance between you and the coffee in meters
	 */
	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
		double earthRadius = 3958.75;	//in miles...
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
				Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = earthRadius * c;

		int meterConversion = 1609;

		return dist * meterConversion;
	}

	/**
	 * sort the coffee list by distance
	 */
	private void sortListByDistance() {
		Collections.sort(coffeeList, new Comparator<HashMap<String, String>>() {
			@Override
			public int compare(HashMap<String, String> coffee1, HashMap<String, String> coffee2) {
				//negative if coffee1 is closer then coffee2, positive if coffee2 is closer
				Double d = Double.valueOf(coffee1.get(KEY_DISTANCE)) - Double.valueOf(coffee2.get(KEY_DISTANCE));


				if (d<0) return -1;
				else return 1;
			}
		});		
	}

	private void pupulateListView() {
		adapter = new MyListAdapter();
		listView.setAdapter(adapter);

		// make it possible to fast scroll over the list of coffees!
		listView.setFastScrollEnabled(true);
		registerItemClickCallBack();
	}
	
	private void registerItemClickCallBack() {
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View vieClicked,	int position, long id) {
				// get the sms
				HashMap<String, String> clickedCoffee = coffeeList.get(position);

				// rasing a toest
				String message = "Distance to the coffee in meters: " +clickedCoffee.get(KEY_DISTANCE);
				Toast.makeText(MainActivity.this, message,	Toast.LENGTH_SHORT).show();
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 *  Custom ListAdapter 
	 * @author Gil
	 *
	 */
	private class MyListAdapter extends ArrayAdapter<HashMap<String, String>> {
		public MyListAdapter() {
			super(MainActivity.this, R.layout.listview_item, coffeeList);
		}

		@Override
		public int getCount() {
			return coffeeList.size();
		}

		@Override
		public HashMap<String, String> getItem(int index) {
			return coffeeList.get(index);
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		};

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder mHolder = null;

			// Make sure we have a view to work with (may have been given null)
			View itemView = convertView;
			if (itemView == null) {
				mHolder = new ViewHolder();
				itemView = getLayoutInflater().inflate(R.layout.listview_item, parent, false);

				mHolder.nameView = (TextView) itemView.findViewById(R.id.name);
				mHolder.addressView = (TextView) itemView.findViewById(R.id.address);
				//mHolder.distanceView = (TextView) itemView.findViewById(R.id.distance);
				itemView.setTag(mHolder);
			}
			else {
				mHolder = (ViewHolder)itemView.getTag();
			}

			//get the current Coffee to work with
			HashMap<String, String> currentCoffee = getItem(position);

			if (currentCoffee != null) {
				mHolder.nameView.setText(currentCoffee.get(KEY_NAME));
				mHolder.addressView.setText(currentCoffee.get(KEY_ADDRESS));
				//mHolder.distanceView.setText(currentCoffee.get(KEY_DISTANCE));
			}
			return itemView;

		}

		private class ViewHolder {
			TextView nameView; 
			TextView addressView;
			//TextView distanceView;
		}
	}

}
