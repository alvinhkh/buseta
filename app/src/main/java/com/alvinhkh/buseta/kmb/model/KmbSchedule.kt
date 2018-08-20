package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbSchedule(
    @field:SerializedName("DayType") var dayType: String? = null,  // MF=1-5,S=6,H=7&Holiday,D=Everyday
    @field:SerializedName("BoundTime1") var boundTime1: String? = null,
    @field:SerializedName("ServiceType_Eng") var serviceTypeEn: String? = null,
    @field:SerializedName("BoundText1") var boundText1: String? = null,
    @field:SerializedName("Origin_Eng") var originEn: String? = null,
    @field:SerializedName("ServiceType") var serviceType: String? = null,
    @field:SerializedName("Destination_Chi") var destinationTc: String? = null,
    @field:SerializedName("OrderSeq") var orderSequence: String? = null,
    @field:SerializedName("Route") var routeNo: String? = null,
    @field:SerializedName("Destination_Eng") var destinationEn: String? = null,
    @field:SerializedName("BoundTime2") var boundTime2: String? = null,
    @field:SerializedName("Origin_Chi") var originTc: String? = null,
    @field:SerializedName("BoundText2") var boundText2: String? = null,
    @field:SerializedName("ServiceType_Chi") var serviceTypeTc: String? = null
)