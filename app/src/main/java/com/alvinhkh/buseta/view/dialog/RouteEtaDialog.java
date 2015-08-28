package com.alvinhkh.buseta.view.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteStop;

import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class RouteEtaDialog extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "RouteEtaDialog";

    private Context mContext;
    private ImageView iStar;
    private ImageView iRefresh;
    private ProgressBar progressBar;
    private TextView tStopName;
    private TextView tEta;
    private TextView lServerTime;
    private TextView tServerTime;
    private TextView lLastUpdated;
    private TextView tLastUpdated;

    RouteStop object = null;
    Integer position = null;

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
        // get widgets
        iStar = (ImageView) findViewById(R.id.star);
        iRefresh = (ImageView) findViewById(R.id.refresh);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tStopName = (TextView) findViewById(R.id.stop_name);
        tEta = (TextView) findViewById(android.R.id.text1);
        lServerTime = (TextView) findViewById(R.id.label_serverTime);
        tServerTime = (TextView) findViewById(R.id.textView_serverTime);
        lLastUpdated = (TextView) findViewById(R.id.label_updated);
        tLastUpdated = (TextView) findViewById(R.id.textView_updated);
        //
        iRefresh.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        // check from the saved Instance

        // Or passed from the other activity
        if (extras != null) {
            object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
            parse();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
    
    private void parse() {
        if (null == object || null == object.eta) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        tStopName.setText(object.name_tc);
        // Request Time
        SimpleDateFormat display_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String server_time = "";
        Date server_date = null;
        if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
            if (object.eta.api_version == 2) {
                server_date = new Date(Long.parseLong(object.eta.server_time));
            } else if (object.eta.api_version == 1) {
                SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                try {
                    server_date = date_format.parse(object.eta.server_time);
                } catch (ParseException ep) {
                    ep.printStackTrace();
                }
            }
            server_time = (null != server_date) ?
                    display_format.format(server_date) : object.eta.server_time;
        }
        // last updated
        String updated_time = "";
        Date updated_date = null;
        if (null != object.eta.updated && !object.eta.updated.equals("")) {
            if (object.eta.api_version == 2) {
                updated_date = new Date(Long.parseLong(object.eta.updated));
            }
            updated_time = (null != updated_date) ?
                    display_format.format(updated_date) : object.eta.updated;
        }
        // ETAs
        String eta = Jsoup.parse(object.eta.etas).text();
        String[] etas = eta.replaceAll("　", " ").split(", ?");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etas.length; i++) {
            sb.append(etas[i]);
            if (object.eta.api_version == 1) {
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
            } else if (object.eta.api_version == 2) {
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
                            sb.append("現在");
                        } else {
                            sb.append(etaMinutes);
                            sb.append("分鐘");
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                iRefresh.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != object)
            outState.putParcelable(Constants.BUNDLE.STOP_OBJECT, object);
        if (null != position)
            outState.putInt(Constants.BUNDLE.ITEM_POSITION, position);
    }

}
