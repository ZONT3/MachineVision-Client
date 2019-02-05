package ru.zont.mvc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ObjectAdapter adapter;

    private ProgressBar pb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.main_toolbar));

        RecyclerView recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setAdapter(adapter = new ObjectAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pb = findViewById(R.id.main_pb);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //TODO CheckerThreads and getList usage
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void getList() {
        new ListGetter(new ListGetter.AsyncRunnable() {
            @Override
            public void onPreExecute() {
                pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPostExecute(ArtifactObject[] objects, Exception e) {
                pb.setVisibility(View.GONE);
                if (e != null) {
                    Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (objects == null) {
                    Toast.makeText(MainActivity.this, "Error on getting list", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                adapter.updateDataset(objects);
            }
        }).execute();
    }

    @SuppressWarnings("CanBeFinal")
    private static class ListGetter extends AsyncTask<Void, Void, ArtifactObject[]> {
        private AsyncRunnable runnable;
        private ListGetter(@Nullable AsyncRunnable runnable) {
            this.runnable = runnable;
        }

        private Exception e;

        @Override
        protected void onPreExecute() {
            runnable.onPreExecute();
        }

        @Override
        protected ArtifactObject[] doInBackground(Void... voids) {
            HashMap<String, String> request = new HashMap<>();
            request.put("request_code", "list_objects");
            try {
                return new Gson().fromJson(Client.sendJsonForResult(new Gson().toJson(request)), ListResponse.class).objects;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArtifactObject[] objects) {
            if (runnable != null) runnable.onPostExecute(objects, e);
        }

        @Override
        protected void onCancelled() {
            if (runnable != null) runnable.onPostExecute(null, e);
        }

        @Override
        protected void onCancelled(ArtifactObject[] artifactObjects) {
            if (runnable != null) runnable.onPostExecute(null, e);
        }

        interface AsyncRunnable {
            void onPreExecute();
            void onPostExecute(ArtifactObject[] objects, Exception e);
        }

        @SuppressWarnings("unused")
        static class ListResponse {
            private String response_code;
            private ArtifactObject[] objects;
        }
    }

    public void onClickGuess(View v) {
        EditText view = new EditText(this);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        new AlertDialog.Builder(this)
                .setTitle("Enter URL of image")
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setCancelable(false);
                    pd.create();
                    pd.show();

                    new Thread(() -> {
                        HashMap<String, String> request = new HashMap<>();
                        request.put("request_code", "guess");
                        request.put("url", view.getText().toString());

                        try {
                            String response = Client.sendJsonForResult(new Gson().toJson(request), 1200);
                            URL url = new URL(response);
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(Uri.parse(url.toString()), "image/*"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            pd.dismiss();
                        }
                    }).start();
                })
                .create().show();
    }

    @NonNull
    static String getActionString(ArtifactObject object, Context context) {
        switch (object.getLastActType()) {
            default:
            case ArtifactObject.ACTION.CREATED: return context.getString(R.string.artobj_created);
            case ArtifactObject.ACTION.EDITED: return context.getString(R.string.artobj_edited);
            case ArtifactObject.ACTION.TRAINED: return context.getString(R.string.artobj_learned);
            case ArtifactObject.ACTION.STARTED_TRAINING: return context.getString(R.string.artobj_started);
        }
    }

    @NonNull
    static String getStatusString(ArtifactObject object, Context context) {
        switch (object.getStatus()) {
            case ArtifactObject.STATUS.READY_TL: return context.getString(R.string.artobj_status_rtl);
            case ArtifactObject.STATUS.DOWNLOADING: return context.getString(R.string.artobj_status_dl);
            case ArtifactObject.STATUS.TRAINING: return context.getString(R.string.artobj_status_lrn);
            case ArtifactObject.STATUS.READY_FU: return context.getString(R.string.artobj_status_rfu);
            case ArtifactObject.STATUS.OUTDATED: return context.getString(R.string.artobj_status_odt);
            case ArtifactObject.STATUS.ERROR_LEARN: return context.getString(R.string.artobj_status_errl);
            case ArtifactObject.STATUS.ERROR: return context.getString(R.string.artobj_status_err);
            default: return context.getString(R.string.artobj_status_err);
        }
    }
}
