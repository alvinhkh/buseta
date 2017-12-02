package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbNewsRes {

    @SerializedName("news")
    public NlbNews news;

    public String toString() {
        return "NlbNewsRes{news=" + news + "}";
    }

}
