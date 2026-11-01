package com.vibration.lite.core;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class GameUpdateManager {

    private static final String TAG = "GameUpdateManager";
    public static final String EFOOTBALL_PACKAGE = "com.konami.efootballpes";
    public static final String PLAY_STORE_PACKAGE = "com.android.vending";

    public static boolean openGameInPlayStore(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            packageName = EFOOTBALL_PACKAGE;
        }

        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (isPackageInstalled(context, PLAY_STORE_PACKAGE)) {
                marketIntent.setPackage(PLAY_STORE_PACKAGE);
            }

            context.startActivity(marketIntent);
            return true;
        } catch (ActivityNotFoundException anfe) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed store fallback", e);
                return false;
            }
        }
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
