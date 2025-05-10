package com.unigo.models;

public class Transport {
    private String mode;
    private String duration;
    private String distance;

    public Transport(String mode, String duration, String distance) {
        this.mode = mode;
        this.duration = duration;
        this.distance = distance;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
