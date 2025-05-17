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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.unigo.models.Transport;
import com.unigo.models.api.RoutesResponse;
import com.unigo.utils.APIService;
import com.unigo.utils.CustomInfoWindow;
import com.unigo.utils.ForecastDay;
import com.unigo.utils.ForecastDialogFragment;
import com.unigo.utils.LocaleHelper;
import com.unigo.utils.MarkerType;
import com.unigo.utils.RouteCalculator;
import com.unigo.utils.SnackbarUtils;
import com.unigo.utils.SvgUtil;
import com.unigo.utils.TranslatorUtil;
import com.unigo.utils.WeatherHelper;

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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicReference;

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

    private WeatherHelper weatherHelper;

    // --------------------
    // Ciclo de vida
    // --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        initializeMap();

        // Configuración de UI
        setupWindow();
        configureLogo();
        configureControls();
        configureNavigation();
        configureZoom();
        configureBottomSheet();
        configureRecyclerView();
        configureHorizontalScrollView();
        weatherHelper = new WeatherHelper(this);
        View header = navigationView.getHeaderView(0);
        TextView tvTemp = header.findViewById(R.id.nav_temp);
        TextView tvHumidity = header.findViewById(R.id.nav_humidity);
        TextView tvMeteoDesc = header.findViewById(R.id.nav_meteo_desc);
        ImageView ivIcon = header.findViewById(R.id.nav_weather_icon);
        weatherHelper.fetchMeteo(tvMeteoDesc, ivIcon);

        weatherHelper.fetchWeather(tvTemp, tvHumidity);

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
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        if (prefs.getString("mapa", "auto").equals("auto")) {
            applyMapMode("auto");
        }
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

            ivLogo.setImageResource(R.drawable.logo_dark);
        } else {
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
        ImageButton recalculateButton = findViewById(R.id.button_recalculate);
        recalculateButton.setOnClickListener(v -> {
            routeCalculator.clearExistingRoute();
            calculateAllRoutes(FIXED_DESTINATION);
        });

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
            /*SnackbarUtils.showSuccess(
                    findViewById(android.R.id.content),
                    this,
                    "Ruta: " + transport.getMode() + "\n" +
                            transport.getFormattedDistance() + " - " +
                            transport.getFormattedDuration()
            );*/
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
            else if (id == R.id.nav_mapa) {
                mostrarOpcionesMapa();
            }
            else if (id == R.id.nav_tema) {
                mostrarDialogoTema();
            }
            else if (id == R.id.nav_tiempo) {
                weatherHelper.fetchForecastList(list -> {
                    if (!list.isEmpty()) {
                        // Fragment necesita ArrayList parcelable
                        ForecastDialogFragment dialog =
                                ForecastDialogFragment.newInstance(
                                        new ArrayList<>(list));
                        dialog.show(getSupportFragmentManager(),
                                "forecastDialog");
                    } else {
                        Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
            // Cerrar el drawer tras la selección
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }

    // --------------------
    // Configuración del MAPA
    // --------------------

    private void initializeMap() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String mode = prefs.getString("mapa", "auto");
        applyMapMode(mode);
    }

    // Helper para saber si estamos en tema nocturno:
    private boolean isNightMode() {
        int uiMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyMapMode(String mode) {
        //Guardar la configuración nueva
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("mapa", mode).apply();
        // Extraer la configuración actual
        String actual = mode.equals("auto")
                ? (isNightMode() ? "dark" : "light")
                : mode;

        map = findViewById(R.id.map);

        switch (actual) {
            case "light":
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
                    }}
                );
                break;
            case "dark":
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
                break;
            case "mapnik":
                // Modo "Mapnik" (OSM clásico)
                // Establecer User-Agent (para usar este mapa es necesario)
                Context ctx = getApplicationContext();
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
                Configuration.getInstance().setUserAgentValue("UnigoApp/1.0");

                map.setTileSource(new XYTileSource("Mapnik", 0, 19, 256, ".png",
                        new String[]{
                                "https://tile.openstreetmap.org/"
                        }) {
                    @Override
                    public String getTileURLString(long pMapTileIndex) {
                        int z = MapTileIndex.getZoom(pMapTileIndex);
                        int x = MapTileIndex.getX(pMapTileIndex);
                        int y = MapTileIndex.getY(pMapTileIndex);
                        return getBaseUrl() + z + "/" + x + "/" + y + ".png";
                    }
                });
                break;
            case"satellite":
                // Modo “satélite”
                map.setTileSource(new XYTileSource("EsriWorldImagery", 0, 19, 256, ".jpg",
                        new String[]{
                                "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
                        }) {
                    @Override
                    public String getTileURLString(long pMapTileIndex) {
                        int z = MapTileIndex.getZoom(pMapTileIndex);
                        int x = MapTileIndex.getX(pMapTileIndex);
                        int y = MapTileIndex.getY(pMapTileIndex);
                        return getBaseUrl() + z + "/" + y + "/" + x;
                    }
                });
                break;
        }

        map.setMultiTouchControls(true);
        centerMapOnLocation(FIXED_DESTINATION, 17.0); // Gasteiz
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
                handleMapLongTap();
                return true;
            }
        });

        map.getOverlays().add(eventsOverlay);
    }

    private void handleMapTap(GeoPoint position) {
        /*routeCalculator.clearExistingRoute();*/
        addNewMarker(position);
    }

    private void handleMapLongTap() {
        routeCalculator.clearExistingRoute();
        removeExistingMarker();
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
        Marker marker = new Marker(map);
        marker.setPosition(FIXED_DESTINATION);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Universidad de Vitoria-Gazteiz");
        marker.setSubDescription("Universidad de Vitoria-Gazteiz");
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.destination_marker));

        marker.setInfoWindow(new CustomInfoWindow(map, MarkerType.LIBRARY));
        map.getOverlays().add(marker);

        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getApplicationContext()),
                map
        );

        Drawable arrowDrawable = ContextCompat.getDrawable(this, R.drawable.navigation);
        if (arrowDrawable != null) {
            Bitmap arrowBitmap = drawableToBitmap(arrowDrawable);
            int width = (int) (arrowBitmap.getWidth() * 0.9);
            int height = (int) (arrowBitmap.getHeight() * 0.9);
            arrowBitmap = Bitmap.createScaledBitmap(arrowBitmap, width, height, true);

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

        // Usamos un executor con pool de hilos para manejar las diferentes solicitudes
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (String profile : PROFILE_PORT_MAP.keySet()) {
            executor.execute(() -> {
                try {
                    if ("car".equals(profile)) {
                        // Lógica específica para autobuses
                        APIService apiService = new APIService();
                        GeoJsonStop stops = apiService.getAllStops();

                        List<RoutesResponse.RouteOption> routesResponse = apiService.findRoutes(
                            start.getLatitude(),
                            start.getLongitude());

                        if (routesResponse.isEmpty()) {
                            Log.d(TAG, "No se encontraron rutas para bus");
                            return;
                        }

                        for (RoutesResponse.RouteOption routeOption : routesResponse){
                            if (routeOption.type.equals("direct")){
                                Log.d(TAG, "Viaje directo encontrado!");
                                int stop_1 = routeOption.from_stop;
                                int stop_2 = routeOption.to_stop;
                                String route_1 = routeOption.route_id;
                                GeoPoint busOrigin = stops.findStopByID(stop_1);
                                GeoPoint busDestination = stops.findStopByID(stop_2);

                                List<List<GeoPoint>> segments = Collections.synchronizedList(new ArrayList<>(Arrays.asList(null, null, null)));
                                AtomicInteger totalDuration = new AtomicInteger(0);
                                AtomicReference<Double> totalDistance = new AtomicReference<>(0.0);
                                AtomicInteger routesCompleted = new AtomicInteger(0);

                                Transport.TransportMode modeName = Transport.TransportMode.BUS_DIRECT;

                                Runnable checkAndCreateTransport = () -> {
                                    if (routesCompleted.get() == 3) {
                                        List<GeoPoint> fullRoute = new ArrayList<>();
                                        for (List<GeoPoint> segment : segments) {
                                            fullRoute.addAll(segment);
                                        }

                                        Transport transport = new Transport(
                                                modeName,
                                                totalDistance.get(),
                                                totalDuration.get(),
                                                fullRoute
                                        );

                                        transport.setStop1(stop_1);
                                        transport.setStop2(stop_2);
                                        transport.setRoute1(route_1);

                                        transportOptions.add(transport);
                                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                                    }
                                };

                                // Inicio -> parada origen
                                routeCalculator.calculateRoute("foot", start, busOrigin, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(0, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta inicio -> origen: " + message);
                                    }
                                });

                                // Bus
                                routeCalculator.calculateRoute("car", busOrigin, busDestination, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(1, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta bus: " + message);
                                    }
                                });

                                // Parada destino -> destino final
                                routeCalculator.calculateRoute("foot", busDestination, destination, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(2, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta destino -> destino final: " + message);
                                    }
                                });

                            }
                            else if (routeOption.type.equals("transfer_direct")) {
                                Log.d(TAG, "Viaje con transbordo directo encontrado!");
                                RoutesResponse.Leg firstLeg = routeOption.first_leg;
                                RoutesResponse.Leg secondLeg = routeOption.second_leg;

                                int stop_1 = routeOption.from_stop;
                                int stop_2 = firstLeg.arrival_stop;
                                int stop_3 = firstLeg.arrival_stop;
                                int stop_4 = secondLeg.arrival_stop;
                                String route_1 = firstLeg.route;
                                String route_2 = secondLeg.route;
                                GeoPoint busOrigin1 = stops.findStopByID(stop_1);
                                GeoPoint busDestination1 = stops.findStopByID(stop_2);
                                GeoPoint busOrigin2 = busDestination1;
                                GeoPoint busDestination2 = stops.findStopByID(stop_4);

                                List<List<GeoPoint>> segments = Collections.synchronizedList(new ArrayList<>(Arrays.asList(null, null, null, null)));
                                AtomicInteger totalDuration = new AtomicInteger(0);
                                AtomicReference<Double> totalDistance = new AtomicReference<>(0.0);
                                AtomicInteger routesCompleted = new AtomicInteger(0);

                                Transport.TransportMode modeName = Transport.TransportMode.BUS_TDIRECT;

                                Runnable checkAndCreateTransport = () -> {
                                    if (routesCompleted.get() == 4) {
                                        List<GeoPoint> fullRoute = new ArrayList<>();
                                        for (List<GeoPoint> segment : segments) {
                                            fullRoute.addAll(segment);
                                        }

                                        Transport transport = new Transport(
                                                modeName,
                                                totalDistance.get(),
                                                totalDuration.get(),
                                                fullRoute
                                        );

                                        transport.setStop1(stop_1);
                                        transport.setStop2(stop_2);
                                        transport.setStop3(stop_3);
                                        transport.setStop4(stop_4);
                                        transport.setRoute1(route_1);
                                        transport.setRoute2(route_2);

                                        transportOptions.add(transport);
                                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                                    }
                                };

                                // Inicio -> parada origen
                                routeCalculator.calculateRoute("foot", start, busOrigin1, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(0, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta inicio -> origen: " + message);
                                    }
                                });

                                // Bus 1
                                routeCalculator.calculateRoute("car", busOrigin1, busDestination1, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(1, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta bus 1: " + message);
                                    }
                                });

                                // Bus 2
                                routeCalculator.calculateRoute("car", busOrigin2, busDestination2, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(2, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta bus 2: " + message);
                                    }
                                });

                                // Parada destino -> destino final
                                routeCalculator.calculateRoute("foot", busDestination2, destination, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(3, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta destino -> destino final: " + message);
                                    }
                                });
                            }
                            else if (routeOption.type.equals("transfer_walk")) {
                                Log.d(TAG, "Viaje con transbordo andando encontrado!");
                                RoutesResponse.Leg firstLeg = routeOption.first_leg;
                                RoutesResponse.Walk walk = routeOption.walk;
                                RoutesResponse.Leg secondLeg = routeOption.second_leg;

                                int stop_1 = routeOption.from_stop;
                                int stop_2 = firstLeg.arrival_stop;
                                int stop_3 = walk.to_stop;
                                int stop_4 = secondLeg.arrival_stop;
                                String route_1 = firstLeg.route;
                                String route_2 = secondLeg.route;
                                GeoPoint busOrigin1 = stops.findStopByID(stop_1);
                                GeoPoint busDestination1 = stops.findStopByID(stop_2);
                                GeoPoint busOrigin2 = stops.findStopByID(stop_3);
                                GeoPoint busDestination2 = stops.findStopByID(stop_4);

                                List<List<GeoPoint>> segments = Collections.synchronizedList(new ArrayList<>(Arrays.asList(null, null, null, null, null)));
                                AtomicInteger totalDuration = new AtomicInteger(0);
                                AtomicReference<Double> totalDistance = new AtomicReference<>(0.0);
                                AtomicInteger routesCompleted = new AtomicInteger(0);

                                Transport.TransportMode modeName = Transport.TransportMode.BUS_TWALK;

                                Runnable checkAndCreateTransport = () -> {
                                    if (routesCompleted.get() == 5) {
                                        List<GeoPoint> fullRoute = new ArrayList<>();
                                        for (List<GeoPoint> segment : segments) {
                                            fullRoute.addAll(segment);
                                        }

                                        Transport transport = new Transport(
                                                modeName,
                                                totalDistance.get(),
                                                totalDuration.get(),
                                                fullRoute
                                        );

                                        transport.setStop1(stop_1);
                                        transport.setStop2(stop_2);
                                        transport.setStop3(stop_3);
                                        transport.setStop4(stop_4);
                                        transport.setRoute1(route_1);
                                        transport.setRoute2(route_2);

                                        transportOptions.add(transport);
                                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                                    }
                                };

                                // Inicio -> parada origen
                                routeCalculator.calculateRoute("foot", start, busOrigin1, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(0, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta inicio -> origen: " + message);
                                    }
                                });

                                // Bus 1
                                routeCalculator.calculateRoute("car", busOrigin1, busDestination1, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(1, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta bus 1: " + message);
                                    }
                                });

                                routeCalculator.calculateRoute("foot", busDestination1, busOrigin2, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(2, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta trasbordo: " + message);
                                    }
                                });

                                // Bus 2
                                routeCalculator.calculateRoute("car", busOrigin2, busDestination2, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(3, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta bus 2: " + message);
                                    }
                                });

                                // Parada destino -> destino final
                                routeCalculator.calculateRoute("foot", busDestination2, destination, new RouteCalculator.RouteCallback() {
                                    @Override
                                    public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                        segments.set(4, points);
                                        totalDistance.updateAndGet(v -> v + distanceKm);
                                        totalDuration.addAndGet(durationMinutes);
                                        routesCompleted.incrementAndGet();
                                        checkAndCreateTransport.run();
                                    }

                                    @Override
                                    public void onRouteError(String message) {
                                        Log.e(TAG, "Error ruta destino -> destino final: " + message);
                                    }
                                });
                            }
                        }

                    } else {
                        // Lógica para otros modos de transporte (pie, bicicleta)
                        routeCalculator.calculateRoute(profile, start, destination, new RouteCalculator.RouteCallback() {
                            @Override
                            public void onRouteCalculated(double distanceKm, int durationMinutes, List<GeoPoint> points) {
                                Transport.TransportMode modeName = null;
                                switch(profile) {
                                    case "foot": modeName = Transport.TransportMode.FOOT; break;
                                    case "bike": modeName = Transport.TransportMode.BIKE; break;
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
                } catch (IOException e) {
                    Log.e(TAG, "Error en API: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error general: " + e.getMessage());
                }
            });
        }

        executor.shutdown(); // Asegurar que se liberen los recursos
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
        String[] idiomas = {"Español", "Euskara", "English"};
        final String[] codigos = {"es", "eu", "en"};

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

    private void mostrarOpcionesMapa() {
        final String[] modos = { "auto", "mapnik", "satellite" };
        final String[] titulos = {
                getString(R.string.Minimalista),
                getString(R.string.Detallado),
                getString(R.string.Satelite)
        };
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String current = prefs.getString("mapa", "auto");
        int checked = Arrays.asList(modos).indexOf(current);

        new AlertDialog.Builder(this, R.style.ThemeOverlay_Unigo_MaterialAlertDialog)
                .setTitle(R.string.map)
                .setSingleChoiceItems(titulos, checked, null)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    int selected = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0) {
                        applyMapMode(modos[selected]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    private void mostrarDialogoTema() {
        final String[] valores = { "auto", "light", "dark" };
        final String[] titulos = {
                getString(R.string.tema_sistema),
                getString(R.string.tema_claro),
                getString(R.string.tema_oscuro)
        };

        // Lee la preferencia actual para marcarla
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String current = prefs.getString("tema", "auto");
        int checked = Arrays.asList(valores).indexOf(current);

        new AlertDialog.Builder(this, R.style.ThemeOverlay_Unigo_MaterialAlertDialog)
                .setTitle(R.string.menu_tema)
                .setSingleChoiceItems(titulos, checked, null)
                .setPositiveButton(android.R.string.ok, (dlg, which) -> {
                    int sel = ((AlertDialog)dlg).getListView().getCheckedItemPosition();
                    if (sel >= 0) {
                        setAppTheme(valores[sel]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    private void setAppTheme(String mode) {
        // 1) Persistir elección
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("tema", mode).apply();

        // 2) Configurar AppCompatDelegate
        int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if ("light".equals(mode)) nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        else if ("dark".equals(mode)) nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(nightMode);

        // 3) Recrear para que cambien estilos UI (si estaba en auto hay que recrearlo (con el nuevo tema))
        String mapaMode = prefs.getString("mapa", "auto");
        applyMapMode(mapaMode);
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es");
        String tema = prefs.getString("tema",   "auto");

        Context localeUpdatedCtx = LocaleHelper.setLocale(newBase, idioma);

        int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if ("light".equals(tema)) nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        if ("dark".equals(tema))  nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(nightMode);
        super.attachBaseContext(localeUpdatedCtx);
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Para que se vuelva a aplicar si hay cambios de configuración (p.e. rotación)
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String idioma = prefs.getString("idioma", "es");
        LocaleHelper.setLocale(this, idioma);
        String mapaMode = prefs.getString("mapa", "auto");
        if ("auto".equals(mapaMode)) {
            applyMapMode("auto");
        }
    }

}
