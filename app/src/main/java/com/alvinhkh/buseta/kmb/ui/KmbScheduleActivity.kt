package com.alvinhkh.buseta.kmb.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.Toast
import com.alvinhkh.buseta.C

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.utils.ColorUtil
import com.google.android.material.tabs.TabLayout


class KmbScheduleActivity : BaseActivity() {

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
                activityColor(if (!route.colour.isNullOrEmpty()) {
                    Color.parseColor(route.colour)
                } else {
                    route.companyColour(this)
                })
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                val fragment = KmbScheduleFragment()
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

    private fun activityColor(color: Int) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.statusBarColor = ColorUtil.darkenColor(color)
            window?.navigationBarColor = ColorUtil.darkenColor(color)
        }
        findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
        findViewById<TabLayout>(R.id.tabs)?.background = ColorDrawable(color)
    }
}
