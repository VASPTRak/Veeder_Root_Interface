package com.TrakEngineering.veeder_rootinterface;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class AppLaunchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            System.out.println("AppLaunch onReceive");

            boolean isForeground = new ForegroundCheckTask().execute(context).get();
            CommonUtils.LogMessage("AppLaunchReceiver", "Is VR App in the foreground: " + isForeground);
            if (!isForeground) {
                CommonUtils.LogMessage("AppLaunchReceiver", "The VR App is in the background. Launching the VR Interface App..");

                /*Intent i = new Intent(context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()));
                i.setComponent(new ComponentName("com.TrakEngineering.veeder_rootinterface","com.TrakEngineering.veeder_rootinterface.SplashActivity"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);*/
                Intent activity = new Intent(context, SplashActivity.class);
                activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(activity);

            }
        } catch (Exception e) {
            CommonUtils.LogMessage("AppLaunchReceiver", "AppLaunchReceiver: Exception: " + e.getMessage());
        }
    }
}

class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Context... params) {
        final Context context = params[0].getApplicationContext();
        return isAppOnForeground(context);
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}