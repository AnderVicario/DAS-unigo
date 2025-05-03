package com.unigo.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.unigo.R;

public class SnackbarUtils {

    private SnackbarUtils() {}

    public static void showSuccess(@NonNull View view, @NonNull Context context, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        configBaseSnackbar(snackbar, context, R.color.surface, null);
        snackbar.show();
    }

    public static void showWarning(@NonNull View view, @NonNull Context context, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        configBaseSnackbar(snackbar, context, R.color.warning, R.drawable.ic_warning);
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.onError));
        snackbar.show();
    }

    public static void showError(@NonNull View view, @NonNull Context context, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        configBaseSnackbar(snackbar, context, R.color.error, R.drawable.ic_error);
        snackbar.getView().setBackgroundTintList(
                ColorStateList.valueOf(Color.argb(200, 255, 0, 0))
        );
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.onError));
        snackbar.show();
    }

    private static void configBaseSnackbar(Snackbar snackbar, Context context, int backgroundColorRes, Integer iconRes) {
        View snackbarView = snackbar.getView();

        snackbarView.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, backgroundColorRes))
        );

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (iconRes != null){
            textView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            textView.setCompoundDrawablePadding(context.getResources().getDimensionPixelOffset(R.dimen.padding_icon));
            textView.setTextColor(Color.WHITE);
        }
        else {
            textView.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurface)));
        }

    }

}