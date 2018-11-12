package ru.zont.mvc;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MVCApp extends Application {
    public void onCreate() {
        super.onCreate();

        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder" );
        File logDirectory = new File( appDirectory + "/log" );
        File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

        if ( !appDirectory.exists() ) appDirectory.mkdir();
        if ( !logDirectory.exists() ) logDirectory.mkdir();

        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("logcat -f " + logFile);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
