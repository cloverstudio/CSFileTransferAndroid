package com.example.chunkedupload;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.example.chunkedupload.upload.CSUpload;
import com.example.chunkedupload.upload.SingleFile;
import com.example.chunkedupload.upload.listeners.OnServerListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UploadAdapter mAdapter;
    private LinearLayoutManager layoutManager;
    private List<SingleFile> uploads;
    final String url = "http://192.168.1.16:3000/";
    private int sizeOfChunks = 1024 * 1024;
    private int numberOfConnections = 5;
    private CSUpload csUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uploads = new ArrayList<>();
        recyclerView = findViewById(R.id.my_recycler_view);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        csUpload = CSUpload.getInstance(url, sizeOfChunks, numberOfConnections, getContentResolver());
        mAdapter = new UploadAdapter(uploads);
        recyclerView.setAdapter(mAdapter);

        Button chooseFile = findViewById(R.id.chooseFIle);

        chooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                startActivityForResult(intent, 42);
            }
        });

        csUpload.setOnServerListener(new OnServerListener() {
            @Override
            public void onFailedConnection() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Can't Connect To Server!")
                        .setMessage("")
                        .show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm.getActiveNetworkInfo() != null) {
                if (data.getClipData() == null) {
                    Uri uri = data.getData();
                    mAdapter.addToAdapter(csUpload.sendFile(uri));
                } else {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        mAdapter.addToAdapter(csUpload.sendFile(uri));
                    }
                }

                mAdapter.notifyDataSetChanged();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("No Internet Connection!")
                        .setMessage("Please Connect To Internet")
                        .show();
            }

        }
    }
}
