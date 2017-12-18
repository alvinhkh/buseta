package com.alvinhkh.buseta.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FollowStop implements Parcelable {

    public String _id;

    public String code;

    public String companyCode;

    public String direction;

    public String etaGet;

    public String latitude;

    public String locationEnd;

    public String locationStart;

    public String longitude;

    public String name;

    public Integer order = 0;

    public String route;

    public String routeId;

    public String sequence;

    public Long updatedAt = 0L;

    public FollowStop() {
    }

    public String toString() {
        return "FollowStop{_id=" + this._id + ", code=" + this.code + ", companyCode=" + this.companyCode
                + ", direction=" + this.direction + ", etaGet=" + this.etaGet
                + ", latitude=" + this.latitude + ", locationEnd=" + this.locationEnd
                + ", locationStart=" + this.locationStart + ", longitude=" + this.longitude
                + ", name=" + this.name + ", order=" + this.order + ", route=" + this.route
                + ", routeId=" + this.routeId + ", sequence=" + this.sequence + ", updatedAt=" + this.updatedAt + "}";
    }

    /**
     * Constructs a FollowStop from a Parcel
     * @param p Source Parcel
     */
    public FollowStop(Parcel p) {
        this._id = p.readString();
        this.code = p.readString();
        this.companyCode = p.readString();
        this.direction = p.readString();
        this.etaGet = p.readString();
        this.latitude = p.readString();
        this.locationEnd = p.readString();
        this.locationStart = p.readString();
        this.longitude = p.readString();
        this.name = p.readString();
        this.order = p.readInt();
        this.route = p.readString();
        this.routeId = p.readString();
        this.sequence = p.readString();
        this.updatedAt = p.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //The parcelable object has to be the first one
        dest.writeString(this._id);
        dest.writeString(this.code);
        dest.writeString(this.companyCode);
        dest.writeString(this.direction);
        dest.writeString(this.etaGet);
        dest.writeString(this.latitude);
        dest.writeString(this.locationEnd);
        dest.writeString(this.locationStart);
        dest.writeString(this.longitude);
        dest.writeString(this.name);
        dest.writeInt(this.order);
        dest.writeString(this.route);
        dest.writeString(this.routeId);
        dest.writeString(this.sequence);
        dest.writeLong(this.updatedAt);
    }

    /*
     * Method to recreate class object from a Parcel
     */
    public static final Creator CREATOR = new Creator() {
        public FollowStop createFromParcel(Parcel in) {
            return new FollowStop(in);
        }

        public FollowStop[] newArray(int size) {
            return new FollowStop[size];
        }
    };
}