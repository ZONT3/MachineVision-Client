package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ProgressBar main_pb;
    private RecyclerView recyclerView;
    private WeakReference<MainActivity> wr;
    private String ip;
    boolean idle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ip = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getString("svip", "dltngz.ddns.net");
        main_pb = findViewById(R.id.main_pb);
        wr = new WeakReference<>(this);

        recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ObjectAdapter(new ArtifactObject[]{}));

        Client.setup(ip, 1337);
        new Thread(new Runnable(){
            @Override
            public void run() {
                MainActivity act;
                do {
                    boolean b = false;
                    act = wr.get();
                    if (act.idle) {
                        try {
                            Client.establish();
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
        }).start();
    }

    private static class ListGetter extends AsyncTask<Void, Void, ArtifactObject[]> {
        private OnPostExecute postExec;
        private ListGetter(@Nullable OnPostExecute postExec) {
            this.postExec = postExec;
        }

        private Exception e;

        @Override
        protected ArtifactObject[] doInBackground(Void... voids) {
            HashMap<String, String> request = new HashMap<>();
            request.put("request_code", "list_objects");
            try {
                return new Gson().fromJson(Client.sendJsonForResult(new Gson().toJson(request)), ListResponse.class).objects;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArtifactObject[] objects) {
            if (postExec != null) postExec.onPostExecute(objects, e);
        }

        @Override
        protected void onCancelled() {
            if (postExec != null) postExec.onPostExecute(null, e);
        }

        @Override
        protected void onCancelled(ArtifactObject[] artifactObjects) {
            if (postExec != null) postExec.onPostExecute(null, e);
        }

        interface OnPostExecute {
            void onPostExecute(ArtifactObject[] objects, Exception e);
        }

        static class ListResponse {
            private String response_code;
            private ArtifactObject[] objects;
        }
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
        main_pb.setVisibility(View.VISIBLE);
        new ListGetter(new ListGetter.OnPostExecute() {
            @Override
            public void onPostExecute(ArtifactObject[] objects, Exception e) {
                if (e != null || objects == null) {
                    Toast.makeText(MainActivity.this, e != null ? e.getMessage() : "Objects is null", Toast.LENGTH_LONG).show();
                    toolbar.setTitle(R.string.main_restart);
                } else ((ObjectAdapter) recyclerView.getAdapter()).updateDataset(objects);

                main_pb.setVisibility(View.GONE);
                idle = true;
            }
        }).execute();
        super.onResume();
    }
}
