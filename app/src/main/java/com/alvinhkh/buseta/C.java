package com.alvinhkh.buseta;


public class C {

    public interface ACTION {
        String APP_UPDATE = "com.alvinhkh.buseta.APP_UPDATE";

        String CANCEL = "com.alvinhkh.buseta.CANCEL";

        String ETA_UPDATE = "com.alvinhkh.buseta.ETA_UPDATE";

        String FOLLOW_UPDATE = "com.alvinhkh.buseta.FOLLOW_UPDATE";

        String HISTORY_UPDATE = "com.alvinhkh.buseta.HISTORY_UPDATE";

        String LOCATION_UPDATE = "com.alvinhkh.buseta.LOCATION_UPDATE";

        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";

        String NOTIFICATION_UPDATE = "com.alvinhkh.buseta.NOTIFICATION_ID";

        String SUGGESTION_ROUTE_UPDATE = "com.alvinhkh.buseta.SUGGESTION_ROUTE_UPDATE";
    }

    public interface EXTRA {
        String COMPANY = "com.alvinhkh.buseta.COMPANY";

        String COMPLETE = "com.alvinhkh.buseta.COMPLETE";

        String REQUEST = "com.alvinhkh.buseta.REQUEST";

        String QUERY = "com.alvinhkh.buseta.QUERY";

        String FAIL = "com.alvinhkh.buseta.FAIL";

        String UPDATED = "com.alvinhkh.buseta.UPDATED";

        String UPDATING = "com.alvinhkh.buseta.UPDATING";

        String LOCATION_OBJECT = "com.alvinhkh.buseta.LOCATION_OBJECT";

        String ROUTE_NO = "com.alvinhkh.buseta.ROUTE_NO";

        String ROUTE_OBJECT = "com.alvinhkh.buseta.ROUTE_OBJECT";

        String STOP_LIST = "com.alvinhkh.buseta.STOP_LIST";

        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";

        String STOP_OBJECT_STRING = "com.alvinhkh.buseta.STOP_OBJECT_STRING";

        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";

        String NOTIFICATION_ID = "com.alvinhkh.buseta.NOTIFICATION_ID";

        String MANUAL = "com.alvinhkh.buseta.MANUAL";

        String ROW = "com.alvinhkh.buseta.ROW";

        String MESSAGE_RID = "com.alvinhkh.buseta.MESSAGE_RID";

        String APP_UPDATE_OBJECT = "com.alvinhkh.buseta.APP_UDPATE_OBJECT";
    }

    public interface PREF {
        String APP_UPDATE_VERSION = "com.alvinhkh.buseta.APP_UPDATE_VERSION";

        String VERSION_RECORD = "com.alvinhkh.buseta.VERSION_RECORD";

        String AD_KEY = "I HATE ADS!";

        String AD_SHOW = "SHOW ME";

        String AD_HIDE = "com.alvinhkh.buseta.AD_HIDE";

        String GEOFENCES_KEY = "com.alvinhkh.buseta.GEOFENCES_KEY";
    }

    public interface URI {
        String APP = "android-app://com.alvinhkh.buseta/buseta/route";

        String ROUTE = "buseta://route/";

        String BOUND = "buseta://route/bound/";

        String STOP = "buseta://route/stop/";
    }

    public interface NOTIFICATION {
        String CHANNEL_FOREGROUND = "CHANNEL_ID_FOREGROUND";

        String CHANNEL_ETA = "CHANNEL_ID_ETA";

        String CHANNEL_ARRIVAL_ALERT = "CHANNEL_ID_ARRIVAL_ALERT";
    }

    public interface GEOFENCE {
        Integer RADIUS_IN_METERS = 200;

        Integer EXPIRATION_IN_MILLISECONDS = 14400000;
    }
}
