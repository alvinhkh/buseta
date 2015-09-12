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
        Integer VERSION = 3;
        String SUGGESTION_UPDATE = "com.alvinhkh.buseta.ROUTES.SUGGESTION_UPDATE";
        String MESSAGE_ID = "com.alvinhkh.buseta.ROUTES.MESSAGE_ID";
    }

    public interface MESSAGE {
        String HISTORY_UPDATED = "com.alvinhkh.buseta.HISTORY_UPDATED";
        String STOP_UPDATED = "com.alvinhkh.buseta.STOP_UPDATED";
        String ETA_UPDATED = "com.alvinhkh.buseta.ETA_UPDATED";
        String HIDE_STAR = "com.alvinhkh.buseta.HIDE_STAR";
        String WIDGET_UPDATE = "com.alvinhkh.buseta.WIDGET_UPDATE";
        String WIDGET_TRIGGER_UPDATE = "com.alvinhkh.buseta.WIDGET_TRIGGER_UPDATE";
    }

    public interface BUNDLE {
        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";
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
