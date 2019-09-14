package com.alvinhkh.buseta.ui.webview

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.utils.ColorUtil
import com.google.android.material.tabs.TabLayout


class WebViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        setToolbar()
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.subtitle = null
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab?.hide()

        val bundle = intent.extras
        if (bundle != null) {
            val color = bundle.getInt(COLOUR, 0)
            if (color != 0) {
                activityColor(color)
            }
            val contentTitle = bundle.getString(TITLE)
            val contentHtml = bundle.getString(HTML)
            if (contentTitle.isNullOrEmpty() || contentHtml.isNullOrEmpty()) {
                Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, WebViewFragment.newInstance(contentTitle, contentHtml))
            fragmentTransaction.commit()
        } else {
            finish()
        }
    }

    private fun activityColor(color: Int) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            window?.statusBarColor = color
            window?.navigationBarColor = ContextCompat.getColor(this, R.color.transparent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.statusBarColor = ColorUtil.darkenColor(color)
            window?.navigationBarColor = ColorUtil.darkenColor(color)
        }
        findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
    }

    companion object {

        const val TITLE = "title"

        const val HTML = "html"

        const val COLOUR = "colour"

    }
}
