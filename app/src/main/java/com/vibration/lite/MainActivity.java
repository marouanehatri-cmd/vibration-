package com.vibration.lite;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.vibration.lite.core.GameUpdateManager;
import com.vibration.lite.core.PerformanceOptimizer;
import com.vibration.lite.core.VirtualCoreEngine;

public class MainActivity extends AppCompatActivity {

    public static final String TARGET_GAME_PACKAGE = "com.konami.efootballpes";

    private TextView mTvEngineStatus;
    private TextView mTvRamBadge;
    private TextView mTvGpuBadge;
    private MaterialButton mBtnAddApp;
    private MaterialButton mBtnLaunch;
    private TextView mTvCheckUpdate;

    private final Handler mTelemetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTelemetryRunnable = new Runnable() {
        @Override
        public void run() {
            updateTelemetry();
            mTelemetryHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PerformanceOptimizer.applyHardwareAcceleration(this);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        updateClonedState();
    }

    private void initViews() {
        mTvEngineStatus = findViewById(R.id.tvEngineStatus);
        mTvRamBadge = findViewById(R.id.tvRamBadge);
        mTvGpuBadge = findViewById(R.id.tvGpuBadge);
        mBtnAddApp = findViewById(R.id.btnAddApp);
        mBtnLaunch = findViewById(R.id.btnLaunch);
        mTvCheckUpdate = findViewById(R.id.tvCheckUpdate);
    }

    private void setupListeners() {
        mBtnAddApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAddApp();
            }
        });

        mBtnLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLaunchVibration();
            }
        });

        mTvCheckUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Checking Google Play Store for eFootball updates...", Toast.LENGTH_SHORT).show();
                GameUpdateManager.openGameInPlayStore(MainActivity.this, TARGET_GAME_PACKAGE);
            }
        });
    }

    private void handleAddApp() {
        boolean isInstalled = GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE);
        if (!isInstalled) {
            Toast.makeText(this, getString(R.string.msg_package_not_installed), Toast.LENGTH_LONG).show();
            GameUpdateManager.openGameInPlayStore(this, TARGET_GAME_PACKAGE);
            return;
        }

        mTvEngineStatus.setText("● Cloning eFootball into isolated sandbox...");
        boolean success = VirtualCoreEngine.get().clonePackage(TARGET_GAME_PACKAGE);

        if (success) {
            Toast.makeText(this, getString(R.string.msg_clone_success), Toast.LENGTH_SHORT).show();
            updateClonedState();
        } else {
            mTvEngineStatus.setText("● Failed to clone package.");
            Toast.makeText(this, "Could not clone eFootball. Ensure permissions are granted.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleLaunchVibration() {
        mTvEngineStatus.setText("● Boosting GPU & Optimizing RAM...");
        long freeRam = PerformanceOptimizer.prepareGameEnvironment(this);
        boolean launched = VirtualCoreEngine.get().launchClonedApp(this, TARGET_GAME_PACKAGE);

        if (launched) {
            mTvEngineStatus.setText("● eFootball Active (60 FPS Mode)");
            Toast.makeText(this, "Vibration Boost Active: 60 FPS Target", Toast.LENGTH_SHORT).show();
        } else {
            if (!GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE)) {
                Toast.makeText(this, "Please install eFootball from Google Play first.", Toast.LENGTH_LONG).show();
                GameUpdateManager.openGameInPlayStore(this, TARGET_GAME_PACKAGE);
            } else {
                Toast.makeText(this, "Please tap 'Add eFootball' first.", Toast.LENGTH_SHORT).show();
            }
            updateClonedState();
        }
    }

    private void updateClonedState() {
        boolean isCloned = VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE);
        if (isCloned) {
            mTvEngineStatus.setText("● eFootball is Cloned & Ready (Isolated)");
            mBtnAddApp.setText("Re-Clone eFootball");
            mBtnLaunch.setEnabled(true);
        } else {
            boolean isInstalledOnHost = GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE);
            if (isInstalledOnHost) {
                mTvEngineStatus.setText("● eFootball detected on device. Ready to clone.");
            } else {
                mTvEngineStatus.setText("● eFootball not detected. Tap Add to install.");
            }
        }
    }

    private void updateTelemetry() {
        long usedMb = PerformanceOptimizer.getAppUsedMemoryMB();
        mTvRamBadge.setText("RAM: " + usedMb + "MB (Target < 150MB)");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTelemetryHandler.post(mTelemetryRunnable);
        updateClonedState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTelemetryHandler.removeCallbacks(mTelemetryRunnable);
    }
}
