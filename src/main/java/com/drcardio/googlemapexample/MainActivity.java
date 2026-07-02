package com.drcardio.googlemapexample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);

        // Enable pinch-to-zoom
        map.setMultiTouchControls(true);

        // Get map controller
        MapController controller = (MapController) map.getController();

        // Set zoom level
        controller.setZoom(15.0);

        // Pune coordinates
        GeoPoint point = new GeoPoint(18.5204, 73.8567);

        // Center the map on Pune
        controller.setCenter(point);

        // Create marker
        Marker puneMarker = new Marker(map);
        puneMarker.setPosition(point);
        puneMarker.setTitle("Pune City");
        puneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Add marker to the map
        map.getOverlays().add(puneMarker);

        // Refresh the map
        map.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}