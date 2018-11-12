package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    WeakReference<MainActivity> wr;
    String ip;
    boolean idle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ip = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getString("svip", "dltngz.ddns.net");

        wr = new WeakReference<>(this);

        RecyclerView recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new Thread(new Runnable(){
            @Override
            public void run() {
                MainActivity act;
                do {
                    boolean b = false;
                    act = wr.get();
                    if (act.idle) {
                        try {
                            Client.establish(act.ip, 1337);
                            b = true;
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                        ((ImageView)act.findViewById(R.id.main_svst))
                                .setImageResource(b
                                        ? android.R.drawable.presence_online
                                        : android.R.drawable.presence_offline);
                    }
                    //Log.d("Checker Thread", "Tick");
                    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); return; }
                } while (!act.isFinishing() && !act.isDestroyed());
            }
        }){
            WeakReference<MainActivity> wr;
            Thread set(MainActivity activity) {
                wr = new WeakReference<>(activity);
                return this;
            }
        }.set(this).start();
        idle = true;
    }

    public void onClickAdd(View v) {
        startActivity( new Intent(MainActivity.this, EditActivity.class).putExtra("new", true) );
    }

    @SuppressLint("StaticFieldLeak")
    public void onClickGuess(View v) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_editip:
                final EditText et = new EditText(this);
                et.setText(ip);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.main_menu_chip)
                        .setView(et)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ip = et.getText().toString();
                                getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).edit()
                                        .putString("svip", et.getText().toString()).apply();
                            }
                        }).show();
                return true;
            default: return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPause() {
        idle = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        idle = true;
        super.onResume();
    }
}
