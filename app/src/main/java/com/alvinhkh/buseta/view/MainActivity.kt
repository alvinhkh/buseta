package com.alvinhkh.buseta.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // legacy placeholder activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent()
        intent.setClass(this, com.alvinhkh.buseta.ui.MainActivity::class.java)
        if (getIntent().extras != null) {
            intent.putExtras(getIntent().extras!!)
        }
        startActivity(intent)
        finish()
    }
}
