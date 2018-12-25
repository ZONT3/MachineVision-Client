package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private String SHARED_PREFS_IP;

    private ProgressBar main_pb;
    private RecyclerView recyclerView;
    private String ip;
    private int port;
    private ImageView svst;
    private boolean idle;

    private Thread mainChecker;
    private Thread lanChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ip = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getString("svip", "dltngz.ddns.net");
        port = getSharedPreferences("ru.zont.mvc.sys", MODE_PRIVATE).getInt("svport", 1337);
        SHARED_PREFS_IP = ip;
        main_pb = findViewById(R.id.main_pb);
        svst = findViewById(R.id.main_svst);

        recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ObjectAdapter(new ArtifactObject[]{}, new OnItemClick()));

        Client.setup(ip, port);
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
                            SHARED_PREFS_IP = ip;
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
    protected void onStop() {
        super.onStop();
        if (mainChecker != null && mainChecker.isAlive()) mainChecker.interrupt();
        if (lanChecker != null && lanChecker.isAlive()) lanChecker.interrupt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        svst.setImageResource(android.R.drawable.presence_invisible);
        main_pb.setVisibility(View.VISIBLE);
        idle = true;

        startCheckers();
    }

    @SuppressWarnings("deprecation")
    private void startCheckers() {
        Runnable lanCheckerRunnable = () -> {
            Thread.currentThread().setPriority(2);
            Log.d("LanChecker", "Started");
            while (Thread.currentThread().isAlive() && !Thread.interrupted()) {
                try {
                    Context context = MainActivity.this;

                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);


                    Log.d("LanScanner", "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d("LanScanner", "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d("LanScanner", "prefix: " + prefix);

                    for (int i = 0; i < 255; i++) {
                        String testIp = prefix + String.valueOf(i);

                        InetAddress address = InetAddress.getByName(testIp);
                        String hostName = address.getCanonicalHostName();

                        if (!hostName.contains(testIp))
                            if (Client.tryConnection(testIp, port) == null) {
                                ip = testIp;
                                Client.setup(testIp, port);
                            }
                    }
                } catch (Throwable t) {
                    Log.e("LanScanner", "Well that's not good.", t);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d("LanChecker", "Interrupted.");
                    return;
                }
            }
        };

        mainChecker = new Thread(() -> {
            Thread.currentThread().setPriority(3);

            boolean first = true;
            boolean wasConnected = false;
            while (Thread.currentThread().isAlive() && !Thread.interrupted()) {
                if (idle) {
                    Throwable result = Client.tryConnection(ip, port);

                    if (result == null) {
                        if (!wasConnected) runOnUiThread(this::onConnectionResumed);
                        if (lanChecker != null && lanChecker.isAlive()) lanChecker.interrupt();
                    } else {
                        ip = SHARED_PREFS_IP;
                        if (wasConnected || first) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, result.getLocalizedMessage(), Toast.LENGTH_LONG)
                                        .show();
                                onConnectionLost();
                            });
                        }
                        if (lanChecker == null || !lanChecker.isAlive())
                            (lanChecker = new Thread(lanCheckerRunnable)).start();
                    }

                    wasConnected = result == null;
                    first = false;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.d("MainChecker", "Interrupted.");
                    return;
                }
            }
        });
        mainChecker.start();
    }

    private void onConnectionResumed() {
        svst.setImageResource(android.R.drawable.presence_online);
        getList();
    }

    private void onConnectionLost() {
        svst.setImageResource(android.R.drawable.presence_offline);
        main_pb.setVisibility(View.GONE);

        ObjectAdapter adapter = (ObjectAdapter) recyclerView.getAdapter();
        if (adapter != null) adapter.clear();
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
                } else {
                    ObjectAdapter adapter = (ObjectAdapter) recyclerView.getAdapter();
                    assert adapter != null;
                    adapter.updateDataset(objects);
                    svst.setImageResource(android.R.drawable.presence_online);
                }

                main_pb.setVisibility(View.GONE);
                idle = true;
            }
        }).execute());
    }
}
