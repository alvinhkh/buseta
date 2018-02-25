package com.alvinhkh.buseta.model

import android.os.Parcel
import android.os.Parcelable

data class Route(
        var code: String? = null,
        var companyCode: String? = null,
        var origin: String? = null,
        var destination: String? = null,
        var name: String? = null,
        var sequence: String? = null,
        var serviceType: String? = null,
        var description: String? = null,
        var isSpecial: Boolean? = false,
        var stopsStartSequence: Int? = 0,
        var infoKey: String? = null,
        var rdv: String? = null
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
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeString(companyCode)
        parcel.writeString(origin)
        parcel.writeString(destination)
        parcel.writeString(name)
        parcel.writeString(sequence)
        parcel.writeString(serviceType)
        parcel.writeString(description)
        parcel.writeValue(isSpecial)
        parcel.writeValue(stopsStartSequence)
        parcel.writeString(infoKey)
        parcel.writeString(rdv)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Route> {
        override fun createFromParcel(parcel: Parcel): Route {
            return Route(parcel)
        }

        override fun newArray(size: Int): Array<Route?> {
            return arrayOfNulls(size)
        }
    }
}
