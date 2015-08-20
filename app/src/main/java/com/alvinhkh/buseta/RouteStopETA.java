package com.alvinhkh.buseta;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteStopETA implements Parcelable {

    public RouteStopETA() {
    }

    @SerializedName("STOP_SEQ")
    public String seq;

    //@SerializedName("STOP_NAME_CHI")
    //public String name_tc;

    @SerializedName("ETA_TIME")
    public String etas;

    @SerializedName("ETA_EXPIRE")
    public String expires;

    @SerializedName("server_time")
    public String server_time;

    // Parcelling
    /**
     * Constructs a RouteStopETA from a Parcel
     * @param p Source Parcel
     */
    public RouteStopETA(Parcel p) {
        this.seq = p.readString();
        this.etas = p.readString();
        this.expires = p.readString();
        this.server_time = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(this.seq);
        p.writeString(this.etas);
        p.writeString(this.expires);
        p.writeString(this.server_time);
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
