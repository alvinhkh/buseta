package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.datagovhk.DataGovHkService;
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbEtaRoutes;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.AppUpdate;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class CheckUpdateService extends IntentService {

    private final CompositeDisposable disposables = new CompositeDisposable();

    public CheckUpdateService() {
        super(CheckUpdateService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Boolean manualUpdate = false;
        if (null != intent) {
            manualUpdate = intent.getBooleanExtra(C.EXTRA.MANUAL, false);
        }
        // Check internet connection
        if (!ConnectivityUtil.isConnected(this)) {
            Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
            i.putExtra(C.EXTRA.UPDATED, true);
            i.putExtra(C.EXTRA.MANUAL, manualUpdate);
            i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_no_internet_connection);
            sendBroadcast(i);
            return;
        }
        // app update check
        Api apiService = Api.retrofit.create(Api.class);
        apiService.appUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(appUpdateObserver(manualUpdate));
        // clear existing suggested routes
        getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                SuggestionTable.COLUMN_TYPE + "=?",
                new String[]{SuggestionTable.TYPE_DEFAULT});
        // start fetch available kmb route with eta
        KmbService kmbService = KmbService.etadatafeed.create(KmbService.class);
        disposables.add(kmbService.getEtaRoutes()
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(kmbRoutesObserver(manualUpdate)));
        NlbService nlbService = NlbService.api.create(NlbService.class);
        disposables.add(nlbService.getDatabase()
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(nlbDatabaseObserver(manualUpdate)));
        NwstService nwstService = NwstService.api.create(NwstService.class);
        Map<String, String> options = new HashMap<>();
        options.put(QUERY_ROUTE_NO, "");
        options.put(QUERY_MODE, TYPE_ALL_ROUTES);
        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
        options.put(QUERY_PLATFORM, PLATFORM);
        options.put(QUERY_APP_VERSION, APP_VERSION);
        options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
        disposables.add(nwstService.routeList(options)
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(nwstRouteListObserver(manualUpdate)));
        DataGovHkService dataGovHkService = DataGovHkService.resource.create(DataGovHkService.class);
        disposables.add(dataGovHkService.mtrBusRoutes()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(mtrBusRoutesObserver(manualUpdate)));
    }

    DisposableObserver<List<AppUpdate>> appUpdateObserver(Boolean manualUpdate) {
        return new DisposableObserver<List<AppUpdate>>() {
            @Override
            public void onNext(List<AppUpdate> res) {
                if (res.size() < 1) return;
                AppUpdate appUpdate = res.get(0);
                Intent i = new Intent(C.ACTION.APP_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.APP_UPDATE_OBJECT, appUpdate);
                sendBroadcast(i);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    DisposableObserver<List<KmbEtaRoutes>> kmbRoutesObserver(Boolean manualUpdate) {
        return new DisposableObserver<List<KmbEtaRoutes>>() {
            @Override
            public void onNext(List<KmbEtaRoutes> res) {
                if (res.size() < 1 || TextUtils.isEmpty(res.get(0).r_no)) return;
                String routes = res.get(0).r_no;
                String[] routeArray = routes.split(",");
                List<ContentValues> contentValues = new ArrayList<>();
                for (int i = 0; i < routeArray.length; i++) {
                    ContentValues values = new ContentValues();
                    values.put(SuggestionTable.COLUMN_TEXT, routeArray[i]);
                    values.put(SuggestionTable.COLUMN_COMPANY, BusRoute.COMPANY_KMB);
                    values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_DEFAULT);
                    values.put(SuggestionTable.COLUMN_DATE, "0");
                    contentValues.add(values);
                }
                int insertedRows = getContentResolver().bulkInsert(SuggestionProvider.CONTENT_URI,
                        contentValues.toArray(new ContentValues[contentValues.size()]));
                if (insertedRows > 0) {
                    Timber.d("updated %s: %s", BusRoute.COMPANY_KMB, insertedRows);
                } else {
                    Timber.d("error when inserting: %s", BusRoute.COMPANY_KMB);
                }
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated);
                sendBroadcast(i);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                sendBroadcast(i);
            }

            @Override
            public void onComplete() { }
        };
    }

    DisposableObserver<NlbDatabase> nlbDatabaseObserver(Boolean manualUpdate) {
        return new DisposableObserver<NlbDatabase>() {
            @Override
            public void onNext(NlbDatabase database) {
                if (database == null) return;
                List<ContentValues> contentValues = new ArrayList<>();
                for (int i = 0; i < database.routes.size(); i++) {
                    if (TextUtils.isEmpty(database.routes.get(i).route_no)) continue;
                    ContentValues values = new ContentValues();
                    values.put(SuggestionTable.COLUMN_TEXT, database.routes.get(i).route_no);
                    values.put(SuggestionTable.COLUMN_COMPANY, BusRoute.COMPANY_NLB);
                    values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_DEFAULT);
                    values.put(SuggestionTable.COLUMN_DATE, "0");
                    contentValues.add(values);
                }
                int insertedRows = getContentResolver().bulkInsert(SuggestionProvider.CONTENT_URI,
                        contentValues.toArray(new ContentValues[contentValues.size()]));
                if (insertedRows > 0) {
                    Timber.d("updated %s: %s", BusRoute.COMPANY_NLB, insertedRows);
                } else {
                    Timber.d("error when inserting: %s", BusRoute.COMPANY_NLB);
                }
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated);
                sendBroadcast(i);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                sendBroadcast(i);
            }

            @Override
            public void onComplete() { }
        };
    }

    DisposableObserver<ResponseBody> nwstRouteListObserver(Boolean manualUpdate) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                try {
                    String[] routes = body.string().split("\\|\\*\\|", -1);
                    List<ContentValues> contentValues = new ArrayList<>();
                    for (String route: routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstRoute nwstRoute = NwstRoute.Companion.fromString(text);
                        if (nwstRoute == null || TextUtils.isEmpty(nwstRoute.getRouteNo())) continue;
                        ContentValues values = new ContentValues();
                        values.put(SuggestionTable.COLUMN_TEXT, nwstRoute.getRouteNo());
                        values.put(SuggestionTable.COLUMN_COMPANY, nwstRoute.getCompanyCode());
                        values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_DEFAULT);
                        values.put(SuggestionTable.COLUMN_DATE, "0");
                        contentValues.add(values);
                    }
                    int insertedRows = getContentResolver().bulkInsert(SuggestionProvider.CONTENT_URI,
                            contentValues.toArray(new ContentValues[contentValues.size()]));
                    if (insertedRows > 0) {
                        Timber.d("updated %s: %s", BusRoute.COMPANY_NWST, insertedRows);
                    } else {
                        Timber.d("error when inserting: %s", BusRoute.COMPANY_NWST);
                    }
                    Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                    i.putExtra(C.EXTRA.UPDATED, true);
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                    i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated);
                    sendBroadcast(i);
                } catch (IOException e) {
                    Timber.d(e);
                    Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                    i.putExtra(C.EXTRA.UPDATED, true);
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                    i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                    sendBroadcast(i);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                sendBroadcast(i);
            }

            @Override
            public void onComplete() { }
        };
    }

    DisposableObserver<ResponseBody> mtrBusRoutesObserver(Boolean manualUpdate) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                try {
                    List<ContentValues> contentValues = new ArrayList<>();
                    List<MtrBusRoute> routes = MtrBusRoute.Companion.fromCSV(body.string());
                    for (MtrBusRoute route: routes) {
                        if (TextUtils.isEmpty(route.getRouteId())) continue;
                        ContentValues values = new ContentValues();
                        values.put(SuggestionTable.COLUMN_TEXT, route.getRouteId());
                        values.put(SuggestionTable.COLUMN_COMPANY, BusRoute.COMPANY_LRTFEEDER);
                        values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_DEFAULT);
                        values.put(SuggestionTable.COLUMN_DATE, "0");
                        contentValues.add(values);
                    }
                    int insertedRows = getContentResolver().bulkInsert(SuggestionProvider.CONTENT_URI,
                            contentValues.toArray(new ContentValues[contentValues.size()]));
                    if (insertedRows > 0) {
                        Timber.d("updated %s: %s", BusRoute.COMPANY_LRTFEEDER, insertedRows);
                    } else {
                        Timber.d("error when inserting: %s", BusRoute.COMPANY_LRTFEEDER);
                    }
                    Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                    i.putExtra(C.EXTRA.UPDATED, true);
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                    i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated);
                    sendBroadcast(i);
                } catch (IOException e) {
                    Timber.d(e);
                    Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                    i.putExtra(C.EXTRA.UPDATED, true);
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                    i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                    sendBroadcast(i);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                i.putExtra(C.EXTRA.UPDATED, true);
                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_fail_to_request);
                sendBroadcast(i);
            }

            @Override
            public void onComplete() { }
        };
    }
}
