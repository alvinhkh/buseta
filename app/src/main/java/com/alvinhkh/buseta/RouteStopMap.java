package com.alvinhkh.buseta;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteStopMap implements Parcelable {

    public RouteStopMap() {
    }

    @SerializedName("lat")
    public String lat;

    @SerializedName("lng")
    public String lng;

    @SerializedName("area")
    public String area;

    @SerializedName("subarea")
    public String subarea;

    @SerializedName("title_eng")
    public String name_en;

    @SerializedName("title_chi")
    public String name_tc;

    @SerializedName("address_eng")
    public String address_en;

    @SerializedName("address_chi")
    public String address_tc;

    @SerializedName("normal_fare")
    public String normal_fare;

    @SerializedName("air_cond_fare")
    public String air_cond_fare;

    @SerializedName("rel_bus")
    public String rel_bus;

    // Parcelling
    /**
     * Constructs a RouteStopMap from a Parcel
     * @param p Source Parcel
     */
    public RouteStopMap(Parcel p) {
        this.lat = p.readString();
        this.lng = p.readString();
        this.area = p.readString();
        this.subarea = p.readString();
        this.name_tc = p.readString();
        this.name_en = p.readString();
        this.address_en = p.readString();
        this.address_tc = p.readString();
        this.normal_fare = p.readString();
        this.air_cond_fare = p.readString();
        this.rel_bus = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        //The parcelable object has to be the first one
        p.writeString(this.name_tc);
        p.writeString(this.name_en);
        p.writeString(this.lat);
        p.writeString(this.lng);
        p.writeString(this.area);
        p.writeString(this.subarea);
        p.writeString(this.name_tc);
        p.writeString(this.name_en);
        p.writeString(this.address_en);
        p.writeString(this.address_tc);
        p.writeString(this.normal_fare);
        p.writeString(this.air_cond_fare);
        p.writeString(this.rel_bus);
    }
    // Method to recreate a RouteStopMap from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteStopMap createFromParcel(Parcel in) {
            return new RouteStopMap(in);
        }

        public RouteStopMap[] newArray(int size) {
            return new RouteStopMap[size];
        }
    };

}
