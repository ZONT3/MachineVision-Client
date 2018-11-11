package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {
    private static final String TAG_QUERY_PREFIX = "QUERY:";

    private int DEFAULT_RTCOUNT;

    private boolean edited = false;

    private ArrayList<ArtifactObject.Query> queries = new ArrayList<>();
    private File aoDir;

    private ProgressBar pb;
    private EditText objTitle;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        DEFAULT_RTCOUNT = getDefaultRtcount();

        pb = findViewById(R.id.edit_pb);
        objTitle = findViewById(R.id.edit_title);

        aoDir = new File(getFilesDir(), AppFields.ARTIFATCOBJ_DIR_NAME);
        if (!aoDir.exists()) aoDir.mkdir();

        String id = getIntent().getStringExtra("id");
        if (id == null) edited = true;
        else {
            final File file = new File(aoDir, id);
            final WeakReference<EditActivity> wr = new WeakReference<>(this);
            if (!file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                        ArtifactObject object = (ArtifactObject) in.readObject();
                        in.close();

                        objTitle.setText(object.getTitle());
                        for (ArtifactObject.Query q : object.getQueries())
                            addQuery(wr, new ArrayList<String>(), q.title, DEFAULT_RTCOUNT);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(wr.get(), e.getMessage(), Toast.LENGTH_LONG).show();
                        wr.get().finish();
                    }
                }
            });
        }
    }

    public void onClickAddQ(View b) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText et = new EditText(this);
        et.setHint(R.string.edit_addqdiag_hint);
        et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setTitle(R.string.edit_addqdiag_title)
                .setView(et)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new NewQuery(EditActivity.this, et.getText().toString()).execute();
                    }
                }).create().show();

    }

    private static class NewQuery extends AsyncTask<Void, Void, ArrayList<String>> {
        private WeakReference<EditActivity> ea;
        private String query;

        private IOException e;

        private int DEFAULT_RTCOUNT;

        NewQuery(EditActivity ea, String q) {
            this.ea = new WeakReference<>(ea);
            query = q;
            DEFAULT_RTCOUNT = ea.DEFAULT_RTCOUNT;
        }

        @Override
        protected void onPreExecute() {
            ea.get().pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            HashMap<String, String>[] list;
            try {
                list = new Gson().fromJson(Client.sendJsonForResult(
                        "{\"request_code\":\"reqimg\"," +
                        "\"query\":\""+query+"\"," +
                        "\"rtcount\":\""+DEFAULT_RTCOUNT+"\"}"
                ), MetadataAnswer.class).metadata;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
            ArrayList<String> urls = new ArrayList<>();
            for (HashMap<String, String> ent : list) {
                if (ent.get("image_link")!=null) urls.add(ent.get("image_link"));
            }
            return urls;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            if (strings == null) {
                ea.get().finish();
                Toast.makeText(ea.get(), e == null ? "Unknown error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (strings.size() > 0) {
                addQuery(ea, strings, query, DEFAULT_RTCOUNT);
                ea.get().edited = true;
            } else Toast.makeText(ea.get(), R.string.edit_nores, Toast.LENGTH_LONG).show();
            ea.get().pb.setVisibility(View.GONE);
        }
    }

    private static void addQuery(final WeakReference<EditActivity> ea, ArrayList<String> strings, final String query, final int DEFAULT_RTCOUNT) {
        LinearLayout linearLayout = ea.get().findViewById(R.id.edit_list);
        View frag = LayoutInflater.from(ea.get()).inflate(R.layout.fragment_query, linearLayout, false);
        TextView tw = frag.findViewById(R.id.query_title);
        tw.setText(query);

        int spans = Dimension.toDp(ea.get().getResources().getDisplayMetrics().widthPixels, ea.get()) / 100;
        final RecyclerView rw = frag.findViewById(R.id.query_recycler);
        rw.setLayoutManager(new GridLayoutManager(ea.get(), spans) /*{
            @Override
            public boolean canScrollVertically() { return false; }
        }*/);
        rw.setAdapter(new EditThumbAdapter(strings, new Runnable() {
            @Override
            public void run() {
                ea.get().edited = true;
            }
        }));
        ((EditThumbAdapter)rw.getAdapter()).mOffset += DEFAULT_RTCOUNT;

        Button more = frag.findViewById(R.id.query_more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetMore(new WeakReference<>(rw), ea, query, DEFAULT_RTCOUNT).execute();
            }
        });
        more.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final EditText et = new EditText(ea.get());
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                et.setText("5");
                new AlertDialog.Builder(ea.get())
                        .setTitle(R.string.edit_setrtc)
                        .setView(et)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new GetMore(new WeakReference<>(rw), ea, query, Integer.valueOf(et.getText().toString())).execute();
                            }
                        }).create().show();
                return true;
            }
        });

        frag.setTag(TAG_QUERY_PREFIX+query);
        ea.get().queries.add(new ArtifactObject.Query(query));
        linearLayout.addView(frag, linearLayout.getChildCount() - 1);
    }

    private static class GetMore extends AsyncTask<Void, Void, ArrayList<String>> {
        private WeakReference<RecyclerView> rv;
        private WeakReference<EditActivity> ea;
        private int offset;
        private String query;
        private int rtcount;

        private IOException e;

        private GetMore(WeakReference<RecyclerView> rv, WeakReference<EditActivity> ea, String q, int rtcount) {
            this.rv = rv;
            this.ea = ea;
            query = q;
            offset = ((EditThumbAdapter)rv.get().getAdapter()).mOffset;
            this.rtcount = rtcount;
        }

        @Override
        protected void onPreExecute() {
            ea.get().pb.setVisibility(View.VISIBLE);
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            HashMap<String, String>[] list;
            try {
                list = new Gson().fromJson(Client.sendJsonForResult(String.format(
                        "{\"request_code\":\"reqimg\"," +
                                "\"query\":\"%s\"," +
                                "\"rtcount\":\"%d\","+
                                "\"offset\":\"%d\"}",
                        query, rtcount, offset)
                ), MetadataAnswer.class).metadata;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
            ArrayList<String> urls = new ArrayList<>();
            for (HashMap<String, String> ent : list) {
                if (ent.get("image_link")!=null) urls.add(ent.get("image_link"));
            }
            return urls;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            if (strings == null) {
                ea.get().finish();
                Toast.makeText(rv.get().getContext(), e == null ? "Error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            EditThumbAdapter eta = (EditThumbAdapter) rv.get().getAdapter();
            eta.add(strings);
            eta.mOffset += rtcount;
            ea.get().pb.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void save(@Nullable final Runnable postExec) {
        Log.d("EditActivity", "Extracting blacklists...");
        String thumbnail = null;
        for (ArtifactObject.Query q : queries) {
            View v = findViewById(R.id.edit_list).findViewWithTag(TAG_QUERY_PREFIX+q.title);
            if (v == null) {
                Toast.makeText(this, "Разработчик - долбоеб", Toast.LENGTH_LONG).show();
                return;
            }
            EditThumbAdapter adapter = (EditThumbAdapter)((RecyclerView)v.findViewById(R.id.query_recycler)).getAdapter();
            q.blacklist = adapter.getBlacklist();
            if (thumbnail == null) thumbnail = adapter.getThumbUrl();
        }


        Log.d("EditActivity", "Generating ArtifactObject...");
        final ArtifactObject object = new ArtifactObject(objTitle.getText().toString(), queries);
        object.setThumbnail(thumbnail);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("EditActivity", "Serializing ArtifactObject...");
                try {
                    File file = new File(aoDir, object.id);
                    if (file.exists()) file.delete();
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
                    out.writeObject(object);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Log.d("EditActivity", "Looks like it saved successfully.");
                edited = false;
                if (postExec != null) postExec.run();
            }
        }).start();
    }

    public void onSave(View v) {
        if (objTitle.getText().toString().equals("")) {
            Toast.makeText(this, R.string.edit_err_title_void, Toast.LENGTH_LONG).show();
            return;
        }
        save(new Runnable() {
            WeakReference<EditActivity> wr;
            Runnable set(EditActivity activity) {
                wr = new WeakReference<>(activity);
                return this;
            }

            @Override
            public void run() {
                Toast.makeText(wr.get(), R.string.edit_saved, Toast.LENGTH_SHORT).show();
            }
        }.set(this));
    }

    @Override
    public void onBackPressed() {
        if (edited) {
            if (objTitle.getText().toString().equals("")) {
                Toast.makeText(this, R.string.edit_err_title_void, Toast.LENGTH_LONG).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.edit_save)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pb.setVisibility(View.VISIBLE);
                            final WeakReference<EditActivity> wr = new WeakReference<>(EditActivity.this);
                            new AsyncSave(wr).execute();
                        }})
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditActivity.super.onBackPressed();
                        }})
                    .setNeutralButton(android.R.string.cancel, null)
                    .create().show();
        } else super.onBackPressed();
    }

    private void superOnBackPressed() {
        super.onBackPressed();
    }

    private static class AsyncSave extends AsyncTask<Void, Void, Void> {
        WeakReference<EditActivity> wr;
        AsyncSave(WeakReference<EditActivity> wr) {
            this.wr = wr;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            wr.get().save(null);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            wr.get().superOnBackPressed();
        }
    }

    private int getDefaultRtcount() {
        int count = Dimension.toDp(getResources().getDisplayMetrics().widthPixels, this) / 100;
        if (count <= 4) count *= 2;
        return count;
    }

    @SuppressWarnings("unused")
    private static class MetadataAnswer {
        private String answerCode;
        private HashMap<String, String>[] metadata;
    }

}
