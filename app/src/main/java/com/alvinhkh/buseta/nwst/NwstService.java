package com.alvinhkh.buseta.nwst;

import com.alvinhkh.buseta.App;
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory;

import java.util.Map;

import io.reactivex.Observable;
import kotlinx.coroutines.Deferred;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;


public interface NwstService {

    String APP_VERSION = "3.5.5";

    String APP_VERSION2 = "5";

    String DEVICETYPE = "iphone";

    String LANGUAGE_TC = "0";

    String PLATFORM = "iphone";

    String QUERY_BM = "bm";

    String QUERY_BOUND = "bound";

    String QUERY_DEVICETOKEN = "devicetoken";

    String QUERY_DEVICETYPE = "deviceType";

    String QUERY_ID = "id";

    String QUERY_INFO = "info";

    String QUERY_LANGUAGE = "l";

    String QUERY_MODE = "m";

    String QUERY_PLATFORM = "p";

    String QUERY_R = "r";

    String QUERY_RDV = "rdv";

    String QUERY_ROUTE = "route";

    String QUERY_ROUTE_NO = "rno";

    String QUERY_SERVICE_NO = "service_no";

    String QUERY_STOP_ID = "stopid";

    String QUERY_STOP_SEQ = "stopseq";

    String QUERY_SYSCODE = "syscode";

    String QUERY_SYSCODE2 = "syscode2";

    String QUERY_SYSCODE3 = "syscode3";

    String QUERY_TK = "tk";

    String QUERY_VERSION = "version";

    String QUERY_VERSION2 = "version2";

    String TYPE_ALL_ROUTES = "0";

    String TYPE_AIRPORT_ROUTES = "5";

    String TYPE_ETA_ROUTES = "2";

    String TYPE_NIGHT_ROUTES = "1";

    Retrofit api = new Retrofit.Builder()
            .client(App.Companion.getHttpClient2())
            .baseUrl("https://mobile.nwstbus.com.hk/")
            .build();

    Retrofit apiCoroutine = new Retrofit.Builder()
            .client(App.Companion.getHttpClient2())
            .baseUrl("https://mobile.nwstbus.com.hk/")
            .addCallAdapterFactory(CoroutineCallAdapterFactory.create())
            .build();

    @GET("api6/getadv.php")
    Call<ResponseBody> adv(
            @Query(QUERY_LANGUAGE) String language,
            @Query("width") String width,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(QUERY_VERSION2) String version2,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2,
            @Query(QUERY_TK) String tk
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("push/pushtokenenable.php")
    Call<ResponseBody> pushTokenEnable(
            @Query(QUERY_TK) String tk,
            @Query(QUERY_DEVICETOKEN) String devicetoken,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_BM) String bm,
            @Query(QUERY_MODE) String mode,
            @Query(QUERY_DEVICETYPE) String deviceType,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(QUERY_VERSION2) String version2,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("push/pushtoken.php")
    Call<ResponseBody> pushToken(
            @Query(QUERY_TK) String tk,
            @Query(QUERY_DEVICETOKEN) String devicetoken,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_BM) String bm,
            @Query(QUERY_MODE) String mode,
            @Query(QUERY_DEVICETYPE) String deviceType,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(QUERY_VERSION2) String version2,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/getmmroutelist.php")
    Observable<ResponseBody> mmroutelist(
            @Query(QUERY_ROUTE_NO) String routeNo,
            @Query(QUERY_MODE) String mode,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_TK) String tk,
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/getmmroutelist.php")
    Call<ResponseBody> routeList(
            @Query(QUERY_ROUTE_NO) String routeNo,
            @Query(QUERY_MODE) String mode,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_TK) String tk,
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/getvariantlist.php")
    Call<ResponseBody> variantList(
            @Query(QUERY_ID) String id,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_TK) String tk,
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/ppstoplist.php")
    Call<ResponseBody> ppStopList(
            @Query(QUERY_INFO) String info,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_TK) String tk,
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/get_notice_4.php")
    Deferred<Response<ResponseBody>> noticeList(@QueryMap Map<String, String> options);

    @GET("api6/getnextbus2.php")
    Call<ResponseBody> eta(
            @Query(QUERY_STOP_ID) String stopId,
            @Query(QUERY_SERVICE_NO) String serviceNo,
            @Query("removeRepeatedSuspend") String removeRepeatedSuspend,
            @Query("interval") String interval,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_BOUND) String bound,
            @Query(QUERY_STOP_SEQ) String stopSeq,
            @Query(QUERY_RDV) String rdv,
            @Query("showtime") String showtime,
            @Query("removeRepeatedSuspend") String removeRepeatedSuspend2,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(QUERY_VERSION2) String version2
    );

    @GET("api6/getnextbus2.php")
    Call<ResponseBody> eta(
            @Query(QUERY_STOP_ID) String stopId,
            @Query(QUERY_SERVICE_NO) String serviceNo,
            @Query("removeRepeatedSuspend") String removeRepeatedSuspend,
            @Query("interval") String interval,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_BOUND) String bound,
            @Query(QUERY_STOP_SEQ) String stopSeq,
            @Query(QUERY_RDV) String rdv,
            @Query("showtime") String showtime,
            @Query("removeRepeatedSuspend") String removeRepeatedSuspend2,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(QUERY_VERSION2) String version2,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2,
            @Query(QUERY_TK) String tk
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/getline_multi2.php")
    Call<ResponseBody> lineMulti2(
            @Query(QUERY_R) String r,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2
//            @Query(QUERY_TK) String tk,
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

    @GET("api6/gettimetable.php")
    Call<ResponseBody> timetable(
            @Query(QUERY_RDV) String rdv,
            @Query(QUERY_BOUND) String bound,
            @Query(QUERY_LANGUAGE) String language,
            @Query(QUERY_SYSCODE) String sysCode,
            @Query(QUERY_PLATFORM) String platform,
            @Query(QUERY_VERSION) String version,
            @Query(value = QUERY_SYSCODE2, encoded = true) String sysCode2,
            @Query(QUERY_TK) String tk
//            @Query(QUERY_SYSCODE3) String sysCode3
    );

}
