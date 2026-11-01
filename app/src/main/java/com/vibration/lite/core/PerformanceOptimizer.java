package com.vibration.lite.core;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class PerformanceOptimizer {

    public static void applyHardwareAcceleration(Activity activity) {
        try {
            Window window = activity.getWindow();
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        } catch (Exception ignored) {}
    }

    public static long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        return usedBytes / (1024 * 1024);
    }

    public static long getAvailableDeviceMemoryMB(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                return mi.availMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 1024;
    }

    public static long getTotalDeviceMemoryMB(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                return mi.totalMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 4096;
    }

    public static int getDisplayRefreshRate(Context context) {
        try {
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
        } catch (Exception ignored) {}
        return 60;
    }

    public static void trimMemory(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                    if (proc.pkgList != null && !proc.processName.equals(context.getPackageName())) {
                        for (String pkg : proc.pkgList) {
                            am.killBackgroundProcesses(pkg);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        System.gc();
    }
}
