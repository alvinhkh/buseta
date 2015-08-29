package com.alvinhkh.buseta.view.dialog;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.alvinhkh.buseta.holder.RouteStop;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private RouteStop _routeStop = null;
    private Integer position = null;
    private Boolean favourite = false;

    private Cursor mCursor;
    private FavouriteDatabase mDatabase;
    private Bitmap mBitmap = null;

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
            _routeStop = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
            parse();
        } else {
            finish();
        }
        //
        mCursor = mDatabase.getExist(_routeStop);
        favourite = (null != mCursor && mCursor.getCount() > 0);
        iStar.setImageResource(favourite == true ?
                R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                onRefresh();
                final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        R.string.message_default_auto_refresh, Snackbar.LENGTH_LONG);
                TextView tv = (TextView)
                        snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                snackbar.show();
                break;
            case R.id.star:
                if (null == mDatabase || null == _routeStop || null == _routeStop.route_bound) break;
                mCursor = mDatabase.getExist(_routeStop);
                Boolean org = favourite;
                if (null != mCursor && mCursor.getCount() > 0) {
                    // record exist
                    org = true;
                    favourite = mDatabase.delete(_routeStop) ? false : true;
                } else {
                    org = false;
                    favourite = mDatabase.insertStop(_routeStop) > 0 ? true : false;
                }
                if (org != favourite)
                    iStar.startAnimation(animationRotate);
                iStar.setImageResource(favourite == true ?
                        R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
                _routeStop.favourite = favourite;
                sendUpdate();
                break;
            case R.id.stop_name:
                getStopImage();
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (null != mCursor)
            mCursor.close();
        if (null != mDatabase)
            mDatabase.close();
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroy();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != _routeStop)
            outState.putParcelable(Constants.BUNDLE.STOP_OBJECT, _routeStop);
        if (null != position)
            outState.putInt(Constants.BUNDLE.ITEM_POSITION, position);
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
        // TODO: get data from service
        // iRefresh.setVisibility(View.VISIBLE);
        // progressBar.setVisibility(View.GONE);
        sendUpdate();
    }

    private void sendUpdate() {
        Intent intent = new Intent(Constants.MESSAGE.STOP_UPDATED);
        intent.putExtra(Constants.MESSAGE.STOP_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, _routeStop);
        sendBroadcast(intent);
    }
    
    private void parse() {
        if (null == _routeStop) {
            finish();
            return;
        }
        tStopName.setText(_routeStop.name_tc);
        if (null == _routeStop.eta) {
            tEta.setVisibility(View.GONE);
            lServerTime.setVisibility(View.GONE);
            tServerTime.setVisibility(View.GONE);
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
            return;
        } else {
            tEta.setVisibility(View.VISIBLE);
            lServerTime.setVisibility(View.VISIBLE);
            tServerTime.setVisibility(View.VISIBLE);
            lLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setVisibility(View.VISIBLE);
        }
        // Request Time
        SimpleDateFormat display_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String server_time = "";
        Date server_date = null;
        if (null != _routeStop.eta.server_time && !_routeStop.eta.server_time.equals("")) {
            if (_routeStop.eta.api_version == 2) {
                server_date = new Date(Long.parseLong(_routeStop.eta.server_time));
            } else if (_routeStop.eta.api_version == 1) {
                SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                try {
                    server_date = date_format.parse(_routeStop.eta.server_time);
                } catch (ParseException ep) {
                    ep.printStackTrace();
                }
            }
            server_time = (null != server_date) ?
                    display_format.format(server_date) : _routeStop.eta.server_time;
        }
        // last updated
        String updated_time = "";
        Date updated_date = null;
        if (null != _routeStop.eta.updated && !_routeStop.eta.updated.equals("")) {
            if (_routeStop.eta.api_version == 2) {
                updated_date = new Date(Long.parseLong(_routeStop.eta.updated));
            }
            updated_time = (null != updated_date) ?
                    display_format.format(updated_date) : _routeStop.eta.updated;
        }
        // ETAs
        String eta = Jsoup.parse(_routeStop.eta.etas).text();
        String[] etas = eta.replaceAll("　", " ").split(", ?");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etas.length; i++) {
            sb.append(etas[i]);
            if (_routeStop.eta.api_version == 1) {
                // API v1 from Web, with minutes no time
                String minutes = etas[i].replaceAll("[^0123456789]", "");
                if (null != server_date && !minutes.equals("") &&
                        etas[i].contains("分鐘")) {
                    Long t = server_date.getTime();
                    Date etaDate = new Date(t + (Integer.parseInt(minutes) * 60000));
                    SimpleDateFormat eta_time_format = new SimpleDateFormat("HH:mm");
                    String etaTime = eta_time_format.format(etaDate);
                    sb.append(" (");
                    sb.append(etaTime);
                    sb.append(")");
                }
            } else if (_routeStop.eta.api_version == 2) {
                // API v2 from Mobile v2, with exact time
                if (etas[i].matches(".*\\d.*")) {
                    // if text has digit
                    String etaMinutes = "";
                    long differences = new Date().getTime() - server_date.getTime(); // get device time and compare to server time
                    try {
                        SimpleDateFormat time_format =
                                new SimpleDateFormat("yyyy/MM/dd HH:mm");
                        Date etaDateCompare = server_date;
                        // first assume eta time and server time is on the same date
                        Date etaDate = time_format.parse(
                                new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                        new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                        new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                        etas[i]);
                        // if not minutes will get negative integer
                        int minutes = (int) ((etaDate.getTime() / 60000) -
                                ((server_date.getTime() + differences) / 60000));
                        if (minutes < -12 * 60) {
                            // plus one day to get correct eta date
                            etaDateCompare = new Date(server_date.getTime() + 1 * 24 * 60 * 60 * 1000);
                            etaDate = time_format.parse(
                                    new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                            new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                            new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                            etas[i]);
                            minutes = (int) ((etaDate.getTime() / 60000) -
                                    ((server_date.getTime() + differences) / 60000));
                        }
                        // minutes should be 0 to within a day
                        if (minutes >= 0 && minutes < 1 * 24 * 60 * 60 * 1000)
                            etaMinutes = String.valueOf(minutes);
                    } catch (ParseException ep) {
                        ep.printStackTrace();
                    }
                    if (!etaMinutes.equals("")) {
                        sb.append(" (");
                        if (etaMinutes.equals("0")) {
                            sb.append(getString(R.string.now));
                        } else {
                            sb.append(etaMinutes);
                            sb.append(getString(R.string.minutes));
                        }
                        sb.append(")");
                    }
                }
            }
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
        if (null == _routeStop) return;
        progressBar.setVisibility(View.VISIBLE);
        iStop.setVisibility(View.VISIBLE);
        View container = findViewById(R.id.imageContainer);
        container.setVisibility(View.VISIBLE);
        Ion.with(mContext)
                .load(Constants.URL.ROUTE_STOP_IMAGE + _routeStop.code)
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

}
