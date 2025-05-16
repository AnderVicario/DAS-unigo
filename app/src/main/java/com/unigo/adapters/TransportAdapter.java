package com.unigo.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
            case BUS:
                holder.tvMode.setText(this.context.getString(R.string.autobus));
                holder.imageView.setImageResource(R.drawable.bus_stop_marker);
                holder.imageView.setImageTintList(tintList);
                break;
            case BIKE:
                holder.tvMode.setText(this.context.getString(R.string.Bici));
                holder.imageView.setImageResource(R.drawable.bike_parking_marker);
                holder.imageView.setImageTintList(tintList);
                break;
            case FOOT:
                holder.tvMode.setText(this.context.getString(R.string.a_pie));
                holder.imageView.setImageResource(R.drawable.foot_marker);
                holder.imageView.setImageTintList(tintList);
                break;
            default:
                holder.imageView.setImageResource(R.drawable.ic_warning);
                holder.imageView.setImageTintList(tintList);
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
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMode = itemView.findViewById(R.id.tv_mode);
            imageView = itemView.findViewById(R.id.imageView);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDistance = itemView.findViewById(R.id.tv_distance);
        }
    }

    public interface OnTransportClickListener {
        void onTransportClick(Transport transport);
    }
}