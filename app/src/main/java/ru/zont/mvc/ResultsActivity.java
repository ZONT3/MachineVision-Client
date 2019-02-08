package ru.zont.mvc;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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
    }

    public void onClickMore(View v) {
        int count = adapter.getItemCount() % spanCount == 0
                ? spanCount : spanCount + adapter.getItemCount() % spanCount;
        adapter.addImageIntentions(count);

        AsyncGetImages.execute(adapter.getQuery().title, count, adapter.getOffset(), this::onFetch);
    }

    private void onFetch(String q, String[] urls, Exception e) {
        //TODO сука не успел
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return false;
    }
}
