package com.unigo.models.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TransfersResponse {
    @SerializedName("origin_stop")
    private String originStop;

    @SerializedName("direct_routes")
    private List<DirectRoute> directRoutes;

    @SerializedName("transfer_alternatives")
    private List<TransferAlternative> transferAlternatives;

    // Getters and setters (or make fields public if using direct access)
    public String getOriginStop() {
        return originStop;
    }

    public List<DirectRoute> getDirectRoutes() {
        return directRoutes;
    }

    public List<TransferAlternative> getTransferAlternatives() {
        return transferAlternatives;
    }

    public class TransferAlternative {
        @SerializedName("origin_route")
        private String originRoute;

        @SerializedName("get_off_at")
        private String getOffAt;

        @SerializedName("walk_distance_m")
        private int walkDistanceM;

        @SerializedName("transfer_stop")
        private String transferStop;

        @SerializedName("final_route")
        private String finalRoute;

        @SerializedName("bus_to_uni_distance_m")
        private int busToUniDistanceM;

        @SerializedName("total_cost")
        private double totalCost;

        // Getters and setters (or make fields public)
        public String getOriginRoute() {
            return originRoute;
        }

        public String getGetOffAt() {
            return getOffAt;
        }

        public int getWalkDistanceM() {
            return walkDistanceM;
        }

        public String getTransferStop() {
            return transferStop;
        }

        public String getFinalRoute() {
            return finalRoute;
        }

        public int getBusToUniDistanceM() {
            return busToUniDistanceM;
        }

        public double getTotalCost() {
            return totalCost;
        }
    }

    public class DirectRoute {
        private String route;

        @SerializedName("route_name")
        private String routeName;

        @SerializedName("final_stop")
        private String finalStop;;

        @SerializedName("distance_to_campus_m")
        private int distanceToCampusM;

        public String getRoute() {
            return route;
        }

        public String getRouteName() {
            return routeName;
        }

        public String getFinalStop() {
            return finalStop;
        }

        public int getDistanceToCampusM() {
            return distanceToCampusM;
        }
    }
}