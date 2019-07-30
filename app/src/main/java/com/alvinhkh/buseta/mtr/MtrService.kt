package com.alvinhkh.buseta.mtr

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.mtr.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory

import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url


interface MtrService {

    @GET("reverse_proxy/NT_v2/NTAppModule/getSchedule.php")
    fun getSchedule(@Query("key") key: String,
                    @Query("line") line: String,
                    @Query("sta") sta: String,
                    @Query("lang") lang: String): Call<MtrScheduleRes>

    @Headers("Content-Type: application/json")
    @POST("getRouteStatusDetail")
    fun routeStatusDetail(@Body request: MtrBusRouteStatusRequest): Call<MtrBusRouteStatusRes>

    @Headers("Content-Type: application/json")
    @POST("getBusStopsDetail")
    fun busStopsDetail(@Body request: AESEtaBusStopsRequest): Call<AESEtaBusRes>

    @GET("alert/ryg_line_status.xml")
    fun lineStatus(): Deferred<Response<MtrLineStatusRes>>

    @GET("https://www.mtr.com.hk/mob/mtrmobile_versioncheck_v12_16.xml")
    fun zipResources(): Call<MtrMobileVersionCheck>

    @GET
    @Streaming
    fun downloadFile(@Url url: String): Call<ResponseBody>

    companion object {

        val gson: Gson = GsonBuilder()
                .serializeNulls()
                .create()

        val api: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://mavmapp1044.azurewebsites.net/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val aes: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://mavmwfs1004.azurewebsites.net/AES/AESService.svc/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val mtrBus: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://mavmwfs1004.azurewebsites.net/MTRBus/BusService.svc/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val tnews: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://tnews.mtr.com.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()

        val mob: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://www.mtr.com.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
    }
}
