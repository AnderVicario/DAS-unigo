package com.unigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        holder.tvMode.setText(option.getMode());
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

        public ViewHolder(View itemView) {
            super(itemView);
            tvMode = itemView.findViewById(R.id.tv_mode);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDistance = itemView.findViewById(R.id.tv_distance);
        }
    }

    public interface OnTransportClickListener {
        void onTransportClick(Transport transport);
    }
}