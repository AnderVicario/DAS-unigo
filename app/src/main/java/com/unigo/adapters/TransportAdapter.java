package com.unigo.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.unigo.R;
import com.unigo.models.Transport;

import java.util.List;

public class TransportAdapter extends RecyclerView.Adapter<TransportAdapter.ViewHolder> {

    private List<Transport> options;
    private Context context;
    private OnTransportClickListener listener;


    public TransportAdapter(Context context, List<Transport> options) {
        this.context = context;
        this.options = options;
    }

    public void setOnTransportClickListener(OnTransportClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.route_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transport option = options.get(position);

        ColorStateList tintList = ColorStateList.valueOf(
                ContextCompat.getColor(this.context, R.color.onSurface)
        );
        switch (option.getMode()) {
            case BUS_DIRECT:
                holder.tvMode.setText(this.context.getString(R.string.autobus));
                holder.ivMode.setImageResource(R.drawable.bus_stop_marker);
                holder.ivMode.setImageTintList(tintList);
                holder.routeDetails.setVisibility(View.VISIBLE);

                // elementos invisibles
                holder.ivArrow3.setVisibility(View.GONE);
                holder.tvStop3.setVisibility(View.GONE);
                holder.tvRoute2.setVisibility(View.GONE);
                holder.tvStop4.setVisibility(View.GONE);
                holder.ivArrow4.setVisibility(View.GONE);
                holder.ivWalk3.setVisibility(View.GONE);

                // elementos visibles
                holder.tvStop1.setText(String.valueOf(option.getStop1()));
                holder.tvRoute1.setText(option.getRoute1());
                holder.tvStop2.setText(String.valueOf(option.getStop2()));

                break;
            case BUS_TDIRECT:
                holder.tvMode.setText(this.context.getString(R.string.autobus));
                holder.ivMode.setImageResource(R.drawable.bus_stop_marker);
                holder.ivMode.setImageTintList(tintList);
                holder.routeDetails.setVisibility(View.VISIBLE);

                // elementos invisibles
                holder.ivArrow3.setVisibility(View.GONE);
                holder.ivWalk2.setVisibility(View.GONE);

                // elementos visibles
                holder.tvStop1.setText(String.valueOf(option.getStop1()));
                holder.tvRoute1.setText(option.getRoute1());
                holder.tvStop2.setText(String.valueOf(option.getStop2()));
                holder.tvStop3.setText(String.valueOf(option.getStop3()));
                holder.tvRoute2.setText(option.getRoute2());
                holder.tvStop4.setText(String.valueOf(option.getStop4()));
                break;
            case BUS_TWALK:
                holder.tvMode.setText(this.context.getString(R.string.autobus));
                holder.ivMode.setImageResource(R.drawable.bus_stop_marker);
                holder.ivMode.setImageTintList(tintList);
                holder.routeDetails.setVisibility(View.VISIBLE);
                break;
            case BIKE:
                holder.tvMode.setText(this.context.getString(R.string.Bici));
                holder.ivMode.setImageResource(R.drawable.bike_parking_marker);
                holder.ivMode.setImageTintList(tintList);
                break;
            case FOOT:
                holder.tvMode.setText(this.context.getString(R.string.a_pie));
                holder.ivMode.setImageResource(R.drawable.foot_marker);
                holder.ivMode.setImageTintList(tintList);
                break;
            default:
                holder.ivMode.setImageResource(R.drawable.ic_warning);
                holder.ivMode.setImageTintList(tintList);
        }
        holder.tvDuration.setText(option.getFormattedDuration());
        holder.tvDistance.setText(option.getFormattedDistance());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransportClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMode, tvDuration, tvDistance;
        ImageView ivMode;
        LinearLayout routeDetails;

        // route_details
        ImageView ivWalk1, ivArrow1, ivArrow2, ivWalk2, ivArrow3, ivArrow4, ivWalk3;
        TextView tvStop1, tvRoute1, tvStop2, tvStop3, tvRoute2, tvStop4;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMode = itemView.findViewById(R.id.tv_mode);
            ivMode = itemView.findViewById(R.id.iv_mode);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            routeDetails = itemView.findViewById(R.id.route_details);

            // Inicialización de los elementos de route_details
            ivWalk1 = itemView.findViewById(R.id.iv_walk1);
            ivArrow1 = itemView.findViewById(R.id.iv_arrow1);
            tvStop1 = itemView.findViewById(R.id.tv_stop1);
            tvRoute1 = itemView.findViewById(R.id.tv_route1);
            tvStop2 = itemView.findViewById(R.id.tv_stop2);
            ivArrow2 = itemView.findViewById(R.id.iv_arrow2);
            ivWalk2 = itemView.findViewById(R.id.iv_walk2);
            ivArrow3 = itemView.findViewById(R.id.iv_arrow3);
            tvStop3 = itemView.findViewById(R.id.tv_stop3);
            tvRoute2 = itemView.findViewById(R.id.tv_route2);
            tvStop4 = itemView.findViewById(R.id.tv_stop4);
            ivArrow4 = itemView.findViewById(R.id.iv_arrow4);
            ivWalk3 = itemView.findViewById(R.id.iv_walk3);
        }
    }


    public interface OnTransportClickListener {
        void onTransportClick(Transport transport);
    }
}