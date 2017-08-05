package com.alvinhkh.buseta.lwb;

import com.alvinhkh.buseta.App;
import com.alvinhkh.buseta.lwb.model.LwbRouteStop;
import com.alvinhkh.buseta.lwb.model.network.LwbRouteBoundRes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.util.List;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface LwbService {

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    Retrofit retrofit = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://www.lwb.hk")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("/ajax/getRoute_info.php")
    Observable<LwbRouteBoundRes> getRouteBound(@Query("field9") String routeNo, @Query("t") Double t);

    @FormUrlEncoded
    @POST("http://www.kmb.hk/ajax/getRouteMapByBusno.php")
    Observable<List<LwbRouteStop>> getRouteMap(@Field("bn") String routeNo, @Field("dir") String bound, @Field("ST") String serviceType);

}
