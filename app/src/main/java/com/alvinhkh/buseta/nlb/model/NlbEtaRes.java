package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbEtaRes {

    @SerializedName("estimatedArrivalTime")
    public ETA estimatedArrivalTime;

    public class ETA {

        @SerializedName("html")
        public String html;

        public String toString() {
            return "{html=" + html + "}";
        }

    }

    public String toString() {
        return "NlbEtaRes{estimatedArrivalTime=" + estimatedArrivalTime + "}";
    }

}
