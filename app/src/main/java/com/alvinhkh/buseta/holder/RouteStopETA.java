package com.alvinhkh.buseta.holder;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteStopETA implements Parcelable {

    public RouteStopETA() {
    }

    public int api_version = 1;

    @SerializedName("STOP_SEQ")
    public String seq = "";

    @SerializedName("ETA_TIME")
    public String etas = "";

    @SerializedName("ETA_EXPIRE")
    public String expires = "";

    public String wheelchair = "";

    @SerializedName("server_time")
    public String server_time = "";

    public String updated = "";

    // Parcelling
    /**
     * Constructs a RouteStopETA from a Parcel
     * @param p Source Parcel
     */
    public RouteStopETA(Parcel p) {
        this.api_version = p.readInt();
        this.seq = p.readString();
        this.etas = p.readString();
        this.expires = p.readString();
        this.wheelchair = p.readString();
        this.server_time = p.readString();
        this.updated = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(this.api_version);
        p.writeString(this.seq);
        p.writeString(this.etas);
        p.writeString(this.expires);
        p.writeString(this.wheelchair);
        p.writeString(this.server_time);
        p.writeString(this.updated);
    }
    // Method to recreate a RouteStopETA from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteStopETA createFromParcel(Parcel in) {
            return new RouteStopETA(in);
        }

        public RouteStopETA[] newArray(int size) {
            return new RouteStopETA[size];
        }
    };
}
