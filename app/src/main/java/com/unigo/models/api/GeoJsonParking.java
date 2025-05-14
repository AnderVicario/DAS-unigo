package com.unigo.models.api;

import java.util.List;

public class GeoJsonParking {
    public String type;
    public List<Feature> features;

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
        public int CLUSTER_ID;
        public double sum;

        public double getSum() {
            return sum;
        }

        public int getCLUSTER_ID() {
            return CLUSTER_ID;
        }
    }
}