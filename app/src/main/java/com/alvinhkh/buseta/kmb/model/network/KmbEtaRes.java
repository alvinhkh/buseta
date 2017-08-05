package com.alvinhkh.buseta.kmb.model.network;


import com.alvinhkh.buseta.kmb.model.KmbEta;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class KmbEtaRes {

  @SerializedName("responsecode")
  public Integer responsecode;

  @SerializedName("response")
  public ArrayList<KmbEta> etas;

  @SerializedName("generated")
  public Long generated;

  @SerializedName("updated")
  public Long updated;

  public String toString() {
    return "KmbEtaRes{responsecode=" + this.responsecode + ", response=" + this.etas
        + ", generated=" + this.generated + ", updated=" + this.updated + "}";
  }

}
