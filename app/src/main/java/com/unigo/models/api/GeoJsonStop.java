package com.unigo.models.api;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonStop {
    public String type = "";
    public List<Feature> features = new ArrayList<>();

    public static class Feature {
        public String type;
        public Geometry geometry;
        public Properties properties;

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Geometry {
        public String type;
        public List<Double> coordinates;
    }

    public static class Properties {
        public int stop_id;
        public String stop_name;

        public int getStop_id() {
            return stop_id;
        }

        public String getStop_name() {
            return stop_name;
        }
    }
}