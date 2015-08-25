package com.alvinhkh.buseta.holder;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteNews implements Parcelable {

    public RouteNews() {
    }

    public String title = null;

    public String link = null;

    // Parcelling
    /**
     * Constructs a RouteNews from a Parcel
     * @param p Source Parcel
     */
    public RouteNews(Parcel p) {
        this.title = p.readString();
        this.link = p.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(this.title);
        p.writeString(this.link);
    }
    // Method to recreate a RouteNews from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteNews createFromParcel(Parcel in) {
            return new RouteNews(in);
        }

        public RouteNews[] newArray(int size) {
            return new RouteNews[size];
        }
    };

}
