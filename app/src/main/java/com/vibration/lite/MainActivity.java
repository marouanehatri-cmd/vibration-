package com.vibration.lite;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.vibration.lite.core.PerformanceOptimizer;
import com.vibration.lite.core.VirtualCoreEngine;

public class MainActivity extends AppCompatActivity {

    public static final String TARGET_GAME_PACKAGE = "jp.konami.pesam";

    private TextView tvRamUsage;
    private TextView tvGpuBoost;
    private TextView tvFpsCounter;
    private TextView tvEngineStatus;
    private ProgressBar ramProgressBar;
    private Button btnAddEfootball;
    private Button btnLaunchVibration;
    private Button btnPlayStoreHub;

    private final Handler mTelemetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTelemetryRunnable = new Runnable() {
        @Override
        public void run() {
            updateRealTelemetry();
            mTelemetryHandler.postDelayed(this, 2500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PerformanceOptimizer.applyHardwareAcceleration(this);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        updateRealTelemetry();
        updateAppState();
    }

    private void initViews() {
        tvRamUsage = findViewById(R.id.tvRamUsage);
        tvGpuBoost = findViewById(R.id.tvGpuBoost);
        tvFpsCounter = findViewById(R.id.tvFpsCounter);
        tvEngineStatus = findViewById(R.id.tvEngineStatus);
        ramProgressBar = findViewById(R.id.ramProgressBar);
        btnAddEfootball = findViewById(R.id.btnAddEfootball);
        btnLaunchVibration = findViewById(R.id.btnLaunchVibration);
        btnPlayStoreHub = findViewById(R.id.btnPlayStoreHub);
    }

    private void setupListeners() {
        // Requirement 2: Real Vibrator + Toast
        btnLaunchVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(800);
                Toast.makeText(MainActivity.this, "Vibration Turbo Active", Toast.LENGTH_SHORT).show();

                PerformanceOptimizer.trimMemory(MainActivity.this);

                boolean launched = VirtualCoreEngine.get().launchClonedApp(MainActivity.this, TARGET_GAME_PACKAGE);
                if (!launched) {
                    if (isAppInstalled(TARGET_GAME_PACKAGE)) {
                        Intent launch = getPackageManager().getLaunchIntentForPackage(TARGET_GAME_PACKAGE);
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launch);
                        }
                    } else {
                        openPlayStoreHub();
                    }
                }
            }
        });

        // Requirement 2: ADD EFOOTBALL = clone if installed, if not call openPlayStoreHub()
        btnAddEfootball.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(200);

                if (!isAppInstalled(TARGET_GAME_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "eFootball not found. Opening Google Play Store...", Toast.LENGTH_LONG).show();
                    openPlayStoreHub();
                } else {
                    boolean cloned = VirtualCoreEngine.get().clonePackage(TARGET_GAME_PACKAGE);
                    if (cloned) {
                        Toast.makeText(MainActivity.this, "eFootball Cloned in Vibration Sandbox!", Toast.LENGTH_SHORT).show();
                        updateAppState();
                    }
                }
            }
        });

        // Requirement 2: GOOGLE PLAY STORE HUB = MUST open REAL Play Store
        btnPlayStoreHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(100);
                openPlayStoreHub();
            }
        });
    }

    private void triggerRealVibration(long durationMs) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMs);
                }
            }
        } catch (Exception ignored) {}
    }

    public void openPlayStoreHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
            intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ex) {
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_GAME_PACKAGE));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            }
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateRealTelemetry() {
        // Requirement 3: Real live values from PerformanceOptimizer
        long usedMb = PerformanceOptimizer.getUsedMemoryMB();
        long availMb = PerformanceOptimizer.getAvailableDeviceMemoryMB(this);
        int refreshRate = PerformanceOptimizer.getDisplayRefreshRate(this);

        if (tvRamUsage != null) {
            tvRamUsage.setText(usedMb + " MB App / " + availMb + " MB Free");
        }

        if (ramProgressBar != null) {
            int progress = (int) Math.min(100, Math.max(5, (usedMb * 100) / 150));
            ramProgressBar.setProgress(progress);
        }

        if (tvFpsCounter != null) {
            tvFpsCounter.setText(refreshRate + " FPS HARDWARE");
        }

        if (tvGpuBoost != null) {
            tvGpuBoost.setText("GPU BOOST: ACCELERATED (" + refreshRate + "Hz)");
        }
    }

    private void updateAppState() {
        boolean isCloned = VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE);
        boolean isInstalled = isAppInstalled(TARGET_GAME_PACKAGE);

        if (btnAddEfootball != null) {
            if (isCloned) {
                btnAddEfootball.setText("RE-CLONE eFOOTBALL");
            } else if (isInstalled) {
                btnAddEfootball.setText("ADD eFOOTBALL");
            } else {
                btnAddEfootball.setText("INSTALL eFOOTBALL ↗");
            }
        }

        if (tvEngineStatus != null) {
            if (isCloned) {
                tvEngineStatus.setText("● eFootball Cloned & Ready (Sandbox Active)");
            } else if (isInstalled) {
                tvEngineStatus.setText("● eFootball Detected on Device");
            } else {
                tvEngineStatus.setText("● Vibration Ready • eFootball Not Installed");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTelemetryHandler.post(mTelemetryRunnable);
        updateRealTelemetry();
        updateAppState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTelemetryHandler.removeCallbacks(mTelemetryRunnable);
    }
}
