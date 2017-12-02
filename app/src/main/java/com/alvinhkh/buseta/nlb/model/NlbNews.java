package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbNews {

    @SerializedName("newsId")
    public String newsId;

    @SerializedName("title")
    public String title;

    @SerializedName("content")
    public String content;

    @SerializedName("publishDate")
    public String publishDate;

    public String toString() {
        return "NlbNews{newsId=" + newsId + ", title=" + title + ", content=" + content +
                ", publishDate=" + publishDate + "}";
    }

}
