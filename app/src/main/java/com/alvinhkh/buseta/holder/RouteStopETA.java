package com.alvinhkh.buseta.holder;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.alvinhkh.buseta.provider.EtaTable;
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

    public String scheduled = "";

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
        this.scheduled = p.readString();
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
        p.writeString(this.scheduled);
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

    public static RouteStopETA create(Cursor cursor) {
        RouteStopETA obj = new RouteStopETA();
        obj.seq = getColumnString(cursor, EtaTable.COLUMN_STOP_SEQ);
        obj.etas = getColumnString(cursor, EtaTable.COLUMN_ETA_TIME);
        obj.scheduled = getColumnString(cursor, EtaTable.COLUMN_ETA_SCHEDULED);
        obj.wheelchair = getColumnString(cursor, EtaTable.COLUMN_ETA_WHEELCHAIR);
        obj.expires = getColumnString(cursor, EtaTable.COLUMN_ETA_EXPIRE);
        obj.server_time = getColumnString(cursor, EtaTable.COLUMN_SERVER_TIME);
        obj.updated = getColumnString(cursor, EtaTable.COLUMN_UPDATED);
        return obj;
    }

    private static String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

}
