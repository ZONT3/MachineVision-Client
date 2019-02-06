package ru.zont.mvc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.main_toolbar));

        RecyclerView recyclerView = findViewById(R.id.main_recycler);
        recyclerView.setAdapter(adapter = new ObjectAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pb = findViewById(R.id.main_pb);
        svst = findViewById(R.id.main_svst);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        ip = sp.getString("ip", "dltngz.ddns.net");
        port = sp.getInt("port", 1337);
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
        			Log.d("CheckerThread", "Checker interrupted on sleep");
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
    	getList();
    }

    private void onConnectionFailed() {
    	svst.setImageResource(android.R.drawable.presence_offline);
    	adapter.clear();

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

                    for (int i = 0; i < 255; i++) {
                        if (Thread.currentThread().isInterrupted())
                            break LOOPER;

                        String testIp = prefix + String.valueOf(i);

                        InetAddress address = InetAddress.getByName(testIp);
                        String hostName = address.getCanonicalHostName();

                        if (!hostName.contains(testIp))
                            if (Client.tryConnection(testIp, port) == null) {
                                ip = testIp;
                                Client.setup(testIp, port);
                            }
                    }
	    		} catch (InterruptedException e) {
	    			Log.d("SeekerThread", "Interrupted while sleep");
	    			break;
	    		} catch (UnknownHostException ignored) { }
    		}
    	});
    	seekerThread.setPriority(Thread.MIN_PRIORITY);
    	seekerThread.start();
    }

    private void getList() {
        new ListGetter(new ListGetter.AsyncRunnable() {
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
            }
        }).execute();
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

    public void onClickGuess(View v) {
        EditText view = new EditText(this);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        new AlertDialog.Builder(this)
                .setTitle("Enter URL of image")
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setCancelable(false);
                    pd.create();
                    pd.show();

                    new Thread(() -> {
                        HashMap<String, String> request = new HashMap<>();
                        request.put("request_code", "guess");
                        request.put("url", view.getText().toString());

                        try {
                            String response = Client.sendJsonForResult(new Gson().toJson(request), 1200);
                            URL url = new URL(response);
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(Uri.parse(url.toString()), "image/*"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            pd.dismiss();
                        }
                    }).start();
                })
                .create().show();
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
            default: return context.getString(R.string.artobj_status_err);
        }
    }
}
