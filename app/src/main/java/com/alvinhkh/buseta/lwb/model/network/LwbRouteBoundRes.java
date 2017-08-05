package com.alvinhkh.buseta.lwb.model.network;


import com.alvinhkh.buseta.lwb.model.LwbRouteBound;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class LwbRouteBoundRes {

    @SerializedName("valid")
    public Boolean valid;

    @SerializedName("message")
    public String message;

    @SerializedName("bus_arr")
    public ArrayList<LwbRouteBound> bus_arr;

    public String toString() {
        return "LwbRouteBoundRes{valid=" + this.valid + ", message=" + this.message +
                ", bus_arr=" + this.bus_arr + '}';
    }

}
