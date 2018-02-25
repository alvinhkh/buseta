package com.alvinhkh.buseta.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.ui.search.SearchActivity;

public class MainActivity extends AppCompatActivity {

    // legacy placeholder activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClass(this, com.alvinhkh.buseta.ui.MainActivity.class);
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }
}
