package com.vibration.lite;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
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
    private ProgressBar mRamProgressBar;
    private MaterialButton mBtnAddApp;
    private MaterialButton mBtnLaunch;
    private View mCardPlayStore;
    private TextView mTvGameVersion;

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

        // Safe dynamic IDs: compiles smoothly whether XML is old or new
        int ramProgId = getResources().getIdentifier("ramProgressBar", "id", getPackageName());
        if (ramProgId != 0) {
            mRamProgressBar = findViewById(ramProgId);
        }

        int storeCardId = getResources().getIdentifier("cardPlayStore", "id", getPackageName());
        if (storeCardId != 0) {
            mCardPlayStore = findViewById(storeCardId);
        }

        int gameVerId = getResources().getIdentifier("tvGameVersion", "id", getPackageName());
        if (gameVerId != 0) {
            mTvGameVersion = findViewById(gameVerId);
        }
    }

    private void setupListeners() {
        if (mBtnAddApp != null) {
            mBtnAddApp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleAddApp();
                }
            });
        }

        if (mBtnLaunch != null) {
            mBtnLaunch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleLaunchVibration();
                }
            });
        }

        View.OnClickListener storeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlayStoreHub();
            }
        };

        if (mCardPlayStore != null) {
            mCardPlayStore.setOnClickListener(storeClickListener);
        }

        int checkUpdateId = getResources().getIdentifier("tvCheckUpdate", "id", getPackageName());
        if (checkUpdateId != 0) {
            View tvCheckUpdate = findViewById(checkUpdateId);
            if (tvCheckUpdate != null) {
                tvCheckUpdate.setOnClickListener(storeClickListener);
            }
        }
    }

    private void openPlayStoreHub() {
        try {
            Class<?> activityClass = Class.forName("com.vibration.lite.core.PlayStoreActivity");
            Intent storeIntent = new Intent(MainActivity.this, activityClass);
            startActivity(storeIntent);
        } catch (Throwable e) {
            Toast.makeText(this, "Opening Google Play Store for eFootball...", Toast.LENGTH_SHORT).show();
            GameUpdateManager.openGameInPlayStore(this, TARGET_GAME_PACKAGE);
        }
    }

    private void handleAddApp() {
        boolean isInstalled = GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE);
        if (!isInstalled) {
            Toast.makeText(this, "eFootball is not installed yet. Opening Google Play Store...", Toast.LENGTH_LONG).show();
            openPlayStoreHub();
            return;
        }

        if (mTvEngineStatus != null) {
            mTvEngineStatus.setText("Cloning eFootball into sandbox...");
        }

        boolean success = VirtualCoreEngine.get().clonePackage(TARGET_GAME_PACKAGE);

        if (success) {
            Toast.makeText(this, "eFootball cloned successfully into Vibration space!", Toast.LENGTH_SHORT).show();
            updateClonedState();
        } else {
            if (mTvEngineStatus != null) {
                mTvEngineStatus.setText("Clone failed. Check permissions.");
            }
            Toast.makeText(this, "Could not clone eFootball. Grant storage permissions.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleLaunchVibration() {
        if (mTvEngineStatus != null) {
            mTvEngineStatus.setText("Purging RAM & Boosting GPU (60 FPS)...");
        }

        long freeRam = PerformanceOptimizer.prepareGameEnvironment(this);
        boolean launched = VirtualCoreEngine.get().launchClonedApp(this, TARGET_GAME_PACKAGE);

        if (launched) {
            if (mTvEngineStatus != null) {
                mTvEngineStatus.setText("eFootball Active (60 FPS Fixed)");
            }
            Toast.makeText(this, "Vibration Boost Active: 60 FPS Target", Toast.LENGTH_SHORT).show();
        } else {
            if (!GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE)) {
                Toast.makeText(this, "Please install eFootball from Play Store first.", Toast.LENGTH_LONG).show();
                openPlayStoreHub();
            } else {
                Toast.makeText(this, "Please tap 'ADD eFOOTBALL' first.", Toast.LENGTH_SHORT).show();
            }
            updateClonedState();
        }
    }

    private void updateClonedState() {
        boolean isCloned = VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE);
        boolean isInstalledOnHost = GameUpdateManager.isPackageInstalled(this, TARGET_GAME_PACKAGE);

        if (mTvGameVersion != null) {
            if (isInstalledOnHost) {
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(TARGET_GAME_PACKAGE, 0);
                    mTvGameVersion.setText("Installed: v" + pInfo.versionName + " • Tap to Check Updates");
                } catch (Exception e) {
                    mTvGameVersion.setText("Installed on Device • Tap to Check Updates");
                }
            } else {
                mTvGameVersion.setText("Not installed on device. Tap to install from Play Store");
            }
        }

        if (mTvEngineStatus != null) {
            if (isCloned) {
                mTvEngineStatus.setText("eFootball Cloned & Ready (Isolated)");
            } else if (isInstalledOnHost) {
                mTvEngineStatus.setText("eFootball Detected. Ready to Clone.");
            } else {
                mTvEngineStatus.setText("Vibration Engine Ready");
            }
        }

        if (mBtnAddApp != null) {
            if (isCloned) {
                mBtnAddApp.setText("RE-CLONE eFOOTBALL");
            } else if (isInstalledOnHost) {
                mBtnAddApp.setText("ADD eFOOTBALL");
            } else {
                mBtnAddApp.setText("INSTALL eFOOTBALL");
            }
        }

        if (mBtnLaunch != null) {
            mBtnLaunch.setEnabled(true);
        }
    }

    private void updateTelemetry() {
        long usedMb = PerformanceOptimizer.getAppUsedMemoryMB();
        if (mTvRamBadge != null) {
            mTvRamBadge.setText(usedMb + " MB / 150 MB");
        }
        if (mRamProgressBar != null) {
            int progress = (int) Math.min(100, (usedMb * 100) / 150);
            mRamProgressBar.setProgress(progress);
        }
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
