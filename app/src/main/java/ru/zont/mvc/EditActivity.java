package ru.zont.mvc;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.util.Objects;
import java.util.Random;

public class EditActivity extends AppCompatActivity {

    private QueryAdapter adapter;
    private ViewGroup dialog;
    private View content;

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
        dialog = findViewById(R.id.edit_query_diag);
        RecyclerView list = findViewById(R.id.edit_list);
        adapter = new QueryAdapter(list);
        adapter.setOnClickListener(new OnQueryClick(adapter));
        list.setLayoutManager(new GridLayoutManager(this, 2));
        list.setAdapter(adapter);
    }

    public void addQuery(View v) {
        ArtifactObject.Query q = new ArtifactObject.Query(
                new RandomString(8, new Random()).nextString());
        q.whitelist.add("http://www.детский-мир.net/images/paint/14427.gif");
        q.whitelist.add("https://ot2do6.ru/uploads/posts/2014-08/1408537983_a-8.jpg");
        q.whitelist.add("https://ot2do7.ru/uploads/posts/2017-09/medium/1505998175_8.jpg");
        q.whitelist.add("http://moi-raskraski.ru/images/raskraski/obuchalki/alfavit/bukva-o/raskraska-alfavit-bikva-o-5.jpg");
        adapter.add(q);
    }

    private class OnQueryClick extends QueryAdapter.OnClickListener {
        OnQueryClick(QueryAdapter qa) {
            super(qa);
        }

        @Override
        public void onItemClick(ArtifactObject.Query query) {
            int listWidth = Dimension.toDp(getResources().getDisplayMetrics().widthPixels, EditActivity.this)
                    - 16 - 32;

            RecyclerView queryList = dialog.findViewById(R.id.query_list);
            queryList.setLayoutManager(new GridLayoutManager(EditActivity.this, listWidth / 100));
            queryList.setAdapter(new QueryItemAdapter(queryList, query, null));

            dialog.findViewById(R.id.query_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.startAnimation(AnimationUtils.loadAnimation(EditActivity.this, R.anim.fadeout));
                    dialog.postOnAnimation(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setVisibility(View.GONE);
                            content.setForeground(null);
                        }
                    });
                }
            });
            dialog.findViewById(R.id.query_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO
                }
            });

            dialog.startAnimation(AnimationUtils.loadAnimation(EditActivity.this, R.anim.fadein));
            dialog.setVisibility(View.VISIBLE);
            dialog.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    content.setForeground(getDrawable(android.R.drawable.screen_background_dark_transparent));
                }
            });
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

    @Override
    public void onBackPressed() {
        if (dialog.getVisibility() != View.VISIBLE) super.onBackPressed();
        else dialog.findViewById(R.id.query_ok).callOnClick();
    }
}
