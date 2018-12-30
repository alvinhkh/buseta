package com.alvinhkh.buseta.lwb

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.lwb.model.LwbRouteStop
import com.alvinhkh.buseta.lwb.model.network.LwbRouteBoundRes
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface LwbService {

    @GET("/ajax/getRoute_info.php")
    fun routeBound(
            @Query("field9") routeNo: String,
            @Query("t") t: Double?
    ): Call<LwbRouteBoundRes>

    @FormUrlEncoded
    @POST("http://www.kmb.hk/ajax/getRouteMapByBusno.php")
    fun routeMap(
            @Field("bn") routeNo: String,
            @Field("dir") bound: String,
            @Field("ST") serviceType: String
    ): Call<List<LwbRouteStop>>

    companion object {

        val gson: Gson = GsonBuilder()
                .serializeNulls()
                .create()

        val retrofit: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://www.lwb.hk")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }

}
