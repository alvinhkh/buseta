package com.alvinhkh.buseta.nlb

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.nlb.model.NlbDatabase
import com.alvinhkh.buseta.nlb.model.NlbDatabaseVersion
import com.alvinhkh.buseta.nlb.model.NlbEtaRes
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest
import com.alvinhkh.buseta.nlb.model.NlbNewsList
import com.alvinhkh.buseta.nlb.model.NlbNewsListRequest
import com.alvinhkh.buseta.nlb.model.NlbNewsRequest
import com.alvinhkh.buseta.nlb.model.NlbNewsRes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory

import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface NlbService {

    @GET("app.php?action=getDatabase")
    fun database(): Call<NlbDatabase>

    @GET("app.php?action=getDatabaseVersion")
    fun databaseVersion(): Call<NlbDatabaseVersion>

    @GET("route.php?action=getDetail")
    fun detail(@Query("routeId") routeId: String): Call<ResponseBody>

    @POST("stop.php?action=estimatedArrivalTime")
    fun eta(@Body body: NlbEtaRequest): Call<NlbEtaRes>

    @POST("news.php?action=get")
    fun news(@Body body: NlbNewsRequest): Deferred<Response<NlbNewsRes>>

    @POST("news.php?action=list")
    fun newsList(@Body body: NlbNewsListRequest): Deferred<Response<NlbNewsList>>

    companion object {

        const val TIMETABLE_URL = "https://nlb.kcbh.com.hk:8443/api/passenger/route.php?action=getDetail&routeId="

        val gson: Gson = GsonBuilder()
                .serializeNulls()
                .create()

        val api: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://nlb.kcbh.com.hk:8443/api/passenger/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val apiCoroutine: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://nlb.kcbh.com.hk:8443/api/passenger/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()
    }

}
