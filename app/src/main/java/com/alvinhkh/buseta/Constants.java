package com.alvinhkh.buseta;

public class Constants {

    public interface URL {
        String RELEASE = "http://buseta.alvinhkh.com/release.json";
        String KMB = "http://www.kmb.hk";
        String LWB = "http://www.lwb.hk";
        String ROUTE_AVAILABLE = "http://etadatafeed.kmb.hk:1933/GetData.ashx?type=ETA_R";
        String ROUTE_INFO = "/ajax/getRoute_info.php";
        String ROUTE_MAP = "/ajax/getRouteMapByBusno.php";
        String ROUTE_NEWS = "/ajax/getnews.php";
        String ROUTE_NOTICES = KMB + "/tc/news/realtimenews.html?page=";
        String ROUTE_NOTICES_IMAGE = KMB + "/loadImage.php?page=";
        String ROUTE_STOP_IMAGE = KMB + "/chi/img.php?file=";
        String HTML_ETA = KMB + "/tc/services/eta_enquiry.html";
        String HTML_SEARCH = KMB + "/tc/services/search.html";
        String ETA_API_HOST = "http://etav3.kmb.hk";
        String REQUEST_REFERRER = HTML_ETA;
        String REQUEST_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + 
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2478.0 Safari/537.36";
        String STATIC_MAP = "http://maps.google.com/maps/api/staticmap" + 
                "?zoom=16&size=320x320&sensor=false&center=";
    }

    public interface URI {
        String APP = "android-app://com.alvinhkh.buseta/buseta/route";
        String ROUTE = "buseta://route/";
        String BOUND = "buseta://route/bound/";
        String STOP = "buseta://route/stop/";
    }

    public interface ROUTES {
        Integer VERSION = 20170611;
    }

    public interface MESSAGE {
        String CHECKING_UPDATED = "com.alvinhkh.buseta.CHECKING_UPDATED";
        String BOUNDS_UPDATED = "com.alvinhkh.buseta.BOUNDS_UPDATED";
        String HISTORY_UPDATED = "com.alvinhkh.buseta.HISTORY_UPDATED";
        String STOP_UPDATED = "com.alvinhkh.buseta.STOP_UPDATED";
        String STOPS_UPDATED = "com.alvinhkh.buseta.STOPS_UPDATED";
        String ETA_UPDATED = "com.alvinhkh.buseta.ETA_UPDATED";
        String FOLLOW_UPDATED = "com.alvinhkh.buseta.FOLLOW_UPDATED";
        String SEND_UPDATING = "com.alvinhkh.buseta.SEND_UPDATING";
        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";
        String WIDGET_TRIGGER_UPDATE = "com.alvinhkh.buseta.WIDGET_TRIGGER_UPDATE";
        String SUGGESTION_FORCE_UPDATE = "com.alvinhkh.buseta.SUGGESTION_FORCE_UPDATE";
        String NOTIFICATION_UPDATE = "com.alvinhkh.buseta.NOTIFICATION_UPDATE";
        String NOTIFICATION_TRIGGER_UPDATE = "com.alvinhkh.buseta.NOTIFICATION_TRIGGER_UPDATE";
    }

    public interface STATUS {
        String CONNECT_404 = "com.alvinhkh.buseta.CONNECT_404";
        String CONNECT_FAIL = "com.alvinhkh.buseta.CONNECT_FAIL";
        String CONNECTIVITY_INVALID = "com.alvinhkh.buseta.CONNECTIVITY_INVALID";
        String UPDATED_APP_FOUND = "com.alvinhkh.buseta.UPDATED_APP_FOUND";
        String UPDATED_FARE = "com.alvinhkh.buseta.UPDATED_FARE";
        String UPDATED_BOUNDS = "com.alvinhkh.buseta.UPDATED_BOUNDS";
        String UPDATED_STOPS = "com.alvinhkh.buseta.UPDATED_STOPS";
        String UPDATED_SUGGESTION = "com.alvinhkh.buseta.UPDATED_SUGGESTION";
        String UPDATING_FARE = "com.alvinhkh.buseta.UPDATING_FARE";
        String UPDATING_BOUNDS = "com.alvinhkh.buseta.UPDATING_BOUNDS";
        String UPDATING_STOPS = "com.alvinhkh.buseta.UPDATING_STOPS";
    }

    public interface BUNDLE {
        String APP_UPDATE_OBJECT = "com.alvinhkh.buseta.APP_UPDATE_OBJECT";
        String MESSAGE_ID = "com.alvinhkh.buseta.MESSAGE_ID";
        String ROUTE_NO = "com.alvinhkh.buseta.ROUTE_NO";
        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";
        String BOUND_OBJECT = "com.alvinhkh.buseta.BOUND_OBJECT";
        String UPDATE_MESSAGE = "com.alvinhkh.buseta.UPDATE_MESSAGE";
    }

    public interface PREF {
        String APP_UPDATE_VERSION = "com.alvinhkh.buseta.APP_UPDATE_VERSION";
        String VERSION_RECORD = "com.alvinhkh.buseta.VERSION_RECORD";
        String REQUEST_ID = "com.alvinhkh.buseta.REQUEST_ID";
        String REQUEST_TOKEN = "com.alvinhkh.buseta.REQUEST_TOKEN";
        String REQUEST_API_ETA = "com.alvinhkh.buseta.REQUEST_API_ETA";
        String REQUEST_API_INFO = "com.alvinhkh.buseta.REQUEST_API_INFO";
        String AD_KEY = "I HATE ADS!";
        String AD_SHOW = "SHOW ME";
        String AD_HIDE = "com.alvinhkh.buseta.AD_HIDE";
    }

}
