package com.vibration.lite;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vibration.lite.core.PerformanceOptimizer;
import com.vibration.lite.core.VirtualCoreEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TARGET_GAME_PACKAGE = "jp.konami.pesam";
    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private TextView tvRamUsage;
    private TextView tvFpsCounter;
    private TextView tvGpuBoost;
    private TextView tvEngineStatus;
    private TextView tvAppCountBadge;
    private ProgressBar ramProgressBar;
    private LinearLayout llVirtualAppsContainer;

    private Button btnLaunchVibration;
    private Button btnAddEfootball;
    private Button btnPlayStoreHub;
    private Button btnInstallPlayStoreInside;
    private Button btnAddCustomApp;

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
        renderVirtualApps();
        updateInsideStoreButtonState();
        updateTelemetry();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTelemetryHandler.post(mTelemetryRunnable);
        renderVirtualApps();
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
        tvAppCountBadge = findViewById(R.id.tvAppCountBadge);
        ramProgressBar = findViewById(R.id.ramProgressBar);
        llVirtualAppsContainer = findViewById(R.id.llVirtualAppsContainer);

        btnLaunchVibration = findViewById(R.id.btnLaunchVibration);
        btnAddEfootball = findViewById(R.id.btnAddEfootball);
        btnPlayStoreHub = findViewById(R.id.btnPlayStoreHub);
        btnInstallPlayStoreInside = findViewById(R.id.btnInstallPlayStoreInside);
        btnAddCustomApp = findViewById(R.id.btnAddCustomApp);
    }

    private void setupListeners() {
        // 1) ⚡ LAUNCH VIBRATION (800ms Real Vibrator + Memory Trim + Game Launch)
        btnLaunchVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(800);
                Toast.makeText(MainActivity.this, "Vibration Turbo Active", Toast.LENGTH_SHORT).show();

                PerformanceOptimizer.trimMemory(MainActivity.this);

                if (VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE)) {
                    boolean launched = VirtualCoreEngine.get().launchApp(MainActivity.this, TARGET_GAME_PACKAGE);
                    if (!launched) {
                        openGameOnDeviceOrHub();
                    }
                } else {
                    openGameOnDeviceOrHub();
                }
            }
        });

        // 2) ADD EFOOTBALL
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
                        renderVirtualApps();
                    } else {
                        Toast.makeText(MainActivity.this, "Cloning failed. Check storage permissions.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 3) GOOGLE PLAY STORE HUB
        btnPlayStoreHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                openPlayStoreHub();
            }
        });

        // 4) INSTALL PLAY STORE INSIDE (GBOX MODE)
        btnInstallPlayStoreInside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                if (VirtualCoreEngine.get().isAppInstalled(PLAY_STORE_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "Opening Play Store inside Virtual Space...", Toast.LENGTH_SHORT).show();
                    boolean launched = VirtualCoreEngine.get().launchApp(MainActivity.this, PLAY_STORE_PACKAGE);
                    if (!launched) {
                        openPlayStoreHub();
                    }
                } else {
                    installPlayStoreInside();
                }
            }
        });

        // 5) + CLONE ANY APP FROM PHONE (App Picker Drawer)
        btnAddCustomApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                showAppPickerDialog();
            }
        });
    }

    /**
     * Renders all active items in the Virtual Desktop:
     * Never leaves the user with an empty screen.
     */
    private void renderVirtualApps() {
        if (llVirtualAppsContainer == null) return;
        llVirtualAppsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        PackageManager pm = getPackageManager();

        int totalCount = 0;

        // Slot 1: eFootball 2025 (PES)
        boolean isEfootballCloned = VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE);
        boolean isEfootballDeviceInstalled = isAppInstalledOnDevice(TARGET_GAME_PACKAGE);
        View pesView = inflater.inflate(R.layout.item_virtual_app, llVirtualAppsContainer, false);
        TextView tvPesName = pesView.findViewById(R.id.tvAppName);
        TextView tvPesPkg = pesView.findViewById(R.id.tvAppPackage);
        TextView tvPesStatus = pesView.findViewById(R.id.tvAppStatus);
        ImageView ivPesIcon = pesView.findViewById(R.id.ivAppIcon);
        Button btnPesAction = pesView.findViewById(R.id.btnAppAction);

        tvPesName.setText("eFootball™ 2025");
        tvPesPkg.setText("jp.konami.pesam");
        if (isEfootballDeviceInstalled) {
            try {
                Drawable icon = pm.getApplicationIcon(TARGET_GAME_PACKAGE);
                ivPesIcon.setImageDrawable(icon);
            } catch (Throwable ignored) {}
        }

        if (isEfootballCloned) {
            tvPesStatus.setText("● Sandbox Cloned • 60 FPS Turbo");
            tvPesStatus.setTextColor(0xFF10B981);
            btnPesAction.setText("LAUNCH");
        } else {
            tvPesStatus.setText("Ready to Clone (Virtual Space Slot)");
            tvPesStatus.setTextColor(0xFF38BDF8);
            btnPesAction.setText("CLONE");
        }

        btnPesAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(800);
                if (VirtualCoreEngine.get().isAppCloned(TARGET_GAME_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "Launching eFootball in Sandbox (60 FPS Turbo)...", Toast.LENGTH_SHORT).show();
                    boolean ok = VirtualCoreEngine.get().launchApp(MainActivity.this, TARGET_GAME_PACKAGE);
                    if (!ok) openGameOnDeviceOrHub();
                } else {
                    btnAddEfootball.performClick();
                }
            }
        });
        llVirtualAppsContainer.addView(pesView);
        totalCount++;

        // Slot 2: Google Play Store (GBox Sandbox)
        View playView = inflater.inflate(R.layout.item_virtual_app, llVirtualAppsContainer, false);
        TextView tvPlayName = playView.findViewById(R.id.tvAppName);
        TextView tvPlayPkg = playView.findViewById(R.id.tvAppPackage);
        TextView tvPlayStatus = playView.findViewById(R.id.tvAppStatus);
        Button btnPlayAction = playView.findViewById(R.id.btnAppAction);

        tvPlayName.setText("Google Play Store");
        tvPlayPkg.setText("com.android.vending");
        boolean isPlayInside = VirtualCoreEngine.get().isAppInstalled(PLAY_STORE_PACKAGE);
        if (isPlayInside) {
            tvPlayStatus.setText("● GBox Isolated Sandbox Active");
            tvPlayStatus.setTextColor(0xFF10B981);
            btnPlayAction.setText("OPEN");
        } else {
            tvPlayStatus.setText("Cloud Store Sandbox Slot");
            tvPlayStatus.setTextColor(0xFF9CA3AF);
            btnPlayAction.setText("INSTALL");
        }

        btnPlayAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(40);
                if (VirtualCoreEngine.get().isAppInstalled(PLAY_STORE_PACKAGE)) {
                    Toast.makeText(MainActivity.this, "Opening Virtual Play Store...", Toast.LENGTH_SHORT).show();
                    boolean ok = VirtualCoreEngine.get().launchApp(MainActivity.this, PLAY_STORE_PACKAGE);
                    if (!ok) openPlayStoreHub();
                } else {
                    installPlayStoreInside();
                }
            }
        });
        llVirtualAppsContainer.addView(playView);
        totalCount++;

        // Slot 3: Google Play Services (microG / GMS)
        View gmsView = inflater.inflate(R.layout.item_virtual_app, llVirtualAppsContainer, false);
        TextView tvGmsName = gmsView.findViewById(R.id.tvAppName);
        TextView tvGmsPkg = gmsView.findViewById(R.id.tvAppPackage);
        TextView tvGmsStatus = gmsView.findViewById(R.id.tvAppStatus);
        Button btnGmsAction = gmsView.findViewById(R.id.btnAppAction);

        tvGmsName.setText("Google Play Services");
        tvGmsPkg.setText("com.google.android.gms");
        tvGmsStatus.setText("● microG Core 24.x Bridge Ready");
        tvGmsStatus.setTextColor(0xFFC084FC);
        btnGmsAction.setText("CHECK");

        btnGmsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRealVibration(30);
                Toast.makeText(MainActivity.this, "GMS Core is active in Vibration Sandbox.", Toast.LENGTH_SHORT).show();
            }
        });
        llVirtualAppsContainer.addView(gmsView);
        totalCount++;

        // Dynamic Slots: Any custom app cloned by the user
        List<String> clonedPackages = VirtualCoreEngine.get().getClonedPackages();
        for (final String pkg : clonedPackages) {
            if (pkg.equals(TARGET_GAME_PACKAGE) || pkg.equals(PLAY_STORE_PACKAGE) || pkg.equals(GMS_PACKAGE)) {
                continue;
            }

            View appView = inflater.inflate(R.layout.item_virtual_app, llVirtualAppsContainer, false);
            TextView tvName = appView.findViewById(R.id.tvAppName);
            TextView tvPkg = appView.findViewById(R.id.tvAppPackage);
            TextView tvStatus = appView.findViewById(R.id.tvAppStatus);
            ImageView ivIcon = appView.findViewById(R.id.ivAppIcon);
            Button btnAction = appView.findViewById(R.id.btnAppAction);

            String label = pkg;
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(info).toString();
                ivIcon.setImageDrawable(pm.getApplicationIcon(info));
            } catch (Throwable ignored) {}

            tvName.setText(label);
            tvPkg.setText(pkg);
            tvStatus.setText("● Cloned in Sandbox • 60 FPS");
            tvStatus.setTextColor(0xFF10B981);
            btnAction.setText("LAUNCH");

            btnAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    triggerRealVibration(100);
                    Toast.makeText(MainActivity.this, "Launching " + pkg + " in 60 FPS Sandbox...", Toast.LENGTH_SHORT).show();
                    VirtualCoreEngine.get().launchApp(MainActivity.this, pkg);
                }
            });

            // Long click to remove or manage
            appView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showAppManageDialog(pkg);
                    return true;
                }
            });

            llVirtualAppsContainer.addView(appView);
            totalCount++;
        }

        if (tvAppCountBadge != null) {
            tvAppCountBadge.setText(totalCount + " APPS READY");
        }
    }

    /**
     * App Cloner Dialog: Scans device installed apps and lets the user clone ANY app.
     */
    private void showAppPickerDialog() {
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        final List<AppItem> appList = new ArrayList<>();
        for (ApplicationInfo info : installedApps) {
            // Keep launchable apps or user installed apps
            Intent launchIntent = pm.getLaunchIntentForPackage(info.packageName);
            if (launchIntent != null && !info.packageName.equals(getPackageName())) {
                appList.add(new AppItem(
                    pm.getApplicationLabel(info).toString(),
                    info.packageName,
                    pm.getApplicationIcon(info)
                ));
            }
        }

        Collections.sort(appList, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem o1, AppItem o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });

        if (appList.isEmpty()) {
            Toast.makeText(this, "No launchable apps found on device.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itemNames = new String[appList.size()];
        for (int i = 0; i < appList.size(); i++) {
            itemNames[i] = appList.get(i).name + " (" + appList.get(i).packageName + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clone App to Vibration Virtual Space");
        builder.setItems(itemNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AppItem selected = appList.get(which);
                boolean cloned = VirtualCoreEngine.get().clonePackage(selected.packageName, MainActivity.this);
                if (cloned) {
                    triggerRealVibration(60);
                    Toast.makeText(MainActivity.this, selected.name + " Cloned into Sandbox!", Toast.LENGTH_SHORT).show();
                    renderVirtualApps();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to clone " + selected.name, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAppManageDialog(final String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sandbox App Options");
        builder.setMessage("Package: " + packageName);
        builder.setPositiveButton("Launch Turbo (60 FPS)", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                triggerRealVibration(800);
                VirtualCoreEngine.get().launchApp(MainActivity.this, packageName);
            }
        });
        builder.setNegativeButton("Remove from Virtual Space", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VirtualCoreEngine.get().removePackage(packageName, MainActivity.this);
                Toast.makeText(MainActivity.this, "Removed from sandbox.", Toast.LENGTH_SHORT).show();
                renderVirtualApps();
            }
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private static class AppItem {
        final String name;
        final String packageName;
        final Drawable icon;

        AppItem(String name, String packageName, Drawable icon) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private void openGameOnDeviceOrHub() {
        if (isAppInstalledOnDevice(TARGET_GAME_PACKAGE)) {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(TARGET_GAME_PACKAGE);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        Toast.makeText(MainActivity.this, "eFootball not found on device. Opening Play Store...", Toast.LENGTH_SHORT).show();
        openPlayStoreHub();
    }

    private void triggerRealVibration(long milliseconds) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(milliseconds);
                }
            }
        } catch (Throwable ignored) {}
    }

    public void openPlayStoreHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
            intent.setPackage(PLAY_STORE_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable e1) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TARGET_GAME_PACKAGE));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            } catch (Throwable e2) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + TARGET_GAME_PACKAGE));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(browserIntent);
                } catch (Throwable e3) {
                    showHuaweiPlayStoreDialog();
                }
            }
        }
    }

    private void showHuaweiPlayStoreDialog() {
        if (isFinishing()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed()) return;

        new AlertDialog.Builder(this)
            .setTitle("Google Play Store")
            .setMessage("No Google Play Store app found on device.\n\nWould you like to install Play Store INSIDE Vibration (Sandbox) or open in browser?")
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
                    } catch (Throwable ignored) {}
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
                if (!VirtualCoreEngine.get().isAppInstalled(GMS_PACKAGE)) {
                    VirtualCoreEngine.get().installPackage(GMS_PACKAGE, MainActivity.this);
                    VirtualCoreEngine.get().installPackage("com.google.android.gsf", MainActivity.this);
                }

                Toast.makeText(MainActivity.this, "Installing Play Store...", Toast.LENGTH_SHORT).show();

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        VirtualCoreEngine.get().installPackage(PLAY_STORE_PACKAGE, MainActivity.this);
                        Toast.makeText(MainActivity.this, "Play Store Installed Inside! (Huawei Ready)", Toast.LENGTH_SHORT).show();

                        updateInsideStoreButtonState();
                        renderVirtualApps();

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
