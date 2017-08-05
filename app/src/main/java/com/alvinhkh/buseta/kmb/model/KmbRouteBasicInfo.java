package com.alvinhkh.buseta.kmb.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class KmbRouteBasicInfo {

  @SerializedName("Racecourse")
  public String racecourse;

  @SerializedName("DestEName")
  public String destinationEn;

  @SerializedName("OriCName")
  public String originTc;

  @SerializedName("ServiceTypeENG")
  public String serviceTypeEn;

  @SerializedName("DestCName")
  public String destinationTc;

  @Nullable
  @SerializedName("BusType")
  public String busType;

  @SerializedName("Airport")
  public String airport;

  @SerializedName("ServiceTypeTC")
  public String serviceTypeTc;

  @SerializedName("Overnight")
  public String overnight;

  @SerializedName("ServiceTypeSC")
  public String serviceTypeSc;

  @SerializedName("OriSCName")
  public String originSc;

  @SerializedName("DestSCName")
  public String destinationSc;

  @SerializedName("Special")
  public String special;

  @SerializedName("OriEName")
  public String originEn;
}
