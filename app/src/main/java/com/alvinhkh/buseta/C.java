package com.alvinhkh.buseta;

public class C {

    public interface ACTION {
        String APP_UPDATE = "com.alvinhkh.buseta.APP_UPDATE";

        String CANCEL = "com.alvinhkh.buseta.CANCEL";

        String ETA_UPDATE = "com.alvinhkh.buseta.ETA_UPDATE";

        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";

        String NOTIFICATION_UPDATE = "com.alvinhkh.buseta.NOTIFICATION_ID";

        String SUGGESTION_ROUTE_UPDATE = "com.alvinhkh.buseta.SUGGESTION_ROUTE_UPDATE";
    }

    public interface EXTRA {
        String COMPANY_CODE = "com.alvinhkh.buseta.COMPANY_CODE";

        String COMPLETE = "com.alvinhkh.buseta.COMPLETE";

        String FAIL = "com.alvinhkh.buseta.FAIL";

        String FOLLOW = "com.alvinhkh.buseta.FOLLOW";

        String FOLLOW_OBJECT = "com.alvinhkh.buseta.FOLLOW_OBJECT";

        String GROUP_ID = "com.alvinhkh.buseta.GROUP_ID";

        String UPDATED = "com.alvinhkh.buseta.UPDATED";

        String UPDATING = "com.alvinhkh.buseta.UPDATING";

        String LOAD_STOP = "com.alvinhkh.buseta.LOAD_STOP";

        String LOCATION_OBJECT = "com.alvinhkh.buseta.LOCATION_OBJECT";

        String ROUTE_ID = "com.alvinhkh.buseta.ROUTE_ID";

        String ROUTE_NO = "com.alvinhkh.buseta.ROUTE_NO";

        String ROUTE_OBJECT = "com.alvinhkh.buseta.ROUTE_OBJECT";

        String ROUTE_SEQUENCE = "com.alvinhkh.buseta.ROUTE_SEQUENCE";

        String ROUTE_SERVICE_TYPE = "com.alvinhkh.buseta.ROUTE_SERVICE_TYPE";

        String ROUTE_TIMETABLE_FILE = "com.alvinhkh.buseta.ROUTE_TIMETABLE_FILE";

        String ROUTE_REMARK_FILE = "com.alvinhkh.buseta.ROUTE_REMARK_FILE";

        String STOP_ID = "com.alvinhkh.buseta.STOP_ID";

        String STOP_LIST = "com.alvinhkh.buseta.STOP_LIST";

        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";

        String STOP_OBJECT_STRING = "com.alvinhkh.buseta.STOP_OBJECT_STRING";

        String STOP_SEQUENCE = "com.alvinhkh.buseta.STOP_SEQUENCE";

        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";

        String NOTIFICATION_ID = "com.alvinhkh.buseta.NOTIFICATION_ID";

        String MANUAL = "com.alvinhkh.buseta.MANUAL";

        String MESSAGE_RID = "com.alvinhkh.buseta.MESSAGE_RID";

        String APP_UPDATE_OBJECT = "com.alvinhkh.buseta.APP_UDPATE_OBJECT";

        String TAG = "com.alvinhkh.buseta.TAG";

        String HTML = "com.alvinhkh.buseta.HTMLs";
    }

    public interface PREF {
        String APP_UPDATE_VERSION = "com.alvinhkh.buseta.APP_UPDATE_VERSION";

        String VERSION_RECORD = "com.alvinhkh.buseta.VERSION_RECORD";

        String AD_KEY = "I HATE ADS!";

        String AD_SHOW = "SHOW ME";

        String AD_HIDE = "com.alvinhkh.buseta.AD_HIDE";

        String GEOFENCES_KEY = "com.alvinhkh.buseta.GEOFENCES_KEY";
    }

    public interface PROVIDER {

        String AESBUS = "AESBUS";

        String CTB = "CTB";

        String DATAGOVHK = "DATAGOVHK";

        String DATAGOVHK_NWST = "DATAGOVHK_NWST";

        String LRTFEEDER = "LRTFeeder";

        String LWB = "LWB";

        String KMB = "KMB";

        String MTR = "MTR";

        String NLB = "NLB";

        String NR = "NR";

        String NWFB = "NWFB";

        String NWST = "NWST";

        String GMB901 = "GMB901";

    }

    public interface TYPE {
        String BUS = "com.alvinhkh.buseta.BUS";

        String RAILWAY = "com.alvinhkh.buseta.RAILWAY";
    }

    public interface URI {
        String APP = "android-app://com.alvinhkh.buseta/buseta/route";

        String ROUTE = "buseta://route/";

        String BOUND = "buseta://route/bound/";

        String STOP = "buseta://route/stop/";
    }

    public interface NOTIFICATION {
        String CHANNEL_ARRIVAL_ALERT = "CHANNEL_ID_ARRIVAL_ALERT";

        String CHANNEL_ETA = "CHANNEL_ID_ETA";

        String CHANNEL_FOREGROUND = "CHANNEL_ID_FOREGROUND";

        String CHANNEL_UPDATE = "CHANNEL_ID_CHECK_UPDATE";
    }

    public interface GEOFENCE {
        Integer RADIUS_IN_METERS = 200;

        Integer EXPIRATION_IN_MILLISECONDS = 14400000;
    }
}
