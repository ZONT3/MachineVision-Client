package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.Dimension;

import static ru.zont.mvc.MarkActivity.EXTRA_ITEM;
import static ru.zont.mvc.MarkActivity.EXTRA_QUERY;

public class ResultsActivity extends AppCompatActivity {
    private static final int REQUEST_MARK = 873;
    private static final int REQUEST_MARK_ALL = 874;

    private ResultsAdapter adapter;
    private int spanCount;
    private GridLayoutManager layoutManager;

    private Intent resultData = new Intent();

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
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        actionBar.setTitle(query.title);

        RecyclerView rw = findViewById(R.id.results_rw);
        rw.setAdapter(adapter = new ResultsAdapter(query));
        rw.setLayoutManager(layoutManager = new GridLayoutManager(this, spanCount));

        adapter.setOnItemClickListener(this::onItemClick);
        adapter.setOnItemLongClickListener(this::onItemLongClick);

        findViewById(R.id.results_fab).setOnLongClickListener(this::onLongClickMore);
    }

    private void onItemClick(ArtifactObject.ImageItem item) {
        startActivityForResult(new Intent(this, MarkActivity.class)
                .putExtra(EXTRA_ITEM, item), REQUEST_MARK);
    }

    private void onItemLongClick(ArtifactObject.ImageItem item) {
        setResult(RESULT_OK, resultData.putExtra("newThumb", item.link));
        Toast.makeText(this, R.string.edit_thumb_set, Toast.LENGTH_SHORT).show();
    }

    public void onClickMore(View v) {
        int count = adapter.getItemCount() % spanCount == 0
                ? spanCount : spanCount + adapter.getItemCount() % spanCount;
        adapter.addImageIntentions(count);

        AsyncGetImages.execute(adapter.getQuery().title, count, adapter.getOffset(),
                (query, urls, e) -> onFetch(urls, e, count));
    }

    @SuppressWarnings("unused")
    @SuppressLint("SetTextI18n")
    public boolean onLongClickMore(View v) {
        int count = adapter.getItemCount() % spanCount == 0
                ? spanCount : spanCount + adapter.getItemCount() % spanCount;
        EditText et = new EditText(this);
        et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        et.setText(count+"");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.results, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.results_menu_delete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.areYouSure)
                        .setNegativeButton(R.string.no, null)
                        .setPositiveButton(R.string.yes, (i1, i2) -> {
                            setResult(RESULT_OK, resultData.putExtra("delete", adapter.getQuery()));
                            finish();
                        }).create().show();
                return true;
            case R.id.results_menu_mark:
                startActivityForResult(new Intent(this, MarkActivity.class)
                        .putExtra(EXTRA_QUERY, adapter.getQuery()), REQUEST_MARK_ALL);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        layoutManager.scrollToPosition(0);
        new Thread(() -> {
            try {
                while (layoutManager.findFirstCompletelyVisibleItemPosition() != 0)
                    Thread.sleep(50);

                runOnUiThread(super::onBackPressed);
            } catch (InterruptedException ignored) { }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }

    @Override
    public void finishAfterTransition() {
        findViewById(R.id.results_content).setTransitionName("NULL");
        super.finishAfterTransition();
    }

    @Override
    public void finish() {
        setResult(RESULT_OK, resultData.putExtra("query", adapter.getQuery()));
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_MARK && resultCode == RESULT_OK
                && data != null && data.hasExtra(EXTRA_ITEM))
            adapter.modifyItem(data.getParcelableExtra(EXTRA_ITEM));
        else if (requestCode == REQUEST_MARK_ALL && data != null && data.hasExtra(EXTRA_QUERY))
            adapter.replaceDataset(data.getParcelableExtra(EXTRA_QUERY));
    }
}
