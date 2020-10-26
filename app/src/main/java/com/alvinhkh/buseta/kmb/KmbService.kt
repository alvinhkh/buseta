package com.alvinhkh.buseta.kmb

import com.alvinhkh.buseta.App.Companion.httpClient
import com.alvinhkh.buseta.App.Companion.httpClientUnsafe
import com.alvinhkh.buseta.kmb.model.KmbBBI2
import com.alvinhkh.buseta.kmb.model.KmbEtaRoutes
import com.alvinhkh.buseta.kmb.model.KmbRoutesInStop
import com.alvinhkh.buseta.kmb.model.network.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface KmbService {
    @GET("ajax/BBI/get_BBI2.php?&buscompany=null&jtSorting=sec_routeno%20ASC")
    fun bbi(@Query("routeno") routeno: String?, @Query("bound") bound: String?): Deferred<Response<KmbBBI2>>

    @GET("ajax/BBI/get_BBI2.php?&buscompany=null&jtSorting=sec_routeno%20ASC")
    fun bbi(@Query("routeno") routeno: String?, @Query("bound") bound: String?, @Query("interchangeType") interchangeType: String?): Deferred<Response<KmbBBI2>>

    @GET("Function/FunctionRequest.ashx?action=getRouteBound")
    fun routeBound(@Query("route") route: String?): Call<KmbRouteBoundRes>

    @GET("Function/FunctionRequest.ashx?action=getSpecialRoute")
    fun specialRoute(@Query("route") route: String?, @Query("bound") bound: String?): Call<KmbSpecialRouteRes>

    @GET("Function/FunctionRequest.ashx?action=getStops")
    fun stops(@Query("route") route: String?, @Query("bound") bound: String?, @Query("serviceType") serviceType: String?): Call<KmbStopsRes>

    @GET("Function/FunctionRequest.ashx?action=getRoutesInStop")
    fun routesInStop(@Query("bsiCode") bsiCode: String?): Call<KmbRoutesInStop>

    @GET("Function/FunctionRequest.ashx?action=getAnnounce")
    fun announce(@Query("route") route: String?, @Query("bound") bound: String?): Deferred<Response<KmbAnnounceRes>>

    @GET("Function/FunctionRequest.ashx?action=getschedule")
    fun schedule(@Query("route") route: String?, @Query("bound") bound: String?): Deferred<Response<KmbScheduleRes>>

    @GET("AnnouncementPicture.ashx")
    fun announcementPicture(@Query("url") url: String?): Deferred<Response<ResponseBody>>

    @FormUrlEncoded
    @POST("Function/FunctionRequest.ashx/?action=get_ETA")
    fun eta(@Query("lang") lang: String?,
            @Field("token") token: String?,
            @Field("t") t: String?): Call<KmbWebEtaRes>

    @Headers("Content-Type: application/json")
    @POST("?action=geteta")
    fun eta(@Body body: KmbEtaRequest): Call<List<KmbEtaRes>>

    @GET("GetData.ashx?type=ETA_R")
    fun etaRoutes(): Call<List<KmbEtaRoutes>>

    companion object {
        const val ANNOUNCEMENT_PICTURE = "http://search.kmb.hk/KMBWebSite/AnnouncementPicture.ashx?url="
        val gson: Gson = GsonBuilder()
                .serializeNulls()
                .create()
        val webCoroutine: Retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("http://www.kmb.hk/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()
        val webSearch: Retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://search.kmb.hk/KMBWebSite/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        val webSearchCoroutine: Retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("http://search.kmb.hk/KMBWebSite/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()
        val webSearchHtmlCoroutine: Retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("http://search.kmb.hk/KMBWebSite/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()
        val etav3: Retrofit = Retrofit.Builder()
                .client(httpClientUnsafe)
                .baseUrl("https://etav3.kmb.hk")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        val etadatafeed: Retrofit = Retrofit.Builder()
                .client(httpClient)
                .baseUrl("http://etadatafeed.kmb.hk:1933")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }
}