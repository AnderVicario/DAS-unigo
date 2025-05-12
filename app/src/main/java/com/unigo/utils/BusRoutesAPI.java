package com.unigo.utils;

import com.google.gson.Gson;
import com.unigo.models.GeoJsonStop;
import com.unigo.models.NearStopResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;

public class BusRoutesAPI {

    private static final String BASE_URL = "http://umbra.ddns.net:5003";
    private final OkHttpClient client;
    private final Gson gson;

    public BusRoutesAPI() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
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

    public String getSmartTransfers(String originStopName) throws IOException {
        String encodedStopName = originStopName.replace(" ", "%20");
        String url = BASE_URL + "/transfers?origin_stop_name=" + encodedStopName;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
