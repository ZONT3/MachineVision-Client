package ru.zont.mvc;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private QueryAdapter adapter;
    private ViewGroup content;
    private EditText title;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        Toolbar toolbar = findViewById(R.id.edit_tb);
        setSupportActionBar(toolbar);
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);

        content = findViewById(R.id.edit_content);
        RecyclerView list = findViewById(R.id.edit_list);
        title = findViewById(R.id.edit_title);

        adapter = new QueryAdapter(list);
        adapter.setOnClickListener(new OnQueryClick(adapter));
        list.setLayoutManager(new GridLayoutManager(this, Dimension.getDisplayWidthDp(this) / 190));
        list.setAdapter(adapter);
    }

    public void addQuery(View v) {
        EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_addqdiag_title)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        adapter.addQuery(editText.getText().toString(), EditActivity.this))
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    static class ImageGetter extends AsyncTask<Void, Void, String[]> {
        private WeakReference<EditActivity> wr;
        private ImageGetterPostExec postExec;

        private String query;
        private int rtcount;
        private int offset;

        ImageGetter(EditActivity activity, ImageGetterPostExec postExec,
                    String query, int rtcount) {
            this(activity, postExec, query, rtcount, 0);
        }

        ImageGetter(EditActivity activity, ImageGetterPostExec postExec,
                    String query, int rtcount, int offset) {
            wr = new WeakReference<>(activity);
            this.postExec = postExec;

            this.query = query;
            this.rtcount = rtcount;
            this.offset = offset;
            execute();
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            Request request = new Request(query, rtcount, offset);
            try {
                String responseStr = Client.sendJsonForResult(new Gson().toJson(request),
                        /*"query_metadata",*/
                        rtcount > 8 ? rtcount * 12000 : 30000);
                Response response = new Gson().fromJson(responseStr, Response.class);
                if (!response.response_code.equals("query_metadata")) {
                    new Exception("Incorrect response_code: " + response.response_code).printStackTrace();
                    return null;
                }

                String[] result = new String[response.metadata.length];
                for (int i = 0; i < result.length; i++)
                    result[i] = (String) response.metadata[i].get("image_link");
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (postExec != null)
                postExec.postExec(wr, strings);
        }

        static abstract class ImageGetterPostExec {
            abstract void postExec(WeakReference<EditActivity> wr, String[] result);
        }

        @SuppressWarnings("unused")
        private static class Request {
            private String request_code = "reqimg";
            private String query;
            private int rtcount;
            private int offset;

            private Request(String query, int rtcount, int offset) {
                this.query = query;
                this.rtcount = rtcount;
                this.offset = offset;
            }
        }

        @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
        private static class Response {
            private String response_code;
            private HashMap<Object, Object>[] metadata;
        }
    }

    private class OnQueryClick extends QueryAdapter.OnClickListener {
        OnQueryClick(QueryAdapter qa) {
            super(qa);
        }

        @Override
        public void onItemClick(final ArtifactObject.Query query, int pos) {
            int spanCount = (int) (Dimension.getDisplayWidthDp(EditActivity.this) * 0.9 / 110);

            RecyclerView queryList = new RecyclerView(EditActivity.this);
            queryList.setLayoutManager(new GridLayoutManager(EditActivity.this, spanCount));
            QueryItemAdapter adapter = new QueryItemAdapter(queryList, query, new QueryItemAdapter.OnClickListener() {
                @Override
                public void onItemClick(String item) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(item), "image/*"));
                }
            }, new QueryItemAdapter.OnLongClickListener() {
                @Override
                public void onItemClick(String item) {
                    // TODO: 23.12.2018
                }
            });
            queryList.setAdapter(adapter);
            if (adapter.getItemCount() % spanCount != 0)
                adapter.loadMore(EditActivity.this, spanCount - (adapter.getItemCount() % spanCount));

            AlertDialog dialog = new AlertDialog.Builder(EditActivity.this, R.style.QueryDiag)
                    .setTitle(query.title)
                    .setView(queryList)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.main_op_delete,
                            (i1, i2) -> getAdapter().remove(query))
                    .setNeutralButton(R.string.query_more, null)
                    .setOnDismissListener(dialog1 -> EditActivity.this.adapter.notifyItemChanged(pos))
                    .setCancelable(false)
                    .create();

            dialog.setOnShowListener(dialog1 -> {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(getColor(android.R.color.holo_red_dark));

                Button neutButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

                neutButton.setOnClickListener(v -> {
                    int count = spanCount + ((adapter.getItemCount() % spanCount == 0) ? 0 :
                            (spanCount - (adapter.getItemCount() % spanCount)));
                    adapter.loadMore(EditActivity.this, count < 6 ? count + spanCount : count);
                });

                neutButton.setLongClickable(true);
                neutButton.setOnLongClickListener(v -> {
                    EditText et = new EditText(EditActivity.this);
                    et.setHint(R.string.query_more_count);
                    et.setInputType(InputType.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(EditActivity.this)
                            .setTitle(R.string.query_more)
                            .setView(et)
                            .setPositiveButton(android.R.string.ok, (dialog2, which) ->
                                    adapter.loadMore(EditActivity.this,
                                            Integer.valueOf(et.getText().toString())))
                            .create().show();
                    return true;
                });
            });
            dialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
