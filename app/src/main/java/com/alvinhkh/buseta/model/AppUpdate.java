package com.alvinhkh.buseta.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class AppUpdate implements Parcelable {

    @SerializedName("suggestion_check")
    public Boolean suggestion_check;

    @SerializedName("suggestion_database")
    public int suggestion_database;

    @SerializedName("version_code")
    public int version_code;

    @SerializedName("version_name")
    public String version_name;

    @SerializedName("content")
    public String content;

    @SerializedName("updated")
    public String updated;

    @SerializedName("url")
    public String url;

    @SerializedName("notify")
    public Boolean notify;

    @SerializedName("force")
    public Boolean force;

    @SerializedName("download")
    public Boolean download;

    // Parcelling
    /**
     * Constructs a AppUpdate from a Parcel
     * @param p Source Parcel
     */
    public AppUpdate(Parcel p) {
        this.suggestion_check = p.readByte() == 1;
        this.suggestion_database = p.readInt();
        this.version_code = p.readInt();
        this.version_name = p.readString();
        this.content = p.readString();
        this.updated = p.readString();
        this.url = p.readString();
        this.notify = p.readByte() == 1;
        this.force = p.readByte() == 1;
        this.download = p.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeByte((byte) (this.suggestion_check ? 1 : 0));
        p.writeInt(this.suggestion_database);
        p.writeInt(this.version_code);
        p.writeString(this.version_name);
        p.writeString(this.content);
        p.writeString(this.updated);
        p.writeString(this.url);
        p.writeByte((byte) (this.notify ? 1 : 0));
        p.writeByte((byte) (this.force ? 1 : 0));
        p.writeByte((byte) (this.download ? 1 : 0));
    }
    // Method to recreate a AppUpdate from a Parcel
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AppUpdate createFromParcel(Parcel in) {
            return new AppUpdate(in);
        }

        public AppUpdate[] newArray(int size) {
            return new AppUpdate[size];
        }
    };
}
