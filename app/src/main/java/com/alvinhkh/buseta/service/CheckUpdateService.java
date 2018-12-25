package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.model.AppUpdate;
import com.alvinhkh.buseta.utils.HashUtil;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class CheckUpdateService extends IntentService {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    public CheckUpdateService() {
        super(CheckUpdateService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String randomHex64 = HashUtil.randomHexString(64);
        disposables.add(nwstService.pushTokenEnable(randomHex64, randomHex64, LANGUAGE_TC, "Y",
                NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, APP_VERSION2, NwstRequestUtil.syscode2(), "", "")
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
        if (!ConnectivityUtil.isConnected(this)) {
            return;
        }
        Api apiService = Api.retrofit.create(Api.class);
        disposables.add(apiService.appUpdate()
                .subscribeOn(Schedulers.io())
                .subscribeWith(appUpdateObserver(manualUpdate)));
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

    DisposableObserver<ResponseBody> nwstTkObserver(String randomHex64) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody res) {
                disposables.add(nwstService.pushToken(randomHex64, randomHex64, LANGUAGE_TC, "R", PLATFORM,
                        NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, APP_VERSION2,
                        NwstRequestUtil.syscode2(), "")
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(nwstPushTokenObserver(randomHex64)));
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

    DisposableObserver<ResponseBody> nwstPushTokenObserver(String randomHex64) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody res) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("nwst_tk", randomHex64);
                editor.putString("nwst_syscode3", "");
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
