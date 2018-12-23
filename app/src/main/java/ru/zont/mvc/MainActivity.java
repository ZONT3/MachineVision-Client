package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ProgressBar main_pb;
    private RecyclerView recyclerView;
    private WeakReference<MainActivity> wr;
    private String ip;
    private int port;
    private boolean idle = false;
    private ImageView svst;
    private boolean listGettingFail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ip = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getString("svip", "dltngz.ddns.net");
        port = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getInt("svport", 1337);
        main_pb = findViewById(R.id.main_pb);
        svst = findViewById(R.id.main_svst);
        wr = new WeakReference<>(this);

        recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ObjectAdapter(new ArtifactObject[]{}, new OnItemClick()));

        Client.setup(ip, port);
        new Thread(() -> {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            MainActivity act;
            do {
                boolean b = false;
                act = wr.get();
                if (act.idle) {
                    try {
                        Client.establish();
                        b = true;
                        if (listGettingFail) {
                            getList();
                            listGettingFail = false;
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    final boolean fnB = b;
                    if (svst.getTag() == null || !svst.getTag().equals(fnB))
                        runOnUiThread(() -> {
                            svst.setImageResource(fnB
                                    ? android.R.drawable.presence_online
                                    : android.R.drawable.presence_offline);
                            svst.setTag(fnB);
                            Log.d("ChecerThread", "Changing svst");
                        });

                }
                //Log.d("Checker Thread", "Tick");
                try { Thread.sleep(1500); } catch (InterruptedException e) { e.printStackTrace(); return; }
            } while (!act.isFinishing() && !act.isDestroyed());
        }).start();
    }

    private class OnItemClick extends ObjectAdapter.OnItemClick {
        @SuppressLint("SetTextI18n")
        @Override
        void onItemClick(final ArtifactObject object) {
            @SuppressLint("InflateParams")
            View v = getLayoutInflater().inflate(R.layout.dialog_objectpropts, null);
            TextView title = v.findViewById(R.id.objpts_title);
            Switch swith = v.findViewById(R.id.objpts_switch);
            TextView status = v.findViewById(R.id.objpts_status);
            TextView queries = v.findViewById(R.id.objpts_queries);
            TextView total = v.findViewById(R.id.objpts_total);
            TextView created = v.findViewById(R.id.objpts_created);
            TextView learned = v.findViewById(R.id.objpts_learned);
            ConstraintLayout edit = v.findViewById(R.id.objpts_btnlay_edit);
            ConstraintLayout delete = v.findViewById(R.id.objpts_btnlay_delete);

            boolean available = object.getStatus() != ArtifactObject.STATUS.DOWNLOADING
                    && object.getStatus() != ArtifactObject.STATUS.LEARNING;
            edit.setEnabled(available);
            delete.setEnabled(available);
            swith.setEnabled(available);

            title.setText(object.getTitle());
            swith.setChecked(object.isEnabled());
            status.setText(getStatusString(object, MainActivity.this));
            queries.setText(object.getQueriesSize()+"");
            total.setText(object.getTotal() >= 0 ? object.getTotal()+"" : getString(R.string.main_op_unknown));

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(object.getCreated());
            created.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                    .format(calendar.getTime()));

            String learnedStr = "---";
            if (object.getLearned() >= 0) {
                calendar.setTimeInMillis(object.getLearned());
                learnedStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                        .format(calendar.getTime());
            }
            learned.setText(learnedStr);

            final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(v).create();

            edit.setOnClickListener(v12 -> {
                startActivity(new Intent(MainActivity.this, EditActivity.class)
                        .putExtra("object", object));
                dialog.dismiss();
            });
            delete.setOnClickListener(v1 -> {
                dialog.dismiss();
                new Thread(() -> {
                    HashMap<String, String> request = new HashMap<>();
                    request.put("request_code", "delete_object");
                    request.put("id", object.getId());
                    try {
                        Client.sendJsonForResult(new Gson().toJson(request));
//                            Thread.sleep(1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getList();
                }).start();
            });
            swith.setOnCheckedChangeListener((buttonView, isChecked) -> {
                object.setEnabled(isChecked);
                new Thread(() -> {
                    Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                    HashMap<String, Object> request = new HashMap<>();
                    request.put("request_code", "new_object");
                    request.put("artifact_object", object);
                    try {
                        Client.sendJsonForResult(new Gson().toJson(request));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            });

            dialog.show();
        }
    }

    @NonNull
    static String getStatusString(ArtifactObject object, Context context) {
        String status = context.getString(R.string.artobj_status_undefined);
        switch (object.getStatus()) {
            case ArtifactObject.STATUS.READY_TL: status = context.getString(R.string.artobj_status_rtl); break;
            case ArtifactObject.STATUS.DOWNLOADING: status = context.getString(R.string.artobj_status_dl); break;
            case ArtifactObject.STATUS.LEARNING: status = context.getString(R.string.artobj_status_lrn); break;
            case ArtifactObject.STATUS.READY_FU: status = context.getString(R.string.artobj_status_rfu); break;
            case ArtifactObject.STATUS.OUTDATED: status = context.getString(R.string.artobj_status_odt); break;
            case ArtifactObject.STATUS.ERROR_LEARN: status = context.getString(R.string.artobj_status_errl); break;
            case ArtifactObject.STATUS.ERROR: status = context.getString(R.string.artobj_status_err); break;
        }
        return status;
    }

    @SuppressWarnings("CanBeFinal")
    private static class ListGetter extends AsyncTask<Void, Void, ArtifactObject[]> {
        private AsyncRunnable runnable;
        private ListGetter(@Nullable AsyncRunnable runnable) {
            this.runnable = runnable;
        }

        private Exception e;

        @Override
        protected void onPreExecute() {
            runnable.onPreExecute();
        }

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
            if (runnable != null) runnable.onPostExecute(objects, e);
        }

        @Override
        protected void onCancelled() {
            if (runnable != null) runnable.onPostExecute(null, e);
        }

        @Override
        protected void onCancelled(ArtifactObject[] artifactObjects) {
            if (runnable != null) runnable.onPostExecute(null, e);
        }

        interface AsyncRunnable {
            void onPreExecute();
            void onPostExecute(ArtifactObject[] objects, Exception e);
        }

        @SuppressWarnings("unused")
        static class ListResponse {
            private String response_code;
            private ArtifactObject[] objects;
        }
    }

    public void onClickAdd(View v) {
        startActivity( new Intent(MainActivity.this, EditActivity.class).putExtra("new", true) );
    }

    @SuppressWarnings("EmptyMethod")
    @SuppressLint("StaticFieldLeak")
    public void onClickGuess(View v) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_editip:
                @SuppressLint("InflateParams") View v = getLayoutInflater().inflate(R.layout.dialog_ip, null);
                final EditText etIP = v.findViewById(R.id.main_ipdiag_ip);
                final EditText etPort = v.findViewById(R.id.main_ipdiag_port);
                etIP.setText(ip);
                etPort.setText(""+port);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.main_menu_chip)
                        .setView(v)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            ip = etIP.getText().toString();
                            port = Integer.parseInt(etPort.getText().toString());
                            getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).edit()
                                    .putString("svip", ip).apply();
                            getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).edit()
                                    .putInt("svport", port).apply();
                            Client.setup(ip, port);
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
        getList();
        super.onResume();
    }

    private void getList() {
        idle = false;
        runOnUiThread(() -> new ListGetter(new ListGetter.AsyncRunnable() {
            @Override
            public void onPreExecute() {
                main_pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPostExecute(ArtifactObject[] objects, Exception e) {
                if (e != null || objects == null) {
                    Toast.makeText(MainActivity.this, e != null ? e.getMessage() : "Objects is null", Toast.LENGTH_LONG).show();
                    listGettingFail = true;
                } else {
                    ObjectAdapter adapter = (ObjectAdapter) recyclerView.getAdapter();
                    assert adapter != null;
                    adapter.updateDataset(objects);
                    svst.setImageResource(android.R.drawable.presence_online);
                    listGettingFail = false;
                }

                main_pb.setVisibility(View.GONE);
                idle = true;
            }
        }).execute());
    }
}
