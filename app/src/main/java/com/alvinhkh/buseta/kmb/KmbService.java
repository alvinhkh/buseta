package com.alvinhkh.buseta.kmb;

import com.alvinhkh.buseta.App;
import com.alvinhkh.buseta.kmb.model.KmbEtaRoutes;
import com.alvinhkh.buseta.kmb.model.network.KmbAnnounceRes;
import com.alvinhkh.buseta.kmb.model.network.KmbEtaRes;
import com.alvinhkh.buseta.kmb.model.network.KmbRouteBoundRes;
import com.alvinhkh.buseta.kmb.model.network.KmbScheduleRes;
import com.alvinhkh.buseta.kmb.model.network.KmbSpecialRouteRes;
import com.alvinhkh.buseta.kmb.model.network.KmbStopsRes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory;

import java.util.List;

import io.reactivex.Observable;
import kotlinx.coroutines.experimental.Deferred;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface KmbService {

    String ANNOUNCEMENT_PICTURE = "http://search.kmb.hk/KMBWebSite/AnnouncementPicture.ashx?url=";

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    Retrofit webSearch = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://search.kmb.hk/KMBWebSite/Function/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("FunctionRequest.ashx?action=getAnnounce")
    Observable<KmbAnnounceRes> getAnnounce(@Query("route") String route, @Query("bound") String bound);

    @GET("FunctionRequest.ashx?action=getRouteBound")
    Observable<KmbRouteBoundRes> getRouteBound(@Query("route") String route);

    @GET("FunctionRequest.ashx?action=getSpecialRoute")
    Observable<KmbSpecialRouteRes> getSpecialRoute(@Query("route") String route, @Query("bound") String bound);

    @GET("FunctionRequest.ashx?action=getStops")
    Observable<KmbStopsRes> getStops(@Query("route") String route, @Query("bound") String bound, @Query("serviceType") String serviceType);

    Retrofit webSearchCoroutine = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://search.kmb.hk/KMBWebSite/Function/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory.create())
            .build();

    @GET("FunctionRequest.ashx?action=getschedule")
    Deferred<Response<KmbScheduleRes>> schedule(@Query("route") String route, @Query("bound") String bound);

    Retrofit webSearchHtml = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://search.kmb.hk/KMBWebSite/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("AnnouncementPicture.ashx")
    Observable<ResponseBody> getAnnouncementPicture(@Query("url") String url);

    Retrofit etav3 = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://etav3.kmb.hk")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("?action=geteta")
    Observable<KmbEtaRes> getEta(@Query("route") String route, @Query("bound") String bound,
                                 @Query("stop") String stop, @Query("stop_seq") String stop_seq,
                                 @Query("serviceType") String serviceType, @Query("lang") String lang,
                                 @Query("updated") String updated);

    Retrofit etadatafeed = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://etadatafeed.kmb.hk:1933")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("GetData.ashx?type=ETA_R")
    Observable<List<KmbEtaRoutes>> getEtaRoutes();
}
