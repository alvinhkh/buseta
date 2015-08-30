package com.alvinhkh.buseta.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsHelper {

    private Integer etaApi = 2;

    private Boolean homeScreenSearchButton = true;

    private Boolean loadStopImage = false;

    public SettingsHelper() {
    }

    public Integer getEtaApi() {
        return etaApi;
    }

    public void setEtaApi(Integer etaApi) {
        this.etaApi = etaApi;
    }

    public Boolean getHomeScreenSearchButton() {
        return homeScreenSearchButton;
    }

    public void setHomeScreenSearchButton(Boolean homeScreenSearchButton) {
        this.homeScreenSearchButton = homeScreenSearchButton;
    }

    public Boolean getLoadStopImage(){
        return loadStopImage;
    }

    public void setLoadStopImage(Boolean loadStopImage){
        this.loadStopImage = loadStopImage;
    }

    public SettingsHelper parse(Context ctx) {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return parse(sPref);
    }

    public SettingsHelper parse(SharedPreferences sPref) {
        try {
            setEtaApi(Integer.valueOf(sPref.getString("eta_version", "2")));
        } catch (ClassCastException e) {
            e.printStackTrace();
            setEtaApi(2);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            setEtaApi(2);
        }
        setHomeScreenSearchButton(sPref.getBoolean("home_search_button", true));
        setLoadStopImage(sPref.getBoolean("load_stop_image", false));
        return this;
    }

}