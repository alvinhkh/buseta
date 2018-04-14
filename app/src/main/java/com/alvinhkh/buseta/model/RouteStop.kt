package com.alvinhkh.buseta.model

import android.os.Parcel
import android.os.Parcelable

data class RouteStop(
        var code: String? = null,
        var companyCode: String? = null,
        var destination: String? = null,
        var direction: String? = null,
        var etaGet: String? = null,
        var fare: String? = null,
        var fareHoliday: String? = null,
        var fareChild: String? = null,
        var fareSenior: String? = null,
        var imageUrl: String? = null,
        var latitude: String? = null,
        var location: String? = null,
        var longitude: String? = null,
        var name: String? = null,
        var origin: String? = null,
        var sequence: String? = null,
        var route: String? = null,
        var routeId: String? = null,
        var description: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeString(companyCode)
        parcel.writeString(destination)
        parcel.writeString(direction)
        parcel.writeString(etaGet)
        parcel.writeString(fare)
        parcel.writeString(fareHoliday)
        parcel.writeString(fareChild)
        parcel.writeString(fareSenior)
        parcel.writeString(imageUrl)
        parcel.writeString(latitude)
        parcel.writeString(location)
        parcel.writeString(longitude)
        parcel.writeString(name)
        parcel.writeString(origin)
        parcel.writeString(sequence)
        parcel.writeString(route)
        parcel.writeString(routeId)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RouteStop> {
        override fun createFromParcel(parcel: Parcel): RouteStop {
            return RouteStop(parcel)
        }

        override fun newArray(size: Int): Array<RouteStop?> {
            return arrayOfNulls(size)
        }
    }
}
