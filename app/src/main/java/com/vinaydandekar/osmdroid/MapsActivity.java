package com.vinaydandekar.osmdroid;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

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


public class MapsActivity extends Activity{

    /*// Client and request objects to acquire location
    LocationClient locationClient;
    LocationRequest locationRequest;
    Location myLocation, lastLocation = null;
    boolean locationEnabled;*/

    private MapView mMapView; // The MapView on screen
    private IMapController mapController;
    GeoPoint myLocation = new GeoPoint(new GeoPoint(38.964126, -77.379001));


    // Server IPs, either local or using Amazon Web Services
    String awsIP = "ec2-54-81-0-35.compute-1.amazonaws.com";
    String localhostIP = "192.168.1.103:3000";
    String targetUrl = "";
    String ipAddr = localhostIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Get MapView
        mMapView = (MapView) findViewById(R.id.mapview);
        mapController = mMapView.getController();

        // Get the location and set it as the center of the map
        mapController.setCenter(myLocation);

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

        // Create a marker
        Marker startMarker = new Marker(mMapView);
        startMarker.setPosition(myLocation);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(getResources().getDrawable(R.drawable.person));
        startMarker.setTitle("Start point");
        startMarker.setSnippet("LOLWAT");
        startMarker.setSubDescription("WUT");
        startMarker.setRelatedObject("HUEHUEHUE");
        Log.d("debug", "startMarker.getRelatedObject()");
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
        centerMap();
        mMapView.invalidate();

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

    private void centerMap() {
        mapController.setCenter(myLocation);
        mapController.animateTo(myLocation);
        mapController.setZoom(12);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMapView == null) {
            // Try to obtain the map
            mMapView = (MapView) findViewById(R.id.mapview);
            // Check if we were successful in obtaining the map.
            if (mMapView != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        if (myLocation != null) {
            setTargetUrl(myLocation, ipAddr);
        }
    }

    private void setTargetUrl(GeoPoint l, String ip) {
        targetUrl = "http://" + ip + "/api/v1/flights/find_lat_long?lat=" + l.getLatitude() +"&long=" + l.getLongitude() +"&quantity=40";
    }
}
