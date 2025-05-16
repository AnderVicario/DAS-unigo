package com.unigo.models;

import com.unigo.utils.RouteCalculator;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class Transport {
    private TransportMode mode;
    private int duration;
    private double distance;
    private List<GeoPoint> routePoints;
    private int stop1;
    private int stop2;
    private int stop3;
    private int stop4;
    private String route1;
    private String route2;

    public Transport(TransportMode mode, double distance, int duration, List<GeoPoint> routePoints) {
        this.mode = mode;
        this.distance = distance;
        this.duration = duration;
        this.routePoints = routePoints;
    }

    public enum TransportMode {
        BUS_DIRECT,
        BUS_TDIRECT,
        BUS_TWALK,
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

    public int getDuration() {
        return duration;
    }

    public double getDistance() {
        return distance;
    }

    public int getStop1() {
        return stop1;
    }

    public void setStop1(int stop1) {
        this.stop1 = stop1;
    }

    public int getStop2() {
        return stop2;
    }

    public void setStop2(int stop2) {
        this.stop2 = stop2;
    }

    public int getStop3() {
        return stop3;
    }

    public void setStop3(int stop3) {
        this.stop3 = stop3;
    }

    public int getStop4() {
        return stop4;
    }

    public void setStop4(int stop4) {
        this.stop4 = stop4;
    }

    public String getRoute1() {
        return route1;
    }

    public void setRoute1(String route1) {
        this.route1 = route1;
    }

    public String getRoute2() {
        return route2;
    }

    public void setRoute2(String route2) {
        this.route2 = route2;
    }
}
