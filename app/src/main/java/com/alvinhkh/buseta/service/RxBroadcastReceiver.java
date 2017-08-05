package com.alvinhkh.buseta.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;

/**
 * RxJava based broadcast receiver that registers its local BroadcastReceiver until end of subscription.
 * Listens for update and passes Intent to the Stream (Subscriber).
 * <p>
 * Created on 11/18/16.
 * https://gist.github.com/magillus/927f863987575021dc78249bd8064423
 */
public class RxBroadcastReceiver implements ObservableOnSubscribe<Intent> {

    private final WeakReference<Context> contextWeakReference;

    private IntentFilter intentFilter;

    /**
     * Creates Observable with intent filter for Broadcast receiver.
     *
     * @param context Context
     * @param intentFilter IntentFilter
     * @return Observable
     */
    public static Observable<Intent> create(Context context, IntentFilter intentFilter) {
        return Observable.defer(() -> Observable.create(new RxBroadcastReceiver(context, intentFilter))
                .subscribeOn(Schedulers.io())
        );
    }

    /**
     * @param context Context
     * @param intentFilter IntentFilter
     */
    private RxBroadcastReceiver(Context context, IntentFilter intentFilter) {
        contextWeakReference = new WeakReference<>(context.getApplicationContext());
        this.intentFilter = intentFilter;
    }

    @Override
    public void subscribe(ObservableEmitter<Intent> emitter) throws Exception {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                emitter.onNext(intent);
            }
        };
        emitter.setDisposable(Disposables.fromRunnable(() -> { // thank you Jake W.
            try {
                if (contextWeakReference != null && contextWeakReference.get() != null) {
                    contextWeakReference.get().unregisterReceiver(broadcastReceiver);
                }
            } catch (IllegalArgumentException ignored) {}
        }));

        if (contextWeakReference != null && contextWeakReference.get() != null) {
            contextWeakReference.get().registerReceiver(broadcastReceiver, intentFilter);
        }
    }
}