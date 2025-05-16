package com.unigo.utils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.unigo.R;

/**
 * Modelo de un día de previsión meteorológica.
 */
public class ForecastDay implements Parcelable {
    public String forecastDay;
    public String descEs, descEu;
    public String iconUrl;
    public String tempMin, tempMax;

    public ForecastDay(String forecastDay,
                       String descEs,
                       String descEu,
                       String iconUrl,
                       String tempMin,
                       String tempMax) {
        this.forecastDay = forecastDay;
        this.descEs = descEs;
        this.descEu = descEu;
        this.iconUrl = iconUrl;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
    }

    /**
     * Devuelve la etiqueta de día según el valor raw y los recursos de cadena.
     */
    public String getDayLabel(Context ctx) {
        switch (forecastDay) {
            case "today":
                return ctx.getString(R.string.day_today);
            case "tomorrow":
                return ctx.getString(R.string.day_tomorrow);
            case "next":
                return ctx.getString(R.string.day_next);
            default:
                return ctx.getString(R.string.day_unknown);
        }
    }

    // Parcelable implementation:
    protected ForecastDay(Parcel in) {
        // el orden debe coincidir con writeToParcel
        forecastDay = in.readString();
        descEs = in.readString();
        descEu = in.readString();
        iconUrl = in.readString();
        tempMin = in.readString();
        tempMax = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(forecastDay);
        dest.writeString(descEs);
        dest.writeString(descEu);
        dest.writeString(iconUrl);
        dest.writeString(tempMin);
        dest.writeString(tempMax);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ForecastDay> CREATOR = new Creator<ForecastDay>() {
        @Override
        public ForecastDay createFromParcel(Parcel in) {
            return new ForecastDay(in);
        }

        @Override
        public ForecastDay[] newArray(int size) {
            return new ForecastDay[size];
        }
    };
}
