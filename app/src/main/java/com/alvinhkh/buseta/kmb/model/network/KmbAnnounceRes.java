package com.alvinhkh.buseta.kmb.model.network;


import com.alvinhkh.buseta.kmb.model.KmbAnnounce;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class KmbAnnounceRes {

    @SerializedName("data")
    public ArrayList<KmbAnnounce> data;

    @SerializedName("result")
    public Boolean result;

    public String toString() {
        return "KmbAnnounceRes{data=" + this.data + ", result=" + this.result + '}';
    }

}
