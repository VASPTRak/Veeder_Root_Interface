package com.TrakEngineering.veeder_rootinterface;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.TrakEngineering.veeder_rootinterface.Constants.VR_polling_interval;

public class VRInitAlarmService extends Service {

    private static final String TAG = VRInitAlarmService.class.getSimpleName();

    public VRInitAlarmService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG," ~OnStart~ ");
        CommonUtils.LogMessage(TAG, "~VRInitAlarmService Started~~ PollingInterval: " + VR_polling_interval);

        //Set exact alaram every 24 hours.
        GetExactVRReadings();  //Exact VR-Reading logic added on 3rd august 2021

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        },60000);

        return super.onStartCommand(intent, flags, startId);
    }

    private void GetExactVRReadings() {

        try {

            // setRepeating() lets you specify a precise custom interval.
            //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 600000, pendingIntent); //10min

            switch (VR_polling_interval) {
                case 1:
                    // RemoveAllPreviousSetAlarms();
                    AlarmAt12Am(); //Execute Alarm Only at 00:00 / 12:00 AM  {VR_polling_interval = 1} 24H
                    break;

                case 2:
                    //  RemoveAllPreviousSetAlarms();
                    AlarmAt12Am(); //Execute Alarm at 00:00 / 12:00 AM  {VR_polling_interval = 2} 12H

                    AlarmAt12pm(); // 12:00 / 12:00 PM
                    break;

                case 3:
                    //  RemoveAllPreviousSetAlarms();
                    AlarmAt12Am(); //Execute Alarm at 00:00 / 12:00 AM  {VR_polling_interval = 3} 8H

                    AlarmAt8am(); // 08:00 / 08:00 AM

                    AlarmAt4pm(); // 16:00 / 4:00 PM
                    break;

                case 4:
                    //  RemoveAllPreviousSetAlarms();

                    AlarmAt12Am(); //Execute Alarm at 00:00 / 12:00 AM  {VR_polling_interval = 4} 6H

                    AlarmAt6Am(); // 06:00 / 06:00 AM

                    AlarmAt12pm(); // 12:00 / 12:00 PM

                    AlarmAt6Pm(); // 18:00 / 06:00 PM
                    break;

                case 6:

                    //   RemoveAllPreviousSetAlarms();
                    AlarmAt12Am(); //Execute Alarm at 00:00 / 12:00 AM  {VR_polling_interval = 6} 4H

                    AlarmAt4Am(); // 04:00 / 04:00 AM

                    AlarmAt8am(); // 08:00 / 08:00 AM

                    AlarmAt12pm(); // 12:00 / 12:00 PM

                    AlarmAt4pm(); // 16:00 / 04:00 PM

                    AlarmAt8pm(); // 20:00 / 08:00 PM

                    break;

                default:
                    //   RemoveAllPreviousSetAlarms();
                    AlarmAt12Am(); //Execute Alarm at 00:00 / 12:00 AM  {VR_polling_interval = 6} 4H

                    AlarmAt4Am(); // 04:00 / 04:00 AM

                    AlarmAt8am(); // 08:00 / 08:00 AM

                    AlarmAt12pm(); // 12:00 / 12:00 PM

                    AlarmAt4pm(); // 16:00 / 04:00 PM

                    AlarmAt8pm(); // 20:00 / 08:00 PM
                    Log.i(TAG, "Check VR_polling_interval: " + VR_polling_interval);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            CommonUtils.LogMessage(TAG, "Exception: " + e.toString());
        }

    }


    private void AlarmAt12Am() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 00); //00
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent1 = PendingIntent.getService(getApplicationContext(), 1, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_12am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_12am.cancel(pendingIntent1);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt12Am"); // #2238
        exat_alarm_12am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent1);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 00);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(getApplicationContext(), 1, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_12am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_12am.cancel(pendingIntent1);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt12Am"); // #2238
        exact_alarm_12am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent1);

    }

    private void AlarmAt4Am() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 4); //4
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent2 = PendingIntent.getService(getApplicationContext(), 2, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_4am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_4am.cancel(pendingIntent2);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt4Am"); // #2238
        exat_alarm_4am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent2);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 4);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getApplicationContext(), 2, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_4am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_4am.cancel(pendingIntent2);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt4Am"); // #2238
        exact_alarm_4am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent2);

    }

    private void AlarmAt6Am() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 6); //6
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent3 = PendingIntent.getService(getApplicationContext(), 3, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_6am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_6am.cancel(pendingIntent3);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt6Am"); // #2238
        exat_alarm_6am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent3);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 6);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(getApplicationContext(), 3, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_6am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_6am.cancel(pendingIntent3);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt6Am"); // #2238
        exact_alarm_6am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent3);

    }

    private void AlarmAt8am() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 8); //8
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent4 = PendingIntent.getService(getApplicationContext(), 4, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_8am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_8am.cancel(pendingIntent4);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt8am"); // #2238
        exat_alarm_8am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent4);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 8);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(getApplicationContext(), 4, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_8am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_8am.cancel(pendingIntent4);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt8am"); // #2238
        exact_alarm_8am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent4);

    }

    private void AlarmAt12pm() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 12); //12
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent5 = PendingIntent.getService(getApplicationContext(), 5, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_12pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_12pm.cancel(pendingIntent5);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt12pm"); // #2238
        exat_alarm_12pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent5);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 12);
        PendingIntent pendingIntent5 = PendingIntent.getBroadcast(getApplicationContext(), 5, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_12pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_12pm.cancel(pendingIntent5);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt12pm"); // #2238
        exact_alarm_12pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent5);

    }

    private void AlarmAt4pm() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 16); //16
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent6 = PendingIntent.getService(getApplicationContext(), 6, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_4pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_4pm.cancel(pendingIntent6);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt4pm"); // #2238
        exat_alarm_4pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent6);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 16);
        PendingIntent pendingIntent6 = PendingIntent.getBroadcast(getApplicationContext(), 6, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_4pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_4pm.cancel(pendingIntent6);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt4pm"); // #2238
        exact_alarm_4pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent6);

    }

    private void AlarmAt6Pm() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 18); //18
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent7 = PendingIntent.getService(getApplicationContext(), 7, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_6pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_6pm.cancel(pendingIntent7);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt6Pm"); // #2238
        exat_alarm_6pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent7);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 18);
        PendingIntent pendingIntent7 = PendingIntent.getBroadcast(getApplicationContext(), 7, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_6pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_6pm.cancel(pendingIntent7);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt6Pm"); // #2238
        exact_alarm_6pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent7);

    }

    private void AlarmAt8pm() {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 20); //20
        cal.set(Calendar.MINUTE, 00);
        cal.set(Calendar.SECOND, 00);
        if(cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        /*Intent intent = new Intent(getApplicationContext(), VRAlarmService.class);
        PendingIntent pendingIntent8 = PendingIntent.getService(getApplicationContext(), 8, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exat_alarm_8pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exat_alarm_8pm.cancel(pendingIntent8);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt8pm"); // #2238
        exat_alarm_8pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent8);*/

        // Added back the old code
        Intent intent = new Intent(getApplicationContext(), PendingIntentReceiver.class);
        intent.setAction("GetExactVRReadings");
        intent.putExtra("AlarmHour", 20);
        PendingIntent pendingIntent8 = PendingIntent.getBroadcast(getApplicationContext(), 8, intent, FLAG_CANCEL_CURRENT);
        AlarmManager exact_alarm_8pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        exact_alarm_8pm.cancel(pendingIntent8);
        CommonUtils.LogMessage(TAG, "VRInitAlarmService: AlarmAt8pm"); // #2238
        exact_alarm_8pm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent8);

    }
}