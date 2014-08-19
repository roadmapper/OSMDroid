package com.vinaydandekar.osmdroid;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.views.MapView;

/**
 * Created by Vinay Dandekar on 8/18/2014.
 */
public class MyInfoWindow extends MarkerInfoWindow {

    Flight mSelectedFlight;
    public MyInfoWindow(MapView mapView) {
        super(R.layout.bonuspack_bubble_black, mapView);
        getView().setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP){
                    Log.d("LOL", "haha");
                }
                return true;
            }
        });
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker)item;
        mSelectedFlight = (Flight) marker.getRelatedObject();

    }

    public void setOnMarkerInfoWindowTouchListener(View.OnTouchListener listener) {
        getView().setOnTouchListener(listener);
    }
}
