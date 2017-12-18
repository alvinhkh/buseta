package com.alvinhkh.buseta.ui.route;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.ui.KmbAnnounceFragment;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nlb.ui.NlbNewsFragment;
import com.alvinhkh.buseta.nwst.ui.NwstNoticeFragment;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.NightModeUtil;


public class RouteAnnounceActivity extends BaseActivity {

    public static final String ROUTE_COMPANY = "route_company";

    public static final String ROUTE_NO = "route_no";

    public static final String ROUTE_SEQ = "route_seq";

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle bundle = getIntent().getExtras();
        String routeCompany = bundle.getString(ROUTE_COMPANY);
        String routeNo = bundle.getString(ROUTE_NO);
        String routeSeq = bundle.getString(ROUTE_SEQ);

        // set action bar
        setToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.notice);
            actionBar.setSubtitle(routeNo);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        fab = findViewById(R.id.fab);

        if (TextUtils.isEmpty(routeNo) || TextUtils.isEmpty(routeSeq) || TextUtils.isEmpty(routeCompany)) {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        BusRoute busRoute = new BusRoute();
        busRoute.setCompanyCode(routeCompany);
        busRoute.setName(routeNo);
        busRoute.setSequence(routeSeq);
        switch (routeCompany) {
            case BusRoute.COMPANY_KMB:
                fragmentTransaction.replace(R.id.fragment_container, KmbAnnounceFragment.newInstance(busRoute));
                break;
            case BusRoute.COMPANY_NLB:
                fragmentTransaction.replace(R.id.fragment_container, NlbNewsFragment.newInstance());
                break;
            case BusRoute.COMPANY_CTB:
            case BusRoute.COMPANY_NWFB:
            case BusRoute.COMPANY_NWST:
                fragmentTransaction.replace(R.id.fragment_container, NwstNoticeFragment.newInstance(busRoute));
                break;
            default:
                Toast.makeText(this, "invalid company: " + routeCompany, Toast.LENGTH_SHORT).show();
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
