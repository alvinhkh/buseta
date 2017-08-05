package com.alvinhkh.buseta.kmb.model;

import com.google.gson.annotations.SerializedName;

public class KmbRouteStop {

  @SerializedName("CName")
  public String nameTc;

  @SerializedName("Y")
  public String Y;

  @SerializedName("ELocation")
  public String locationEn;

  @SerializedName("X")
  public String X;

  @SerializedName("AirFare")
  public String airFare;

  @SerializedName("EName")
  public String nameEn;

  @SerializedName("SCName")
  public String nameSc;

  @SerializedName("ServiceType")
  public String serviceType;

  @SerializedName("CLocation")
  public String locationTc;

  @SerializedName("BSICode")
  public String bsiCode;

  @SerializedName("Seq")
  public String seq;

  @SerializedName("SCLocation")
  public String locationSc;

  @SerializedName("Direction")
  public String direction;

  @SerializedName("Bound")
  public String bound;

  @SerializedName("Route")
  public String route;
}
