package com.alvinhkh.buseta.model;


public class ArrivalTime {

    public Integer capacity;

    public String estimate = "";

    public String expire;

    public Boolean expired = false;

    public String id = "0";

    public Boolean isSchedule = false;

    public Boolean hasWheelchair = false;
    
    public Boolean hasWifi = false;

    public String text;

    public Long generatedAt;

    public Long updatedAt;

    public ArrivalTime() { }

    public String toString() {
        return "ArrivalTime{capacity=" + this.capacity + ", estimate=" + this.estimate
                + ", expire=" + this.expire + ", expired=" + this.expired + ", id=" + this.id
                + ", isSchedule=" + this.isSchedule + ", hasWheelchair=" + this.hasWheelchair
                + ", hasWifi=" + this.hasWifi + ", text=" + this.text
                + ", generatedAt=" + this.generatedAt + ", updatedAt=" + this.updatedAt + "}";
    }
}
