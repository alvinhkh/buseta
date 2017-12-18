package com.alvinhkh.buseta.model;


public class ArrivalTime {

    public Integer capacity = -1;

    public String companyCode = "";

    public String estimate = "";

    public String expire = "";

    public Boolean expired = false;

    public String id = "0";

    public Boolean isSchedule = false;

    public Boolean hasWheelchair = false;
    
    public Boolean hasWifi = false;

    public String text = "";

    public String isoTime = "";

    public Float distanceKM = -1.0f;

    public Long generatedAt = 0L;

    public Long updatedAt = 0L;

    public ArrivalTime() { }

    public String toString() {
        return "ArrivalTime{capacity=" + this.capacity + ", companyCode=" + this.companyCode
                + ", distanceKM=" + this.distanceKM + ", estimate=" + this.estimate
                + ", expire=" + this.expire + ", expired=" + this.expired
                + ", id=" + this.id + ", isoTime=" + this.isoTime
                + ", isSchedule=" + this.isSchedule + ", hasWheelchair=" + this.hasWheelchair
                + ", hasWifi=" + this.hasWifi + ", text=" + this.text
                + ", generatedAt=" + this.generatedAt + ", updatedAt=" + this.updatedAt + "}";
    }
}
