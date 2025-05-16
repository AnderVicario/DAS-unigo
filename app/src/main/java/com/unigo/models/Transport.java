package com.unigo.models;

import com.unigo.utils.RouteCalculator;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class Transport {
    private TransportMode mode;
    private int duration;
    private double distance;
    private List<GeoPoint> routePoints;

    public Transport(TransportMode mode, double distance, int duration, List<GeoPoint> routePoints) {
        this.mode = mode;
        this.distance = distance;
        this.duration = duration;
        this.routePoints = routePoints;
    }

    public enum TransportMode {
        BUS,
        BIKE,
        FOOT;
    }

    public TransportMode getMode() {
        return this.mode;
    }

    public String getFormattedDistance() {
        return RouteCalculator.formatDistance(this.distance);
    }

    public String getFormattedDuration() {
        return RouteCalculator.formatDuration(this.duration);
    }

    public List<GeoPoint> getRoutePoints() { return routePoints; }

}
