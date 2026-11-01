package com.vibration.lite;

import android.app.Application;
import android.content.Context;
import com.vibration.lite.core.VirtualCoreEngine;

public class AppApplication extends Application {
    private static AppApplication sInstance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = this;
        try {
            VirtualCoreEngine.get().startup(base);
        } catch (Throwable ignored) {}
    }

    public static AppApplication getInstance() {
        return sInstance;
    }
}
