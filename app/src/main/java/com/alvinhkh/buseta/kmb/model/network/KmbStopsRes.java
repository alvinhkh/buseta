package com.alvinhkh.buseta.kmb.model.network;


import com.alvinhkh.buseta.kmb.model.KmbRouteBasicInfo;
import com.alvinhkh.buseta.kmb.model.KmbRouteStop;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class KmbStopsRes {

    @SerializedName("data")
    public Data data;

    @SerializedName("result")
    public Boolean result;

    public class Data {

        @SerializedName("basicInfo")
        public KmbRouteBasicInfo basicInfo;

        @SerializedName("routeStops")
        public ArrayList<KmbRouteStop> routeStops;

        @SerializedName("route")
        public Route route;

        public class Route {

            @SerializedName("lineGeometry")
            public String lineGeometry;

            @SerializedName("bound")
            public String bound;

            @SerializedName("serviceType")
            public String serviceType;

            @SerializedName("route")
            public String route;
        }
    }

    public String toString() {
        return "KmbSpecialRouteRes{data=" + this.data + ", result=" + this.result + '}';
    }

}
