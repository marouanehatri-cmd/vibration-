package com.vibration.lite.core;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class StubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PerformanceOptimizer.applyHardwareAcceleration(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
