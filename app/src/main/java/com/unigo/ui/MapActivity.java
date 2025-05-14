package com.unigo.ui;

import static com.unigo.utils.RouteCalculator.PROFILE_PORT_MAP;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.unigo.R;
import com.unigo.adapters.TransportAdapter;
import com.unigo.models.api.GeoJsonLibrary;
import com.unigo.models.api.GeoJsonParking;
import com.unigo.models.api.GeoJsonStop;
import com.unigo.models.api.NearStopResponse;
import com.unigo.models.Transport;
import com.unigo.utils.APIService;
import com.unigo.utils.CustomInfoWindow;
import com.unigo.utils.LocaleHelper;
import com.unigo.utils.MarkerType;
import com.unigo.utils.RouteCalculator;
import com.unigo.utils.SnackbarUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapActivity";
    private final GeoPoint FIXED_DESTINATION = new GeoPoint(42.83953288884712, -2.6703476886917477);

    private MapView map;
    private Marker currentMarker;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteCalculator routeCalculator;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private List<Transport> transportOptions = new ArrayList<>();
    private TransportAdapter adapter;

    private boolean inDetailedMode = false;
    private boolean isAnimatingToMyLocation = false;

    private boolean showBusStops = false;
    private List<GeoJsonStop.Feature> cachedBusStops = new ArrayList<>();
    private BusStopsOverlay busStopsOverlay;

    private boolean showBikeParkings = false;
    private List<GeoJsonParking.Feature> cachedBikeParkings = new ArrayList<>();
    private BikeParkingsOverlay bikeParkingsOverlay;

    private boolean showLibraries = false;
    private List<GeoJsonLibrary.Feature> cachedLibraries = new ArrayList<>();
    private LibrariesOverlay librariesOverlay;

    // --------------------
    // Ciclo de vida
    // --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        // Configuración de UI
        setupWindow();
        configureLogo();
        configureControls();
        configureNavigation();
        configureZoom();
        configureBottomSheet();
        configureRecyclerView();
        configureHorizontalScrollView();
        View header = navigationView.getHeaderView(0);
        TextView tvTemp      = header.findViewById(R.id.nav_temp);
        TextView tvHumidity  = header.findViewById(R.id.nav_humidity);

        fetchWeather(tvTemp, tvHumidity);

        // Permisos
        requestLocationPermission();

        // Configuración de mapa
        setupMapEvents();
        loadBusStops();
        loadBikeParkings();
        loadLibraries();
        routeCalculator = new RouteCalculator(map, this);
        calculateAllRoutes(FIXED_DESTINATION);

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

    // --------------------
    // Configuración de UI
    // --------------------

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

    private void configureLogo() {
        ImageView ivLogo = findViewById(R.id.iv_logo);
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            initializeMap("dark");
            ivLogo.setImageResource(R.drawable.logo_dark);
        } else {
            initializeMap("light");
            ivLogo.setImageResource(R.drawable.logo_light);
        }
    }

    private void configureControls() {
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        ImageButton btnZoomIn = findViewById(R.id.btnZoomIn);
        ImageButton btnZoomOut = findViewById(R.id.btnZoomOut);
        ImageButton btnMyLocation = findViewById(R.id.btnMyLocation);

        btnZoomIn.setOnClickListener(v -> map.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> map.getController().zoomOut());
        btnMyLocation.setOnClickListener(v -> {
            // Evitar múltiples clics rápidos
            if (isAnimatingToMyLocation) {
                return;
            }

            GeoPoint myLoc = (myLocationOverlay != null) ? myLocationOverlay.getMyLocation() : null;
            if (myLoc != null) {
                isAnimatingToMyLocation = true;

                double targetZoom = 17.0;
                double targetZoomOut = 11.0;
                double currentZoom = map.getZoomLevelDouble();

                // Verificar si es necesario hacer zoom out primero
                if (currentZoom > targetZoom) {
                    // Zoom out
                    map.getController().animateTo(map.getMapCenter(), targetZoomOut, 600L);

                    // Luego centrar la cámara en la ubicación deseada con el zoom final
                    new Handler().postDelayed(() -> {
                        map.getController().animateTo(myLoc, targetZoom, 1200L);
                        isAnimatingToMyLocation = false;
                    }, 600);  // Tiempo de espera para que se haga el zoom out y empiece la animación de movimiento
                } else {
                    // Si no es necesario el zoom out, centramos directamente
                    map.getController().animateTo(myLoc, targetZoom, 1200L);
                    isAnimatingToMyLocation = false;
                }
            } else {
                SnackbarUtils.showError(
                        findViewById(android.R.id.content),
                        this,
                        getString(R.string.snackbar_warning_location_permission)
                );
            }
        });
    }

    private void configureZoom() {
        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                double currentZoom = event.getZoomLevel();
                if (showBusStops) updateVisualizationModeForBus(currentZoom);
                if (showBikeParkings) updateVisualizationModeForBike(currentZoom);
                if (showLibraries) updateVisualizationModeForLibraries(currentZoom);
                return true;
            }
        });
    }

    private void configureHorizontalScrollView() {
        MaterialButton toggleBusButton = findViewById(R.id.toggle_bus);
        toggleBusButton.setOnClickListener(v -> {
            showBusStops = !showBusStops; // Invertir estado
            updateBusToggleButtonAppearance(); // Actualizar apariencia
            refreshMapOverlays(); // Actualizar overlays del mapa
            if (showBusStops) {
                updateVisualizationModeForBus(map.getZoomLevelDouble()); // Verificar zoom actual
            }
        });

        MaterialButton toggleBikeButton = findViewById(R.id.toggle_bike);
        toggleBikeButton.setOnClickListener(v -> {
            showBikeParkings = !showBikeParkings;
            updateBikeToggleButtonAppearance();
            refreshMapOverlays();
            if (showBikeParkings) {
                updateVisualizationModeForBike(map.getZoomLevelDouble());
            }
        });

        MaterialButton toggleLibraryButton = findViewById(R.id.toggle_library);
        toggleLibraryButton.setOnClickListener(v -> {
            showLibraries = !showLibraries;
            updateLibraryToggleButtonAppearance();
            refreshMapOverlays();
            if (showLibraries) {
                updateVisualizationModeForLibraries(map.getZoomLevelDouble());
            }
        });
    }

    private void updateBusToggleButtonAppearance() {
        MaterialButton toggleBusButton = findViewById(R.id.toggle_bus);
        int backgroundRes = showBusStops ? R.drawable.round_button : R.drawable.round_button;
        int textColor = ContextCompat.getColor(this, showBusStops ? R.color.primary : R.color.onBackground);

        toggleBusButton.setBackgroundResource(backgroundRes);
        toggleBusButton.setTextColor(textColor);
    }

    private void updateBikeToggleButtonAppearance() {
        MaterialButton toggleBikeButton = findViewById(R.id.toggle_bike);
        int backgroundRes = showBikeParkings ? R.drawable.round_button : R.drawable.round_button;
        int textColor = ContextCompat.getColor(this, showBikeParkings ? R.color.primary : R.color.onBackground);
        toggleBikeButton.setBackgroundResource(backgroundRes);
        toggleBikeButton.setTextColor(textColor);
    }

    private void configureBottomSheet() {
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        LinearLayout zoomControlsContainer = findViewById(R.id.zoom_controls_container);

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

    private void configureNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Configurar el botón de hamburguesa para abrir el drawer
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_idioma) {
                dialogoIdioma();
            }
            // Cerrar el drawer tras la selección
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }

    // --------------------
    // Configuración del MAPA
    // --------------------

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
        centerMapOnLocation(FIXED_DESTINATION, 17.0); // Gazteiz
    }

    private void centerMapOnLocation(GeoPoint point, double zoomLevel) {
        map.getController().setZoom(zoomLevel);
        map.getController().setCenter(point);
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

    // -------------------
    // Permisos
    // -------------------

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

    // ----------------------
    // Funcionalidades principales
    // ----------------------

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()),
                map
        );

        Drawable arrowDrawable = ContextCompat.getDrawable(this, R.drawable.navigation);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(ContextCompat.getColor(this, R.color.onPrimary), PorterDuff.Mode.SRC_IN);
            Bitmap arrowBitmap = drawableToBitmap(arrowDrawable);

            // Asignar la flecha de movimiento al overlay
            myLocationOverlay.setDirectionArrow(
                    drawableToBitmap(ContextCompat.getDrawable(this, R.drawable.custom_location)),
                    arrowBitmap
            );
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
            }
        }));

        map.getOverlays().add(myLocationOverlay);
    }

    private void addNewMarker(GeoPoint position) {
        routeCalculator.clearExistingRoute();
        removeExistingMarker();

        currentMarker = new Marker(map);
        currentMarker.setPosition(position);
        currentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.custom_marker));
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle(getString(R.string.selected_marker));

        CustomInfoWindow infoWindow = new CustomInfoWindow(map, MarkerType.DEFAULT);
        currentMarker.setInfoWindow(infoWindow);

        map.getOverlays().add(currentMarker);
        map.invalidate();
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

    private void refreshMapOverlays() {
        map.getOverlays().removeIf(overlay -> {
            // Eliminar overlays según estado de los toggles
            if (!showBusStops && (overlay instanceof BusStopsOverlay || isBusStopMarker(overlay))) return true;
            if (!showBikeParkings && (overlay instanceof BikeParkingsOverlay || isBikeParkingMarker(overlay))) return true;
            if (!showLibraries && (overlay instanceof LibrariesOverlay || isLibraryMarker(overlay))) return true;

            // Lógica de detalle para buses
            if (showBusStops && overlay instanceof BusStopsOverlay) return inDetailedMode;
            if (showBusStops && isBusStopMarker(overlay)) return !inDetailedMode;

            // Lógica de detalle para parkings
            if (showBikeParkings && overlay instanceof BikeParkingsOverlay) return inDetailedMode;
            if (showBikeParkings && isBikeParkingMarker(overlay)) return !inDetailedMode;

            // Lógica de detalle para librerías
            if (showLibraries && overlay instanceof LibrariesOverlay) return inDetailedMode;
            if (showLibraries && isLibraryMarker(overlay)) return !inDetailedMode;

            return false;
        });

        // Añadir overlays activos
        if (showBusStops) {
            if (inDetailedMode) addDetailedMarkers();
            else if (busStopsOverlay != null) map.getOverlays().add(busStopsOverlay);
        }

        if (showBikeParkings) {
            if (inDetailedMode) addDetailedBikeMarkers();
            else if (bikeParkingsOverlay != null) map.getOverlays().add(bikeParkingsOverlay);
        }

        if (showLibraries) {
            if (inDetailedMode) addDetailedLibraryMarkers();
            else if (librariesOverlay != null) map.getOverlays().add(librariesOverlay);
        }

        map.invalidate();
    }

    // ----------------------
    // Calculo de Rutas
    // ----------------------

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

    // ----------------------
    // Paradas de autobús
    // ----------------------

    private void loadBusStops() {
        if (!cachedBusStops.isEmpty()) {
            if (showBusStops) {
                updateVisualizationModeForBus(map.getZoomLevelDouble());
            }
            return;
        }

        new Thread(() -> {
            APIService api = new APIService();
            try {
                GeoJsonStop stops = api.getAllStops();
                if (stops != null && stops.features != null) {
                    cachedBusStops = stops.features;

                    List<GeoPoint> points = new ArrayList<>(stops.features.size());
                    for (GeoJsonStop.Feature feature : stops.features) {
                        if (feature.geometry != null &&
                                feature.geometry.coordinates != null &&
                                feature.geometry.coordinates.size() >= 2) {

                            List<Double> coords = feature.geometry.coordinates;
                            points.add(new GeoPoint(coords.get(1), coords.get(0)));
                        }
                    }

                    runOnUiThread(() -> {
                        busStopsOverlay = new BusStopsOverlay(points);
                        updateVisualizationModeForBus(map.getZoomLevelDouble());
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error cargando paradas", e);
            }
        }).start();
    }

    private void addBusStopMarker(GeoJsonStop.Feature feature) {
        if (feature.geometry == null ||
                feature.geometry.coordinates == null ||
                feature.geometry.coordinates.size() < 2) {
            return;
        }

        List<Double> coordinates = feature.geometry.coordinates;
        GeoPoint point = new GeoPoint(coordinates.get(1), coordinates.get(0));

        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(ContextCompat.getString(this, R.string.bus_stop));
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.bus_stop_marker));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setRelatedObject(feature);
        marker.setInfoWindow(new CustomInfoWindow(map, MarkerType.BUS_STOP));
        map.getOverlays().add(marker);
    }

    private class BusStopsOverlay extends Overlay {
        private final List<GeoPoint> stops;
        private final Bitmap pointBitmap;
        private final Paint paint;
        private final Point reusePoint = new Point(); // Para reutilizar memoria

        public BusStopsOverlay(List<GeoPoint> stops) {
            this.stops = stops; // Almacenar las paradas

            // Configurar bitmap y paint una sola vez
            pointBitmap = Bitmap.createBitmap(6, 6, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(pointBitmap);
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.secondaryVariant));
            paint.setAntiAlias(true);
            canvas.drawCircle(3, 3, 3, paint);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow || stops == null || stops.isEmpty()) return;

            Projection projection = mapView.getProjection();
            for (GeoPoint stop : stops) {
                projection.toPixels(stop, reusePoint);
                canvas.drawBitmap(pointBitmap, reusePoint.x - 3, reusePoint.y - 3, paint);
            }
        }
    }

    private void updateVisualizationModeForBus(double currentZoom) {
        if (!showBusStops) return; // No hacer nada si están desactivadas

        double DETAIL_ZOOM_THRESHOLD = 18;
        boolean shouldBeDetailed = currentZoom >= DETAIL_ZOOM_THRESHOLD;
        if (shouldBeDetailed != inDetailedMode) {
            inDetailedMode = shouldBeDetailed;
            refreshMapOverlays();
        }
    }

    private boolean isBusStopMarker(Overlay overlay) {
        if (!(overlay instanceof Marker)) return false;
        Marker marker = (Marker) overlay;
        return marker.getTitle() != null && !marker.getTitle().equals(getString(R.string.selected_marker));
    }

    private void addDetailedMarkers() {
        if (!showBusStops) return; // No agregar si están desactivadas

        final int BATCH_SIZE = 10;
        final Handler handler = new Handler();
        final AtomicInteger counter = new AtomicInteger(0);

        Runnable markerAdder = new Runnable() {
            @Override
            public void run() {
                if (!inDetailedMode || !showBusStops) return; // Cancelar si cambió el modo

                int start = counter.get();
                int end = Math.min(start + BATCH_SIZE, cachedBusStops.size());

                for (int i = start; i < end; i++) {
                    GeoJsonStop.Feature feature = cachedBusStops.get(i);
                    if (feature != null) {
                        addBusStopMarker(feature);
                    }
                }

                counter.set(end);
                if (end < cachedBusStops.size()) {
                    handler.postDelayed(this, 30);
                }
            }
        };

        handler.post(markerAdder);
    }

    // --------------------
    // Parkings de bicicletas
    // --------------------

    private void loadBikeParkings() {
        if (!cachedBikeParkings.isEmpty()) {
            if (showBikeParkings) {
                updateVisualizationModeForBike(map.getZoomLevelDouble());
            }
            return;
        }

        new Thread(() -> {
            APIService api = new APIService();
            try {
                GeoJsonParking parkings = api.getAllBikeParkings();
                if (parkings != null && parkings.features != null) {
                    cachedBikeParkings = parkings.features;

                    List<GeoPoint> points = new ArrayList<>(parkings.features.size());
                    for (GeoJsonParking.Feature feature : parkings.features) {
                        if (feature.geometry != null &&
                                feature.geometry.coordinates != null &&
                                feature.geometry.coordinates.size() >= 2) {

                            List<Double> coords = feature.geometry.coordinates;
                            points.add(new GeoPoint(coords.get(1), coords.get(0))); // GeoJSON usa [lon, lat]
                        }
                    }

                    runOnUiThread(() -> {
                        bikeParkingsOverlay = new BikeParkingsOverlay(points);
                        updateVisualizationModeForBike(map.getZoomLevelDouble());
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error cargando parkings", e);
            }
        }).start();
    }

    private class BikeParkingsOverlay extends Overlay {
        private final List<GeoPoint> parkings;
        private final Bitmap pointBitmap;
        private final Paint paint;
        private final Point reusePoint = new Point();

        public BikeParkingsOverlay(List<GeoPoint> parkings) {
            this.parkings = parkings;
            pointBitmap = Bitmap.createBitmap(6, 6, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(pointBitmap);
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.secondary));
            paint.setAntiAlias(true);
            canvas.drawCircle(3, 3, 3, paint);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow || parkings == null || parkings.isEmpty()) return;

            Projection projection = mapView.getProjection();
            for (GeoPoint parking : parkings) {
                projection.toPixels(parking, reusePoint);
                canvas.drawBitmap(pointBitmap, reusePoint.x - 3, reusePoint.y - 3, paint);
            }
        }
    }

    private void updateVisualizationModeForBike(double currentZoom) {
        if (!showBikeParkings) return;

        double DETAIL_ZOOM_THRESHOLD = 18;
        boolean shouldBeDetailed = currentZoom >= DETAIL_ZOOM_THRESHOLD;
        if (shouldBeDetailed != inDetailedMode) {
            inDetailedMode = shouldBeDetailed;
            refreshMapOverlays();
        }
    }

    private boolean isBikeParkingMarker(Overlay overlay) {
        if (!(overlay instanceof Marker)) return false;
        Marker marker = (Marker) overlay;
        return marker.getTitle() != null && marker.getTitle().startsWith("Parking");
    }

    private void addDetailedBikeMarkers() {
        if (!showBikeParkings) return;

        // 1. Obtener el bounding box actual del mapa
        BoundingBox mapBounds = map.getBoundingBox();

        // 2. Separar features en dentro/fuera del viewport
        List<GeoJsonParking.Feature> inViewport = new ArrayList<>();
        List<GeoJsonParking.Feature> outOfViewport = new ArrayList<>();

        for (GeoJsonParking.Feature feature : cachedBikeParkings) {
            if (feature.geometry == null || feature.geometry.coordinates == null) continue;

            List<Double> coords = feature.geometry.coordinates;
            double lat = coords.get(1);
            double lon = coords.get(0);

            if (mapBounds.contains(lat, lon)) {
                inViewport.add(feature);
            } else {
                outOfViewport.add(feature);
            }
        }

        // 3. Combinar listas (primero los del viewport)
        List<GeoJsonParking.Feature> prioritizedFeatures = new ArrayList<>();
        prioritizedFeatures.addAll(inViewport);
        prioritizedFeatures.addAll(outOfViewport);

        final int BATCH_SIZE = 5;
        final Handler handler = new Handler();
        final AtomicInteger counter = new AtomicInteger(0);

        Runnable markerAdder = new Runnable() {
            @Override
            public void run() {
                if (!inDetailedMode || !showBikeParkings) return;

                int start = counter.get();
                int end = Math.min(start + BATCH_SIZE, prioritizedFeatures.size());

                for (int i = start; i < end; i++) {
                    GeoJsonParking.Feature feature = prioritizedFeatures.get(i);
                    addBikeParkingMarker(feature);
                }

                counter.set(end);
                if (end < prioritizedFeatures.size()) {
                    handler.postDelayed(this, 30);
                }
            }
        };

        handler.post(markerAdder);
    }

    private void addBikeParkingMarker(GeoJsonParking.Feature feature) {
        if (feature.geometry == null || feature.geometry.coordinates == null) return;

        List<Double> coords = feature.geometry.coordinates;
        GeoPoint point = new GeoPoint(coords.get(1), coords.get(0)); // [lon, lat] to GeoPoint

        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(ContextCompat.getString(this, R.string.bike_parking));
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.bike_parking_marker));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setRelatedObject(feature);
        marker.setInfoWindow(new CustomInfoWindow(map, MarkerType.BIKE_PARKING));
        map.getOverlays().add(marker);
    }

    // ---------------
    // Librerias
    // --------------

    private void updateLibraryToggleButtonAppearance() {
        MaterialButton toggleLibraryButton = findViewById(R.id.toggle_library);
        int textColor = ContextCompat.getColor(this, showLibraries ? R.color.primary : R.color.onBackground);
        toggleLibraryButton.setTextColor(textColor);
    }

    private void loadLibraries() {
        if (!cachedLibraries.isEmpty()) {
            if (showLibraries) {
                updateVisualizationModeForLibraries(map.getZoomLevelDouble());
            }
            return;
        }

        new Thread(() -> {
            APIService api = new APIService();
            try {
                GeoJsonLibrary libraries = api.getAllLibraries();
                if (libraries != null && libraries.features != null) {
                    List<GeoPoint> points = new ArrayList<>();
                    List<GeoJsonLibrary.Feature> filteredLibraries = new ArrayList<>(); // Lista filtrada

                    for (GeoJsonLibrary.Feature feature : libraries.features) {
                        // Verificar municipio y coordenadas
                        if (feature.properties != null &&
                                "Vitoria - Gasteiz".equalsIgnoreCase(feature.properties.municipality) &&
                                feature.geometry != null &&
                                feature.geometry.coordinates != null &&
                                feature.geometry.coordinates.size() >= 2) {

                            List<Double> coords = feature.geometry.coordinates;
                            points.add(new GeoPoint(coords.get(1), coords.get(0)));
                            filteredLibraries.add(feature); // Añadir a la lista filtrada
                        }
                    }

                    cachedLibraries = filteredLibraries; // Guardar solo las filtradas

                    runOnUiThread(() -> {
                        librariesOverlay = new LibrariesOverlay(points);
                        updateVisualizationModeForLibraries(map.getZoomLevelDouble());
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error cargando bibliotecas", e);
            }
        }).start();
    }

    private class LibrariesOverlay extends Overlay {
        private final List<GeoPoint> libraries;
        private final Bitmap pointBitmap;
        private final Paint paint;
        private final Point reusePoint = new Point();

        public LibrariesOverlay(List<GeoPoint> libraries) {
            this.libraries = libraries;
            pointBitmap = Bitmap.createBitmap(6, 6, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(pointBitmap);
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryVariant));
            paint.setAntiAlias(true);
            canvas.drawCircle(3, 3, 3, paint);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow || libraries == null || libraries.isEmpty()) return;

            Projection projection = mapView.getProjection();
            for (GeoPoint library : libraries) {
                projection.toPixels(library, reusePoint);
                canvas.drawBitmap(pointBitmap, reusePoint.x - 3, reusePoint.y - 3, paint);
            }
        }
    }

    private void updateVisualizationModeForLibraries(double currentZoom) {
        if (!showLibraries) return;

        double DETAIL_ZOOM_THRESHOLD = 18;
        boolean shouldBeDetailed = currentZoom >= DETAIL_ZOOM_THRESHOLD;
        if (shouldBeDetailed != inDetailedMode) {
            inDetailedMode = shouldBeDetailed;
            refreshMapOverlays();
        }
    }

    private boolean isLibraryMarker(Overlay overlay) {
        if (!(overlay instanceof Marker)) return false;
        Marker marker = (Marker) overlay;
        return marker.getTitle() != null && marker.getTitle().equals(getString(R.string.library));
    }

    private void addDetailedLibraryMarkers() {
        if (!showLibraries) return;

        BoundingBox mapBounds = map.getBoundingBox();
        List<GeoJsonLibrary.Feature> prioritizedFeatures = new ArrayList<>();

        for (GeoJsonLibrary.Feature feature : cachedLibraries) {
            if (feature.geometry != null && feature.geometry.coordinates != null) {
                double lat = feature.geometry.coordinates.get(1);
                double lon = feature.geometry.coordinates.get(0);
                if (mapBounds.contains(lat, lon)) {
                    prioritizedFeatures.add(0, feature); // Prioridad a los del viewport
                } else {
                    prioritizedFeatures.add(feature);
                }
            }
        }

        final int BATCH_SIZE = 5;
        final Handler handler = new Handler();
        final AtomicInteger counter = new AtomicInteger(0);

        Runnable markerAdder = new Runnable() {
            @Override
            public void run() {
                if (!inDetailedMode || !showLibraries) return;

                int start = counter.get();
                int end = Math.min(start + BATCH_SIZE, prioritizedFeatures.size());

                for (int i = start; i < end; i++) {
                    GeoJsonLibrary.Feature feature = prioritizedFeatures.get(i);
                    addLibraryMarker(feature);
                }

                counter.set(end);
                if (end < prioritizedFeatures.size()) {
                    handler.postDelayed(this, 30);
                }
            }
        };

        handler.post(markerAdder);
    }

    private void addLibraryMarker(GeoJsonLibrary.Feature feature) {
        if (feature.geometry == null || feature.geometry.coordinates == null) return;

        List<Double> coords = feature.geometry.coordinates;
        GeoPoint point = new GeoPoint(coords.get(1), coords.get(0));

        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(ContextCompat.getString(this, R.string.library));
        marker.setSnippet(feature.properties.address);
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.library_marker));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setRelatedObject(feature);
        marker.setInfoWindow(new CustomInfoWindow(map, MarkerType.LIBRARY));
        map.getOverlays().add(marker);
    }

    // -----------------
    // Idiomas
    // ----------------

    private void dialogoIdioma() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String[] idiomas = {"Español", "English", "Euskara"};
        final String[] codigos = {"es", "en", "eu"};

        // Recuperar el idioma actual
        String idiomaActual = prefs.getString("idioma", "es");
        int selectedIndex = 0;
        for (int i = 0; i < codigos.length; i++) {
            if (codigos[i].equals(idiomaActual)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ThemeOverlay_Unigo_MaterialAlertDialog);
        builder.setTitle(R.string.selec_idioma);
        builder.setSingleChoiceItems(idiomas, selectedIndex, (dialog, which) -> {
            String idiomaSeleccionado = codigos[which];
            if (!idiomaSeleccionado.equals(idiomaActual)) {
                prefs.edit().putString("idioma", idiomaSeleccionado).apply();

                // Reiniciar Activity para que se aplique el nuevo locale
                Intent intent = new Intent(MapActivity.this, MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.cancelar, null);
        builder.show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es");
        super.attachBaseContext(LocaleHelper.setLocale(newBase, idioma));
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Para que se vuelva a aplicar si hay cambios de configuración (p.e. rotación)
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es");
        LocaleHelper.setLocale(this, idioma);
    }

    private void fetchWeather(TextView tvTemp, TextView tvHumidity) {
        new Thread(() -> {
            try {
                // 1. Hora actual UTC truncada
                Instant nowHour = Instant.now()
                        .truncatedTo(ChronoUnit.HOURS);

                // 2. Construimos la URL
                String from = URLEncoder.encode(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                                .withZone(ZoneOffset.UTC)
                                .format(nowHour.minus(12, ChronoUnit.HOURS)),
                        "UTF-8"
                ); // Se acota la cantidad de datos filtrando a los datos de las ultimas 12 horas
                String to = URLEncoder.encode(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                                .withZone(ZoneOffset.UTC)
                                .format(nowHour.minus(1, ChronoUnit.HOURS)),
                        "UTF-8"
                ); // Se aplica -1 hora porque la api va con horario UTC
                String urlString = String.format(
                        "https://api.euskadi.eus/air-quality/measurements/hourly/"
                                + "stations/85/from/%s/to/%s?lang=SPANISH",
                        from, to
                );

                // 3. Petición HTTP
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                String json = new Scanner(conn.getInputStream())
                        .useDelimiter("\\A")
                        .next();
                conn.disconnect();

                // 4. Parseo JSON
                JSONObject root = new JSONObject("{\"data\":" + json + "}");
                JSONArray  arr  = root.getJSONArray("data");
                if (arr.length() == 0) throw new IllegalStateException("Sin datos");

                // 5. Buscamos el registro con date más cercano a nowHour, diff ≤ 3h
                DateTimeFormatter isoFmt = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .withZone(ZoneOffset.UTC);

                JSONObject bestRecord = null;
                long bestDiffHours = Long.MAX_VALUE;

                for (int i = 0; i < arr.length(); i++) {
                    String dateStr = arr.getJSONObject(i).getString("date");
                    Instant recInst = Instant.from(isoFmt.parse(dateStr));
                    long diffHours = Math.abs(ChronoUnit.HOURS.between(recInst, nowHour));

                    if (diffHours < bestDiffHours) {
                        bestDiffHours = diffHours;
                        bestRecord    = arr.getJSONObject(i);
                    }
                }

                // Si ni el más cercano está a ≤ 3 horas, fallback al primero
                if (bestRecord == null || bestDiffHours > 3) {
                    bestRecord = arr.getJSONObject(0);
                }

                // 6. Extraemos temperatura y humedad
                JSONArray measurements = bestRecord
                        .getJSONArray("station")
                        .getJSONObject(0)
                        .getJSONArray("measurements");

                String temp = "--", hum = "--";
                for (int j = 0; j < measurements.length(); j++) {
                    JSONObject m = measurements.getJSONObject(j);
                    String name = m.getString("name");
                    double val  = m.getDouble("value");
                    // 1) Solo asignar si val != 0
                    if (val == 0.0) continue;

                    switch (name) {
                        case "Tº":
                            // primera temperatura válida
                            if ("--".equals(temp)) {
                                temp = String.valueOf(val);
                            }
                            break;
                        case "H":
                            // primera humedad válida
                            if ("--".equals(hum)) {
                                hum = String.valueOf(val);
                            }
                            break;
                    }
                    // 2) Si ya tienes ambos, sales del bucle
                    if (!"--".equals(temp) && !"--".equals(hum)) break;
                }

                // 7. Variables finales para el lambda
                final String displayTemp = temp;
                final String displayHum  = hum;

                // 8. Actualizar UI
                runOnUiThread(() -> {
                    tvTemp.setText(
                            String.format(Locale.getDefault(), "Tº: %sºC", displayTemp)
                    );
                    tvHumidity.setText(
                            String.format(Locale.getDefault(), "H: %s%%", displayHum)
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
