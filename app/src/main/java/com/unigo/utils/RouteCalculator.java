package com.unigo.utils;

import android.graphics.Color;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.unigo.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RouteCalculator {
    private static final String TAG = "RouteCalculator";
    private static final String HOST = "http://umbra.ddns.net";

    // Mapa de perfiles y puertos
    private static final Map<String, Integer> PROFILE_PORT_MAP = new HashMap<String, Integer>() {{
        put("foot", 5000);
        put("car", 5001);
        put("bike", 5002);
    }};

    private final MapView mapView;
    private final OkHttpClient client;
    private final Gson gson;
    private Polyline routePolyline;

    public interface RouteCallback {
        void onRouteCalculated(double distanceKm, int durationMinutes);
        void onRouteError(String message);
    }

    public RouteCalculator(MapView mapView) {
        this.mapView = mapView;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void calculateRoute(String profile, GeoPoint start, GeoPoint end, RouteCallback callback) {
        clearExistingRoute();

        Integer port = PROFILE_PORT_MAP.get(profile);
        if (port == null) {
            callback.onRouteError("Perfil no soportado: " + profile);
            return;
        }

        String url = HOST + ":" + port + "/route/v1/" + profile + "/"
                + start.getLongitude() + "," + start.getLatitude() + ";"
                + end.getLongitude() + "," + end.getLatitude()
                + "?overview=full&geometries=polyline";

        Log.d(TAG, "Request URL: " + url);

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error en la solicitud OSRM", e);
                callback.onRouteError("Error al obtener la ruta: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onRouteError("Error en la respuesta: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseData, JsonObject.class);

                    if (!jsonResponse.has("routes") || jsonResponse.getAsJsonArray("routes").size() == 0) {
                        callback.onRouteError("No se encontró ninguna ruta");
                        return;
                    }

                    Log.d(TAG, "Response JSON: " + responseData);

                    JsonObject route = jsonResponse.getAsJsonArray("routes").get(0).getAsJsonObject();
                    String geometry = route.get("geometry").getAsString();
                    List<GeoPoint> points = decodePolyline(geometry);
                    double distanceInKm = route.get("distance").getAsDouble() / 1000;
                    int durationInMinutes = (int) (route.get("duration").getAsDouble() / 60);

                    drawRoute(points);
                    callback.onRouteCalculated(distanceInKm, durationInMinutes);

                } catch (Exception e) {
                    Log.e(TAG, "Error al procesar la respuesta", e);
                    callback.onRouteError("Error al procesar la respuesta: " + e.getMessage());
                }
            }
        });
    }

    private void drawRoute(List<GeoPoint> points) {
        mapView.post(() -> {
            routePolyline = new Polyline(mapView);
            routePolyline.setPoints(points);
            routePolyline.setColor(ContextCompat.getColor(mapView.getContext(), R.color.primary));
            routePolyline.setWidth(14f);
            mapView.getOverlays().add(routePolyline);
            mapView.invalidate();
        });
    }

    public void clearExistingRoute() {
        if (routePolyline != null) {
            final Polyline toRemove = routePolyline;
            mapView.post(() -> {
                mapView.getOverlays().remove(toRemove);
                mapView.invalidate();
            });
            routePolyline = null;
        }
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, lat = 0, lng = 0;

        while (index < encoded.length()) {
            int result = 0, shift = 0, b;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);

            result = 0;
            shift = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);

            poly.add(new GeoPoint(lat / 1E5, lng / 1E5));
        }

        return poly;
    }

    public static String formatDistance(double distanceKm) {
        return new DecimalFormat("#.##").format(distanceKm) + " km";
    }

    public static String formatDuration(int durationMinutes) {
        if (durationMinutes < 60) return durationMinutes + " min";
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        return hours + " h " + mins + " min";
    }
}
