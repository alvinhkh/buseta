package com.alvinhkh.buseta.kmb.model;

import com.google.gson.annotations.SerializedName;

public class KmbRouteBound {

  @SerializedName("SERVICE_TYPE")
  public Integer serviceType;

  @SerializedName("BOUND")
  public Integer bound;

  @SerializedName("ROUTE")
  public String route;
}
