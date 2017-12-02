package com.alvinhkh.buseta.nlb.model;

public class NlbEtaRequest {

    final String routeId;

    final String stopId;

    final String language;

    public NlbEtaRequest(String routeId, String stopId, String language) {
        this.routeId = routeId;
        this.stopId = stopId;
        this.language = language;
    }

}
