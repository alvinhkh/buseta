package com.alvinhkh.buseta.nlb;

import com.alvinhkh.buseta.App;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nlb.model.NlbDatabaseVersion;
import com.alvinhkh.buseta.nlb.model.NlbEtaRes;
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest;
import com.alvinhkh.buseta.nlb.model.NlbNewsList;
import com.alvinhkh.buseta.nlb.model.NlbNewsListRequest;
import com.alvinhkh.buseta.nlb.model.NlbNewsRequest;
import com.alvinhkh.buseta.nlb.model.NlbNewsRes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface NlbService {

    String TIMETABLE_URL = "https://nlb.kcbh.com.hk:8443/api/passenger/route.php?action=getDetail&routeId=";

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    Retrofit api = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("https://nlb.kcbh.com.hk:8443/api/passenger/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("app.php?action=getDatabase")
    Call<NlbDatabase> database();

    @GET("app.php?action=getDatabase")
    Observable<NlbDatabase> getDatabase();

    @GET("app.php?action=getDatabaseVersion")
    Observable<NlbDatabaseVersion> getDatabaseVersion();

    @GET("route.php?action=getDetail")
    Observable<ResponseBody> getTimetable(@Query("routeId") String routeId);

    @POST("news.php?action=list")
    Observable<NlbNewsList> getNewList(@Body NlbNewsListRequest body);

    @POST("news.php?action=get")
    Observable<NlbNewsRes> getNew(@Body NlbNewsRequest body);

    @POST("stop.php?action=estimatedArrivalTime")
    Call<NlbEtaRes> eta(@Body NlbEtaRequest body);

}
