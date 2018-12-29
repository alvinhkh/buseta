package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.model.AppUpdate;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class CheckUpdateService extends IntentService {

    private final CompositeDisposable disposables = new CompositeDisposable();

    public CheckUpdateService() {
        super(CheckUpdateService.class.getSimpleName());
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

}
