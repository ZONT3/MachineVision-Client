package ru.zont.mvc;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {
    private static final String TAG_QUERY_PREFIX = "QUERY:";

    private int DEFAULT_RTCOUNT;

    private boolean edited = false;
    private boolean edit = false;

    private ArtifactObject object;
    private ArrayList<ArtifactObject.Query> queries = new ArrayList<>();

    private ProgressBar pb;
    private EditText objTitle;
    private String thumbnail;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        DEFAULT_RTCOUNT = getDefaultRtcount();

        pb = findViewById(R.id.edit_pb);
        objTitle = findViewById(R.id.edit_title);

        File aoDir = new File(getFilesDir(), AppFields.ARTIFATCOBJ_DIR_NAME);
        if (!aoDir.exists()) aoDir.mkdir();

        object = getIntent().getParcelableExtra("object");
        if (object != null)  {
            edit = true;
            pb.setVisibility(View.VISIBLE);

            queries = object.getQueries();
            final WeakReference<EditActivity> wr = new WeakReference<>(this);
            final ArrayList<String[]> thumbnails = new ArrayList<>();
            if (queries.size() > 0) {
                new GetImages(queries.get(0).title, DEFAULT_RTCOUNT, -1, new GetImages.OnPostExecute() {
                    int i = 0;

                    @Override
                    public void onPostExecute(ArrayList<String> strings, String query, int rtcount, int offset, Exception e) {
                        if (strings == null || e != null) {
                            Toast.makeText(wr.get(), e != null ? e.getMessage() : "Unknown error", Toast.LENGTH_LONG).show();
                            wr.get().finish();
                            return;
                        }
                        for (String s : strings) {
                            if (queries.get(i).blacklist.contains(s))
                                strings.remove(s);
                        }
                        thumbnails.add((String[]) strings.toArray());

                        i++;
                        if (i < queries.size()) new GetImages(
                                queries.get(i).title,
                                DEFAULT_RTCOUNT,
                                -1,
                                this
                        ).execute();
                        else {
                            pb.setVisibility(View.GONE);
                            for (ArtifactObject.Query q : queries) {
                                ArrayList<String> tnls = new ArrayList<>();
                                Collections.addAll(tnls, thumbnails.get(queries.indexOf(q)));
                                addQuery(wr, tnls, q.title, DEFAULT_RTCOUNT);
                            }
                        }
                    }
                }).execute();
            }
        }
        if (!edit) edited = true;
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
                        for (ArtifactObject.Query q : queries) {
                            if (q.title.equals(et.getText().toString())) {
                                Toast.makeText(EditActivity.this, R.string.query_alrex, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        pb.setVisibility(View.VISIBLE);
                        new GetImages(et.getText().toString(),
                                DEFAULT_RTCOUNT, -1,
                                new GetImages.OnPostExecute() {
                                    @Override
                                    public void onPostExecute(ArrayList<String> strings, String query, int rtcount, int offset, Exception e) {
                                        if (strings == null) {
                                            finish();
                                            Toast.makeText(EditActivity.this, e == null ? "Unknown error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        if (strings.size() > 0) {
                                            addQuery(new WeakReference<>(EditActivity.this), strings, query, DEFAULT_RTCOUNT);
                                            queries.add(new ArtifactObject.Query(query));
                                            edited = true;
                                        } else Toast.makeText(EditActivity.this, R.string.edit_nores, Toast.LENGTH_LONG).show();
                                        pb.setVisibility(View.GONE);
                                    }
                                });
                    }
                }).create().show();

    }

    private static void addQuery(final WeakReference<EditActivity> ea, ArrayList<String> strings, final String query, final int DEFAULT_RTCOUNT) {
        final EditActivity activity = ea.get();
        final LinearLayout linearLayout = activity.findViewById(R.id.edit_list);
        final View frag = LayoutInflater.from(activity).inflate(R.layout.fragment_query, linearLayout, false);
        TextView tw = frag.findViewById(R.id.query_title);
        tw.setText(query);

        int spans = Dimension.toDp(activity.getResources().getDisplayMetrics().widthPixels, activity) / 100;
        final RecyclerView rv = frag.findViewById(R.id.query_recycler);
        rv.setLayoutManager(new GridLayoutManager(activity, spans) {

            @Override
            public boolean canScrollVertically() { return false; }
        });
        rv.setAdapter(new EditThumbAdapter(strings, new Runnable() {
            @Override
            public void run() {
                activity.edited = true;
            }
        }, ea.get()));
        ((EditThumbAdapter)rv.getAdapter()).mOffset += DEFAULT_RTCOUNT;

        Button more = frag.findViewById(R.id.query_more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.pb.setVisibility(View.VISIBLE);
                new GetImages(query, DEFAULT_RTCOUNT,
                        ((EditThumbAdapter) rv.getAdapter()).mOffset,
                        new GetImages.OnPostExecute() {
                            @Override
                            public void onPostExecute(ArrayList<String> strings, String query, int rtcount, int offset, Exception e) {
                                if (strings == null) {
                                    activity.finish();
                                    Toast.makeText(activity, e == null ? "Error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                EditThumbAdapter eta = (EditThumbAdapter) rv.getAdapter();
                                eta.add(strings);
                                eta.mOffset += rtcount;
                                activity.pb.setVisibility(View.GONE);
                            }
                        });
            }
        });
        more.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final EditText et = new EditText(activity);
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                et.setText("5");
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.edit_setrtc)
                        .setView(et)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.pb.setVisibility(View.VISIBLE);
                                new GetImages(query,
                                        Integer.valueOf(et.getText().toString()),
                                        ((EditThumbAdapter) rv.getAdapter()).mOffset,
                                        new GetImages.OnPostExecute() {
                                            @Override
                                            public void onPostExecute(ArrayList<String> strings, String query, int rtcount, int offset, Exception e) {
                                                if (strings == null) {
                                                    activity.finish();
                                                    Toast.makeText(activity, e == null ? "Error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                EditThumbAdapter eta = (EditThumbAdapter) rv.getAdapter();
                                                eta.add(strings);
                                                eta.mOffset += rtcount;
                                                activity.pb.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }).create().show();
                return true;
            }
        });

        Button delete = frag.findViewById(R.id.query_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.query_deletediag)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                linearLayout.removeView(frag);
                                for (ArtifactObject.Query q : activity.queries)
                                    if (q.title.equals(query))
                                        activity.queries.remove(q);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            }
        });

        frag.setTag(TAG_QUERY_PREFIX+query);
        linearLayout.addView(frag, linearLayout.getChildCount() - 1);
    }

    private static class GetImages extends AsyncTask<Void, Void, ArrayList<String>> {

        private String query;
        private int rtcount;
        private int offset;
        IOException e;

        private OnPostExecute onPostExecute;

        GetImages(String query, int rtcount, int offset, OnPostExecute onPostExecute) {
            this.query = query;
            this.rtcount = rtcount;
            this.offset = offset;
            this.onPostExecute = onPostExecute;

            execute();
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            HashMap<String, String>[] list;
            try {
                HashMap<String, String> request = new HashMap<>();
                request.put("request_code", "reqimg");
                request.put("query", query+"");
                request.put("rtcount", rtcount+"");
                if (offset >= 0) request.put("offset", offset+"");
                MetadataResponse response =
                        new Gson().fromJson(
                                Client.sendJsonForResult(
                                        new Gson().toJson(request),
                                        rtcount > 10 ? (int) (rtcount / 10.0 * Client.TIMEOUT) : Client.TIMEOUT),
                                MetadataResponse.class);
                if (response.response_code == null || !response.response_code.equals("query_metadata")) throw new IOException("Invalid response from server.");
                list = response.metadata;
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
            onPostExecute.onPostExecute(strings, query, rtcount, offset, e);
        }

        private interface OnPostExecute {

            void onPostExecute(ArrayList<String> strings, String query, int rtcount, int offset, Exception e);
        }
    }
    private static class SendArtifact extends AsyncTask<Void, Void, Void> {

        private Exception e;
        private ArtifactObject object;

        private WeakReference<EditActivity> wr;
        private SendArtifact(ArtifactObject object, EditActivity activity) {
            this.object = object;
            wr = new WeakReference<>(activity);
        }
        @Override
        protected void onPreExecute() {
            wr.get().pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("request_code", "new_object");
                hashMap.put("artifact_object", object);
                String result = Client.sendJsonForResult(new Gson().newBuilder().setPrettyPrinting().create().toJson(hashMap));
                if (!result.equals("success")) throw new IOException("Unexpected response: "+result);
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (e != null) Toast.makeText(wr.get(), e.getMessage(), Toast.LENGTH_LONG).show();
            else Toast.makeText(wr.get(), R.string.edit_saved, Toast.LENGTH_SHORT).show();
            wr.get().finish();
        }

    }
    public void onSave(View v) {
        if (objTitle.getText().toString().equals("")) {
            Toast.makeText(this, R.string.edit_err_title_void, Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("EditActivity", "Extracting blacklists...");
        for (ArtifactObject.Query q : queries) {
            View v1 = findViewById(R.id.edit_list).findViewWithTag(TAG_QUERY_PREFIX+q.title);
            if (v1 == null) {
                Toast.makeText(this, "Разработчик - долбоеб", Toast.LENGTH_LONG).show();
                return;
            }
            EditThumbAdapter adapter = (EditThumbAdapter)((RecyclerView) v1.findViewById(R.id.query_recycler)).getAdapter();
            q.blacklist = adapter.getBlacklist();
            if (thumbnail == null) thumbnail = adapter.getThumbUrl();
        }

        Log.d("EditActivity", "Generating ArtifactObject...");
        if (object == null) {
            object = new ArtifactObject(objTitle.getText().toString(), queries);
            object.setThumbnail(thumbnail);
        } else object.edit(objTitle.getText().toString(), queries);

        new SendArtifact(object, this).execute();
    }

    public void onBack(View v) { onBackPressed(); }

    @Override
    public void onBackPressed() {
        if (edited) {
            final WeakReference<EditActivity> wr = new WeakReference<>(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.edit_save)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (objTitle.getText().toString().equals("")) {
                                Toast.makeText(wr.get(), R.string.edit_err_title_void, Toast.LENGTH_LONG).show();
                                return;
                            }
                            onSave(null);
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

    public void setThumbnail(String url) { thumbnail = url; }

    private int getDefaultRtcount() {
        int count = Dimension.toDp(getResources().getDisplayMetrics().widthPixels, this) / 100;
        if (count <= 4) count *= 2;
        return count;
    }
    @SuppressWarnings("unused")
    private static class MetadataResponse {

        private String response_code;
        private HashMap<String, String>[] metadata;

    }
}
