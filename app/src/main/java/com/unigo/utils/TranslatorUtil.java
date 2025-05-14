package com.unigo.utils;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class TranslatorUtil {

    public interface Callback {
        void onTranslated(String text);
    }

    /**
     * Traduce ES→EN online usando MyMemory. Si targetLang es "es" o "eu", devuelve sourceText sin cambios.
     */
    public static void translateIfNeeded(String sourceText,
                                               String targetLang,
                                               Callback callback) {
        if ("es".equals(targetLang) || "eu".equals(targetLang)) {
            callback.onTranslated(sourceText);
            return;
        }
        new Thread(() -> {
            try {
                // Construye la URL de MyMemory
                String encoded = URLEncoder.encode(sourceText, "UTF-8");
                String langpair = "es|en";
                String urlStr = "https://api.mymemory.translated.net/get?q="
                        + encoded + "&langpair=" + langpair;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                // Leer respuesta
                String json = new Scanner(conn.getInputStream())
                        .useDelimiter("\\A")
                        .next();
                conn.disconnect();

                // Parsear JSON mínimo
                JSONObject root = new JSONObject(json);
                JSONObject resp = root.getJSONObject("responseData");
                String translated = resp.getString("translatedText");

                callback.onTranslated(translated);

            } catch (Exception e) {
                // En caso de fallo, devolvemos el texto original
                callback.onTranslated(sourceText);
            }
        }).start();
    }
}


