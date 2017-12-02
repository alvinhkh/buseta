package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbDatabaseVersion {

    @SerializedName("version")
    public String version;

    public String toString() {
        return "NlbDatabaseVersion{version=" + this.version + "}";
    }

}
