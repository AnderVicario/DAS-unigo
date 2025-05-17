package com.unigo.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unigo.models.api.GeoJsonLibrary;
import com.unigo.models.api.GeoJsonParking;
import com.unigo.models.api.GeoJsonStop;
import com.unigo.models.api.RoutesResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

public class APIService {

    private static final String BASE_URL = "http://umbra.ddns.net:5003";
    private final OkHttpClient client;
    private final Gson gson;

    public APIService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public GeoJsonLibrary getAllLibraries() throws IOException {
        Request request = new Request.Builder()
                .url("https://opendata.euskadi.eus/contenidos/ds_localizaciones/bibliotecas_publicas_euskadi/opendata/bibliotecas.geojson")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            return gson.fromJson(json, GeoJsonLibrary.class);
        }
    }

    public GeoJsonParking getAllBikeParkings() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/parkings/geojson")  // Endpoint específico de parkings
                .build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            return gson.fromJson(json, GeoJsonParking.class);
        }
    }

    public GeoJsonStop getAllStops() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/stops/geojson")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            return gson.fromJson(json, GeoJsonStop.class);
        }
    }

    public List<RoutesResponse.RouteOption> findRoutes(double lat, double lon) throws IOException {
        String url = String.format(Locale.US, BASE_URL + "/routes?lat=%.6f&lon=%.6f", lat, lon);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected response: " + response);
            }
            String json = response.body().string();
            Type listType = new TypeToken<List<RoutesResponse.RouteOption>>(){}.getType();
            return gson.fromJson(json, listType);
        }
    }
}