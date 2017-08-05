package com.alvinhkh.buseta.kmb.model;

import com.google.gson.annotations.SerializedName;

public class KmbRoute {
  @SerializedName("Destination_ENG")
  public String destinationEn;

  @SerializedName("Origin_ENG")
  public String originEn;

  @SerializedName("Origin_CHI")
  public String originTc;

  @SerializedName("To_saturday")
  public String toSaturday;

  @SerializedName("From_saturday")
  public String fromSaturday;

  @SerializedName("Desc_CHI")
  public String descTc;

  @SerializedName("Desc_ENG")
  public String descEn;

  @SerializedName("ServiceType")
  public String serviceType;

  @SerializedName("Route")
  public String route;

  @SerializedName("Destination_CHI")
  public String destinationTc;

  @SerializedName("Bound")
  public String bound;

  @SerializedName("From_weekday")
  public String fromWeekday;

  @SerializedName("From_holiday")
  public String fromHoliday;

  @SerializedName("To_weekday")
  public String toWeekday;

  @SerializedName("To_holiday")
  public String toHoliday;

}
