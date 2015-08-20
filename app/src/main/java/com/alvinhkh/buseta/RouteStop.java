package com.alvinhkh.buseta;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RouteStop implements Parcelable {

    public RouteStop() {
    }

    @SerializedName("STOP_NAME_CHI")
    public String name_tc;

    @SerializedName("STOP_NAME_ENG")
    public String name_en;

    @SerializedName("STOP_CODE")
    public String code;

    public RouteStopETA eta;

    public Boolean eta_loading = false;

    public Boolean eta_fail = false;

    public String fare;

    // Parcelling
    /**
        * Constructs a RouteStop from a Parcel
        * @param p Source Parcel
        */
    public RouteStop(Parcel p) {
        this.eta = p.readParcelable(RouteStopETA.class.getClassLoader());
        this.name_tc = p.readString();
        this.name_en = p.readString();
        this.code = p.readString();
        this.eta_loading = p.readByte() == 1;
        this.eta_fail = p.readByte() == 1;
        this.fare = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        //The parcelable object has to be the first one
        p.writeParcelable(this.eta, flags);
        p.writeString(this.name_tc);
        p.writeString(this.name_en);
        p.writeString(this.code);
        p.writeByte((byte) (this.eta_loading ? 1 : 0));
        p.writeByte((byte) (this.eta_fail ? 1 : 0));
        p.writeString(this.fare);
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
