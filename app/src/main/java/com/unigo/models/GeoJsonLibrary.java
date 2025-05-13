package com.unigo.models;

import java.util.List;

public class GeoJsonLibrary {
    public String type;
    public String name;
    public List<Feature> features;

    public static class Feature {
        public int id;
        public String type;
        public Geometry geometry;
        public Properties properties;
    }

    public static class Geometry {
        public String type;
        public List<Double> coordinates; // [longitude, latitude]
    }

    public static class Properties {
        public String documentname;
        public String documentdescription;
        public String librarytimetable;
        public String librarysummertimetable;
        public String placename;
        public String address;
        public String municipality;
        public String postalcode;
        public String territory;
        public String phone;
        public String email;
        public String friendlyurl;
        public String physicalurl;
        public String dataxml;
        public String metadataxml;
        public String zipfile;
    }
}