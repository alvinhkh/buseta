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
import com.alvinhkh.buseta.datagovhk.ui.MtrBusActivity;
import com.alvinhkh.buseta.kmb.ui.KmbActivity;
import com.alvinhkh.buseta.lwb.ui.LwbActivity;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nlb.ui.NlbActivity;
import com.alvinhkh.buseta.nwst.ui.NwstActivity;
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

    private String lastQuery = "";

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
        appIndexStart(lastQuery);
    }

    @Override
    public void onStop() {
        appIndexStop(lastQuery);
        super.onStop();
    }

    private Intent getIntent(@NonNull String companyCode) {
        Intent intent;
        if (TextUtils.isEmpty(companyCode)) {
            companyCode = BusRoute.COMPANY_KMB;
        }
        switch (companyCode) {
            case BusRoute.COMPANY_CTB:
            case BusRoute.COMPANY_NWFB:
            case BusRoute.COMPANY_NWST:
                intent = new Intent(getApplicationContext(), NwstActivity.class);
                break;
            case BusRoute.COMPANY_LRTFEEDER:
                intent = new Intent(getApplicationContext(), MtrBusActivity.class);
                break;
            case BusRoute.COMPANY_NLB:
                intent = new Intent(getApplicationContext(), NlbActivity.class);
                break;
            case BusRoute.COMPANY_KMB:
            default:
                intent = new Intent(getApplicationContext(), LwbActivity.class);
                if (PreferenceUtil.isUsingNewKmbApi(getApplicationContext())) {
                    intent = new Intent(getApplicationContext(), KmbActivity.class);
                }
                break;
        }
        return intent;
    }

    private void handleIntent(@NonNull Intent intent) {
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
            // TODO: handle company is empty, full page search
            String query = intent.getStringExtra(SearchManager.QUERY);
            String company = intent.getStringExtra(C.EXTRA.COMPANY_CODE);
            Intent i = getIntent(company);
            i.putExtra(C.EXTRA.ROUTE_NO, query);
            startActivity(i);
            finish();
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            if (!TextUtils.isEmpty(lastQuery)) {
                appIndexStop(lastQuery);
            }
            BusRouteStop routeStop = intent.getParcelableExtra(C.EXTRA.STOP_OBJECT);
            String stopText = intent.getStringExtra(C.EXTRA.STOP_OBJECT_STRING);
            String company = intent.getStringExtra(C.EXTRA.COMPANY_CODE);
            String routeNo = intent.getStringExtra(C.EXTRA.ROUTE_NO);
            if (routeStop == null && !TextUtils.isEmpty(stopText)) {
                routeStop = new Gson().fromJson(stopText, BusRouteStop.class);
            }
            if (routeStop != null) {
                Intent i = getIntent(routeStop.companyCode);
                i.putExtra(C.EXTRA.ROUTE_NO, routeStop.route);
                i.putExtra(C.EXTRA.STOP_OBJECT, routeStop);
                startActivity(i);
                lastQuery = routeStop.route;
            } else if (!TextUtils.isEmpty(routeNo) && !TextUtils.isEmpty(company)) {
                Intent i = getIntent(company);
                i.putExtra(C.EXTRA.ROUTE_NO, routeNo);
                startActivity(i);
                lastQuery = routeNo;
            } else if (!TextUtils.isEmpty(data)) {
                String regex = "/route/(.*)/?";
                Pattern regexPattern = Pattern.compile(regex);
                Matcher match = regexPattern.matcher(data);
                if (match.find()) {
                    lastQuery = match.group(1);
                } else {
                    lastQuery = data.substring(data.lastIndexOf("/") + 1);
                }
                Intent i = getIntent("");
                i.putExtra(C.EXTRA.ROUTE_NO, lastQuery);
                startActivity(i);
            }
            appIndexStart(lastQuery);
            finish();
        }
    }

    public Action getIndexApiAction(@NonNull String text) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(text, Uri.parse(C.URI.ROUTE).buildUpon().appendPath(text).build().toString())
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
                .putCustomAttribute("route no", routeNo));
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
