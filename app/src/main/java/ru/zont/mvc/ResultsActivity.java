package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.Dimension;

public class ResultsActivity extends AppCompatActivity {

    private ResultsAdapter adapter;
    private int spanCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        setSupportActionBar(findViewById(R.id.results_tb));
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);

        spanCount = Dimension.getDisplayWidthDp(this) / 100;

        ArtifactObject.Query query = getIntent().getParcelableExtra("query");
        if (query == null) {
            Toast.makeText(this, R.string.results_error, Toast.LENGTH_LONG).show();
            return;
        }
        actionBar.setTitle(query.title);

        RecyclerView rw = findViewById(R.id.results_rw);
        rw.setAdapter(adapter = new ResultsAdapter(query));
        rw.setLayoutManager(new GridLayoutManager(this, spanCount));

        findViewById(R.id.results_fab).setOnLongClickListener(this::onLongClickMore);
    }

    public void onClickMore(View v) {
        int count = adapter.getItemCount() % spanCount == 0
                ? spanCount : spanCount + adapter.getItemCount() % spanCount;
        adapter.addImageIntentions(count);

        AsyncGetImages.execute(adapter.getQuery().title, count, adapter.getOffset(),
                (query, urls, e) -> onFetch(urls, e, count));
    }

    @SuppressLint("SetTextI18n")
    public boolean onLongClickMore(View v) {
        int count = adapter.getItemCount() % spanCount == 0
                ? spanCount : spanCount + adapter.getItemCount() % spanCount;
        EditText et = new EditText(this);
        et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        et.setText(count+"");
        et.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        new AlertDialog.Builder(this)
                .setTitle(R.string.query_more_count)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int nc = Integer.valueOf(et.getText().toString());
                    adapter.addImageIntentions(nc);
                    AsyncGetImages.execute(adapter.getQuery().title, nc, adapter.getOffset(),
                            (query, urls, e) -> onFetch(urls, e, nc));
                }).create().show();
        return true;
    }

    private void onFetch(String[] urls, Exception e, int c) {
        if (urls == null || e != null) {
            Toast.makeText(this, e != null ? e.getLocalizedMessage() : "Error on fetching urls", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        adapter.addImages(urls, c);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }
}
