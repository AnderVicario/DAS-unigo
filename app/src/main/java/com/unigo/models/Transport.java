package com.unigo.models;

import com.unigo.utils.RouteCalculator;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class Transport {
    private String mode;
    private int duration;
    private double distance;
    private List<GeoPoint> routePoints;

    public Transport(String mode, double distance, int duration, List<GeoPoint> routePoints) {
        this.mode = mode;
        this.distance = distance;
        this.duration = duration;
        this.routePoints = routePoints;
    }

    public String getMode() {
        return mode;
    }

    public String getFormattedDistance() {
        return RouteCalculator.formatDistance(this.distance);
    }

    public String getFormattedDuration() {
        return RouteCalculator.formatDuration(this.duration);
    }

    public List<GeoPoint> getRoutePoints() { return routePoints; }

}
