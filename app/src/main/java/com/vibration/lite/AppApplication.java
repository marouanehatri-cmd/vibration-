package com.vibration.lite;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.vibration.lite.core.VirtualCoreEngine;

public class AppApplication extends Application {

    private static final String TAG = "VibrationApp";
    private static AppApplication sInstance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = this;

        try {
            VirtualCoreEngine.get().startup(base);
            Log.i(TAG, "Vibration Virtual Space Engine started successfully.");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to initialize VirtualCoreEngine", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Vibration application created. Memory target < 150MB.");
    }

    public static AppApplication getInstance() {
        return sInstance;
    }
}
