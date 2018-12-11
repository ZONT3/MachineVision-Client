package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;
import java.util.Random;

public class EditActivity extends AppCompatActivity {

    private QueryAdapter adapter;
    private ViewGroup content;

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
        public void onItemClick(final ArtifactObject.Query query) {
            @SuppressLint("InflateParams") View v = getLayoutInflater().inflate(R.layout.fragment_query, null);
            ((TextView)v.findViewById(R.id.query_title)).setText(query.title);
            RecyclerView queryList = v.findViewById(R.id.query_list);
            Button ok = v.findViewById(R.id.query_ok);
            Button delete = v.findViewById(R.id.query_delete);
            final AlertDialog dialog = new AlertDialog.Builder(EditActivity.this)
                    .setView(v)
                    .create();

            queryList.setLayoutManager(new GridLayoutManager(EditActivity.this, 4));
            queryList.setAdapter(new QueryItemAdapter(queryList, query, null));

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    getAdapter().remove(query);
                }
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
