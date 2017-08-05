package com.alvinhkh.buseta.lwb.model;

import com.google.gson.annotations.SerializedName;

public class LwbRouteBound {

  @SerializedName("destination_chi")
  public String destination_tc;

  @SerializedName("destination")
  public String destination_en;

  @SerializedName("origin_chi")
  public String origin_tc;

  @SerializedName("origin")
  public String origin_en;

  public String route_no = "";

  public String route_bound = "";
}
