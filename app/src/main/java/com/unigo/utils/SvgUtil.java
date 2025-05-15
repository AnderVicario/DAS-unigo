package com.unigo.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SvgUtil {
    /**
     * Descarga un SVG y lo muestra en un ImageView.
     * Toda la red va en un Thread de fondo; la UI se actualiza con un Handler.
     */
    public static void loadSvgIntoImageView(Context ctx, String svgUrl, ImageView iv) {
        new Thread(() -> {
            try {
                // 1) Descarga el SVG
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(svgUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                InputStream is = conn.getInputStream();

                // 2) Parseo con AndroidSVG
                SVG svg = SVG.getFromInputStream(is);
                is.close();
                conn.disconnect();

                // 3) Convertir a PictureDrawable
                PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());

                // 4) Post al hilo principal
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Deshabilitar HW accel para SVG
                    iv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    iv.setImageDrawable(drawable);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Si falla, limpiar la imagen en UI thread
                new Handler(Looper.getMainLooper()).post(() -> iv.setImageDrawable(null));
            }
        }).start();
    }
}