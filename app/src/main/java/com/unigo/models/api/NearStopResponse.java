package com.unigo.models.api;

import java.util.List;

public class NearStopResponse {
    public String stop_name;
    public double distance_m;
    public List<String> routes;
    public boolean is_university_route;

    public String getStop_name() {
        return stop_name;
    }

    public double getDistance_m() {
        return distance_m;
    }

    public List<String> getRoutes() {
        return routes;
    }

    public boolean isIs_university_route() {
        return is_university_route;
    }
}