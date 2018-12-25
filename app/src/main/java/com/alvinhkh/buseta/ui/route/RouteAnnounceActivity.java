package com.alvinhkh.buseta.ui.route;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.ui.KmbAnnounceFragment;
import com.alvinhkh.buseta.nlb.ui.NlbNewsFragment;
import com.alvinhkh.buseta.nwst.ui.NwstNoticeFragment;
import com.alvinhkh.buseta.route.model.Route;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;


public class RouteAnnounceActivity extends BaseActivity {

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle bundle = getIntent().getExtras();
        Route route = null;
        if (bundle != null) {
            route = bundle.getParcelable(C.EXTRA.ROUTE_OBJECT);
        }
        if (route == null || TextUtils.isEmpty(route.getName()) || TextUtils.isEmpty(route.getCompanyCode())) {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // set action bar
        setToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.notice);
            actionBar.setSubtitle(route.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        adViewContainer = findViewById(R.id.adView_container);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false);
        }

        fab = findViewById(R.id.fab);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch (route.getCompanyCode()) {
            case C.PROVIDER.KMB:
                fragmentTransaction.replace(R.id.fragment_container, KmbAnnounceFragment.newInstance(route));
                break;
            case C.PROVIDER.NLB:
                fragmentTransaction.replace(R.id.fragment_container, NlbNewsFragment.newInstance());
                break;
            case C.PROVIDER.CTB:
            case C.PROVIDER.NWFB:
            case C.PROVIDER.NWST:
                fragmentTransaction.replace(R.id.fragment_container, NwstNoticeFragment.newInstance(route));
                break;
            default:
                Toast.makeText(this, "invalid company: " + route.getCompanyCode(), Toast.LENGTH_SHORT).show();
                finish();
                return;
        }
        fragmentTransaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fab != null) {
            fab.hide();
        }
    }
}
