package com.unigo.models.api;

import java.util.List;

public class NearStopResponse {
    public String type;

    // Para "direct"
    public Integer stop_id;
    public String route_id;

    // Para ambos
    public String departure;
    public String arrival;
    public int travel_time;

    // Solo para "transfer"
    public Integer initial_stop;
    public Integer transfer_stop;
    public String transfer_departure;
    public Double total_distance;

    // Getters
    public String getType() {
        return type;
    }

    public Integer getStop_id() {
        return stop_id;
    }

    public String getRoute_id() {
        return route_id;
    }

    public String getDeparture() {
        return departure;
    }

    public String getArrival() {
        return arrival;
    }

    public int getTravel_time() {
        return travel_time;
    }

    public Integer getInitial_stop() {
        return initial_stop;
    }

    public Integer getTransfer_stop() {
        return transfer_stop;
    }

    public String getTransfer_departure() {
        return transfer_departure;
    }

    public Double getTotal_distance() {
        return total_distance;
    }
}