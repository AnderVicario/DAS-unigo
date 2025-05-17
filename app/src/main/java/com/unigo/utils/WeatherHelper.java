package com.unigo.utils;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.widget.TextView;

import com.unigo.R;
import com.unigo.ui.MapActivity;
import com.unigo.utils.SvgUtil;
import com.unigo.utils.TranslatorUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherHelper {
    private final Activity activity;

    public WeatherHelper(Activity activity) {
        this.activity = activity;
    }

    public void fetchWeather(TextView tvTemp, TextView tvHumidity) {
        new Thread(() -> {
            try {
                // 1. Hora actual UTC truncada
                Instant nowHour = Instant.now()
                        .truncatedTo(ChronoUnit.HOURS);

                // 2. Construimos la URL
                String from = URLEncoder.encode(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                                .withZone(ZoneOffset.UTC)
                                .format(nowHour.minus(12, ChronoUnit.HOURS)),
                        "UTF-8"
                ); // Se acota la cantidad de datos filtrando a los datos de las ultimas 12 horas
                String to = URLEncoder.encode(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                                .withZone(ZoneOffset.UTC)
                                .format(nowHour.minus(1, ChronoUnit.HOURS)),
                        "UTF-8"
                ); // Se aplica -1 hora porque la api va con horario UTC
                String urlString = String.format(
                        "https://api.euskadi.eus/air-quality/measurements/hourly/"
                                + "stations/85/from/%s/to/%s?lang=SPANISH",
                        from, to
                );

                // 3. Petición HTTP
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                String json = new Scanner(conn.getInputStream())
                        .useDelimiter("\\A")
                        .next();
                conn.disconnect();

                // 4. Parseo JSON
                JSONObject root = new JSONObject("{\"data\":" + json + "}");
                JSONArray  arr  = root.getJSONArray("data");
                if (arr.length() == 0) throw new IllegalStateException("Sin datos");

                // 5. Buscamos el registro con date más cercano a nowHour, diff ≤ 3h
                DateTimeFormatter isoFmt = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .withZone(ZoneOffset.UTC);

                JSONObject bestRecord = null;
                long bestDiffHours = Long.MAX_VALUE;

                for (int i = 0; i < arr.length(); i++) {
                    String dateStr = arr.getJSONObject(i).getString("date");
                    Instant recInst = Instant.from(isoFmt.parse(dateStr));
                    long diffHours = Math.abs(ChronoUnit.HOURS.between(recInst, nowHour));

                    if (diffHours < bestDiffHours) {
                        bestDiffHours = diffHours;
                        bestRecord    = arr.getJSONObject(i);
                    }
                }

                // Si ni el más cercano está a ≤ 3 horas, fallback al primero
                if (bestRecord == null || bestDiffHours > 3) {
                    bestRecord = arr.getJSONObject(0);
                }

                // 6. Extraemos temperatura y humedad
                JSONArray measurements = bestRecord
                        .getJSONArray("station")
                        .getJSONObject(0)
                        .getJSONArray("measurements");

                String temp = "--", hum = "--";
                for (int j = 0; j < measurements.length(); j++) {
                    JSONObject m = measurements.getJSONObject(j);
                    String name = m.getString("name");
                    double val  = m.getDouble("value");
                    // 1) Solo asignar si val != 0
                    if (val == 0.0) continue;

                    switch (name) {
                        case "Tº":
                            // primera temperatura válida
                            if ("--".equals(temp)) {
                                temp = String.valueOf(val);
                            }
                            break;
                        case "H":
                            // primera humedad válida
                            if ("--".equals(hum)) {
                                hum = String.valueOf(val);
                            }
                            break;
                    }
                    // 2) Si ya tienes ambos, sales del bucle
                    if (!"--".equals(temp) && !"--".equals(hum)) break;
                }

                // 7. Variables finales para el lambda
                final String displayTemp = temp;
                final String displayHum  = hum;

                // 8. Actualizar UI
                activity.runOnUiThread(() -> {
                    tvTemp.setText(
                            String.format(Locale.getDefault(), "Tº: %sºC", displayTemp)
                    );
                    tvHumidity.setText(
                            String.format(Locale.getDefault(), "H: %s%%", displayHum)
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void fetchMeteo(TextView tvMeteoDesc, ImageView ivIcon) {
        new Thread(() -> {
            try {
                // 1. Conexión HTTP al XML
                URL url = new URL("https://opendata.euskadi.eus/contenidos/prevision_tiempo/" +
                        "met_forecast/opendata/met_forecast.xml");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                InputStream in = conn.getInputStream();

                // 2. Preparamos XmlPullParser
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(in, null);

                // 3. Variables de control
                boolean inTodayForecast = false;
                boolean inTargetCity = false;
                boolean inEs = false;
                boolean inEu = false;
                String descEs = null;
                String descEu = null;
                boolean inSymbolImage = false;
                String iconPath = null;

                // 4. Recorremos el documento
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String tag = parser.getName();
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if ("forecast".equals(tag)) {
                                String day = parser.getAttributeValue(null, "forecastDay");
                                inTodayForecast = "today".equals(day);
                            } else if (inTodayForecast && "cityForecastData".equals(tag)) {
                                String city = parser.getAttributeValue(null, "cityName");
                                inTargetCity = "Vitoria-Gasteiz".equals(city);
                            } else if (inTodayForecast && inTargetCity && "es".equals(tag)) {
                                inEs = true;
                            } else if (inTodayForecast && inTargetCity && "eu".equals(tag)) {
                                inEu = true;
                            } else if (inTodayForecast && inTargetCity && "symbolImage".equals(tag)) {
                                inSymbolImage = true;
                            }
                            break;

                        case XmlPullParser.TEXT:
                            if (inTodayForecast && inTargetCity && inEs) {
                                descEs = parser.getText().trim();
                            } else if (inTodayForecast && inTargetCity && inEu) {
                                descEu = parser.getText().trim();
                            } else if (inSymbolImage && iconPath == null) {
                                iconPath = parser.getText().trim();
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            if ("symbolImage".equals(tag)) {
                                inSymbolImage = false;
                            } else if ("cityForecastData".equals(tag) && inTargetCity) {
                                eventType = XmlPullParser.END_DOCUMENT;
                            } else if ("es".equals(tag)) {
                                inEs = false;
                            } else if ("eu".equals(tag)) {
                                inEu = false;
                            } else if ("forecast".equals(tag) && inTodayForecast) {
                                inTodayForecast = false;
                            }
                            break;
                    }
                    eventType = parser.next();
                }

                in.close();
                conn.disconnect();

                // 5. Mostrar en UI (fallback a guión si no existe)
                final String outEs = descEs != null ? descEs : "—";
                final String outEu = descEu != null ? descEu : "—";
                String svgUrl = null;
                if (iconPath != null) {
                    // ejemplo iconPath = "/.../images/14.gif"
                    Matcher m = Pattern.compile("(\\d+)\\.gif$").matcher(iconPath);
                    if (m.find()) {
                        String num = m.group(1);
                        svgUrl = "https://www.euskalmet.euskadi.eus/media/assets/icons/euskalmet/" +
                                "webmet00-i" + num + "d.svg";
                    }
                }
                final String iconUrl = svgUrl;
                activity.runOnUiThread(() -> {
                    // 5a. Cargamos icono si lo tenemos
                    if (iconUrl != null) {
                        SvgUtil.loadSvgIntoImageView(activity, iconUrl, ivIcon);
                    }
                    // 5b. Texto según preferencia e idioma
                    SharedPreferences prefs = activity.getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                    String idioma = prefs.getString("idioma", Locale.getDefault().getLanguage());
                    // Elegimos el origen
                    String textoOrigen = "eu".equals(idioma) ? outEu : outEs;

                    // Usamos TranslatorUtil para traducir si es inglés (OpenData solo devuelve estos datos en español y euskera, por lo tanto se utiliza una traducción externa)
                    TranslatorUtil.translateIfNeeded(
                            textoOrigen,
                            idioma,
                            translated -> activity.runOnUiThread(() -> tvMeteoDesc.setText(translated))
                    );
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> tvMeteoDesc.setText(activity.getString(R.string.no_data)));
            }
        }).start();
    }

    public void fetchForecastList(Consumer<List<ForecastDay>> callback) {
        new Thread(() -> {
            List<ForecastDay> list = new ArrayList<>();
            try {
                URL url = new URL("https://opendata.euskadi.eus/contenidos/prevision_tiempo/"
                        + "met_forecast/opendata/met_forecast.xml");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET"); conn.connect();
                InputStream in = conn.getInputStream();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(in, null);

                // Variables temporales
                boolean inForecast = false, inTargetCity = false, inTempMax = false, inTempMin = false;
                boolean inEs = false, inEu = false, inSymbol = false;
                String tempMaxStr = null, tempMinStr = null;
                String dayAttr = null, descEs = null, descEu = null, iconPath = null;

                int count = 0;
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT && count < 3) {
                    String tag = parser.getName();
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if ("forecast".equals(tag)) {
                                dayAttr = parser.getAttributeValue(null, "forecastDay");
                                inForecast = true;
                            } else if (inForecast && "cityForecastData".equals(tag)) {
                                String city = parser.getAttributeValue(null, "cityName");
                                inTargetCity = "Vitoria-Gasteiz".equals(city);
                            } else if (inForecast && inTargetCity) {
                                if ("es".equals(tag)) inEs = true;
                                if ("eu".equals(tag)) inEu = true;
                                if ("symbolImage".equals(tag)) inSymbol = true;
                                if ("tempMax".equals(tag)) inTempMax = true;
                                if ("tempMin".equals(tag)) inTempMin = true;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            if (inForecast && inTargetCity) {
                                if (inTempMax)    tempMaxStr = parser.getText().trim();
                                if (inTempMin) tempMinStr = parser.getText().trim();
                                if (inEs) descEs = parser.getText().trim();
                                if (inEu) descEu = parser.getText().trim();
                                if (inSymbol && iconPath == null) iconPath = parser.getText().trim();
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if ("tempMax".equals(tag)) inTempMax = false;
                            else if ("tempMin".equals(tag)) inTempMin = false;
                            else if ("symbolImage".equals(tag)) inSymbol = false;
                            else if ("es".equals(tag)) inEs = false;
                            else if ("eu".equals(tag)) inEu = false;
                            else if ("cityForecastData".equals(tag) && inTargetCity) {
                                // Al cerrar cada cityForecastData, hemos recogido un día completo
                                // Montamos URL SVG
                                String svgUrl = null;
                                if (iconPath != null) {
                                    Matcher m = Pattern.compile("(\\d+)\\.gif$").matcher(iconPath);
                                    if (m.find()) {
                                        String num = m.group(1);
                                        svgUrl = "https://www.euskalmet.euskadi.eus/media/assets/icons/"
                                                + "euskalmet/webmet00-i" + num + "d.svg";
                                    }
                                }
                                list.add(new ForecastDay(dayAttr,
                                        descEs != null ? descEs : "—",
                                        descEu != null ? descEu : "—",
                                        svgUrl, tempMinStr,
                                        tempMaxStr));
                                count++;
                                // Reseteamos flags/temp para el siguiente forecast
                                inForecast = inTargetCity = false;
                                descEs = descEu = iconPath = null;
                            } else if ("forecast".equals(tag)) {
                                inForecast = false;
                            }
                            break;
                    }
                    eventType = parser.next();
                }

                in.close(); conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Devolvemos al hilo UI
            activity.runOnUiThread(() -> callback.accept(list));
        }).start();
    }
}
