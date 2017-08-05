package com.alvinhkh.buseta.kmb.model.network;


import com.alvinhkh.buseta.kmb.model.KmbRoute;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class KmbSpecialRouteRes {

  @SerializedName("data")
  public Data data;

  @SerializedName("result")
  public Boolean result;

  public class Data {

    @SerializedName("routes")
    public ArrayList<KmbRoute> routes;

    @SerializedName("CountSpecal")
    public Integer countSpecial;
  }

  public String toString() {
    return "KmbSpecialRouteRes{data=" + this.data + ", result=" + this.result + '}';
  }

}
