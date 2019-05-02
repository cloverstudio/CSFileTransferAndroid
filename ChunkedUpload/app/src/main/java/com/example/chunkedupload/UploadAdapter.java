package com.example.chunkedupload;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.chunkedupload.upload.SingleFile;
import com.example.chunkedupload.upload.listeners.OnProgressListener;

import java.util.List;

public class UploadAdapter extends RecyclerView.Adapter<UploadAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.upload_item, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final SingleFile singleFile = uploads.get(i);

        Button pause = viewHolder.pause;
        Button continueButton = viewHolder.continueButton;
        final ProgressBar progressBar = viewHolder.progressBar;
        TextView fileName = viewHolder.fileName;
        fileName.setText(singleFile.getFileName());

        progressBar.setMax((int)singleFile.getNumberOfChunks());
        progressBar.setProgress(singleFile.chunksUploaded);

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleFile.pause();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleFile.resume();
            }
        });

        singleFile.setOnProgressListener(new OnProgressListener() {
            @Override
            public void onProgress(long max, int uploaded) {
                progressBar.setMax((int)max);
                progressBar.setProgress(uploaded);
            }
        });

    }

    @Override
    public int getItemCount() {
        if (uploads == null) {
            return 0;
        }
        return uploads.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public Button pause;
        public Button continueButton;
        public ProgressBar progressBar;
        public TextView fileName;

        public ViewHolder(View itemView) {
            super(itemView);

            pause = itemView.findViewById(R.id.button_pause);
            continueButton = itemView.findViewById(R.id.button_resume);
            progressBar = itemView.findViewById(R.id.progressBar1);
            fileName = itemView.findViewById(R.id.fileName);
        }
    }

    private List<SingleFile> uploads;

    public void addToAdapter(SingleFile file) {
        uploads.add(file);
    }

    public UploadAdapter(List<SingleFile> uploads) {
        this.uploads = uploads;
    }
}
