package com.unigo.utils;

import static android.provider.Settings.System.getString;

import android.view.View;
import android.widget.TextView;

import com.unigo.R;
import com.unigo.models.GeoJsonLibrary;
import com.unigo.models.GeoJsonParking;
import com.unigo.models.GeoJsonStop;

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
                return R.layout.bus_infoview;
            case LIBRARY:
                return R.layout.library_infoview;
            default:
                return R.layout.custom_infoview;
        }
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        View view = getView();
        bindData(marker, view, markerType);

        view.postDelayed(marker::closeInfoWindow, 4000);
    }

    private void bindData(Marker marker, View view, MarkerType type) {
        switch (type) {
            case BIKE_PARKING:
                bindBikeParkingData(marker, view);
                break;
            case BUS_STOP:
                bindBusStopData(marker, view);
                break;
            case LIBRARY:
                bindLibraryData(marker, view);
                break;
            default:
                bindDefaultData(marker, view);
        }
    }

    private void bindLibraryData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.tv_title);
        TextView description = view.findViewById(R.id.tv_description);
        TextView name = view.findViewById(R.id.tv_name);
        TextView adress = view.findViewById(R.id.tv_adress);
        TextView id = view.findViewById(R.id.tv_id);

        title.setText(marker.getTitle());
        description.setText(
                view.getContext().getString(
                        R.string.coords_format,
                        marker.getPosition().getLatitude(),
                        marker.getPosition().getLongitude()
                )
        );
        GeoJsonLibrary.Feature data = (GeoJsonLibrary.Feature) marker.getRelatedObject();
        name.setText(insertLineBreaksSmart(data.getProperties().getDocumentname(), 30));
        adress.setText(insertLineBreaksSmart(data.getProperties().getAddress(), 30));
        id.setText(view.getContext().getString(R.string.bike_parking_id, Integer.toString(data.id)));
    }

    private void bindBikeParkingData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.tv_title);
        TextView description = view.findViewById(R.id.tv_description);
        TextView space_n = view.findViewById(R.id.tv_space_n);
        TextView id = view.findViewById(R.id.tv_id);

        title.setText(marker.getTitle());
        description.setText(
                view.getContext().getString(
                        R.string.coords_format,
                        marker.getPosition().getLatitude(),
                        marker.getPosition().getLongitude()
                )
        );
        GeoJsonParking.Feature data = (GeoJsonParking.Feature) marker.getRelatedObject();
        space_n.setText(String.format(Locale.getDefault(), "%.0f", data.getProperties().getSum()));
        id.setText(view.getContext().getString(R.string.bike_parking_id, Integer.toString(data.getProperties().getCLUSTER_ID())));
    }

    private void bindBusStopData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.tv_title);
        TextView description = view.findViewById(R.id.tv_description);
        TextView stop_name = view.findViewById(R.id.tv_stop_name);
        TextView id = view.findViewById(R.id.tv_id);

        title.setText(marker.getTitle());
        description.setText(
                view.getContext().getString(
                        R.string.coords_format,
                        marker.getPosition().getLatitude(),
                        marker.getPosition().getLongitude()
                )
        );
        GeoJsonStop.Feature data = (GeoJsonStop.Feature) marker.getRelatedObject();
        stop_name.setText(data.getProperties().getStop_name());
        id.setText(view.getContext().getString(R.string.bike_parking_id, Integer.toString(data.getProperties().getStop_id())));
    }

    private void bindDefaultData(Marker marker, View view) {
        TextView title = view.findViewById(R.id.info_title);
        TextView snippet = view.findViewById(R.id.info_snippet);

        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
    }

    private String insertLineBreaksSmart(String text, int maxLineLength) {
        StringBuilder result = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLineLength, text.length());

            if (end < text.length() && text.charAt(end) != ' ') {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            result.append(text, start, end).append("\n");
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
        return result.toString().trim();
    }

    @Override
    public void onClose() {
        // Limpieza opcional
    }
}

