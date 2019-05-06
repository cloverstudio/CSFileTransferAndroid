package com.example.csupload.service;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseObject {

    @SerializedName("length")
    @Expose
    public String length;

    @SerializedName("chunks")
    @Expose
    public List<Boolean> chunks;
}
