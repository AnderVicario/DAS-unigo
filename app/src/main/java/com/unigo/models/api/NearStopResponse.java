package com.unigo.models.api;

import java.util.List;

public class NearStopResponse {
    public String stop_name;
    public double distance_m;
    public List<String> routes;
    public boolean is_university_route;
}