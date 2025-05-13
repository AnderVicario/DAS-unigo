package com.unigo.models;

import java.util.List;

public class GeoJsonParking {
    public String type;
    public List<Feature> features;

    public static class Feature {
        public String type;
        public Geometry geometry;
        public Properties properties;
    }

    public static class Geometry {
        public String type;
        public List<Double> coordinates;
    }

    public static class Properties {
        public int PLAZAS;
        public String DENOMINACI;
    }
}