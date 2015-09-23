package com.alvinhkh.buseta;

public class Constants {

    public interface URL {
        String KMB = "http://www.kmb.hk";
        String PATH_ETA_API = "/ajax/eta_api/prod/";
        String PATH_ETA_JS = "/js/services/eta/";
        String ROUTE_AVAILABLE = "http://etadatafeed.kmb.hk:1933/GetData.ashx?type=ETA_R";
        String ROUTE_INFO = KMB + "/ajax/getRouteInfo.php";
        String ROUTE_INFO_V1 = KMB + "/ajax/getRoute_info.php";
        String ROUTE_MAP = KMB + "/ajax/getRouteMapByBusno.php";
        String ROUTE_NEWS = KMB + "/ajax/getnews.php";
        String ROUTE_NOTICES = KMB + "/tc/news/realtimenews.html?page=";
        String ROUTE_NOTICES_IMAGE = KMB + "/loadImage.php?page=";
        String ROUTE_STOP_IMAGE = KMB + "/chi/img.php?file=";
        String HTML_ETA = KMB + "/tc/services/eta_enquiry.html";
        String HTML_SEARCH = KMB + "/tc/services/search.html";
        String ETA_MOBILE_API = "http://etav2.kmb.hk";
        String REQUEST_REFERRER = HTML_ETA;
        String REQUEST_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2478.0 Safari/537.36";
    }

    public interface ROUTES {
        Integer VERSION = 4;
        String SUGGESTION_UPDATE = "com.alvinhkh.buseta.ROUTES.SUGGESTION_UPDATE";
        String MESSAGE_ID = "com.alvinhkh.buseta.ROUTES.MESSAGE_ID";
    }

    public interface MESSAGE {
        String BOUNDS_UPDATED = "com.alvinhkh.buseta.BOUNDS_UPDATED";
        String HISTORY_UPDATED = "com.alvinhkh.buseta.HISTORY_UPDATED";
        String STOP_UPDATED = "com.alvinhkh.buseta.STOP_UPDATED";
        String STOPS_UPDATED = "com.alvinhkh.buseta.STOPS_UPDATED";
        String ETA_UPDATED = "com.alvinhkh.buseta.ETA_UPDATED";
        String FOLLOW_UPDATED = "com.alvinhkh.buseta.FOLLOW_UPDATED";
        String SEND_UPDATING = "com.alvinhkh.buseta.SEND_UPDATING";
        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";
        String WIDGET_TRIGGER_UPDATE = "com.alvinhkh.buseta.WIDGET_TRIGGER_UPDATE";
    }

    public interface STATUS {
        String CONNECT_404 = "com.alvinhkh.buseta.CONNECT_404";
        String CONNECT_FAIL = "com.alvinhkh.buseta.CONNECT_FAIL";
        String CONNECTIVITY_INVALID = "com.alvinhkh.buseta.CONNECTIVITY_INVALID";
        String UPDATED_FARE = "com.alvinhkh.buseta.UPDATED_FARE";
        String UPDATED_BOUNDS = "com.alvinhkh.buseta.UPDATED_BOUNDS";
        String UPDATED_STOPS = "com.alvinhkh.buseta.UPDATED_STOPS";
        String UPDATING_FARE = "com.alvinhkh.buseta.UPDATING_FARE";
        String UPDATING_BOUNDS = "com.alvinhkh.buseta.UPDATING_BOUNDS";
        String UPDATING_STOPS = "com.alvinhkh.buseta.UPDATING_STOPS";
    }

    public interface BUNDLE {
        String ROUTE_NO = "com.alvinhkh.buseta.ROUTE_NO";
        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";
        String BOUND_OBJECT = "com.alvinhkh.buseta.BOUND_OBJECT";
        String UPDATE_MESSAGE = "com.alvinhkh.buseta.UPDATE_MESSAGE";
    }

    public interface PREF {
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
