package com.example.csupload;

import android.content.ContentResolver;
import android.net.Uri;


import com.example.csupload.listeners.OnProgressListener;
import com.example.csupload.service.FileClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SingleFile {
    private String fileName;
    private Uri uri;
    private long numberOfChunks;
    private List<Boolean> uploadedChunks;
    public int chunkNumber;
    public int numberOfChunksSend;
    private int fileID;
    private int startByte;
    private List<Chunk> listOfChunks;
    public boolean isPaused;
    private CSUpload csUpload;
    private String url;
    private FileClient client;
    public OnProgressListener progressListener;
    public int chunksUploaded = 0;

    public SingleFile(String url, String fileName, Uri uri, long numberOfChunks, int chunkSize, long fileSize, int fileID, CSUpload csUpload){
        this.fileName = fileName;
        this.uri = uri;
        this.numberOfChunks = numberOfChunks;
        this.uploadedChunks = new ArrayList<>();
        this.chunkNumber = 1;
        this.numberOfChunksSend = 0;
        this.fileID = fileID;
        this.startByte = 0;
        this.listOfChunks = new ArrayList<>();
        this.isPaused = false;
        this.csUpload = csUpload;
        this.url = url;

        if (url != null) {
            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(this.url)
                    .addConverterFactory(GsonConverterFactory.create());
            Retrofit retrofit = builder.build();
            client = retrofit.create(FileClient.class);
        } else {
            client = null;
        }

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

    public FileClient getClient() {
        return this.client;
    }

    public void resume() {
        if (chunksUploaded < numberOfChunks) {
            isPaused = false;
            csUpload.resumeFile(this.fileID);
        } else {
            progressListener.onProgress(numberOfChunks, chunksUploaded);
        }
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.progressListener = onProgressListener;
    }

    public int getFileID () {
        return fileID;
    }

}
