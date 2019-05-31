package com.alvinhkh.buseta.ui.image

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.View
import android.widget.Toast

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.ui.BaseActivity


class ImageActivity : BaseActivity() {

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
            val imageTitle = bundle.getString(IMAGE_TITLE)
            val imageUrl = bundle.getString(IMAGE_URL)
            val taskDescription = bundle.getString(TASK_DESC)?:""
            if (imageTitle.isNullOrEmpty() || imageUrl.isNullOrEmpty()) {
                Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
                finish()
            }
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, ImageFragment.newInstance(imageTitle?:"", imageUrl?:"", taskDescription))
            fragmentTransaction.commit()
        } else {
            finish()
        }
    }

    companion object {

        const val IMAGE_TITLE = "title"

        const val IMAGE_URL = "url"

        const val TASK_DESC = "task_description"
    }
}
