package com.unigo.utils;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import com.unigo.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class CustomInfoWindow extends InfoWindow {

    public CustomInfoWindow(MapView mapView) {
        super(R.layout.custom_infoview, mapView);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        View view = getView();

        TextView titleText = view.findViewById(R.id.info_title);
        titleText.setText(marker.getTitle());

        TextView descriptionText = view.findViewById(R.id.info_snippet);
        descriptionText.setText(String.format("%.4f, %.4f",
                marker.getPosition().getLatitude(),
                marker.getPosition().getLongitude()));

        view.postDelayed(marker::closeInfoWindow, 3000);
    }

    @Override
    public void onClose() {
    }
}

