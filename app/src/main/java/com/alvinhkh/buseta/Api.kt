package com.alvinhkh.buseta

import com.alvinhkh.buseta.model.AppUpdate
import com.google.gson.GsonBuilder

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface Api {

    @GET
    operator fun get(@Url url: String): Observable<ResponseBody>

    @GET("/release.json")
    fun appUpdate(): Call<List<AppUpdate>>

    companion object {

        val gson = GsonBuilder()
                .serializeNulls()
                .create()

        val raw = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://buseta.alvinhkh.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        val retrofit = Retrofit.Builder()
                .client(App.httpClient)
                .baseUrl("https://buseta.alvinhkh.com")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }
}
