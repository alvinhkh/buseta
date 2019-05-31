package com.alvinhkh.buseta.ui.route

import android.os.Bundle

import com.alvinhkh.buseta.kmb.ui.KmbAnnounceFragment
import com.alvinhkh.buseta.nlb.ui.NlbNewsFragment
import com.alvinhkh.buseta.nwst.ui.NwstNoticeFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.Toast

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.utils.AdViewUtil


class RouteAnnounceActivity : BaseActivity() {

    private var fab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bundle = intent.extras
        var route: Route? = null
        if (bundle != null) {
            route = bundle.getParcelable(C.EXTRA.ROUTE_OBJECT)
        }
        if (route == null || route.name.isNullOrEmpty() || route.companyCode.isNullOrEmpty()) {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // set action bar
        setToolbar()
        supportActionBar?.setTitle(R.string.notice)
        supportActionBar?.subtitle = route.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adViewContainer = findViewById(R.id.adView_container)
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false)
        }

        fab = findViewById(R.id.fab)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val b = Bundle()
        b.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
        when (route.companyCode) {
            C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                val f = KmbAnnounceFragment()
                f.arguments = b
                fragmentTransaction.replace(R.id.fragment_container, f)
            }
            C.PROVIDER.NLB -> fragmentTransaction.replace(R.id.fragment_container, NlbNewsFragment())
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                val f = NwstNoticeFragment()
                f.arguments = b
                fragmentTransaction.replace(R.id.fragment_container, f)
            }
            else -> {
                Toast.makeText(this, "invalid provider: " + route.companyCode, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }
        fragmentTransaction.commit()
    }

    public override fun onResume() {
        super.onResume()
        fab?.hide()
    }
}
