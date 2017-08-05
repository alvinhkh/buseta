package com.alvinhkh.buseta.model;

import android.support.annotation.NonNull;

public class BusRoute {

    public static final String COMPANY_KMB = "KMB";

    private String companyCode;

    private String locationEndName;

    private String locationStartName;

    private String name;

    private String sequence;

    private String serviceType;

    private String description;

    public BusRoute() {
    }

    public BusRoute(@NonNull String companyCode, @NonNull String name,
                    @NonNull String sequence, @NonNull String serviceType) {
        this.companyCode = companyCode;
        this.name = name;
        this.sequence = sequence;
        this.serviceType = serviceType;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getLocationEndName() {
        return locationEndName;
    }

    public String getName() {
        return name;
    }

    public String getSequence() {
        return sequence;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getLocationStartName() {
        return locationStartName;
    }

    public String getDescription() {
        return description;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public void setLocationEndName(String locationEndName) {
        this.locationEndName = locationEndName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setLocationStartName(String locationStartName) {
        this.locationStartName = locationStartName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String childKey;

    private String key;

    public String getChildKey() {
        return childKey;
    }

    public String getKey() {
        return key;
    }

    public void setChildKey(String key) {
        this.childKey = key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String toString() {
        return "BusRoute{companyCode=" + this.companyCode
                + ", description=" + this.description
                + ", locationEndName=" + this.locationEndName
                + ", locationStartName=" + this.locationStartName + ", name=" + this.name
                + ", sequence=" + this.sequence + ", serviceType=" + this.serviceType
                + ", key=" + this.key + ", childKey=" + this.childKey + "}";
    }
}
