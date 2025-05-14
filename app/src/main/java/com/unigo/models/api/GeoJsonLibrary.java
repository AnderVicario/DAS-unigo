package com.unigo.models.api;

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

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Geometry {
        public String type;
        public List<Double> coordinates;
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

        public String getDocumentname() {
            return documentname;
        }

        public String getDocumentdescription() {
            return documentdescription;
        }

        public String getLibrarytimetable() {
            return librarytimetable;
        }

        public String getLibrarysummertimetable() {
            return librarysummertimetable;
        }

        public String getPlacename() {
            return placename;
        }

        public String getAddress() {
            return address;
        }

        public String getMunicipality() {
            return municipality;
        }

        public String getPostalcode() {
            return postalcode;
        }

        public String getTerritory() {
            return territory;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public String getFriendlyurl() {
            return friendlyurl;
        }

        public String getPhysicalurl() {
            return physicalurl;
        }

        public String getDataxml() {
            return dataxml;
        }

        public String getMetadataxml() {
            return metadataxml;
        }

        public String getZipfile() {
            return zipfile;
        }
    }
}