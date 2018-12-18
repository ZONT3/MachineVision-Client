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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;
import java.util.Random;

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
        public void onItemClick(final ArtifactObject.Query query, int pos) {
            RecyclerView queryList = new RecyclerView(EditActivity.this);
            queryList.setLayoutManager(new GridLayoutManager(EditActivity.this,
                    (int) (Dimension.getDisplayWidthDp(EditActivity.this) * 0.9 / 110)));
            queryList.setAdapter(new QueryItemAdapter(queryList, query, null));
            AlertDialog dialog = new AlertDialog.Builder(EditActivity.this)
                    .setTitle(query.title)
                    .setView(queryList)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.main_op_delete,
                            (i1, i2) -> getAdapter().remove(query))
                    .setOnDismissListener(dialog1 -> adapter.notifyItemChanged(pos))
                    .create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(android.R.color.holo_red_dark));
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
