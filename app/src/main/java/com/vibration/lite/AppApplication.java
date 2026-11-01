package com.vibration.lite;

import android.app.Application;
import com.vibration.lite.core.VirtualCoreEngine;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VirtualCoreEngine.get().startup(this);
    }
}
