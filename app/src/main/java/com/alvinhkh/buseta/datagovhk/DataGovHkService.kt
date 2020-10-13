package com.alvinhkh.buseta.datagovhk

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.datagovhk.model.*
import com.alvinhkh.buseta.mtr.model.MtrScheduleRes
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface DataGovHkService {

    @GET("td/routes-fares-xml/COMPANY_CODE.xml")
    fun tdCompanyCode(): Call<TdCompanyCode>

    @GET("td/routes-fares-xml/ROUTE_BUS.xml")
    fun tdRouteBus(): Call<TdRouteBus>

    @GET("td/routes-fares-xml/RSTOP_BUS.xml")
    fun tdRouteStop(): Call<TdRouteStop>

    @GET("mtr/getSchedule.php")
    fun getSchedule(@Query("line") line: String,
                    @Query("sta") sta: String,
                    @Query("lang") lang: String): Call<MtrScheduleRes>

    @GET("citybus-nwfb/company/{company_id}")
    fun nwstCompany(@Path("company_id") companyId: String): Call<NwstResponseList<NwstCompany>>

    @GET("citybus-nwfb/route/{company_id}")
    fun nwstRouteList(@Path("company_id") companyId: String): Call<NwstResponseList<NwstRoute>>

    @GET("citybus-nwfb/route/{company_id}/{route}")
    fun nwstRoute(@Path("company_id") companyId: String, @Path("route") route: String): Call<NwstResponse<NwstRoute>>

    @GET("citybus-nwfb/route-stop/{company_id}/{route}/{direction}")
    fun nwstRouteStop(@Path("company_id") companyId: String, @Path("route") route: String, @Path("direction") direction: String): Call<NwstResponseList<NwstRouteStop>>

    @GET("citybus-nwfb/stop/{stop_id}")
    fun nwstStop(@Path("stop_id") stopId: String): Call<NwstResponse<NwstStop>>

    @GET("citybus-nwfb/eta/{company_id}/{stop_id}/{route}")
    fun nwstETA(@Path("company_id") companyId: String, @Path("stop_id") stopId: String, @Path("route") route: String): Call<NwstResponseList<NwstEta>>

    @GET("td/en/specialtrafficnews.xml")
    fun trafficnewsTcAsync(): Deferred<Response<TdTrafficNewsV1>>

    companion object {

        val static: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://static.data.gov.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()

        val tdCoroutine: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://resource.data.one.gov.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .build()

        val transport: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://rt.data.gov.hk/v1/transport/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}
