package ru.zont.mvc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.Client;
import ru.zont.mvc.core.Request;

public class MainActivity extends AppCompatActivity {
	private static final int CONSTATUS_UNKNOWN = -1;
	private static final int CONSTATUS_FAIL = 0;
	private static final int CONSTATUS_SUCCESS = 1;

    private ObjectAdapter adapter;

    private ProgressBar pb;
    private ImageView svst;

    private Thread checkerThread;
    private Thread seekerThread;

    private String ip;
    private int port;
    private int connectionStatus;

    private FloatingActionButton add;
    private FloatingActionButton guess;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.main_toolbar));

        ((TextView)findViewById(R.id.main_ver)).setText("Client v." + BuildConfig.VERSION_NAME);

        guess = findViewById(R.id.main_guess);
        add = findViewById(R.id.main_add);

        guess.hide();
        add.hide();

        RecyclerView recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter = new ObjectAdapter());
        adapter.setOnItemClickListener(this::onItemClick);

        pb = findViewById(R.id.main_pb);
        svst = findViewById(R.id.main_svst);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        ip = sp.getString("ip", "dltngz.ddns.net");
        port = sp.getInt("port", 1337);
        Client.setup(ip, port);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionStatus = CONSTATUS_UNKNOWN;

        Thread oldThread = null;
        if (checkerThread != null 
        		&& checkerThread.isAlive() 
        		&& !checkerThread.isInterrupted())
    		return;
    	else if (checkerThread != null && checkerThread.isInterrupted())
    		oldThread = checkerThread;

        Thread finalOldThread = oldThread;
        checkerThread = new Thread(() -> {
        	while (!Thread.currentThread().isInterrupted()) {
        		try {
        			while (finalOldThread != null && finalOldThread.isAlive())
        				Thread.sleep(50);

        			if (Client.tryConnection(ip, port) == null) {
        				if (connectionStatus != CONSTATUS_SUCCESS)
        					runOnUiThread(this::onConnectionResumed);
        				connectionStatus = CONSTATUS_SUCCESS;
        			} else {
        				if (connectionStatus != CONSTATUS_FAIL)
        					runOnUiThread(this::onConnectionFailed);
        				connectionStatus = CONSTATUS_FAIL;
        			}

        			Thread.sleep(300);
        		} catch (InterruptedException e) {
        			Log.d("CheckerThread", "Interrupted while being sleeping");
        			break;
        		}
        	}
        });
        checkerThread.setPriority(3);
        checkerThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (checkerThread != null)
        	checkerThread.interrupt();
        if (seekerThread != null)
        	seekerThread.interrupt();
    }

    private void onConnectionResumed() {
        svst.setImageResource(android.R.drawable.presence_online);

        if (seekerThread != null) seekerThread.interrupt();
    	getList();
    	add.show();
    }

    private void onConnectionFailed() {
    	svst.setImageResource(android.R.drawable.presence_offline);
    	adapter.clear();
        guess.hide();
        add.hide();

    	Thread oldThread = null;
    	if (seekerThread != null && !seekerThread.isInterrupted()) {
    		seekerThread.interrupt();
    		oldThread = seekerThread;
    	}

        Thread finalOldThread = oldThread;
        seekerThread = new Thread(() -> {
            LOOPER:
    		while (!Thread.currentThread().isInterrupted()) {
	    		try {
	    		    while (finalOldThread != null && !finalOldThread.isInterrupted())
	    		        Thread.sleep(50);

                    ConnectivityManager cm = (ConnectivityManager) MainActivity.this
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) MainActivity.this
                            .getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    @SuppressWarnings("deprecation") String ipString = Formatter.formatIpAddress(ipAddress);


                    Log.d("LanScanner", "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d("LanScanner", "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d("LanScanner", "prefix: " + prefix);

                    for (int i = 0; i < 20; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
                    for (int i = 40; i < 60; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
                    for (int i = 100; i < 120; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
                    for (int i = 20; i < 40; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
                    for (int i = 60; i < 100; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
                    for (int i = 120; i < 255; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;
                        checkIP(prefix, i);
                    }
	    		} catch (InterruptedException e) {
	    			Log.d("SeekerThread", "Interrupted while being sleeping");
	    			break;
	    		}
    		}
    	});
    	seekerThread.setPriority(Thread.MIN_PRIORITY);
    	seekerThread.start();
    }

    private void checkIP(String prefix, int i) {
        try {
            String testIp = prefix + String.valueOf(i);

            InetAddress address = InetAddress.getByName(testIp);
//            String hostName = address.getCanonicalHostName();

//            if (!hostName.contains(testIp)) {
            if (Client.tryConnection(testIp, port) == null) {
                ip = testIp;
                Client.setup(ip, port);
            }
//            }
        } catch (UnknownHostException ignored) { }
    }

    private void getList() {
        runOnUiThread(() -> new ListGetter(new ListGetter.AsyncRunnable() {
            @Override
            public void onPreExecute() {
                adapter.clear();
                pb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPostExecute(ArtifactObject[] objects, Exception e) {
                pb.setVisibility(View.GONE);
                if (e != null) {
                    Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (objects == null) {
                    Toast.makeText(MainActivity.this, "Error on getting list", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                adapter.updateDataset(objects);
                if (objects.length > 0) guess.show();
                else guess.hide();
            }
        }).execute());

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
                @SuppressLint("InflateParams") View layout = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_ip, null);
                EditText ipField = layout.findViewById(R.id.main_ipdiag_ip);
                EditText portField = layout.findViewById(R.id.main_ipdiag_port);
                ipField.setText(ip);
                portField.setText(port+"");

                new AlertDialog.Builder(this)
                        .setTitle(R.string.main_serverip)
                        .setView(layout)
                        .setPositiveButton(android.R.string.ok, (i1,i2) -> {
                            ip = ipField.getText().toString();
                            port = Integer.valueOf(portField.getText().toString());
                            Client.setup(ip, port);
                            PreferenceManager.getDefaultSharedPreferences(this).edit()
                                    .putString("ip", ip)
                                    .putInt("port", port)
                                    .apply();
                        }).create().show();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    public void onClickGuess(View v) {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED || permission1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        } else guess();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            guess();
        }
    }

    private void guess() {
        EditText view = new EditText(this);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        new AlertDialog.Builder(this)
                .setTitle("Enter URL of image")
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setCancelable(false);
                    pd.setMessage(getString(R.string.guess_w4s));
                    pd.create();
                    pd.show();

                    new Thread(() -> {
                        HashMap<String, String> request = new HashMap<>();
                        request.put("request_code", "guess");
                        String url = view.getText().toString();
                        request.put("url", url);

                        try {
                            File dir = new File(Environment.getExternalStorageDirectory().toString(), "MVC");
                            File file = new File(dir, "Image.png");
                            if (!dir.exists() || !dir.isDirectory()) {
                                dir.delete();
                                dir.mkdir();
                            }
                            if (file.exists()) file.delete();
                            file.createNewFile();

                            String response = Client.sendJsonForResult(new Gson().toJson(request), 1200000);
                            pd.setMessage(getString(R.string.guess_processing));

                            BitmapHandler.ObjectDataList objectData = new Gson().fromJson(response, BitmapHandler.ObjectDataList.class);
                            Bitmap bitmap = BitmapHandler.getBitmap(this, url, objectData, null);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));

                            runOnUiThread(() -> {
                                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                                Toast.makeText(this, getString(R.string.guess_saved,
                                        file.getAbsolutePath()), Toast.LENGTH_LONG).show();
                            });

                            Uri uri = FileProvider.getUriForFile(this, getApplicationContext()
                                    .getPackageName() + ".mvcprovider", file);

                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(uri, "image/*")
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            pd.dismiss();
                        }
                    }).start();
                })
                .create().show();
    }

    public void onClickAdd(View v) {
        startActivity(new Intent(this, EditActivity.class));
    }

    @SuppressLint("SetTextI18n")
    public void onItemClick(ArtifactObject item) {
        @SuppressLint("InflateParams") View root = LayoutInflater.from(this)
                .inflate(R.layout.dialog_objectpropts, null);

        TextView title = root.findViewById(R.id.objpts_title);
        TextView status = root.findViewById(R.id.objpts_status);
        TextView queries = root.findViewById(R.id.objpts_queries);
        TextView total = root.findViewById(R.id.objpts_total);
        TextView created = root.findViewById(R.id.objpts_created);
        TextView trained = root.findViewById(R.id.objpts_learned);
        Button train = root.findViewById(R.id.objpts_train);
        Button edit = root.findViewById(R.id.objpts_edit);
        Button delete = root.findViewById(R.id.objpts_delete);
        Switch svvitch = root.findViewById(R.id.objpts_switch);

        Calendar createdCal = Calendar.getInstance();
        createdCal.setTimeInMillis(item.getCreated());

        title.setText(item.getTitle());
        status.setText(getStatusString(item, this));
        status.setTextColor(getStatusColor(item));
        queries.setText(item.getQueriesSize()+"");
        total.setText(item.getTotal()+"");
        created.setText(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM)
                .format(createdCal.getTime()));
        trained.setText("---");
        svvitch.setChecked(item.isEnabled());

        boolean busy = item.getStatus() == ArtifactObject.STATUS.DOWNLOADING
                || item.getStatus() == ArtifactObject.STATUS.TRAINING;
        boolean canTrain = !item.hasUnmarked();

        svvitch.setEnabled(!busy);
        train.setEnabled(!busy && canTrain);
        edit.setEnabled(!busy);
        delete.setEnabled(!busy);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root).create();

        train.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> {
                try {
                    Client.sendJsonForResult(Request.create("train_model")
                            .put("object_id", item.getId()).toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
        edit.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, EditActivity.class)
                    .putExtra("object", item));
        });
        delete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.areYouSure)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, (i1, i2) -> {
                        dialog.dismiss();
                        new Thread(() -> {
                            try {
                                Client.sendJsonForResult(Request.create("delete_object")
                                        .put("id", item.getId()).toString());
                                getList();
                            } catch (IOException e) {
                                e.printStackTrace();
                                runOnUiThread(() ->
                                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show());
                            }
                        }).start();
                    }).create().show();
        });
        svvitch.setOnCheckedChangeListener((buttonView, isChecked) -> new Thread(() -> {
            try {
                Client.sendJsonForResult(Request.create("new_object")
                        .put("artifact_object", item.getId()).toString());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show());
            }
        }).start());

        dialog.show();
    }

    @NonNull
    static String getActionString(ArtifactObject object, Context context) {
        switch (object.getLastActType()) {
            default:
            case ArtifactObject.ACTION.CREATED: return context.getString(R.string.artobj_created);
            case ArtifactObject.ACTION.EDITED: return context.getString(R.string.artobj_edited);
            case ArtifactObject.ACTION.TRAINED: return context.getString(R.string.artobj_learned);
            case ArtifactObject.ACTION.STARTED_TRAINING: return context.getString(R.string.artobj_started);
        }
    }

    @NonNull
    static String getStatusString(ArtifactObject object, Context context) {
        switch (object.getStatus()) {
            case ArtifactObject.STATUS.READY_TL: return context.getString(R.string.artobj_status_rtl);
            case ArtifactObject.STATUS.DOWNLOADING: return context.getString(R.string.artobj_status_dl);
            case ArtifactObject.STATUS.TRAINING: return context.getString(R.string.artobj_status_lrn);
            case ArtifactObject.STATUS.READY_FU: return context.getString(R.string.artobj_status_rfu);
            case ArtifactObject.STATUS.OUTDATED: return context.getString(R.string.artobj_status_odt);
            case ArtifactObject.STATUS.ERROR_LEARN: return context.getString(R.string.artobj_status_errl);
            case ArtifactObject.STATUS.ERROR: return context.getString(R.string.artobj_status_err);
            case ArtifactObject.STATUS.NOT_MARKED: return context.getString(R.string.artobj_status_notmarked);
            default: return context.getString(R.string.artobj_status_err);
        }
    }

    static int getStatusColor(ArtifactObject object) {
        switch (object.getStatus()) {
            case ArtifactObject.STATUS.READY_TL:
            case ArtifactObject.STATUS.READY_FU: return 0xFF00CC00;
            case ArtifactObject.STATUS.OUTDATED:
            case ArtifactObject.STATUS.NOT_MARKED: return 0xFFCCCC00;
            case ArtifactObject.STATUS.ERROR_LEARN:
            case ArtifactObject.STATUS.ERROR: return 0xFFCC0000;
            case ArtifactObject.STATUS.DOWNLOADING:
            case ArtifactObject.STATUS.TRAINING:
            default: return Color.BLACK;
        }
    }


}
