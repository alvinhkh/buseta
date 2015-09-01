package com.alvinhkh.buseta;

public class Constants {

    public interface URL {
        String ALVINHKH = "http://www.alvinhkh.com";
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
        String REQUEST_REFERER = HTML_ETA;
        String REQUEST_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2478.0 Safari/537.36";
    }

    public interface ROUTES {
        String VERSION_RECORD = "com.alvinhkh.buseta.VERSION_RECORD";
        Integer VERSION = 1;
        String SUGGESTION_UPDATE = "com.alvinhkh.buseta.ROUTES.SUGGESTION_UPDATE";
        String MESSAGE_ID = "com.alvinhkh.buseta.ROUTES.MESSAGE_ID";
    }

    public interface MESSAGE {
        String HISTORY_UPDATED = "com.alvinhkh.buseta.HISTORY_UPDATED";
        String STOP_UPDATED = "com.alvinhkh.buseta.STOP_UPDATED";
        String ETA_UPDATED = "com.alvinhkh.buseta.ETA_UPDATED";
        String STATE_UPDATED = "com.alvinhkh.buseta.STATE_UPDATED";
        String HIDE_STAR = "com.alvinhkh.buseta.HIDE_STAR";
    }

    public interface BUNDLE {
        String ITEM_POSITION = "com.alvinhkh.buseta.ITEM_POSITION";
        String STOP_OBJECT = "com.alvinhkh.buseta.STOP_OBJECT";
        String STOP_OBJECTS = "com.alvinhkh.buseta.STOP_OBJECTS";
    }

}
