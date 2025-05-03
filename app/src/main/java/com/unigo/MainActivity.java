package com.unigo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.unigo.utils.CustomInfoWindow;
import com.unigo.utils.SnackbarUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private MapView map;
    private Marker currentMarker;
    private MyLocationNewOverlay myLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        setContentView(R.layout.activity_main);

        // Configurar el botón
        ExtendedFloatingActionButton buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(v -> {
            if (currentMarker != null) {
                GeoPoint position = currentMarker.getPosition();
                // Formatear las coordenadas a 4 decimales
                String location = String.format("%.4f, %.4f", position.getLatitude(), position.getLongitude());
                Intent data = new Intent();
                data.putExtra("location", location);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        initializeMap();
        configureZoomControls();
        setupMapEvents();
        requestLocationPermission();
    }


    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            setupLocationOverlay();
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()),
                map
        );

        // Personalizar el icono de ubicación
        Drawable locationDrawable = ContextCompat.getDrawable(this, R.drawable.custom_location);
        if (locationDrawable != null) {
            Bitmap locationBitmap = drawableToBitmap(locationDrawable);
            myLocationOverlay.setPersonIcon(locationBitmap);
        }
        myLocationOverlay.setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                map.getController().setCenter(myLocationOverlay.getMyLocation());
                map.getController().setZoom(17.0);
            }
        }));
        map.getOverlays().add(myLocationOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationOverlay();
            } else {
                SnackbarUtils.showError(findViewById(android.R.id.content), this, getString(R.string.snackbar_warning_location_permission));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    private void addNewMarker(GeoPoint position) {
        currentMarker = new Marker(map);
        currentMarker.setPosition(position);

        // Personalizar el marcador
        currentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.custom_marker));
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle(getString(R.string.selected_marker));

        CustomInfoWindow infoWindow = new CustomInfoWindow(map);
        currentMarker.setInfoWindow(infoWindow);

        map.getOverlays().add(currentMarker);
        map.invalidate();
    }

    private void setupWindow() {
        EdgeToEdge.enable(this);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.onBackground));

        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            insetsController.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            );
        }
    }

    private void initializeMap() {
        Configuration.getInstance().load(getApplicationContext(), getPreferences(MODE_PRIVATE));
        map = findViewById(R.id.map);

        map.setTileSource(new XYTileSource("CartoVoyager", 0, 20, 512, ".png",
                new String[] { "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://c.basemaps.cartocdn.com/rastertiles/voyager/" }) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + "@2x.png";
            }
        });
        map.setMultiTouchControls(true);
        centerMapOnLocation(new GeoPoint(42.8467, -2.6731), 17.0);
    }

    private void centerMapOnLocation(GeoPoint point, double zoomLevel) {
        map.getController().setZoom(zoomLevel);
        map.getController().setCenter(point);
    }

    private void configureZoomControls() {
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        ImageButton btnZoomIn = findViewById(R.id.btnZoomIn);
        ImageButton btnZoomOut = findViewById(R.id.btnZoomOut);

        btnZoomIn.setOnClickListener(v -> map.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> map.getController().zoomOut());
    }

    private void setupMapEvents() {
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                handleMapTap(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        map.getOverlays().add(eventsOverlay);
    }

    private void handleMapTap(GeoPoint position) {
        removeExistingMarker();
        addNewMarker(position);
        showCoordinatesToast(position);

        ExtendedFloatingActionButton buttonSend = findViewById(R.id.button_send);
        buttonSend.setVisibility(View.VISIBLE);
    }

    private void removeExistingMarker() {
        if (currentMarker != null) {
            map.getOverlays().remove(currentMarker);
        }
    }

    @SuppressLint("DefaultLocale")
    private String createMarkerTitle(GeoPoint position) {
        return String.format("Lat: %.4f\nLon: %.4f",
                position.getLatitude(),
                position.getLongitude());
    }

    private void showCoordinatesToast(GeoPoint position) {
        SnackbarUtils.showSuccess(findViewById(android.R.id.content), this, getString(R.string.selected_marker) + createMarkerTitle(position));
    }
}