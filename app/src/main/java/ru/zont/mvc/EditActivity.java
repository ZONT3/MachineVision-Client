package ru.zont.mvc;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import java.util.Objects;
import java.util.Random;

public class EditActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView list;
    private QueryAdapter adapter;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        toolbar = findViewById(R.id.edit_tb);
        setSupportActionBar(toolbar);
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);

        list = findViewById(R.id.edit_list);
        adapter = new QueryAdapter();
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
