package com.alvinhkh.buseta.ui.route;

import android.os.Bundle;

import com.alvinhkh.buseta.kmb.ui.KmbAnnounceFragment;
import com.alvinhkh.buseta.nlb.ui.NlbNewsFragment;
import com.alvinhkh.buseta.nwst.ui.NwstNoticeFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import android.text.TextUtils;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
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
        Bundle b = new Bundle();
        b.putParcelable(C.EXTRA.ROUTE_OBJECT, route);
        switch (route.getCompanyCode()) {
            case C.PROVIDER.KMB:
            case C.PROVIDER.LWB:
            {
                Fragment f = new KmbAnnounceFragment();
                f.setArguments(b);
                fragmentTransaction.replace(R.id.fragment_container, f);
            }
                break;
            case C.PROVIDER.NLB:
                fragmentTransaction.replace(R.id.fragment_container, new NlbNewsFragment());
                break;
            case C.PROVIDER.CTB:
            case C.PROVIDER.NWFB:
            case C.PROVIDER.NWST:
            {
                Fragment f = new NwstNoticeFragment();
                f.setArguments(b);
                fragmentTransaction.replace(R.id.fragment_container, f);
            }
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
