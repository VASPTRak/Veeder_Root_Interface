package com.TrakEngineering.veeder_rootinterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class PendingIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            System.out.println("PendingIntent onReceive");

            Calendar c = Calendar.getInstance();
            int currentTime = c.get(Calendar.HOUR_OF_DAY);
            int currenth = Calendar.getInstance().getTime().getHours();
            Bundle notificationData = intent.getExtras();
            String Action = intent.getAction(); //Action = GetExactVRReadings
            int AlarmHour = notificationData.getInt("AlarmHour");
            CommonUtils.LogMessage("TAG", "ExactAlarm current Hour:" + currentTime + " AlarmHour:" + AlarmHour);
            Log.i("PendingIntent", "surelockcheck ExactAlarm current Hour:" + currentTime + " AlarmHour:" + AlarmHour);
            if (currentTime == AlarmHour) {
                CommonUtils.LogMessage("TAG", "ExactAlarm broadcast sent");
                Log.i("PendingIntent","surelockcheck ExactAlarm broadcast sent");
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("GetExactVRReadings");
                context.sendBroadcast(broadcastIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
