package com.alvinhkh.buseta.nwst;

import com.alvinhkh.buseta.App;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;


public interface NwstService {

    String APP_VERSION = "3.3";

    String LANGUAGE_TC = "0";

    String PLATFORM = "android";

    String QUERY_APP_VERSION = "appversion";

    String QUERY_BOUND = "bound";

    String QUERY_ID = "id";

    String QUERY_INFO = "info";

    String QUERY_LANGUAGE = "l";

    String QUERY_MODE = "m";

    String QUERY_PLATFORM = "p";

    String QUERY_RDV = "rdv";

    String QUERY_ROUTE = "route";

    String QUERY_ROUTE_NO = "rno";

    String QUERY_SERVICE_NO = "service_no";

    String QUERY_STOP_ID = "stopid";

    String QUERY_STOP_SEQ = "stopseq";

    String QUERY_SYSCODE = "syscode";

    String TYPE_ALL_ROUTES = "0";

    String TYPE_AIRPORT_ROUTES = "5";

    String TYPE_ETA_ROUTES = "2";

    String TYPE_NIGHT_ROUTES = "1";

    Retrofit api = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://mobile.nwstbus.com.hk/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("api6/getmmroutelist.php")
    Observable<ResponseBody> routeList(@QueryMap Map<String, String> options);

    @GET("api6/getvariantlist.php")
    Observable<ResponseBody> variantList(@QueryMap Map<String, String> options);

    @GET("api6/ppstoplist.php")
    Observable<ResponseBody> stopList(@QueryMap Map<String, String> options);

    @GET("api6/get_notice_4.php")
    Observable<ResponseBody> notice(@QueryMap Map<String, String> options);

    @GET("api6/getnextbus2.php")
    Observable<ResponseBody> eta(@QueryMap Map<String, String> options);

}
