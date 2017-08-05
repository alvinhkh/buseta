package com.alvinhkh.buseta.ui.search;


import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.kmb.ui.KmbActivity;
import com.alvinhkh.buseta.lwb.ui.LwbActivity;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.service.LocationService;
import com.alvinhkh.buseta.utils.PreferenceUtil;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.gson.Gson;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;


// Search holder, redirect search event
public class SearchActivity extends AppCompatActivity {

    private String mLastQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        appIndexStart(mLastQuery);
    }

    @Override
    public void onStop() {
        appIndexStop(mLastQuery);
        super.onStop();
    }

    private void handleIntent(Intent intent) {
        if (null == intent) return;

        String action = intent.getAction();
        String data = intent.getDataString();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                startService(new Intent(this, LocationService.class));
            } catch (IllegalStateException ignored) {}
        }

        if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Intent i = new Intent(getApplicationContext(), LwbActivity.class);
            if (PreferenceUtil.isUsingNewKmbApi(getApplicationContext())) {
                i = new Intent(getApplicationContext(), KmbActivity.class);
            }
            i.putExtra(C.EXTRA.ROUTE_NO, query);
            startActivity(i);
            finish();
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            if (!TextUtils.isEmpty(mLastQuery)) {
                appIndexStop(mLastQuery);
            }
            BusRouteStop routeStop = intent.getParcelableExtra(C.EXTRA.STOP_OBJECT);
            String stopText = intent.getStringExtra(C.EXTRA.STOP_OBJECT_STRING);
            if (routeStop == null && !TextUtils.isEmpty(stopText)) {
                routeStop = new Gson().fromJson(stopText, BusRouteStop.class);
            }
            if (routeStop != null) {
                Intent routeIntent = new Intent(getApplicationContext(), LwbActivity.class);
                if (PreferenceUtil.isUsingNewKmbApi(getApplicationContext())) {
                    routeIntent = new Intent(getApplicationContext(), KmbActivity.class);
                }
                routeIntent.putExtra(C.EXTRA.ROUTE_NO, routeStop.route);
                routeIntent.putExtra(C.EXTRA.STOP_OBJECT, routeStop);
                startActivity(routeIntent);
                mLastQuery = routeStop.route;
            }
            if (!TextUtils.isEmpty(data)) {
                String regex = "/route/(.*)/?";
                Pattern regexPattern = Pattern.compile(regex);
                Matcher match = regexPattern.matcher(data);
                if (match.find()) {
                    mLastQuery = match.group(1);
                } else {
                    mLastQuery = data.substring(data.lastIndexOf("/") + 1);
                }
                Intent routeIntent = new Intent(getApplicationContext(), LwbActivity.class);
                if (PreferenceUtil.isUsingNewKmbApi(getApplicationContext())) {
                    routeIntent = new Intent(getApplicationContext(), KmbActivity.class);
                }
                routeIntent.putExtra(C.EXTRA.ROUTE_NO, mLastQuery);
                startActivity(routeIntent);
            }
            appIndexStart(mLastQuery);
            finish();
        }
    }

    public Action getIndexApiAction(@NonNull String text) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(text, Uri.parse(C.URI.APP).buildUpon().appendPath(text).build().toString())
                // Keep action data for personal content on the device
                //.setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private void appIndexStart(@NonNull String routeNo) {
        if (TextUtils.isEmpty(routeNo)) return;
        FirebaseUserActions.getInstance().start(getIndexApiAction(routeNo))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Timber.d("App Indexing: Recorded start successfully");
                    } else {
                        Timber.d("App Indexing: fail");
                    }
                });
        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("search")
                .putContentType("route")
                .putCustomAttribute("route Nno", routeNo));
    }

    private void appIndexStop(@NonNull String routeNo) {
        if (TextUtils.isEmpty(routeNo)) return;
        FirebaseUserActions.getInstance().end(getIndexApiAction(routeNo))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Timber.d("App Indexing: Recorded end successfully");
                    } else {
                        Timber.d("App Indexing: fail");
                    }
                });
    }
}
