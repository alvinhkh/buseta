package com.alvinhkh.buseta.ui.image;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.ui.BaseActivity;


public class ImageActivity extends BaseActivity {

    public final static String IMAGE_TITLE = "title";

    public final static String IMAGE_URL = "url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // set action bar
        setToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        Bundle bundle = getIntent().getExtras();
        String imageTitle = bundle.getString(IMAGE_TITLE);
        String imageUrl = bundle.getString(IMAGE_URL);
        if (TextUtils.isEmpty(imageTitle) || TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show();
            finish();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, ImageFragment.newInstance(imageTitle, imageUrl));
        fragmentTransaction.commit();
    }
}
