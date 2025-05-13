package com.unigo.utils;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import com.unigo.R;
import com.unigo.models.GeoJsonParking;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.Locale;

public class CustomInfoWindow extends InfoWindow {

    private final MarkerType markerType;

    public CustomInfoWindow(MapView mapView, MarkerType type) {
        super(getLayoutResource(type), mapView);
        this.markerType = type;
    }

    private static int getLayoutResource(MarkerType type) {
        switch (type) {
            case BIKE_PARKING:
                return R.layout.bike_infoview;
            case BUS_STOP:
                return R.layout.custom_infoview;
            default:
                return R.layout.custom_infoview;
        }
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        View view = getView();
        bindData(marker, view, markerType);

        view.postDelayed(marker::closeInfoWindow, 3000);
    }

    private void bindData(Marker marker, View view, MarkerType type) {
        switch (type) {
            case BIKE_PARKING:
                bindBikeParkingData(marker, view);
                break;
            case BUS_STOP:
                bindBusStopData(marker, view);
                break;
            default:
                bindDefaultData(marker, view);
        }
    }

    private void bindBikeParkingData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.tv_title);
        TextView description = view.findViewById(R.id.tv_description);
        TextView space_n = view.findViewById(R.id.tv_space_n);
        TextView id = view.findViewById(R.id.tv_id);

        title.setText(marker.getTitle());
        description.setText(String.format(Locale.getDefault(), "Coords: %.5f, %.5f",
                marker.getPosition().getLatitude(),
                marker.getPosition().getLongitude()));
        GeoJsonParking.Feature data = (GeoJsonParking.Feature) marker.getRelatedObject();
        space_n.setText(String.format(Locale.getDefault(), "%.0f", data.getProperties().getSum()));
        id.setText(view.getContext().getString(R.string.bike_parking_id, Integer.toString(data.getProperties().getCLUSTER_ID())));
    }

    private void bindBusStopData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.info_title);
        TextView snippet = view.findViewById(R.id.info_snippet);

        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
    }

    private void bindDefaultData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.info_title);
        TextView snippet = view.findViewById(R.id.info_snippet);

        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
    }

    @Override
    public void onClose() {
        // Limpieza opcional
    }
}

