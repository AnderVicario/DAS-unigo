package com.unigo.models;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonStop {
    public String type = "";
    public List<Feature> features = new ArrayList<>();;

    public static class Feature {
        public String type;
        public Geometry geometry;
        public Properties properties;
    }

    public static class Geometry {
        public String type;
        public List<Double> coordinates; // [lon, lat]
    }

    public static class Properties {
        public String name;
        public List<String> routes;
    }
}
