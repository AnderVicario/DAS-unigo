package com.unigo.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unigo.R;
import com.unigo.utils.ForecastDay;
import com.unigo.utils.SvgUtil;
import com.unigo.utils.TranslatorUtil;

import java.util.List;
import java.util.Locale;

public class ForecastAdapter
        extends RecyclerView.Adapter<ForecastAdapter.VH> {
    private List<ForecastDay> list;
    private Context ctx;

    public ForecastAdapter(List<ForecastDay> list, Context ctx) {
        this.list = list; this.ctx = ctx;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv; TextView tvDay, tvDesc, tvTempMin, tvTempMax;
        VH(View v) {
            super(v);
            iv = v.findViewById(R.id.ivIcon);
            tvDay = v.findViewById(R.id.tvDay);
            tvDesc = v.findViewById(R.id.tvDesc);
            tvTempMin = v.findViewById(R.id.tvTempMin);
            tvTempMax = v.findViewById(R.id.tvTempMax);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_forecast, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        ForecastDay f = list.get(pos);
        holder.tvDay.setText(f.getDayLabel(ctx));
        // Texto según idioma
        SharedPreferences prefs =
                ctx.getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE);
        String idioma = prefs.getString("idioma",
                Locale.getDefault().getLanguage());
        String origen = "eu".equals(idioma) ? f.descEu : f.descEs;

        if ("en".equals(idioma)) {
            TranslatorUtil.translateIfNeeded(origen, "en", translated -> {
                ((Activity) ctx).runOnUiThread(() ->
                        holder.tvDesc.setText(translated)
                );
            });
        } else {
            // Español o euskera: texto directo
            holder.tvDesc.setText(origen);
        }

        if (f.iconUrl != null) {
            SvgUtil.loadSvgIntoImageView((Activity)ctx, f.iconUrl, holder.iv);
        }
        // Formatea con el símbolo °C
        holder.tvTempMin.setText(f.tempMin + "°C");
        holder.tvTempMax.setText(f.tempMax + "°C");

    }

    @Override public int getItemCount() { return list.size(); }
}
