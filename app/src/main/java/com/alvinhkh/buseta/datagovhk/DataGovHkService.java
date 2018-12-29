package com.alvinhkh.buseta.datagovhk;

import com.alvinhkh.buseta.App;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.GET;


public interface DataGovHkService {

    Retrofit resource = new Retrofit.Builder()
            .client(App.httpClient)
            .baseUrl("http://resource.data.one.gov.hk/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    @GET("mtr/data/mtr_lines_and_stations.csv")
    Observable<ResponseBody> getMtrLinesAndStations();

    @GET("mtr/data/mtr_bus_routes.csv")
    Call<ResponseBody> mtrBusRoutes();

    @GET("mtr/data/mtr_bus_stops.csv")
    Call<ResponseBody> mtrBusStops();

    @GET("mtr/data/mtr_bus_fares.csv")
    Call<ResponseBody> mtrBusFares();

    @GET("mtr/data/mtr_lines_and_stations.csv")
    Call<ResponseBody> mtrLinesAndStations();
}
