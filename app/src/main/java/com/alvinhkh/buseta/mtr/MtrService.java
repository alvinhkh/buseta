package com.alvinhkh.buseta.mtr;

import com.alvinhkh.buseta.App;
import com.alvinhkh.buseta.mtr.model.AESEtaBusRes;
import com.alvinhkh.buseta.mtr.model.AESEtaBusStopsRequest;
import com.alvinhkh.buseta.mtr.model.MtrLineStatusRes;
import com.alvinhkh.buseta.mtr.model.MtrMobileVersionCheck;
import com.alvinhkh.buseta.mtr.model.ScheduleRes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;


public interface MtrService {

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    Retrofit api = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("https://mavmapp1044.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    Retrofit aes = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("https://mavmwfs1004.azurewebsites.net/AES/AESService.svc/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    Retrofit tnews = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("https://tnews.mtr.com.hk/")
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    Retrofit mob = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("https://www.mtr.com.hk/mob/")
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("reverse_proxy/NT_v2/NTAppModule/getSchedule.php")
    Observable<ScheduleRes> getSchedule(@Query("key") String key, @Query("line") String line, @Query("sta") String sta, @Query("lang") String lang);

    @Headers("Content-Type: application/json")
    @POST("getBusStopsDetail")
    Observable<AESEtaBusRes> getBusStopsDetail(@Body AESEtaBusStopsRequest request);

    @GET("alert/ryg_line_status.xml")
    Observable<MtrLineStatusRes> lineStatus();

    @GET("mtrmobile_versioncheck_v12_6.xml")
    Observable<MtrMobileVersionCheck> zipResources();

    @GET
    @Streaming
    Observable<ResponseBody> downloadFile(@Url String url);
}
