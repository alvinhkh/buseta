package com.alvinhkh.buseta.mtr.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.ui.MtrLineStationsFragment
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.utils.AdViewUtil

class MtrActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbar()
        supportActionBar?.run {
            title = getString(R.string.app_name)
            subtitle = null
            setDisplayHomeAsUpEnabled(true)
        }
        adViewContainer = findViewById(R.id.adView_container)
        adView = AdViewUtil.banner(adViewContainer)
        intent.extras?.run {
            val lineCode = this.getString(C.EXTRA.LINE_CODE)
            val lineColour = this.getString(C.EXTRA.LINE_COLOUR)
            val lineName = this.getString(C.EXTRA.LINE_NAME)
            if (!lineCode.isNullOrEmpty()) {
                adViewContainer.setBackgroundColor(Color.parseColor(lineColour))
                val fragmentManager = supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragment_container, MtrLineStationsFragment.newInstance(lineCode, lineColour, lineName))
                fragmentTransaction.commit()
                return
            }
        }
        Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
        finish()
    }
}
