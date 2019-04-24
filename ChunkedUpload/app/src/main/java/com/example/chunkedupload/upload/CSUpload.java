package com.example.chunkedupload.upload;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;

import com.example.chunkedupload.upload.service.FileClient;
import com.example.chunkedupload.upload.service.ResponseObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CSUpload {
    private static CSUpload firstInstance = null;
    private static FileClient client;
    private static Sender[] senders;
    private static List<SingleFile> files;
    private static int numberOfConnections;
    private static int chunkSize;
    private static ContentResolver contentResolver;
    private static Uri uri;

    private CSUpload () {}

    public static CSUpload getInstance( String url, int sizeOfChunks, int connections, ContentResolver resolver) {
        if (firstInstance == null) {
            firstInstance = new CSUpload();
            numberOfConnections = connections;
            senders = new Sender[numberOfConnections];
            files = new ArrayList<>();
            contentResolver = resolver;
            chunkSize = sizeOfChunks;
            for (int i = 0; i < senders.length; i++) {
                senders[i] = new Sender(null, i + 1);
            }

            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create());
            Retrofit retrofit = builder.build();
            client = retrofit.create(FileClient.class);

            for (int i = 0; i < numberOfConnections; i++) {
                senders[i] = new Sender(null, i + 1);
            }

        }
        return firstInstance;
    }

    private static List<Boolean> getUploadedChunks(final String fileName, final long numberOfChunks, int fileID) {

        Call<ResponseObject> call = client.getUploadedList(fileName, numberOfChunks);
        try {
            Response<ResponseObject> response = call.execute();
            List<Boolean> uploadedChunks = response.body().chunks;

            files.get(fileID).chunksUploaded = Integer.parseInt(response.body().length);
            files.get(fileID).progressListener.onProgress(uploadedChunks.size(), files.get(fileID).chunksUploaded);

            for (int i = 0; i < uploadedChunks.size(); i++) {
                Log.e(String.valueOf(i + 1), String.valueOf(uploadedChunks.get(i)));
            }
            return uploadedChunks;
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
            return null;
        }
    }

    public SingleFile sendFile(Uri uri) {
        String fileName = new File(uri.getPath()).getName();
        Cursor returnCursor = contentResolver.query(uri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long fileSize = returnCursor.getLong(sizeIndex);
        returnCursor.close();


        long numberOfChunks = fileSize / chunkSize;
        if ( fileSize % chunkSize != 0) {
            numberOfChunks++;
        }
        SingleFile file = new SingleFile(fileName, uri, numberOfChunks, chunkSize, fileSize, contentResolver, files.size(), this);
        new SendFile().execute(file);
        return file;
    }

    public static void startSenders() {
        for (int i = 0; i < senders.length; i++) {
            if (!senders[i].isSending) {
                for (int j = 0; j <  files.size(); j++) {
                    if (files.get(j).isPaused) {
                        continue;
                    }
                    senders[i].chunk = files.get(j).getNext();
                    if (senders[i].chunk.chunkNumber != -1) {
                        senders[i].isSending = true;
                        senders[i].sendChunk();
                        Log.e("StartSenders", "sending" + senders[i].id);
                        break;
                    }
                }
            }
        }
    }

    public void abortSenders() {
        for (int i = 0; i < senders.length; i++) {
            senders[i].abortCall();
        }
        startSenders();
    }

    public void resumeFile(int fileID) {
        SingleFile file = files.get(fileID);
        file.numberOfChunksSend = 0;
        files.remove(fileID);
        new SendFile().execute(file);
    }


    private static class SendFile extends AsyncTask<SingleFile, Integer, Boolean> {

        protected Boolean doInBackground(SingleFile... singleFiles) {

            SingleFile file = singleFiles[0];
            files.add(file);

            List<Boolean> uploadedChunks = getUploadedChunks(file.getFileName(), file.getNumberOfChunks(), file.getFileID());

            if (uploadedChunks == null) {
                return false;
            } else {
                file.setUploadedChunks(uploadedChunks);
            }

            startSenders();

            return true;

        }
        protected void onPostExecute(Boolean flag) {
            for (int i = 0; i < numberOfConnections; i++) {
                senders[i] = new Sender(null, i + 1);
            }


        }

    }


    private static class Sender {
        private int id;
        private int retries = 1;
        private Chunk chunk;
        private Call<ResponseBody> call;
        private boolean isSending = false;

        public Sender(Chunk chunk, int id) {
            this.id = id;
            if (chunk != null) {
                this.chunk = chunk;
            }
        }

        public void abortCall() {
            if (this.call != null) {
                this.call.cancel();
                retries += 6;
            }
            isSending = false;
        }

        private String getBase64(Chunk chunk) {
            BufferedInputStream in;
            InputStream fileInputStream = null;
            try {
                fileInputStream = contentResolver.openInputStream(chunk.uri);
                in = new BufferedInputStream(fileInputStream);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int counter = 0;
                int b;
                in.skip(chunk.startByte);
                while (((b = in.read()) != -1) && counter < chunkSize) {
                    out.write(b);
                    counter++;
                }
                out.close();
                in.close();
                return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        private void sendChunk() {

            isSending = true;
            RequestBody slice = RequestBody.create(MultipartBody.FORM, getBase64(chunk));
            RequestBody name = RequestBody.create(MultipartBody.FORM, this.chunk.fileName);
            RequestBody chunkNumberPart = RequestBody.create(MultipartBody.FORM, String.valueOf(this.chunk.chunkNumber));
            final RequestBody numberOfChunks = RequestBody.create(MultipartBody.FORM, String.valueOf(this.chunk.numberOfChunks));
            call = client.fileUpload(slice, name, chunkNumberPart, numberOfChunks);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    isSending = false;

                    SingleFile uploadedFile = files.get(chunk.parentID);
                    uploadedFile.chunksUploaded++;
                    if (!uploadedFile.isPaused) {
                        uploadedFile.progressListener.onProgress(chunk.numberOfChunks, uploadedFile.chunksUploaded);
                    }

                    Log.e("sender " + id, "send " + chunk.fileName + chunk.chunkNumber);

                    startSenders();
                    retries = 1;
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    isSending = false;
                    Log.e("Error", "Sender " + id + " Could not send " + chunk.chunkNumber );
                    if (retries < 5) {
                        retries++;
                        if (!files.get(chunk.parentID).isPaused) {
                            sendChunk();
                        }
                    }
                }
            });

        }
    }
}
