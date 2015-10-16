package com.alvinhkh.buseta.holder;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteBound implements Parcelable {

    public RouteBound() {
    }

    public String route_no = "";

    public String route_bound = "";

    @SerializedName("destination_chi")
    public String destination_tc;

    @SerializedName("destination")
    public String destination_en;

    @SerializedName("origin_chi")
    public String origin_tc;

    @SerializedName("origin")
    public String origin_en;

    // Parcelling
    /**
     * Constructs a RouteBound from a Parcel
     * @param p Source Parcel
     */
    public RouteBound(Parcel p) {
        this.route_no = p.readString();
        this.route_bound = p.readString();
        this.destination_tc = p.readString();
        this.destination_en = p.readString();
        this.origin_tc = p.readString();
        this.origin_en = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(this.route_no);
        p.writeString(this.route_bound);
        p.writeString(this.destination_tc);
        p.writeString(this.destination_en);
        p.writeString(this.origin_tc);
        p.writeString(this.origin_en);
    }
    // Method to recreate a RouteBound from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteBound createFromParcel(Parcel in) {
            return new RouteBound(in);
        }

        public RouteBound[] newArray(int size) {
            return new RouteBound[size];
        }
    };

}
