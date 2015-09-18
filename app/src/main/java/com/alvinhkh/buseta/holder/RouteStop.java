package com.alvinhkh.buseta.holder;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteStop implements Parcelable {

    public RouteStop() {
    }

    public RouteBound route_bound;

    public String stop_seq;

    @SerializedName("STOP_NAME_CHI")
    public String name_tc;

    @SerializedName("STOP_NAME_ENG")
    public String name_en;

    @SerializedName("STOP_CODE")
    public String code;

    public RouteStopETA eta;

    public Boolean eta_loading = false;

    public Boolean eta_fail = false;

    public Boolean favourite = false;

    public RouteStopMap details;

    // Parcelling
    /**
        * Constructs a RouteStop from a Parcel
        * @param p Source Parcel
        */
    public RouteStop(Parcel p) {
        this.route_bound = p.readParcelable(RouteBound.class.getClassLoader());
        this.eta = p.readParcelable(RouteStopETA.class.getClassLoader());
        this.details = p.readParcelable(RouteStopMap.class.getClassLoader());
        this.stop_seq = p.readString();
        this.name_tc = p.readString();
        this.name_en = p.readString();
        this.code = p.readString();
        this.eta_loading = p.readByte() == 1;
        this.eta_fail = p.readByte() == 1;
        this.favourite = p.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        //The parcelable object has to be the first one
        p.writeParcelable(this.route_bound, flags);
        p.writeParcelable(this.eta, flags);
        p.writeParcelable(this.details, flags);
        p.writeString(this.stop_seq);
        p.writeString(this.name_tc);
        p.writeString(this.name_en);
        p.writeString(this.code);
        p.writeByte((byte) (this.eta_loading ? 1 : 0));
        p.writeByte((byte) (this.eta_fail ? 1 : 0));
        p.writeByte((byte) (this.favourite ? 1 : 0));
    }
    // Method to recreate a RouteStop from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteStop createFromParcel(Parcel in) {
            return new RouteStop(in);
        }

        public RouteStop[] newArray(int size) {
            return new RouteStop[size];
        }
    };

}
