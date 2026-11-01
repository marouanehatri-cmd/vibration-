package com.vibration.lite.core;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;

public class PerformanceOptimizer {

    private static final String TAG = "PerformanceOptimizer";

    public static void applyHardwareAcceleration(Activity activity) {
        try {
            Window window = activity.getWindow();
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.w(TAG, "Hardware accel flag note: " + e.getMessage());
        }
    }

    public static long prepareGameEnvironment(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
                if (runningProcesses != null) {
                    String myPackage = context.getPackageName();
                    for (ActivityManager.RunningAppProcessInfo proc : runningProcesses) {
                        if (proc.pkgList != null && proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                            for (String pkg : proc.pkgList) {
                                if (!pkg.equals(myPackage) && !pkg.startsWith("com.android.") && !pkg.startsWith("android")) {
                                    am.killBackgroundProcesses(pkg);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning background tasks", e);
        }

        System.runFinalization();
        System.gc();

        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        } catch (Throwable ignored) {}

        return getAvailableMemoryMB(context);
    }

    public static long getAvailableMemoryMB(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return 0;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    public static long getAppUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}
