package com.alvinhkh.buseta.holder;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteStopContainer implements Parcelable {

    public RouteStop routeStop;

    public int position;

    public RouteStopContainer() {
    }

    public RouteStopContainer(int position, RouteStop routeStop) {
        this.position = position;
        this.routeStop = routeStop;
    }

    // Parcelling
    /**
     * Constructs a RouteStopContainer from a Parcel
     * @param p Source Parcel
     */
    public RouteStopContainer(Parcel p) {
        this.routeStop = p.readParcelable(RouteStop.class.getClassLoader());
        this.position = p.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        //The parcelable object has to be the first one
        p.writeParcelable(this.routeStop, flags);
        p.writeInt(this.position);
    }
    // Method to recreate a RouteStopContainer from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RouteStopContainer createFromParcel(Parcel in) {
            return new RouteStopContainer(in);
        }

        public RouteStopContainer[] newArray(int size) {
            return new RouteStopContainer[size];
        }
    };

}
