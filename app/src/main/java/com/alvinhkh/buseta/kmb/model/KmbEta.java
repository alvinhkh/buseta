package com.alvinhkh.buseta.kmb.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class KmbEta {

  @Nullable
  @SerializedName("w")
  public String wheelchair;

  @Nullable
  @SerializedName("ex")
  public String expire;

  @Nullable
  @SerializedName("eot")
  public String eot;

  @Nullable
  @SerializedName("t")
  public String time;

  @Nullable
  @SerializedName("ei")
  public String schedule;

  @Nullable
  @SerializedName("bus_service_type")
  public String serviceType;

  @Nullable
  @SerializedName("ol")
  public String ol;

  @Nullable
  @SerializedName("wifi")
  public String wifi;

  public String toString() {
    return "KmbEta{wheelchair=" + this.wheelchair + ", expire=" + this.expire + ", eot=" + this.eot +
        ", timeText=" + this.time + ", schedule=" + this.schedule +
        ", serviceType=" + this.serviceType + ", ol=" + this.ol + ", wifi=" + this.wifi + "}";
  }
}
