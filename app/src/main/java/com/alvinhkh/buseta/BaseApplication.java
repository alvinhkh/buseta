package com.alvinhkh.buseta;

import android.app.Application;

import com.alvinhkh.buseta.utils.NightModeHelper;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
        formUri = "https://alvinhkh.cloudant.com/acra-buseta/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="whingerriandesionfeeksou",
        formUriBasicAuthPassword="3316b6e0c5b2371ec8cff1652557378d649abec3",
        // usual configuration
        mode = ReportingInteractionMode.SILENT
)

public class BaseApplication extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        if (!BuildConfig.DEBUG) ACRA.init(this); // filter out debugging

        NightModeHelper.update(this);

    }

}