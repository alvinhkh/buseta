package com.alvinhkh.buseta;

import com.alvinhkh.buseta.model.AppUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface Api {

    Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    Retrofit raw = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://buseta.alvinhkh.com")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    Retrofit retrofit = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://buseta.alvinhkh.com")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET
    Observable<ResponseBody> get(@Url String url);

    @GET("/release.json")
    Observable<List<AppUpdate>> appUpdate();
}
