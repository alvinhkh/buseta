package com.alvinhkh.buseta.datagovhk

import com.alvinhkh.buseta.App
import com.alvinhkh.buseta.datagovhk.model.TdCompanyCode
import com.alvinhkh.buseta.datagovhk.model.TdRouteBus
import com.alvinhkh.buseta.datagovhk.model.TdRouteStop

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET


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

    companion object {

        val resource = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://resource.data.one.gov.hk/")
                .build()

        val static = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("http://static.data.gov.hk/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
    }
}
