package com.example.csupload;

import android.net.Uri;

public class Chunk {
    public String fileName;
    public int chunkNumber;
    public long numberOfChunks;
    public long startByte;
    public int chunkSize;
    public Uri uri;
    public int parentID;

    public Chunk(String fileName, int chunkNumber, long numberOfChunks, long startByte, int chunkSize, Uri uri, int parentID) {
        this.fileName = fileName;
        this.chunkNumber = chunkNumber;
        this.numberOfChunks = numberOfChunks;
        this.startByte = startByte;
        this.chunkSize = chunkSize;
        this.uri = uri;
        this.parentID = parentID;
    }

}
