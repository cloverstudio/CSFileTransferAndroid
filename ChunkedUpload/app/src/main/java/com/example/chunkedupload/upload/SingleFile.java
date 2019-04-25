package com.example.chunkedupload.upload;

import android.content.ContentResolver;
import android.net.Uri;

import com.example.chunkedupload.upload.listeners.OnProgressListener;

import java.util.ArrayList;
import java.util.List;

public class SingleFile {
    private String fileName;
    private Uri uri;
    private long numberOfChunks;
    private int chunkSize;
    private long fileSize;
    private List<Boolean> uploadedChunks;
    private ContentResolver contentResolver;
    public int chunkNumber;
    public int numberOfChunksSend;
    private int fileID;
    private int startByte;
    private List<Chunk> listOfChunks;
    public boolean isPaused;
    private CSUpload csUpload;
    public OnProgressListener progressListener;
    public int chunksUploaded = 0;

    public SingleFile(String fileName, Uri uri, long numberOfChunks, int chunkSize, long fileSize, ContentResolver resolver, int fileID, CSUpload csUpload){
        this.fileName = fileName;
        this.uri = uri;
        this.numberOfChunks = numberOfChunks;
        this.chunkSize = chunkSize;
        this.fileSize = fileSize;
        this.uploadedChunks = new ArrayList<>();
        this.contentResolver = resolver;
        this.chunkNumber = 1;
        this.numberOfChunksSend = 0;
        this.fileID = fileID;
        this.startByte = 0;
        this.listOfChunks = new ArrayList<>();
        this.isPaused = false;
        this.csUpload = csUpload;

        while (startByte < fileSize) {
            listOfChunks.add(new Chunk(fileName, chunkNumber, numberOfChunks, startByte, chunkSize, uri, fileID));
            startByte += chunkSize;
            chunkNumber++;
        }

    }

    public Chunk getNext() {
        if (uploadedChunks.size() == 0) {
            return null;
        }
        while (numberOfChunksSend < listOfChunks.size() && uploadedChunks.get(numberOfChunksSend)) {
            numberOfChunksSend++;
        }

        if ((numberOfChunksSend) >= listOfChunks.size()) {
            return null;
        }

        Chunk returnChunk = listOfChunks.get(numberOfChunksSend);
        numberOfChunksSend++;

        return returnChunk;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getNumberOfChunks() {
        return numberOfChunks;
    }

    public void setUploadedChunks(List<Boolean> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public void pause() {
        isPaused = true;
        csUpload.abortSenders();
    }

    public void resume() {
        if (chunksUploaded < numberOfChunks) {
            isPaused = false;
            csUpload.resumeFile(this.fileID);
        }
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.progressListener = onProgressListener;
    }

    public int getFileID () {
        return fileID;
    }

}
