package com.alvinhkh.buseta.kmb.model.network;


import com.alvinhkh.buseta.kmb.model.KmbRouteBound;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class KmbRouteBoundRes {

    @SerializedName("data")
    public ArrayList<KmbRouteBound> data;

    @SerializedName("result")
    public Boolean result;

    public String toString() {
        return "KmbSpecialRouteRes{data=" + this.data + ", result=" + this.result + '}';
    }

}
