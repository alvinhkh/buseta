package com.alvinhkh.buseta;

public class Constants {

    public interface URL {
        String ALVINHKH = "http://www.alvinhkh.com";
        String KMB = "http://www.kmb.hk";
        String PATH_ETA_API = "/ajax/eta_api/prod/";
        String PATH_ETA_JS = "/js/services/eta/";
        String ROUTE_INFO = KMB + "/ajax/getRouteInfo.php";
        String ROUTE_INFO_V1 = KMB + "/ajax/getRoute_info.php";
        String ROUTE_MAP = KMB + "/ajax/getRouteMapByBusno.php";
        String ROUTE_NEWS = KMB + "/ajax/getnews.php";
        String ROUTE_NOTICES = KMB + "/tc/news/realtimenews.html?page=";
        String ROUTE_NOTICES_IMAGE = KMB + "/loadImage.php?page=";
        String HTML_ETA = KMB + "/tc/services/eta_enquiry.html";
        String HTML_SEARCH = KMB + "/tc/services/search.html";
        String ETA_MOBILE_API = "http://etav2.kmb.hk";
        String REQUEST_REFERER = HTML_ETA;
        String REQUEST_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2478.0 Safari/537.36";
    }

    public interface ROUTES {
        String[] AVAILABLE = new String[] {
                // As of 15 August 2015
                "1","1A","10","11C","13X","14B","14X","16M","108","2","2B","2E","2F","23","24","26","29M","203C","203E","203S","208","211","212","216M","219X","224X","230X","234P","234X","235","237A","238P","238S","238X","240X","242X","243M","243P","249M","249X","251A","251B","251M","252B","258D","258P","258S","259B","259C","259D","259E","260B","260C","260X","261","261B","263","265B","265S","265M","267S","268B","268C","268X","269A","269B","269C","269D","269M","269P","270A","270B","270P","270S","271","271P","272P","272X","276","276A","276B","276P","279X","280X","281A","281B","281M","281X","283","284","286C","286P","286X","287X","288","289K","290","290A","296C","296D","296M","297","298E","299X","3C","3D","3M","3P","30","30X","31","31B","31M","34","35A","35X","36B","36M","36X","37","37M","38","373","373A","40","40P","41","41A","42","42A","42C","43","43A","43B","43M","44M","45","46","5A","5M","51","52X","53","54","57M","58P","58M","58X","59A","59M","59S","59X","6","6C","6F","60M","60X","61M","61X","62X","63X","65K","66M","66X","67M","67X","68A","68E","68M","68X","69M","69P","69X","603","603P","603S","7","7B","7M","70K","71B","71K","72","72A","72X","73A","73K","74A","74P","74X","75K","75X","77K","79K","8","8A","8P","80","80K","80P","80M","81","81K","81C","81S","84M","85B","86","86A","87D","89","89B","89X","9","91R","92","93K","94","95","95M","98D","98P","99","934","935","936","960","960A","960B","960P","960S","960X","961","961P","968","968X","B1","N216","N237","N241","N260","N269","N271","N281","N368","N73","A31","A33","A41","A41P","A43","E31","E33","E33P","E34A","E34B","E34P","E41","E42","N30","N30P","N31","N42","N42A","N64","S64","S64C","S64P","S64X"
        };
        String VERSION_RECORD = "com.alvinhkh.buseta.VERSION_RECORD";
        Integer VERSION = 1;
    }

    public interface MESSAGE {
        String HISTORY_UPDATED = "HISTORY_UPDATED";
    }

}
