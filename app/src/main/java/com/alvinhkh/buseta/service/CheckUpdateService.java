package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.datagovhk.DataGovHkService;
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbEtaRoutes;
import com.alvinhkh.buseta.mtr.MtrService;
import com.alvinhkh.buseta.mtr.dao.AESBusDatabase;
import com.alvinhkh.buseta.mtr.model.AESBusRoute;
import com.alvinhkh.buseta.mtr.model.MtrMobileVersionCheck;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.search.dao.SuggestionDatabase;
import com.alvinhkh.buseta.search.model.Suggestion;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.AppUpdate;
import com.alvinhkh.buseta.utils.DatabaseUtil;
import com.alvinhkh.buseta.utils.HashUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;
import com.alvinhkh.buseta.utils.ZipUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class CheckUpdateService extends IntentService {

    private final MtrService mtrMobService = MtrService.Companion.getMob().create(MtrService.class);

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SuggestionDatabase suggestionDatabase;

    public CheckUpdateService() {
        super(CheckUpdateService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        suggestionDatabase = SuggestionDatabase.Companion.getInstance(this);

        String randomHex64 = HashUtil.randomHexString(64);
        disposables.add(nwstService.pushTokenEnable(randomHex64, LANGUAGE_TC, "Y",
                NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, APP_VERSION2, NwstRequestUtil.syscode2())
                .subscribeOn(Schedulers.io())
                .subscribeWith(nwstTkObserver(randomHex64)));
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
        disposables.add(apiService.appUpdate()
                .subscribeOn(Schedulers.io())
                .subscribeWith(appUpdateObserver(manualUpdate)));
        // clear existing suggested routes
        if (suggestionDatabase != null) {
            suggestionDatabase.suggestionDao().clearDefault();
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // start fetch available kmb route with eta
        KmbService kmbService = KmbService.etadatafeed.create(KmbService.class);
        disposables.add(kmbService.getEtaRoutes()
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .subscribeWith(kmbRoutesObserver(manualUpdate)));
        NlbService nlbService = NlbService.api.create(NlbService.class);
        disposables.add(nlbService.getDatabase()
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .subscribeWith(nlbDatabaseObserver(manualUpdate)));
        NwstService nwstService = NwstService.api.create(NwstService.class);
        disposables.add(nwstService.routeList("", TYPE_ALL_ROUTES,
                LANGUAGE_TC, NwstRequestUtil.syscode(), PLATFORM, APP_VERSION,
                NwstRequestUtil.syscode2(), preferences.getString("nwst_tk", ""))
                .retryWhen(new RetryWithDelay(3, 3000))
                .subscribeOn(Schedulers.io())
                .subscribeWith(nwstRouteListObserver(manualUpdate)));
        DataGovHkService dataGovHkService = DataGovHkService.resource.create(DataGovHkService.class);
        disposables.add(dataGovHkService.mtrBusRoutes()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .subscribeWith(mtrBusRoutesObserver(manualUpdate)));
        disposables.add(mtrMobService.zipResources()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .subscribeWith(mtrMobResourcesObserver(manualUpdate)));
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
                List<Suggestion> suggestions = new ArrayList<>();
                for (int i = 0; i < routeArray.length; i++) {
                    suggestions.add(new Suggestion(0, C.PROVIDER.KMB, routeArray[i],
                            0, Suggestion.TYPE_DEFAULT));
                }
                if (suggestionDatabase != null) {
                    suggestionDatabase.suggestionDao().insert(suggestions);
                }
                Timber.d("kmb: %s", suggestions.size());
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
                List<Suggestion> suggestions = new ArrayList<>();
                for (int i = 0; i < database.routes.size(); i++) {
                    if (TextUtils.isEmpty(database.routes.get(i).route_no)) continue;
                    suggestions.add(new Suggestion(0, C.PROVIDER.NLB, database.routes.get(i).route_no,
                            0, Suggestion.TYPE_DEFAULT));
                }
                if (suggestionDatabase != null) {
                    suggestionDatabase.suggestionDao().insert(suggestions);
                }
                Timber.d("nlb: %s", suggestions.size());
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
                    List<Suggestion> suggestions = new ArrayList<>();
                    for (String route: routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstRoute nwstRoute = NwstRoute.Companion.fromString(text);
                        if (nwstRoute == null || TextUtils.isEmpty(nwstRoute.getRouteNo())) continue;
                        suggestions.add(new Suggestion(0, nwstRoute.getCompanyCode(), nwstRoute.getRouteNo(),
                                0, Suggestion.TYPE_DEFAULT));
                    }
                    if (suggestionDatabase != null) {
                        suggestionDatabase.suggestionDao().insert(suggestions);
                    }
                    Timber.d("nwst: %s", suggestions.size());
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
                    List<Suggestion> suggestions = new ArrayList<>();
                    List<MtrBusRoute> routes = MtrBusRoute.Companion.fromCSV(body.string());
                    for (MtrBusRoute route: routes) {
                        if (TextUtils.isEmpty(route.getRouteId())) continue;
                        suggestions.add(new Suggestion(0, C.PROVIDER.LRTFEEDER, route.getRouteId(),
                                0, Suggestion.TYPE_DEFAULT));
                    }
                    if (suggestionDatabase != null) {
                        suggestionDatabase.suggestionDao().insert(suggestions);
                    }
                    Timber.d("mtrbus: %s", suggestions.size());
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

    DisposableObserver<MtrMobileVersionCheck> mtrMobResourcesObserver(Boolean manualUpdate) {
        return new DisposableObserver<MtrMobileVersionCheck>() {
            @Override
            public void onNext(MtrMobileVersionCheck xml) {
                if (xml == null) return;
                MtrMobileVersionCheck.ResourcesV12 resourcesV12 = xml.getResources();
                if (resourcesV12.getAes() != null) {
                    String aesDatabaseFileUrl = resourcesV12.getAes().getUrl();
                    if (!TextUtils.isEmpty(aesDatabaseFileUrl)) {
                        Uri uri = Uri.parse(aesDatabaseFileUrl);
                        deleteDatabase("E_AES.db");
                        disposables.add(mtrMobService.downloadFile(aesDatabaseFileUrl)
                                .subscribeOn(Schedulers.io())
                                .subscribeWith(mtrMobFileObserver(manualUpdate, uri.getLastPathSegment())));
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() { }
        };
    }

    DisposableObserver<ResponseBody> mtrMobFileObserver(Boolean manualUpdate, String fileName) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                try {
                    File zipFile = downloadFile(body, fileName);
                    if (zipFile.exists()) {
                        if (zipFile.getName().endsWith(".zip")) {
                            ZipUtil.decompress(zipFile);
                        }
                        zipFile.deleteOnExit();
                    }
                    fileToDatabase();
                } catch (IOException e) {
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() { }

            private void fileToDatabase() {
                AESBusDatabase database = DatabaseUtil.Companion.getAESBusDatabase(getApplicationContext());
                disposables.add(database.aesBusDao().getAllRoutes()
                        .subscribe(aesBusRoutes -> {
                            if (aesBusRoutes != null) {
                                List<Suggestion> suggestions = new ArrayList<>();
                                for (AESBusRoute aesBusRoute : aesBusRoutes) {
                                    suggestions.add(new Suggestion(0, C.PROVIDER.AESBUS,
                                            aesBusRoute.getBusNumber(), 0, Suggestion.TYPE_DEFAULT));
                                }
                                if (suggestionDatabase != null) {
                                    suggestionDatabase.suggestionDao().insert(suggestions);
                                }
                                Timber.d("aesbus: %s", suggestions.size());
                                Intent i = new Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE);
                                i.putExtra(C.EXTRA.UPDATED, true);
                                i.putExtra(C.EXTRA.MANUAL, manualUpdate);
                                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated);
                                sendBroadcast(i);
                            }
                        }));
            }

            private File downloadFile(@NonNull ResponseBody body, @NonNull String fileName) throws IOException {
                int count;
                byte data[] = new byte[1024 * 4];
                long fileSize = body.contentLength();
                InputStream bis = new BufferedInputStream(body.byteStream(), 1024 * 8);
                File outputFile = new File(getCacheDir(), fileName);
                OutputStream output = new FileOutputStream(outputFile);
                long total = 0;
                long startTime = System.currentTimeMillis();
                int timeCount = 1;
                int totalFileSize = 0;
                while ((count = bis.read(data)) != -1) {
                    total += count;
                    totalFileSize = (int) (fileSize / (Math.pow(1024, 2)));
                    double current = Math.round(total / (Math.pow(1024, 2)));
                    int progress = (int) ((total * 100) / fileSize);
                    long currentTime = System.currentTimeMillis() - startTime;
                    if (currentTime > 1000 * timeCount) {
                        timeCount++;
                    }
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                bis.close();
                return outputFile;
            }
        };
    }

    DisposableObserver<ResponseBody> nwstTkObserver(String randomHex64) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody res) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("nwst_tk", randomHex64);
                editor.apply();
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
}
