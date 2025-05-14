package com.unigo.utils;

import com.google.gson.Gson;
import com.unigo.models.api.GeoJsonLibrary;
import com.unigo.models.api.GeoJsonParking;
import com.unigo.models.api.GeoJsonStop;
import com.unigo.models.api.NearStopResponse;
import com.unigo.models.api.TransfersResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
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

    public NearStopResponse findNearStop(double lat, double lon, int maxDistM) throws IOException {
        String url = String.format(Locale.US, BASE_URL + "/stops/near?lat=%.6f&lon=%.6f&max_dist_m=%d", lat, lon, maxDistM);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            return gson.fromJson(json, NearStopResponse.class);
        }
    }

    public TransfersResponse getSmartTransfers(String originStopName) throws IOException {
        String encodedStopName = originStopName.replace(" ", "%20");
        String url = BASE_URL + "/transfers?origin_stop_name=" + encodedStopName;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            return gson.fromJson(json, TransfersResponse.class);
        }
    }
}