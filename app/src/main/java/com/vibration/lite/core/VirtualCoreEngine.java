package com.vibration.lite.core;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VirtualCoreEngine {

    private static final String TAG = "VirtualCoreEngine";
    private static final VirtualCoreEngine sInstance = new VirtualCoreEngine();

    private Context mHostContext;
    private File mVirtualRoot;
    private final Set<String> mInstalledVirtualPackages = Collections.synchronizedSet(new HashSet<>());
    private boolean mIsInitialized = false;

    public static VirtualCoreEngine get() {
        return sInstance;
    }

    private VirtualCoreEngine() {}

    public synchronized void startup(Context context) {
        if (mIsInitialized) return;
        mHostContext = context;

        unsealHiddenApiRestrictions();

        mVirtualRoot = new File(context.getFilesDir(), "virtual_space");
        if (!mVirtualRoot.exists()) {
            mVirtualRoot.mkdirs();
        }

        scanClonedPackages();
        mIsInitialized = true;
        Log.i(TAG, "VirtualCoreEngine initialized. Sandbox root: " + mVirtualRoot.getAbsolutePath());
    }

    @SuppressLint("PrivateApi")
    private void unsealHiddenApiRestrictions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Method forNameMethod = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethodMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

                Class<?> vmRuntimeClass = (Class<?>) forNameMethod.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntimeMethod = (Method) getDeclaredMethodMethod.invoke(vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethodMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});

                Object runtime = getRuntimeMethod.invoke(null);
                setHiddenApiExemptions.invoke(runtime, (Object) new String[]{"L"});
                Log.d(TAG, "Android hidden API restrictions unsealed.");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Hidden API unseal note: " + t.getMessage());
        }
    }

    private void scanClonedPackages() {
        File userZero = new File(mVirtualRoot, "data/user/0");
        if (userZero.exists() && userZero.isDirectory()) {
            File[] files = userZero.listFiles();
            if (files != null) {
                for (File dir : files) {
                    if (dir.isDirectory()) {
                        mInstalledVirtualPackages.add(dir.getName());
                    }
                }
            }
        }
    }

    public boolean isAppCloned(String packageName) {
        return mInstalledVirtualPackages.contains(packageName);
    }

    public boolean clonePackage(String packageName) {
        try {
            PackageManager pm = mHostContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);

            File packageDataDir = getVirtualDataDir(packageName);
            File codeCacheDir = new File(packageDataDir, "code_cache");
            File filesDir = new File(packageDataDir, "files");
            File sharedPrefsDir = new File(packageDataDir, "shared_prefs");
            File libDir = new File(packageDataDir, "lib");

            packageDataDir.mkdirs();
            codeCacheDir.mkdirs();
            filesDir.mkdirs();
            sharedPrefsDir.mkdirs();
            libDir.mkdirs();

            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    codeCacheDir.setReadOnly();
                } catch (SecurityException ignored) {}
            }

            mInstalledVirtualPackages.add(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found on host: " + packageName);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error cloning package: " + packageName, e);
            return false;
        }
    }

    public boolean launchClonedApp(Context context, String packageName) {
        if (!isAppCloned(packageName)) {
            boolean cloned = clonePackage(packageName);
            if (!cloned) return false;
        }

        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) return false;

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            launchIntent.putExtra("vibration.virtual.enabled", true);
            launchIntent.putExtra("vibration.virtual.pkg", packageName);
            launchIntent.putExtra("vibration.virtual.dataDir", getVirtualDataDir(packageName).getAbsolutePath());

            context.startActivity(launchIntent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch cloned app", e);
            return false;
        }
    }

    public File getVirtualDataDir(String packageName) {
        return new File(mVirtualRoot, "data/user/0/" + packageName);
    }

    public static class ShadowFileProvider extends ContentProvider {
        @Override
        public boolean onCreate() { return true; }
        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
        @Override
        public String getType(Uri uri) { return "application/octet-stream"; }
        @Override
        public Uri insert(Uri uri, ContentValues values) { return null; }
        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    }
}
