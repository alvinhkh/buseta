package com.alvinhkh.buseta.view.dialog;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.view.ControllableAppBarLayout;
import com.alvinhkh.buseta.view.fragment.RouteEtaFragment;
import com.koushikdutta.ion.Ion;

import static android.util.TypedValue.*;

public class RouteEtaActivity extends AppCompatActivity {

    private static final String TAG = RouteEtaActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_eta);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTaskDescription(getString(R.string.app_name));
        setFinishOnTouchOutside(true);
        Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        ControllableAppBarLayout appBarLayout = (ControllableAppBarLayout) findViewById(R.id.AppBar);
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int layoutHeightDp = 384;
        if (size.y < layoutHeightDp) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) dpToPixels(this, layoutHeightDp));
        }
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        lp.height = (int) dpToPixels(this, 256);
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            appBarLayout.setExpanded(false);
        }
        appBarLayout.collapseToolbar();

        // Display the fragment as the main content.
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            RouteEtaFragment f = new RouteEtaFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, f).commit();
        }
    }

    @Override
    public void onDestroy() {
        Ion.getDefault(this).cancelAll(this);
        super.onDestroy();
    }

    public static float dpToPixels(Context context, int dp) {
        return applyDimension(COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int pixelsToDp(Context context, float pixels) {
        float density = context.getResources().getDisplayMetrics().densityDpi;
        return Math.round(pixels / (density / 160f));
    }

    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    // orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    // orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    // orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    // orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }
        return orientation;
    }

    private void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(this, R.color.primary_600));
            setTaskDescription(taskDesc);
        }
    }

}
