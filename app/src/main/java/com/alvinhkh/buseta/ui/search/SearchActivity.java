package com.alvinhkh.buseta.ui.search;


import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.datagovhk.ui.MtrBusActivity;
import com.alvinhkh.buseta.kmb.ui.KmbActivity;
import com.alvinhkh.buseta.lwb.ui.LwbActivity;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.mtr.ui.AESBusActivity;
import com.alvinhkh.buseta.mtr.ui.MtrActivity;
import com.alvinhkh.buseta.nlb.ui.NlbActivity;
import com.alvinhkh.buseta.nwst.ui.NwstActivity;
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

    private Intent getBusIntent(@NonNull String companyCode) {
        Intent intent;
        if (TextUtils.isEmpty(companyCode)) {
            companyCode = C.PROVIDER.KMB;
        }
        switch (companyCode) {
            case C.PROVIDER.AESBUS:
                intent = new Intent(getApplicationContext(), AESBusActivity.class);
                break;
            case C.PROVIDER.CTB:
            case C.PROVIDER.NWFB:
            case C.PROVIDER.NWST:
                intent = new Intent(getApplicationContext(), NwstActivity.class);
                break;
            case C.PROVIDER.LRTFEEDER:
                intent = new Intent(getApplicationContext(), MtrBusActivity.class);
                break;
            case C.PROVIDER.NLB:
                intent = new Intent(getApplicationContext(), NlbActivity.class);
                break;
            case C.PROVIDER.KMB:
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
        String type = intent.getStringExtra(C.EXTRA.TYPE);

        if (Intent.ACTION_SEARCH.equals(action)) {
            // TODO: handle company is empty, full page search
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(type) && type.equals(C.TYPE.RAILWAY)) {
                String lineCode = intent.getStringExtra(C.EXTRA.LINE_CODE);
                Intent i = new Intent(getApplicationContext(), MtrActivity.class);
                i.putExtra(C.EXTRA.LINE_CODE, TextUtils.isEmpty(lineCode) ? query : lineCode);
                startActivity(i);
            } else {
                String company = intent.getStringExtra(C.EXTRA.COMPANY_CODE);
                Intent i = getBusIntent(company);
                i.putExtra(C.EXTRA.ROUTE_NO, query);
                startActivity(i);
            }
            finish();
        }

        if (Intent.ACTION_VIEW.equals(action)) {
            if (!TextUtils.isEmpty(type) && type.equals(C.TYPE.RAILWAY)) {
                String lineCode = intent.getStringExtra(C.EXTRA.LINE_CODE);
                String lineColour = intent.getStringExtra(C.EXTRA.LINE_COLOUR);
                String lineName = intent.getStringExtra(C.EXTRA.LINE_NAME);
                Intent i = new Intent(getApplicationContext(), MtrActivity.class);
                i.putExtra(C.EXTRA.LINE_CODE, lineCode);
                i.putExtra(C.EXTRA.LINE_COLOUR, lineColour);
                i.putExtra(C.EXTRA.LINE_NAME, lineName);
                startActivity(i);
            } else {
                if (!TextUtils.isEmpty(lastQuery)) {
                    appIndexStop(lastQuery);
                }
                RouteStop routeStop = intent.getParcelableExtra(C.EXTRA.STOP_OBJECT);
                String stopText = intent.getStringExtra(C.EXTRA.STOP_OBJECT_STRING);
                String company = intent.getStringExtra(C.EXTRA.COMPANY_CODE);
                String routeNo = intent.getStringExtra(C.EXTRA.ROUTE_NO);
                if (routeStop == null && !TextUtils.isEmpty(stopText)) {
                    routeStop = new Gson().fromJson(stopText, RouteStop.class);
                }
                if (routeStop != null) {
                    Intent i = getBusIntent(routeStop.getCompanyCode());
                    i.putExtra(C.EXTRA.ROUTE_NO, routeStop.getRoute());
                    i.putExtra(C.EXTRA.STOP_OBJECT, routeStop);
                    startActivity(i);
                    lastQuery = routeStop.getRoute();
                } else if (!TextUtils.isEmpty(routeNo) && !TextUtils.isEmpty(company)) {
                    Intent i = getBusIntent(company);
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
                    Intent i = getBusIntent("");
                    i.putExtra(C.EXTRA.ROUTE_NO, lastQuery);
                    startActivity(i);
                }
                appIndexStart(lastQuery);
            }
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
