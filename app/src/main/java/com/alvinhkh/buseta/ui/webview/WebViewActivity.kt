package com.alvinhkh.buseta.ui.webview

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.Toast

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.ui.BaseActivity


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

    companion object {

        const val TITLE = "title"

        const val HTML = "html"

    }
}
