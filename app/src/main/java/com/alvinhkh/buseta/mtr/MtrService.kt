package com.alvinhkh.buseta.mtr

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.mtr.model.AESEtaBusRes
import com.alvinhkh.buseta.mtr.model.AESEtaBusStopsRequest
import com.alvinhkh.buseta.mtr.model.MtrLineStatusRes
import com.alvinhkh.buseta.mtr.model.MtrMobileVersionCheck
import com.alvinhkh.buseta.mtr.model.MtrScheduleRes
import com.google.gson.GsonBuilder

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
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
                    @Query("lang") lang: String): Observable<MtrScheduleRes>

    @Headers("Content-Type: application/json")
    @POST("getBusStopsDetail")
    fun getBusStopsDetail(@Body request: AESEtaBusStopsRequest): Observable<AESEtaBusRes>

    @GET("alert/ryg_line_status.xml")
    fun lineStatus(): Observable<MtrLineStatusRes>

    @GET("https://www.mtr.com.hk/mob/mtrmobile_versioncheck_v12_10_2.xml")
    fun zipResources(): Call<MtrMobileVersionCheck>

    @GET
    @Streaming
    fun downloadFile(@Url url: String): Call<ResponseBody>

    companion object {

        val gson = GsonBuilder()
                .serializeNulls()
                .create()

        val api = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://mavmapp1044.azurewebsites.net/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        val aes = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://mavmwfs1004.azurewebsites.net/AES/AESService.svc/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        val tnews = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://tnews.mtr.com.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        val mob = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://www.mtr.com.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
    }
}
