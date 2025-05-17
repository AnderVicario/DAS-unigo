package com.unigo.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.unigo.R;
import com.unigo.models.api.GeoJsonLibrary;
import com.unigo.models.api.GeoJsonParking;
import com.unigo.models.api.GeoJsonStop;

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

    public MarkerType getType() {
        return markerType;
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
        TextView tvMoreInfo = view.findViewById(R.id.tv_more_info);
        tvMoreInfo.setOnClickListener(v -> showDetailDialog(marker));

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

    private void showDetailDialog(Marker marker) {
        GeoJsonLibrary.Feature data = (GeoJsonLibrary.Feature) marker.getRelatedObject();
        GeoJsonLibrary.Properties properties = data.getProperties();

        // Inflar el layout del diálogo
        View dialogView = LayoutInflater.from(mMapView.getContext())
                .inflate(R.layout.dialog_library_details, null);

        // Obtener referencias de las vistas
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvDescription = dialogView.findViewById(R.id.tv_dialog_description);
        TextView tvSchedule = dialogView.findViewById(R.id.tv_schedule);
        TextView tvSummerSchedule = dialogView.findViewById(R.id.tv_summer_schedule);
        TextView tvAddress = dialogView.findViewById(R.id.tv_address);
        TextView tvPhone = dialogView.findViewById(R.id.tv_phone);
        TextView tvEmail = dialogView.findViewById(R.id.tv_email);
        TextView tvWebsite = dialogView.findViewById(R.id.tv_website);
        TextView tvDataSource = dialogView.findViewById(R.id.tv_data_source);

        // Configurar los datos
        tvTitle.setText(properties.getDocumentname());
        tvDescription.setText(marker.getTitle());

        tvSchedule.setText(
                String.format("%s: %s",
                        mMapView.getContext().getString(R.string.schedule),
                        properties.getLibrarytimetable())
        );

        tvSummerSchedule.setText(
                String.format("%s: %s",
                        mMapView.getContext().getString(R.string.summer_schedule),
                        properties.getLibrarysummertimetable())
        );

        tvAddress.setText(
                String.format("%s: %s, %s (%s)",
                        mMapView.getContext().getString(R.string.address),
                        properties.getAddress(),
                        properties.getMunicipality(),
                        properties.getPostalcode())
        );

        tvPhone.setText(properties.getPhone());
        tvEmail.setText(properties.getEmail());
        tvWebsite.setText(properties.getFriendlyurl());
        tvDataSource.setText(properties.getPhysicalurl());

        // Crear y mostrar el diálogo
        new MaterialAlertDialogBuilder(mMapView.getContext(), R.style.RoundedDialog)
                .setView(dialogView)
                .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onClose() {}
}

