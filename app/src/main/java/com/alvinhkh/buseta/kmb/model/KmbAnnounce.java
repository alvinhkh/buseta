package com.alvinhkh.buseta.kmb.model;

import com.google.gson.annotations.SerializedName;

public class KmbAnnounce {

  @SerializedName("krbpiid")
  public String krbpiid;

  @SerializedName("krbpiid_boundno")
  public String krbpiid_boundno;

  @SerializedName("krbpiid_routeno")
  public String krbpiid_routeno;

  @SerializedName("kpi_title")
  public String titleEn;

  @SerializedName("kpi_title_chi_s")
  public String titleSc;

  @SerializedName("kpi_title_chi")
  public String titleTc;

  @SerializedName("kpi_noticeimageurl")
  public String url;

  @SerializedName("kpi_referenceno")
  public String referenceNo;

  public String toString() {
    return "KmbAnnounce{krbpiid=" + this.krbpiid + ", krbpiid_boundno=" + this.krbpiid_boundno
        + ", krbpiid_routeno=" + this.krbpiid_routeno
        + ", titleEn=" + this.titleEn + ", titleSc=" + this.titleSc + ", titleTc=" + this.titleTc
        + ", url=" + this.url + ", referenceNo=" + this.referenceNo + '}';
  }

}
