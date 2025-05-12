package com.unigo.ui;

import static com.unigo.utils.RouteCalculator.PROFILE_PORT_MAP;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.unigo.R;
import com.unigo.adapters.TransportAdapter;
import com.unigo.models.NearStopResponse;
import com.unigo.models.Transport;
import com.unigo.utils.BusRoutesAPI;
import com.unigo.utils.CustomInfoWindow;
import com.unigo.utils.RouteCalculator;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapActivity";
    private final GeoPoint FIXED_DESTINATION = new GeoPoint(42.83953288884712, -2.6703476886917477);

    private MapView map;
    private Marker currentMarker;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteCalculator routeCalculator;
    private ExtendedFloatingActionButton fabCalculateRoute;

    private List<Transport> transportOptions = new ArrayList<>();
    private TransportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupWindow();

        ImageView ivLogo = findViewById(R.id.iv_logo);
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            initializeMap("dark");
            ivLogo.setImageResource(R.drawable.logo_dark);
        } else {
            initializeMap("light");
            ivLogo.setImageResource(R.drawable.logo_light);
        }

        configureZoomControls();
        setupMapEvents();
        requestLocationPermission();

        routeCalculator = new RouteCalculator(map);
        fabCalculateRoute = findViewById(R.id.fab_calculate_route);
        fabCalculateRoute.setVisibility(View.GONE);
        /*fabCalculateRoute.setOnClickListener(v -> calculateRouteToDestination());*/

        configureBottomSheet();
        configureRecyclerView();
        calculateAllRoutes(FIXED_DESTINATION);
    }

    private void configureRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.route_options);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransportAdapter(this, transportOptions);
        recyclerView.setAdapter(adapter);

        adapter.setOnTransportClickListener(transport -> {
            routeCalculator.clearExistingRoute();
            routeCalculator.drawRoute(transport.getRoutePoints());
            SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content),
                    this,
                    "Ruta: " + transport.getMode() + "\n" +
                            transport.getFormattedDistance() + " - " +
                            transport.getFormattedDuration()
            );
        });
    }

    private void calculateAllRoutes(GeoPoint destination) {
        if (myLocationOverlay == null || myLocationOverlay.getMyLocation() == null) return;

        transportOptions.clear();
        GeoPoint start = myLocationOverlay.getMyLocation();

        for (String profile : PROFILE_PORT_MAP.keySet()) {
            routeCalculator.calculateRoute(profile, start, destination, new RouteCalculator.RouteCallback() {
                @Override
                public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                    String modeName = "";
                    switch(profile) {
                        case "foot": modeName = "A pie"; break;
                        case "car": modeName = "Autobús"; break;
                        case "bike": modeName = "Bicicleta"; break;
                    }

                    transportOptions.add(new Transport(
                            modeName,
                            distanceKm,
                            durationMinutes,
                            points
                    ));

                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                }

                @Override
                public void onRouteError(String message) {
                    Log.e(TAG, "Error en perfil " + profile + ": " + message);
                }
            });
        }
    }


    private void configureBottomSheet() {
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        LinearLayout zoomControlsContainer = findViewById(R.id.zoom_controls_container);
        LinearLayout buttonContainer = findViewById(R.id.button_container);

        BottomSheetBehavior<LinearLayout> behavior = BottomSheetBehavior.from(bottomSheet);

        int peekHeightPx = dpToPx(50);
        behavior.setPeekHeight(peekHeightPx);
        behavior.setHideable(false);
        behavior.setFitToContents(true);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);

        // Callback para actualizar durante el drag
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bs, int newState) { /* ... */ }

            @Override
            public void onSlide(@NonNull View bs, float slideOffset) {
                int height = bs.getHeight();
                int baseOffset = dpToPx(70);
                float translationY = -baseOffset - (slideOffset * (height - peekHeightPx));
                zoomControlsContainer.setTranslationY(translationY);
                buttonContainer.setTranslationY(translationY);
            }
        });

        // Listener para saber cuándo bottomSheet ya tiene tamaño
        bottomSheet.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Una vez medido, aplicamos la posición inicial (slideOffset = 0)
                        int height = bottomSheet.getHeight();
                        int baseOffset = dpToPx(70);
                        float translationY = -baseOffset; // -baseOffset - (0 * ...)
                        zoomControlsContainer.setTranslationY(translationY);
                        buttonContainer.setTranslationY(translationY);

                        // Y eliminamos el listener para no repetir
                        bottomSheet.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
        );

        // Click sobre el handle para alternar estados
        findViewById(R.id.drag_handle).setOnClickListener(v -> {
            if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    // Método de utilidad para convertir dp a píxeles
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()),
                map
        );

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

                // Recalcular rutas cuando se obtiene la ubicación
                calculateAllRoutes(FIXED_DESTINATION);

                // Se ejecuta en un hilo porque las operaciones de red no pueden ir en el hilo principal
                BusRoutesAPI api = new BusRoutesAPI();
                new Thread(() -> {
                    try {
                        NearStopResponse res = api.findNearStop(myLocationOverlay.getMyLocation().getLatitude(), myLocationOverlay.getMyLocation().getLongitude(), 300);
                        Log.i(TAG, "Parada: " + res.stop_name);
                        Log.i(TAG, "Distancia: " + res.distance_m + "m");
                        Log.i(TAG, "Rutas: " + String.join(", ", res.routes));
                        Log.i(TAG, "¿Universidad?: " + res.is_university_route);
                    } catch (IOException e) {
                        Log.e(TAG, "Error al consultar la API", e);
                    }
                }).start();
            }
        }));

        map.getOverlays().add(myLocationOverlay);
    }

    private void initializeMap(String mode) {
        Configuration.getInstance().load(getApplicationContext(), getPreferences(MODE_PRIVATE));
        map = findViewById(R.id.map);

        if (Objects.equals(mode, "light")) {
            map.setTileSource(new XYTileSource("CartoVoyager", 0, 20, 512, ".png",
                    new String[] {
                            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://c.basemaps.cartocdn.com/rastertiles/voyager/" }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                            MapTileIndex.getX(pMapTileIndex) + "/" +
                            MapTileIndex.getY(pMapTileIndex) + "@2x.png";
                }
            });
        } else {
            map.setTileSource(new XYTileSource("CartoDark", 0, 20, 512, ".png",
                    new String[] {
                            "https://a.basemaps.cartocdn.com/dark_all/",
                            "https://b.basemaps.cartocdn.com/dark_all/",
                            "https://c.basemaps.cartocdn.com/dark_all/" }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                            MapTileIndex.getX(pMapTileIndex) + "/" +
                            MapTileIndex.getY(pMapTileIndex) + "@2x.png";
                }
            });
        }

        map.setMultiTouchControls(true);
        centerMapOnLocation(new GeoPoint(42.853065, -2.673206), 17.0); // Gazteiz
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
        /*routeCalculator.clearExistingRoute();*/
        addNewMarker(position);
    }

    private void addNewMarker(GeoPoint position) {
        routeCalculator.clearExistingRoute();
        removeExistingMarker();

        currentMarker = new Marker(map);
        currentMarker.setPosition(position);
        currentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.custom_marker));
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle(getString(R.string.selected_marker));

        CustomInfoWindow infoWindow = new CustomInfoWindow(map);
        currentMarker.setInfoWindow(infoWindow);

        map.getOverlays().add(currentMarker);
        map.invalidate();

        /*fabCalculateRoute.setVisibility(View.VISIBLE);*/
    }

    private void removeExistingMarker() {
        if (currentMarker != null) {
            map.getOverlays().remove(currentMarker);
            currentMarker = null;
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

    private void setupWindow() {
        EdgeToEdge.enable(this);
        View view = findViewById(R.id.main);
        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.background, getTheme()));
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // padding top para evitar que el contenido esté debajo del status bar
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationOverlay();
            } else {
                SnackbarUtils.showError(findViewById(android.R.id.content), this,
                        getString(R.string.snackbar_warning_location_permission));
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private String createMarkerTitle(GeoPoint position) {
        return String.format("Lat: %.4f\nLon: %.4f",
                position.getLatitude(), position.getLongitude());
    }

    private void showCoordinatesToast(GeoPoint position) {
        SnackbarUtils.showSuccess(findViewById(android.R.id.content), this,
                getString(R.string.selected_marker) + createMarkerTitle(position));
    }
}
