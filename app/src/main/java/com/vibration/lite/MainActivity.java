package com.vibration.lite;

import android.content.Context;
import android.content.DialogInterface;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vibration.lite.core.PerformanceOptimizer;
import com.vibration.lite.core.VirtualCoreEngine;

public class MainActivity extends AppCompatActivity {
    public static final String TARGET_GAME_PACKAGE = "jp.konami.pesam";
    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private TextView tvRamUsage;
    private TextView tvFpsCounter;
    private TextView tvGpuBoost;
    private TextView tvEngineStatus;
    private ProgressBar ramProgressBar;

    private Button btnLaunchVibration;
    private Button btnAddEfootball;
    private Button btnPlayStoreHub;
    private Button btnInstallPlayStoreInside;

    private final Handler mTelemetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTelemetryRunnable = new Runnable() {
        @Override
        public void run() {
            updateTelemetry();
            mTelemetryHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PerformanceOptimizer.applyHardwareAcceleration(this);

        initViews();
        setupListeners();
        updateInsideStoreButtonState();
        updateTelemetry();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTelemetryHandler.post(mTelemetryRunnable);
        updateInsideStoreButtonState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTelemetryHandler.removeCallbacks(mTelemetryRunnable);
    }

    private void initViews() {
        tvRamUsage = findViewById(R.id.tvRamUsage);
        tvFpsCounter = findViewById(R.id.tvFpsCounter);
        tvGpuBoost = findViewById(R.id.tvGpuBoost);
        tvEngineStatus = findViewById(R.id.tvEngineStatus);
        ramProgressBar = findViewById(R.id.ramProgressBar);

        btnLaunchVibration = findViewById(R.id.btnLaunchVibration);
        btnAddEfootball = findViewById(R.id.btnAddEfootball);
        btnPlayStoreHub = findViewById(R.id.btnPlayStoreHub);
        btnInstallPlayStoreInside = findViewById(R.id.btnInstallPlayStoreInside);
    }

    private void setupListeners() {
        // A) LAUNCH VIBRATION: 800ms real vibration + Toast + Launch
        btnLaunchVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(800);
                Toast.makeText(MainActivity.this, "Vibration Turbo Active", Toast.LENGTH_SHORT).show();

                PerformanceOptimizer.trimMemory(MainActivity.this);

                if (VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE)) {
                    VirtualCoreEngine.get().launchApp(MainActivity.this, TARGET_GAME_PACKAGE);
                } else if (isAppInstalledOnDevice(TARGET_GAME_PACKAGE)) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(TARGET_GAME_PACKAGE);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "eFootball not found. Opening Store...", Toast.LENGTH_SHORT).show();
                    openPlayStoreHub();
                }
            }
        });

        // B) ADD EFOOTBALL: Clone eFootball jp.konami.pesam via VirtualCore. If not installed, openPlayStoreHub().
        btnAddEfootball.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(50);
                if (!isAppInstalledOnDevice(TARGET_GAME_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "eFootball is not installed on device. Opening Play Store...", Toast.LENGTH_SHORT).show();
                    openPlayStoreHub();
                } else {
                    boolean success = VirtualCoreEngine.get().clonePackage(TARGET_GAME_PACKAGE, MainActivity.this);
                    if (success) {
                        Toast.makeText(MainActivity.this, "eFootball Cloned in Sandbox!", Toast.LENGTH_SHORT).show();
                        btnAddEfootball.setText("RE-CLONE eFOOTBALL");
                        if (tvEngineStatus != null) {
                            tvEngineStatus.setText("eFootball Cloned in Sandbox (60 FPS)");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Cloning failed. Check storage permissions.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // C) GOOGLE PLAY STORE HUB: Open REAL Play Store outside
        btnPlayStoreHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                openPlayStoreHub();
            }
        });

        // D) NEW - INSTALL PLAY STORE INSIDE (Like GBox)
        btnInstallPlayStoreInside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                if (VirtualCoreEngine.get().isAppInstalled(PLAY_STORE_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "Opening Play Store inside Virtual Space...", Toast.LENGTH_SHORT).show();
                    VirtualCoreEngine.get().launchApp(MainActivity.this, PLAY_STORE_PACKAGE);
                } else {
                    installPlayStoreInside();
                }
            }
        });
    }

    private void triggerRealVibration(long milliseconds) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(milliseconds);
                }
            }
        } catch (Exception ignored) {}
    }

    public void openPlayStoreHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
            intent.setPackage(PLAY_STORE_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            } catch (Exception e2) {
                // Huawei fallback dialog
                showHuaweiPlayStoreDialog();
            }
        }
    }

    private void showHuaweiPlayStoreDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Google Play Store Not Found")
            .setMessage("No Google Play Store was found on this device (common on Huawei/Honor devices).\n\nWould you like to install Play Store INSIDE Vibration (GBox virtual space)?")
            .setPositiveButton("Install Inside", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    installPlayStoreInside();
                }
            })
            .setNeutralButton("Open in Browser", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_GAME_PACKAGE));
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(webIntent);
                    } catch (Exception ignored) {}
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public void installPlayStoreInside() {
        btnInstallPlayStoreInside.setEnabled(false);
        btnInstallPlayStoreInside.setText("INSTALLING GMS & PLAY STORE...");

        Toast.makeText(this, "Installing Google Play Services...", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Step 1: Install microG GmsCore inside virtual space
                if (!VirtualCoreEngine.get().isAppInstalled(GMS_PACKAGE)) {
                    VirtualCoreEngine.get().installPackage(GMS_PACKAGE, MainActivity.this);
                    VirtualCoreEngine.get().installPackage("com.google.android.gsf", MainActivity.this);
                }

                Toast.makeText(MainActivity.this, "Installing Play Store...", Toast.LENGTH_SHORT).show();

                // Step 2: Install Play Store inside virtual space
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        VirtualCoreEngine.get().installPackage(PLAY_STORE_PACKAGE, MainActivity.this);
                        Toast.makeText(MainActivity.this, "Play Store Installed Inside! (Huawei Ready)", Toast.LENGTH_SHORT).show();

                        updateInsideStoreButtonState();

                        if (tvEngineStatus != null) {
                            tvEngineStatus.setText("Play Store & GMS Active in Sandbox");
                        }
                    }
                }, 1200);
            }
        }, 1200);
    }

    private void updateInsideStoreButtonState() {
        if (btnInstallPlayStoreInside == null) return;
        btnInstallPlayStoreInside.setEnabled(true);
        if (VirtualCoreEngine.get().isAppInstalled(PLAY_STORE_PACKAGE)) {
            btnInstallPlayStoreInside.setText("OPEN PLAY STORE INSIDE");
        } else {
            btnInstallPlayStoreInside.setText("INSTALL PLAY STORE INSIDE (GBOX)");
        }
    }

    private boolean isAppInstalledOnDevice(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateTelemetry() {
        long usedMb = PerformanceOptimizer.getUsedMemoryMB();
        long totalMb = PerformanceOptimizer.getTotalDeviceMemoryMB(this);
        int refreshRate = PerformanceOptimizer.getDisplayRefreshRate(this);

        if (tvRamUsage != null) {
            tvRamUsage.setText(usedMb + " MB / " + totalMb + " MB");
        }
        if (ramProgressBar != null && totalMb > 0) {
            int progress = (int) ((usedMb * 100) / Math.max(1, totalMb));
            ramProgressBar.setProgress(Math.max(5, progress));
        }
        if (tvFpsCounter != null) {
            tvFpsCounter.setText(refreshRate + " Hz / 60 FPS TARGET");
        }
    }
}
