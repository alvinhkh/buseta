package com.alvinhkh.buseta.kmb.ui

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.Toast
import com.alvinhkh.buseta.C

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.ui.BaseActivity


class KmbBBIActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setToolbar()
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.subtitle = null
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab?.hide()

        val bundle = intent.extras
        if (bundle != null) {
            val route: Route? = bundle.getParcelable(C.EXTRA.ROUTE_OBJECT)
            if (route != null) {
                val routeNo = route.name
                val routeBound = route.sequence
                if (routeNo.isNullOrEmpty() || routeBound.isNullOrEmpty()) {
                    Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
                    finish()
                }
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                val fragment = KmbBBIFragment()
                fragment.arguments = bundle
                fragmentTransaction.replace(R.id.fragment_container, fragment)
                fragmentTransaction.commit()
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
}
