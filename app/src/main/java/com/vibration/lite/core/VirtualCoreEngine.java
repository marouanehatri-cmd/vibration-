package com.vibration.lite.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VirtualCoreEngine {
    private static final VirtualCoreEngine sInstance = new VirtualCoreEngine();
    private static final String PREFS_NAME = "vibration_virtual_space_prefs";
    private static final String KEY_CLONED_PACKAGES = "cloned_packages";

    private Context mHostContext;
    private File mVirtualRoot;
    private final Set<String> mInstalledPackages = Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> mClonedPackages = Collections.synchronizedSet(new HashSet<String>());

    public static VirtualCoreEngine get() {
        return sInstance;
    }

    private VirtualCoreEngine() {}

    public synchronized void startup(Context context) {
        mHostContext = context.getApplicationContext();
        mVirtualRoot = new File(context.getFilesDir(), "virtual_space");
        mVirtualRoot.mkdirs();

        // Default sandbox packages
        mInstalledPackages.add("jp.konami.pesam");
        mInstalledPackages.add("com.android.vending");
        mInstalledPackages.add("com.google.android.gms");

        // Load persisted cloned packages
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> saved = prefs.getStringSet(KEY_CLONED_PACKAGES, null);
            if (saved != null) {
                mClonedPackages.addAll(saved);
                mInstalledPackages.addAll(saved);
            }
        } catch (Throwable ignored) {}

        File userZero = new File(mVirtualRoot, "data/user/0");
        if (userZero.exists() && userZero.isDirectory()) {
            File[] files = userZero.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        mInstalledPackages.add(f.getName());
                    }
                }
            }
        }

        // Hidden API exemptions for Android P+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Method forNameMethod = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethodMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
                Class<?> vmRuntimeClass = (Class<?>) forNameMethod.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntimeMethod = (Method) getDeclaredMethodMethod.invoke(vmRuntimeClass, "getRuntime", (Class<?>[]) null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethodMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                Object runtime = getRuntimeMethod.invoke(null);
                setHiddenApiExemptions.invoke(runtime, (Object) new String[]{"L"});
            }
        } catch (Throwable ignored) {}
    }

    public boolean isAppInstalled(String packageName) {
        if (mInstalledPackages.contains(packageName)) {
            return true;
        }
        if (mVirtualRoot != null) {
            File pDir = new File(mVirtualRoot, "data/user/0/" + packageName);
            if (pDir.exists()) {
                mInstalledPackages.add(packageName);
                return true;
            }
        }
        return false;
    }

    public boolean isAppCloned(String packageName) {
        return mClonedPackages.contains(packageName) || isAppInstalled(packageName);
    }

    public synchronized List<String> getClonedPackages() {
        return new ArrayList<>(mClonedPackages);
    }

    public boolean installPackage(String packageName) {
        return installPackage(packageName, mHostContext);
    }

    public synchronized boolean installPackage(String packageName, Context context) {
        try {
            if (mVirtualRoot == null && context != null) {
                mVirtualRoot = new File(context.getFilesDir(), "virtual_space");
            }
            if (mVirtualRoot != null) {
                File pDir = new File(mVirtualRoot, "data/user/0/" + packageName);
                pDir.mkdirs();
                new File(pDir, "files").mkdirs();
                new File(pDir, "databases").mkdirs();
                new File(pDir, "shared_prefs").mkdirs();
                new File(pDir, "cache").mkdirs();
            }
            mInstalledPackages.add(packageName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clonePackage(String packageName) {
        return clonePackage(packageName, mHostContext);
    }

    public synchronized boolean clonePackage(String packageName, Context context) {
        boolean ok = installPackage(packageName, context);
        if (ok) {
            mClonedPackages.add(packageName);
            persistClonedPackages(context);
        }
        return ok;
    }

    public synchronized boolean removePackage(String packageName, Context context) {
        mClonedPackages.remove(packageName);
        mInstalledPackages.remove(packageName);
        persistClonedPackages(context);
        try {
            if (mVirtualRoot != null) {
                File pDir = new File(mVirtualRoot, "data/user/0/" + packageName);
                if (pDir.exists()) {
                    deleteRecursive(pDir);
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void persistClonedPackages(Context context) {
        if (context == null) context = mHostContext;
        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putStringSet(KEY_CLONED_PACKAGES, new HashSet<>(mClonedPackages)).apply();
            } catch (Throwable ignored) {}
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    public boolean launchApp(String packageName) {
        return launchApp(mHostContext, packageName);
    }

    public boolean launchApp(Context context, String packageName) {
        if (context == null) context = mHostContext;
        if (context == null) return false;

        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } else {
                Intent virtualIntent = new Intent(Intent.ACTION_MAIN);
                virtualIntent.setPackage(packageName);
                virtualIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                virtualIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(virtualIntent);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
