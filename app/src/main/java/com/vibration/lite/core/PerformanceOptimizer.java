package com.vibration.lite.core;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

public class PerformanceOptimizer {

    public static void applyHardwareAcceleration(Activity activity) {
        // Hardware acceleration is safely handled at manifest level: android:hardwareAccelerated="true"
        // Avoid illegal runtime window flag modifications that crash OEM skins (EMUI/MIUI).
    }

    public static long getUsedMemoryMB() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedBytes = runtime.totalMemory() - runtime.freeMemory();
            return Math.max(1, usedBytes / (1024 * 1024));
        } catch (Throwable t) {
            return 32;
        }
    }

    public static long getAvailableDeviceMemoryMB(Context context) {
        try {
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mi);
                    return Math.max(1, mi.availMem / (1024 * 1024));
                }
            }
        } catch (Throwable ignored) {}
        return 1024;
    }

    public static long getTotalDeviceMemoryMB(Context context) {
        try {
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mi);
                    return Math.max(1, mi.totalMem / (1024 * 1024));
                }
            }
        } catch (Throwable ignored) {}
        return 4096;
    }

    public static int getDisplayRefreshRate(Context context) {
        try {
            if (context != null) {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    Display display = wm.getDefaultDisplay();
                    if (display != null) {
                        float refreshRate = display.getRefreshRate();
                        if (refreshRate > 0) {
                            return Math.round(refreshRate);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 60;
    }

    public static void trimMemory(Context context) {
        try {
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                        if (proc != null && proc.pkgList != null && !proc.processName.equals(context.getPackageName())) {
                            for (String pkg : proc.pkgList) {
                                if (pkg != null) {
                                    am.killBackgroundProcesses(pkg);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        System.gc();
    }
}
