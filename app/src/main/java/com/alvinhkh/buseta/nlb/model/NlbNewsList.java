package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NlbNewsList {

    @SerializedName("newses")
    public List<NlbNews> newses;

    public String toString() {
        return "NlbNewsList{newses=" + newses + "}";
    }

}
