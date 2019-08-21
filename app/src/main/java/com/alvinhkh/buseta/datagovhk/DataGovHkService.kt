package com.alvinhkh.buseta.datagovhk

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.datagovhk.model.*

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


interface DataGovHkService {

    @GET("mtr/data/mtr_bus_routes.csv")
    fun mtrBusRoutes(): Call<ResponseBody>

    @GET("mtr/data/mtr_bus_stops.csv")
    fun mtrBusStops(): Call<ResponseBody>

    @GET("mtr/data/mtr_bus_fares.csv")
    fun mtrBusFares(): Call<ResponseBody>

    @GET("mtr/data/mtr_lines_and_stations.csv")
    fun mtrLinesAndStations(): Call<ResponseBody>

    @GET("td/routes-fares-xml/COMPANY_CODE.xml")
    fun tdCompanyCode(): Call<TdCompanyCode>

    @GET("td/routes-fares-xml/ROUTE_BUS.xml")
    fun tdRouteBus(): Call<TdRouteBus>

    @GET("td/routes-fares-xml/RSTOP_BUS.xml")
    fun tdRouteStop(): Call<TdRouteStop>

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

    companion object {

        val resource: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://resource.data.one.gov.hk/")
                .build()

        val static: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://static.data.gov.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()

        val transport: Retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://rt.data.gov.hk/v1/transport/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}
