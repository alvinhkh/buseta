package com.alvinhkh.buseta.kmb.model;


import com.google.gson.annotations.SerializedName;

public class KmbEtaRoutes {

  @SerializedName("r_no")
  public String r_no;

  public String[] toArray() {
    return this.r_no.split(",");
  }

  public String toString() {
    return "KmbEtaRoutes{r_no=" + this.r_no + "}";
  }

}
