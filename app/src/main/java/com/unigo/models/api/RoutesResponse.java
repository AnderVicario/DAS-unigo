package com.unigo.models.api;

import java.util.List;

public class RoutesResponse {
    public List<RouteOption> options;

    public static class RouteOption {
        public String type; // "direct", "transfer_direct" o "transfer_walk"

        // Común
        public Integer from_stop;
        public Integer total_time;
        public Double total_distance;

        // Direct
        public String departure_time;
        public String arrival_time;
        public String route_id;
        public Integer to_stop;

        // Transfer
        public Leg first_leg;
        public Leg second_leg;
        public Integer wait_time;

        // Transfer_walk
        public Walk walk;
    }

    public static class Leg {
        public String trip_id;
        public String route;
        public String departure_time;
        public Integer arrival_stop;
        public String arrival_time;
    }

    public static class Walk {
        public Integer distance_m;
        public Integer time_sec;
        public Integer to_stop;
    }
}
