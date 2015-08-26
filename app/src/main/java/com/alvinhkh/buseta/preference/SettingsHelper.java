package com.alvinhkh.buseta.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsHelper {

    private Integer etaApi = 2;

    public SettingsHelper() {
    }

    public Integer getEtaApi() {
        return etaApi;
    }

    public void setEtaApi(Integer etaApi) {
        this.etaApi = etaApi;
    }

    public SettingsHelper parse(Context ctx) {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        try {
            setEtaApi(Integer.valueOf(sPref.getString("eta_version", "2")));
        } catch (ClassCastException e) {
            e.printStackTrace();
            setEtaApi(2);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            setEtaApi(2);
        }
        return this;
    }

}