package com.alvinhkh.buseta.nwst

import com.alvinhkh.buseta.App
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory

import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface NwstService {

    @GET("api6/getadv.php")
    fun adv(
            @Query(QUERY_LANGUAGE) language: String,
            @Query("width") width: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String,
            @Query(QUERY_TK) tk: String
    ): Call<String>

    @GET("push/pushtokenenable.php")
    fun pushTokenEnable(
            @Query(QUERY_TK) tk: String,
            @Query(QUERY_DEVICETOKEN) devicetoken: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_BM) bm: String,
            @Query(QUERY_MODE) mode: String,
            @Query(QUERY_DEVICETYPE) deviceType: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String
    ): Call<String>

    @GET("push/pushtoken.php")
    fun pushToken(
            @Query(QUERY_TK) tk: String,
            @Query(QUERY_DEVICETOKEN) devicetoken: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_BM) bm: String,
            @Query(QUERY_MODE) mode: String,
            @Query(QUERY_DEVICETYPE) deviceType: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String
    ): Call<String>

    @GET("api6/getroutelist2.php")
    fun routeList(
            @Query(QUERY_ROUTE_NO) routeNo: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_TK) tk: String
    ): Call<String>

    @GET("api6/getvariantlist.php")
    fun variantList(
            @Query(QUERY_ID) id: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_RDV) rdv: String,
            @Query(QUERY_BOUND) bound: String,
            @Query("syscode5") sysCode5: String,
            @Query("ui_v2") ui_v2: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query("appid") appId: String
    ): Call<String>

    @GET("api6/ppstoplist.php")
    fun ppStopList(
            @Query(QUERY_INFO) info: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query("syscode5") sysCode5: String,
            @Query("ui_v2") ui_v2: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query("appid") appId: String
    ): Call<String>

    @GET("api6/get_notice_4.php?ui_v2=Y")
    fun noticeListAsync(@QueryMap options: Map<String, String>): Deferred<Response<ResponseBody>>

    @GET("api6/getEta.php?mode=3eta")
    fun eta(
            @Query(QUERY_STOP_ID) stopId: String,
            @Query(QUERY_SERVICE_NO) serviceNo: String,
            @Query("removeRepeatedSuspend") removeRepeatedSuspend: String,
            @Query("interval") interval: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_BOUND) bound: String,
            @Query(QUERY_STOP_SEQ) stopSeq: String,
            @Query(QUERY_RDV) rdv: String,
            @Query("showtime") showtime: String,
            @Query("removeRepeatedSuspend") removeRepeatedSuspend2: String,
            @Query("syscode5") sysCode5: String,
            @Query("ui_v2") ui_v2: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(QUERY_VERSION2) version2: String,
            @Query("appid") appId: String
    ): Call<String>

    @GET("api6/getline_multi2.php?ui_v2=Y")
    fun lineMulti2(
            @Query(QUERY_R) r: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String
    ): Call<String>

    @GET("api6/gettimetable.php?ui_v2=Y")
    fun timetable(
            @Query(QUERY_RDV) rdv: String,
            @Query(QUERY_BOUND) bound: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String,
            @Query(QUERY_TK) tk: String
    ): Call<String>

    @GET("api6/getremark_2.php")
    fun remark(
            @Query(QUERY_RDV) rdv: String,
            @Query(QUERY_LANGUAGE) language: String,
            @Query(QUERY_SYSCODE) sysCode: String,
            @Query(QUERY_PLATFORM) platform: String,
            @Query(QUERY_VERSION) version: String,
            @Query(value = QUERY_SYSCODE2, encoded = true) sysCode2: String,
            @Query(QUERY_TK) tk: String
    ): Call<String>

    @GET("api6/getspecial.php?ui_v2=Y")
    fun specialAsync(@QueryMap options: Map<String, String>): Deferred<Response<ResponseBody>>

    companion object {

        const val APP_VERSION = "4.1.3"

        const val APP_VERSION2 = "66"

        const val DEVICETYPE = "android"

        const val LANGUAGE_TC = "0"

        const val PLATFORM = "android"

        const val QUERY_BM = "bm"

        const val QUERY_BOUND = "bound"

        const val QUERY_DEVICETOKEN = "devicetoken"

        const val QUERY_DEVICETYPE = "deviceType"

        const val QUERY_ID = "id"

        const val QUERY_INFO = "info"

        const val QUERY_LANGUAGE = "l"

        const val QUERY_MODE = "m"

        const val QUERY_PLATFORM = "p"

        const val QUERY_R = "r"

        const val QUERY_RDV = "rdv"

        const val QUERY_ROUTE = "route"

        const val QUERY_ROUTE_NO = "rno"

        const val QUERY_SERVICE_NO = "service_no"

        const val QUERY_STOP_ID = "stopid"

        const val QUERY_STOP_SEQ = "stopseq"

        const val QUERY_SYSCODE = "syscode"

        const val QUERY_SYSCODE2 = "syscode2"

        const val QUERY_TK = "tk"

        const val QUERY_VERSION = "version"

        const val QUERY_VERSION2 = "version2"

        val api = Retrofit.Builder()
                .client(App.httpClient2)
                .baseUrl("https://mobile05.nwstbus.com.hk/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

        val apiCoroutine = Retrofit.Builder()
                .client(App.httpClient2)
                .baseUrl("https://mobile05.nwstbus.com.hk/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
    }

}
