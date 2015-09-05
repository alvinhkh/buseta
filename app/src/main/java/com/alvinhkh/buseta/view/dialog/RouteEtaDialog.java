package com.alvinhkh.buseta.view.dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteDatabase;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Date;


public class RouteEtaDialog extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RouteEtaDialog";

    private Context mContext;
    private ImageView iStop;
    private ImageView iStar;
    private ImageView iRefresh;
    private ProgressBar progressBar;
    private TextView tStopName;
    private TextView tEta;
    private TextView lServerTime;
    private TextView tServerTime;
    private TextView lLastUpdated;
    private TextView tLastUpdated;
    private Animation animationRotate;
    private Bitmap mBitmap = null;

    private ArrayList<RouteStopContainer> list = null;
    private RouteStop object = null;
    private Integer position = null;
    private Boolean favourite = false;
    private Boolean hideStar = false;

    private Cursor mCursor;
    private FavouriteDatabase mDatabase;
    private UpdateEtaReceiver mReceiver;

    Handler mAutoRefreshHandler = new Handler();
    Runnable mAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            onRefresh();
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 30 * 1000); // every half minute
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.dialog_eta);
        // side dialog width and height
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // prevent cancel when touch outside
        setFinishOnTouchOutside(true);
        // get context
        mContext = RouteEtaDialog.this;
        // set database
        SettingsHelper settingsHelper = new SettingsHelper().parse(mContext.getApplicationContext());
        mDatabase = new FavouriteDatabase(mContext);
        // get widgets
        iStop = (ImageView) findViewById(R.id.imageView);
        iStar = (ImageView) findViewById(R.id.star);
        iRefresh = (ImageView) findViewById(R.id.refresh);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tStopName = (TextView) findViewById(R.id.stop_name);
        tEta = (TextView) findViewById(android.R.id.text1);
        lServerTime = (TextView) findViewById(R.id.label_serverTime);
        tServerTime = (TextView) findViewById(R.id.textView_serverTime);
        lLastUpdated = (TextView) findViewById(R.id.label_updated);
        tLastUpdated = (TextView) findViewById(R.id.textView_updated);
        animationRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_once);
        animationRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        //
        tStopName.setOnClickListener(this);
        iStar.setOnClickListener(this);
        iRefresh.setOnClickListener(this);
        // check from the saved Instance
        Bundle extras = getIntent().getExtras();
        // Or passed from the other activity
        if (extras != null) {
            hideStar = extras.getBoolean(Constants.MESSAGE.HIDE_STAR);
            position = extras.getInt(Constants.BUNDLE.ITEM_POSITION, -1);
            object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
            list = extras.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
            parse();
        } else {
            finish();
        }
        //
        mCursor = mDatabase.getExist(object);
        favourite = (null != mCursor && mCursor.getCount() > 0);
        iStar.setImageResource(favourite ?
                R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
        if (settingsHelper.getLoadStopImage())
            getStopImage();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                onRefresh();
                final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        R.string.message_reminder_auto_refresh, Snackbar.LENGTH_LONG);
                TextView tv = (TextView)
                        snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                snackbar.show();
                break;
            case R.id.star:
                if (null == mDatabase || null == object || null == object.route_bound) break;
                mCursor = mDatabase.getExist(object);
                Boolean org;
                if (null != mCursor && mCursor.getCount() > 0) {
                    // record exist
                    org = true;
                    favourite = !mDatabase.delete(object);
                } else {
                    org = false;
                    favourite = mDatabase.insertStop(object) > 0;
                }
                if (org != favourite)
                    iStar.startAnimation(animationRotate);
                iStar.setImageResource(favourite ?
                        R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
                object.favourite = favourite;
                sendUpdate();
                break;
            case R.id.stop_name:
                getStopImage();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.post(mAutoRefreshRunnable);
        if (null != mContext) {
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mReceiver = new UpdateEtaReceiver();
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver, mFilter_eta);
        }
    }

    @Override
    public void onPause() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        if (null != mContext) {
            if (null != mReceiver)
                mContext.unregisterReceiver(mReceiver);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        if (null != mCursor)
            mCursor.close();
        if (null != mDatabase)
            mDatabase.close();
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroy();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != object)
            outState.putParcelable(Constants.BUNDLE.STOP_OBJECT, object);
        if (null != position)
            outState.putInt(Constants.BUNDLE.ITEM_POSITION, position);
        if (null != list)
            outState.putParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS, list);
        mBitmap = Ion.with(iStop).getBitmap();
        outState.putParcelable("stop_image_bitmap", mBitmap);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (null == savedInstanceState) return;
        mBitmap = savedInstanceState.getParcelable("stop_image_bitmap");
        if (null != mBitmap) {
            iStop.setImageBitmap(mBitmap);
            iStop.setVisibility(View.VISIBLE);
            View container = findViewById(R.id.imageContainer);
            container.setVisibility(View.VISIBLE);
        }
    }

    private void onRefresh() {
        iRefresh.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, CheckEtaService.class);
        intent.putExtra(Constants.BUNDLE.ITEM_POSITION, position);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, list);
        startService(intent);
    }

    private void sendUpdate() {
        Intent intent = new Intent(Constants.MESSAGE.STOP_UPDATED);
        intent.putExtra(Constants.MESSAGE.STOP_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.ITEM_POSITION, position);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, list);
        getApplication().sendBroadcast(intent);
    }
    
    private void parse() {
        if (null == object) {
            finish();
            return;
        }
        iStar.setVisibility(hideStar ? View.GONE : View.VISIBLE);
        tStopName.setText(object.name_tc);
        tEta.setVisibility(View.VISIBLE);
        if (object.eta_loading != null && object.eta_loading) {
            if (tEta.getText().equals(""))
                tEta.setText(R.string.message_loading);
        } else if (object.eta_fail != null && object.eta_fail) {
            tEta.setText(R.string.message_fail_to_request);
        } else if (null == object.eta || object.eta.etas.equals("")) {
            tEta.setText(R.string.message_no_data);
        }
        if (null == object.eta || object.eta.etas.equals("")) {
            lServerTime.setVisibility(View.GONE);
            tServerTime.setVisibility(View.GONE);
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
            return;
        } else {
            lServerTime.setVisibility(View.VISIBLE);
            tServerTime.setVisibility(View.VISIBLE);
            lLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setVisibility(View.VISIBLE);
        }
        // Request Time
        String server_time = "";
        Date server_date = null;
        if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
            server_date = EtaAdapterHelper.serverDate(object);
            server_time = (null != server_date) ?
                    EtaAdapterHelper.display_format.format(server_date) : object.eta.server_time;
        }
        // last updated
        String updated_time = "";
        Date updated_date;
        if (null != object.eta.updated && !object.eta.updated.equals("")) {
            updated_date = EtaAdapterHelper.updatedDate(object);
            updated_time = (null != updated_date) ?
                    EtaAdapterHelper.display_format.format(updated_date) : object.eta.updated;
        }
        // ETAs
        String eta = Jsoup.parse(object.eta.etas).text();
        String[] etas = eta.replaceAll("　", " ").split(", ?");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etas.length; i++) {
            sb.append(etas[i]);
            String estimate = EtaAdapterHelper.etaEstimate(object, etas, i, server_date, null, null, null);
            sb.append(estimate);
            if (i < etas.length - 1)
                sb.append("\n");
        }
        tEta.setText(sb.toString());
        if (null != server_time && !server_time.equals("")) {
            tServerTime.setText(server_time);
        }
        if (null != updated_time && !updated_time.equals("")) {
            lLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setText(updated_time);
        } else {
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
        }
    }

    private void getStopImage() {
        if (null == object) return;
        progressBar.setVisibility(View.VISIBLE);
        iStop.setVisibility(View.VISIBLE);
        View container = findViewById(R.id.imageContainer);
        container.setVisibility(View.VISIBLE);
        Ion.with(mContext)
                .load(Constants.URL.ROUTE_STOP_IMAGE + object.code)
                .progressBar(progressBar)
                .withBitmap()
                .error(R.drawable.ic_error_outline_black_48dp)
                .resize(340, 255)
                .centerCrop()
                .animateLoad(R.anim.fade_in)
                .intoImageView(iStop)
                .setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        mBitmap = Ion.with(iStop).getBitmap();
                        iStop.setVisibility(View.VISIBLE);
                        if (null != progressBar)
                            progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public class UpdateEtaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (aBoolean) {
                RouteStop routeStop = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != routeStop) {
                    object.eta = routeStop.eta;
                    object.eta_loading = routeStop.eta_loading;
                    object.eta_fail = routeStop.eta_fail;
                    parse();
                }
                iRefresh.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }
    }

}
