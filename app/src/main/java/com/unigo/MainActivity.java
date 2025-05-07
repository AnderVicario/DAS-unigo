package com.unigo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private MapView map;
    private Marker currentMarker;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteCalculator routeCalculator;
    private ExtendedFloatingActionButton fabCalculateRoute;

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
        fabCalculateRoute.setOnClickListener(v -> calculateRouteToDestination());

        configureBottomSheet();
    }


    private void configureBottomSheet() {
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        LinearLayout zoomControlsContainer = findViewById(R.id.zoom_controls_container);
        LinearLayout buttonContainer = findViewById(R.id.button_container);

        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Configuración básica
        int peekHeightPx = dpToPx(50);
        bottomSheetBehavior.setPeekHeight(peekHeightPx);
        bottomSheetBehavior.setHideable(false);

        // Distancia constante que deseas mantener entre los botones y el borde superior del BottomSheet
        final int CONSTANT_DISTANCE = dpToPx(20); // Ajusta este valor según necesites

        // Establecer que el contenido se ajuste completamente
        bottomSheetBehavior.setFitToContents(true);

        // Iniciar en estado colapsado
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Guardar el estado
        bottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);

        // Callback para el arrastre y para actualizar posición de botones
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.d("BottomSheet", "Completamente expandido");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.d("BottomSheet", "Colapsado");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.d("BottomSheet", "Arrastrando");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // La clave está aquí: queremos que los botones mantengan una distancia constante
                // respecto al borde superior del BottomSheet

                // Obtener la posición actual del borde superior del BottomSheet
                float bottomSheetTop = bottomSheet.getY();

                // Calcular la posición Y para los contenedores de botones
                // Siempre estarán a CONSTANT_DISTANCE por encima del borde superior
                float buttonY = bottomSheetTop - CONSTANT_DISTANCE - zoomControlsContainer.getHeight();

                // Aplicar la posición absoluta en lugar de una translación relativa
                zoomControlsContainer.setY(buttonY);
                buttonContainer.setY(buttonY);
            }
        });

        // Configurar clic en el handle para alternar estados
        findViewById(R.id.drag_handle).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
            }
        }));

        map.getOverlays().add(myLocationOverlay);
    }

    private void calculateRouteToDestination() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null && currentMarker != null) {
            GeoPoint myLocation = myLocationOverlay.getMyLocation();
            GeoPoint destination = currentMarker.getPosition();

            SnackbarUtils.showSuccess(findViewById(android.R.id.content), this, getString(R.string.calculating_route));
            routeCalculator.clearExistingRoute();

            routeCalculator.calculateRoute("foot", myLocation, destination, new RouteCalculator.RouteCallback() {
                @Override
                public void onRouteCalculated(final double distanceKm, final int durationMinutes) {
                    runOnUiThread(() -> {
                        String message = String.format("Distancia: %s\nTiempo estimado: %s",
                                RouteCalculator.formatDistance(distanceKm),
                                RouteCalculator.formatDuration(durationMinutes));
                        SnackbarUtils.showSuccess(findViewById(android.R.id.content), MainActivity.this,
                                getString(R.string.route_calculated) + "\n" + message);
                    });
                }

                @Override
                public void onRouteError(final String message) {
                    runOnUiThread(() -> SnackbarUtils.showError(findViewById(android.R.id.content), MainActivity.this, message));
                }
            });
        } else {
            SnackbarUtils.showError(findViewById(android.R.id.content), this,
                    myLocationOverlay == null || myLocationOverlay.getMyLocation() == null ?
                            getString(R.string.location_not_available) :
                            getString(R.string.no_destination_selected));
        }
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
        routeCalculator.clearExistingRoute();
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

        fabCalculateRoute.setVisibility(View.VISIBLE);
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
        window.setStatusBarColor(getResources().getColor(R.color.background));

        int densityDpi = getResources().getConfiguration().densityDpi;
        WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
        Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.statusBars());

        int statusBarHeight;
        switch (densityDpi) {
            case DisplayMetrics.DENSITY_HIGH: statusBarHeight = 38; break;
            case DisplayMetrics.DENSITY_MEDIUM: statusBarHeight = 25; break;
            case DisplayMetrics.DENSITY_LOW: statusBarHeight = 19; break;
            default: statusBarHeight = 25;
        }

        int topMarginInPx = (int) (statusBarHeight * getResources().getDisplayMetrics().density + 0.5f);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.topMargin = topMarginInPx;
        view.setLayoutParams(layoutParams);
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
