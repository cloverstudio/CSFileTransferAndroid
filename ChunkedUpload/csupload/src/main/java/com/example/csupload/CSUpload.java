package com.example.csupload;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;

import com.example.csupload.listeners.OnServerListener;
import com.example.csupload.service.FileClient;
import com.example.csupload.service.ResponseObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_LOWEST;
import static android.os.Process.setThreadPriority;
import static java.lang.Process.*;

public class CSUpload {
    private static CSUpload firstInstance = null;
    private static FileClient client;
    private static Sender[] senders;
    private static List<SingleFile> files;
    private static int numberOfConnections;
    private static int chunkSize;
    private static ContentResolver contentResolver;
    private static boolean isConnected;
    private static int numberOFFiles = 0;
    private static int fileToChooseFrom = 0;
    private static OnServerListener serverListener;
    private static String baseUrl;
    private static String endPoint;
    private static ExecutorService executor;


    public void setOnServerListener(OnServerListener listener) {
        serverListener = listener;
    }

    private CSUpload () {}

    public static CSUpload getInstance(String url, int sizeOfChunks, int connections, ContentResolver resolver) {
        if (firstInstance == null) {
            if (connections > 5) {
                connections = 5;
            }
            if (connections < 1) {
                connections = 1;
            }

            if (sizeOfChunks > (0.1 * 1024 *1024)) {
                chunkSize = (int)(0.1 * 1024 *1024);
            } else {
                chunkSize = sizeOfChunks;
            }
            firstInstance = new CSUpload();
            numberOfConnections = connections;
            senders = new Sender[numberOfConnections];
            files = new ArrayList<>();
            contentResolver = resolver;
            isConnected = true;

            int cores = Runtime.getRuntime().availableProcessors();
            executor = new ThreadPoolExecutor(cores, cores,
                    50, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

            initializeSenders();

            int pos = url.lastIndexOf('/');
            baseUrl = url.substring(0, pos + 1);
            endPoint = url.substring(pos + 1);

            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create());
            Retrofit retrofit = builder.build();
            client = retrofit.create(FileClient.class);

        }
        return firstInstance;
    }

    private static void initializeSenders() {
        for (int i = 0; i < senders.length; i++) {
            senders[i] = new Sender(null, i + 1);
        }
    }

    private static List<Boolean> getUploadedChunks(final String fileName, final long numberOfChunks, int fileID, int chunkSize) {

        Call<ResponseObject> call = client.getUploadedList(endPoint,fileName, numberOfChunks, chunkSize);
        try {
            Response<ResponseObject> response = call.execute();
            List<Boolean> uploadedChunks = response.body().chunks;

            files.get(fileID).chunksUploaded = Integer.parseInt(response.body().length);

            if (files.get(fileID).progressListener != null) {
                files.get(fileID).progressListener.onProgress(uploadedChunks.size(), files.get(fileID).chunksUploaded);
            }


            for (int i = 0; i < uploadedChunks.size(); i++) {
                Log.e(String.valueOf(i + 1), String.valueOf(uploadedChunks.get(i)));
            }
            return uploadedChunks;
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            Log.e("Error", e.getMessage());
            return null;
        }
    }

    private SingleFile createSingleFile(Uri uri, String url) {
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
        SingleFile file = new SingleFile(url, fileName, uri, numberOfChunks, chunkSize, fileSize, numberOFFiles, this);
        numberOFFiles++;


        pushFileToExecutor(file);

        return file;
    }

    private boolean pushFileToExecutor(final SingleFile file) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (file.getFileID() == files.size()) {
                    files.add(file);
                } else {
                    files.set(file.getFileID(), file);
                }

                List<Boolean> uploadedChunks = getUploadedChunks(file.getFileName(), file.getNumberOfChunks(), file.getFileID(), chunkSize);

                if (uploadedChunks == null) {

                } else {
                    file.setUploadedChunks(uploadedChunks);
                }

                startSenders();
            }
        });
        return true;
    }

    public SingleFile uploadFile(Uri uri) {
        return createSingleFile(uri, null);
    }

    public SingleFile uploadFile(Uri uri, String url) {
        return createSingleFile(uri, url);
    }

    public static void startSenders() {
        for (int i = 0; i < Math.max(numberOFFiles, senders.length); i++) {
            int currentSender = i % senders.length;
            if (!senders[currentSender].isSending) {
                for (int j = 0; j <  numberOFFiles; j++) {

                    if (fileToChooseFrom >= files.size()) {
                        fileToChooseFrom = 0;
                    }

                    if (files.get(fileToChooseFrom).isPaused) {
                        fileToChooseFrom++;
                        continue;
                    }

                    Chunk current = files.get(fileToChooseFrom).getNext();

                    if (current != null) {
                        senders[currentSender].chunk = current;
                        senders[currentSender].isSending = true;
                        senders[currentSender].sendChunk();
                        break;
                    }
                }
                fileToChooseFrom++;
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
        pushFileToExecutor(file);
    }



    private static class Sender {
        private int id;
        private int retries = 1;
        private Chunk chunk;
        private Call<ResponseBody> call;
        private boolean isSending = false;

        private Sender(Chunk chunk, int id) {
            this.id = id;
            if (chunk != null) {
                this.chunk = chunk;
            }
        }

        private void abortCall() {
            if (this.call != null && files.get(chunk.parentID).isPaused) {
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
            RequestBody size = RequestBody.create(MultipartBody.FORM, String.valueOf(chunkSize));

            SingleFile currentFile = files.get(chunk.parentID);

            if (currentFile.getClient() != null) {
                call = currentFile.getClient().fileUpload(endPoint, slice, name, chunkNumberPart, numberOfChunks, size);
            } else {
                call = client.fileUpload(endPoint, slice, name, chunkNumberPart, numberOfChunks, size);
            }

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    isSending = false;

                    SingleFile uploadedFile = files.get(chunk.parentID);
                    uploadedFile.chunksUploaded++;
                    if (!uploadedFile.isPaused && uploadedFile.progressListener != null) {
                        uploadedFile.progressListener.onProgress(chunk.numberOfChunks, uploadedFile.chunksUploaded);
                    }

                    Log.e("sender " + id, "send " + chunk.fileName + chunk.chunkNumber);
                    startSenders();
                    retries = 1;
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    isSending = false;
                    if (retries < 5) {
                        retries++;
                        if (!files.get(chunk.parentID).isPaused) {
                            sendChunk();
                        }
                    } else if (retries == 5 && isConnected) {
                        isConnected = false;
                        for (int i = 0; i < files.size(); i++) {
                            files.get(i).isPaused = true;
                        }
                        serverListener.onFailedConnection();
                        chunk.isSending = false;
                    } else {
                        chunk.isSending = false;
                    }
                }
            });

        }
    }
}
