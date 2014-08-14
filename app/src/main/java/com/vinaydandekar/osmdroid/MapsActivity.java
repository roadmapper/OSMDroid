package com.vinaydandekar.osmdroid;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.*;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class MapsActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    // Client and request objects to acquire location
    LocationClient locationClient;
    LocationRequest locationRequest;
    Location myLocation, lastLocation = null;
    boolean locationEnabled;

    private MapView mMapView; // The MapView on screen
    private IMapController mapController;

    // Server IPs, either local or using Amazon Web Services
    String awsIP = "ec2-54-81-0-35.compute-1.amazonaws.com";
    String localhostIP = "192.168.1.103:3000";
    String targetUrl = "";
    String ipAddr = localhostIP;

    // Layout objects for sliding up panel
    private SlidingUpPanelLayout slidingup_panel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        // Gets location information from the device
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationEnabled = false;
            Toast.makeText(getApplicationContext(), "Please enable location.", Toast.LENGTH_SHORT).show();
        }
        else {
            locationEnabled = true;
        }
        locationClient = new LocationClient(this, this, this);
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        lastLocation = locationClient.getLastLocation();
        if (lastLocation == null) {
            locationClient.requestLocationUpdates(locationRequest, this);
        }
        else {
            Toast.makeText(getApplicationContext(), "Location: " + lastLocation.getLatitude() + "," + lastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            myLocation = lastLocation;
            setUpMapIfNeeded();
        }
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected from Google Play Services", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Could not connect to Google Play Services. Please try again later.", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onLocationChanged(Location location) {
        locationClient.removeLocationUpdates(this);
        myLocation = location;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationEnabled && (lastLocation != null))
            setUpMapIfNeeded();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_centermap) {
            centerMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (slidingup_panel != null && slidingup_panel.isPanelExpanded() || slidingup_panel.isPanelAnchored()) {
            slidingup_panel.collapsePanel();
        } else {
            super.onBackPressed();
        }
    }

    private void centerMap() {
        GeoPoint gMyLocation = new GeoPoint(myLocation.getLatitude(), myLocation.getLongitude());
        mapController.setCenter(gMyLocation);
        mapController.animateTo(gMyLocation);
        mapController.setZoom(12);
    }

    /**
     * Allows the user the refresh the data manually if data is stale or if the application
     * is not able to connect to the flight server.
     */
    public void refresh() {
        locationClient.requestLocationUpdates(locationRequest, this);
        flightMarkerMap.clear();
        mMap.clear();

        setUpMap();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMapView == null) {
            // Try to obtain the map
            mMapView = (MapView) findViewById(R.id.mapview);
            mapController = mMapView.getController();
            // Check if we were successful in obtaining the map.
            if (mMapView != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p>
     * This should only be called once and when we are sure that {@link #mMapView} is not null.
     */
    private void setUpMap() {

        // Get the location and set it as the center of the map
        GeoPoint point = new GeoPoint(myLocation.getLatitude(), myLocation.getLongitude());
        mapController.setCenter(point);

        // Set attributes of the map
        mMapView.setUseDataConnection(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setAnimationCacheEnabled(true);
        mMapView.setClickable(true);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        // Add a listener for a single tap on the map to close any open InfoWindows
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Log.d("debug", "Single tap helper");
                InfoWindow.closeAllInfoWindowsOn(mMapView);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        // Add the listener as an overlay to the map
        MapEventsOverlay mOverlayEvents = new MapEventsOverlay(getApplication().getBaseContext(), mReceive);
        mMapView.getOverlays().add(mOverlayEvents);

        // Create a marker for current location
        Marker startMarker = new Marker(mMapView);
        startMarker.setPosition(new GeoPoint(myLocation.getLatitude(), myLocation.getLongitude()));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(getResources().getDrawable(R.drawable.person));
        mMapView.getOverlays().add(startMarker);
        mMapView.invalidate();

        if (myLocation != null) {
            setTargetUrl(myLocation, ipAddr);
        }

        GetFlightsTask t = new GetFlightsTask();
        Toast.makeText(getApplicationContext(), "Connecting to flight server...", Toast.LENGTH_SHORT).show();
        t.execute(targetUrl);
    }

    /**
     * Once location is acquired, the coordinates are put in as parameters in the URL to connect
     * to the flight data server
     */
    private void setTargetUrl(Location l, String ip) {
        targetUrl = "http://" + ip + "/api/v1/flights/find_lat_long?lat=" + l.getLatitude() +"&long=" + l.getLongitude() +"&quantity=40";
    }

    private String getToken() {
        return "5b8f8af47320255762b748f45d34399a"; // token on localhost
        //return "212d9ca7e9090a71925d158646130ab4"; // token on AWS
    }

    /**
     * Acquire additional information about flight, connects to flightradar24.com
     */
    class GetFlightInfoTask extends AsyncTask<String, Void, HashMap<String, String>> {


        @Override
        protected HashMap<String, String> doInBackground(String... params) {
            String targetUrl = "http://krk.fr24.com/_external/planedata_json.1.3.php?f=" + params[0];
            HttpGet get = new HttpGet(targetUrl);
            HttpClient client = new DefaultHttpClient();
            HashMap<String, String> map = new HashMap<String, String>();

            try {
                HttpResponse response = client.execute(get);
                String result = EntityUtils.toString(response.getEntity());
                JSONObject j = new JSONObject(result);
                Iterator keys = j.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    map.put(key, j.getString(key));
                }
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }


        }

        @Override
        protected void onPostExecute(HashMap<String, String> map) {
            TextView originName = (TextView) findViewById(R.id.origin_airport_name);
            TextView destinationName = (TextView) findViewById(R.id.destination_airport_name);
            TextView aircraft = (TextView) findViewById(R.id.aircraft);
            TextView airline = (TextView) findViewById(R.id.airline);

            if (map.keySet().contains("from_city"))
                originName.setText(map.get("from_city").split(", ")[1]);
            else
                originName.setText("Unknown");
            if (map.keySet().contains("to_city"))
                destinationName.setText(map.get("to_city").split(", ")[1]);
            else
                destinationName.setText("Unknown");
            if (map.keySet().contains("aircraft"))
                aircraft.setText(map.get("aircraft"));
            else
                aircraft.setText("Unknown");
            if (map.keySet().contains("airline"))
                airline.setText(map.get("airline"));
            else
                airline.setText("Unknown");

        }

    }

    class GetFlightsTask extends AsyncTask<String, Void, ArrayList<Flight>> {

        //private Exception exception;
        private ArrayList<Flight> flights = new ArrayList<Flight>();

        @Override
        protected ArrayList<Flight> doInBackground(String... params) {

            String targetUrl = params[0];
            //String result = "";
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(targetUrl);
            get.setHeader("Authorization", "Token token=" + getToken());
            System.out.println(get.getFirstHeader("Authorization").toString());
            System.out.println(get.toString());

            try{
                HttpResponse response = client.execute(get);
                String result = EntityUtils.toString(response.getEntity());
                JSONArray ja = new JSONArray(result);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject j = ja.getJSONObject(i);
                    String flight_num = j.getString("ICAOflightnum");
                    if (!flight_num.equals("")) {
                        Flight f = new Flight();
                        if (!j.getString("IATAflightnum").equals(""))
                            f.setFlightNum(j.getString("IATAflightnum") + " / " +flight_num);
                        else
                            f.setFlightNum(flight_num);
                        f.setOrigin(j.getString("origin"));
                        f.setDestination(j.getString("destination"));
                        f.setAircraft(j.getString("aircraft"));
                        f.setLatitude(j.getJSONArray("location").getDouble(0));
                        f.setLongitude(j.getJSONArray("location").getDouble(1));
                        f.setTrack(j.getInt("aircraft_track"));
                        f.setRegistration(j.getString("registration"));
                        f.setSpeed(j.getInt("speed_kts"));
                        f.setAltitude(j.getInt("altitude_ft"));
                        f.setFlight_id(j.getString("flight_id"));
                        flights.add(f);
                    }
                }
                System.out.println(ja.toString());
                System.out.println(response.getStatusLine().getStatusCode());
                for (Flight f : flights) {
                    System.out.print(f.toString());
                }
                return flights;

            } catch (Exception e){

                System.out.println(e.toString());
                return null;
            }

        }

        @Override
        protected void onPostExecute(ArrayList<Flight> flights) {
            if (flights == null)
                Toast.makeText(getApplicationContext(), "Failed to connect to flight server.", Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(getApplicationContext(), "Found " + flights.size() + " flights near your location.", Toast.LENGTH_SHORT).show();
                for (Flight f : flights) {

                    // Create a marker
                    Marker startMarker = new Marker(mMapView);
                    startMarker.setPosition(new GeoPoint (f.getLatitude(), f.getLongitude()));
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    startMarker.setIcon(getResources().getDrawable(R.drawable.person));
                    startMarker.setTitle(f.getFlightNum());
                    startMarker.setSnippet(f.getAircraft());
                    startMarker.setRelatedObject(f);
                    MarkerInfoWindow window = new MarkerInfoWindow(R.layout.bonuspack_bubble_black, mMapView);
                    window.setOnMarkerInfoWindowTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            Log.d("debug", "Touched InfoWindow");
                            return true;
                        }
                    });
                    startMarker.setInfoWindow(window);
                    mMapView.getOverlays().add(startMarker);
                    mMapView.invalidate();

                    com.google.android.gms.maps.model.Marker m = mMapView.addMarker(new MarkerOptions().position(new LatLng(f.getLatitude(), f.getLongitude())).title(f.getFlightNum()).snippet(f.getAircraft()).icon(BitmapDescriptorFactory.fromBitmap(newImage)));
                    m.setRotation(f.getTrack()-180);
                    flightMarkerMap.put(m, f);

                    /*CameraPosition cameraPosition = new CameraPosition.Builder().target(
                            new LatLng(myLocation.getLatitude(), myLocation.getLongitude())).zoom(8).build();*/

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(com.google.android.gms.maps.model.Marker marker) {
                            Flight f = flightMarkerMap.get(marker);
                            TextView flight_num = (TextView) findViewById(R.id.flight_num);
                            TextView origin = (TextView) findViewById(R.id.origin_IATA);
                            TextView destination = (TextView) findViewById(R.id.destination_IATA);
                            TextView reg = (TextView) findViewById(R.id.registration);
                            TextView speed = (TextView) findViewById(R.id.speed);
                            TextView alt = (TextView) findViewById(R.id.altitude);
                            TextView originName = (TextView) findViewById(R.id.origin_airport_name);
                            TextView destinationName = (TextView) findViewById(R.id.destination_airport_name);
                            TextView aircraft = (TextView) findViewById(R.id.aircraft);
                            TextView airline = (TextView) findViewById(R.id.airline);

                            flight_num.setText(f.getFlightNum());
                            origin.setText(f.getOrigin());
                            destination.setText(f.getDestination());
                            if (f.getRegistration().equals("")) {
                                reg.setText("No registration");
                            } else {
                                reg.setText(f.getRegistration());
                            }
                            speed.setText(f.getSpeed() + " kts");
                            alt.setText(String.format("%,8d",f.getAltitude()) + " ft");

                            originName.setText("Loading...");
                            destinationName.setText("Loading...");
                            aircraft.setText("Loading...");
                            airline.setText("Loading...");
                            GetFlightInfoTask infoTask = new GetFlightInfoTask();
                            infoTask.execute(f.getFlight_id());

                            slidingup_panel.expandPanel();
                        }
                    });
                }
            }
        }

    /**
     * Acquire additional information about flight, connects to flightradar24.com
     */
    class GetFlightInfoTask extends AsyncTask<String, Void, HashMap<String, String>> {


        @Override
        protected HashMap<String, String> doInBackground(String... params) {
            String targetUrl = "http://krk.fr24.com/_external/planedata_json.1.3.php?f=" + params[0];
            HttpGet get = new HttpGet(targetUrl);
            HttpClient client = new DefaultHttpClient();
            HashMap<String, String> map = new HashMap<String, String>();

            try {
                HttpResponse response = client.execute(get);
                String result = EntityUtils.toString(response.getEntity());
                JSONObject j = new JSONObject(result);
                Iterator keys = j.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    map.put(key, j.getString(key));
                }
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, String> map) {
            TextView originName = (TextView) findViewById(R.id.origin_airport_name);
            TextView destinationName = (TextView) findViewById(R.id.destination_airport_name);
            TextView aircraft = (TextView) findViewById(R.id.aircraft);
            TextView airline = (TextView) findViewById(R.id.airline);

            if (map.keySet().contains("from_city"))
                originName.setText(map.get("from_city").split(", ")[1]);
            else
                originName.setText("Unknown");
            if (map.keySet().contains("to_city"))
                destinationName.setText(map.get("to_city").split(", ")[1]);
            else
                destinationName.setText("Unknown");
            if (map.keySet().contains("aircraft"))
                aircraft.setText(map.get("aircraft"));
            else
                aircraft.setText("Unknown");
            if (map.keySet().contains("airline"))
                airline.setText(map.get("airline"));
            else
                airline.setText("Unknown");
        }

    }

}
}

