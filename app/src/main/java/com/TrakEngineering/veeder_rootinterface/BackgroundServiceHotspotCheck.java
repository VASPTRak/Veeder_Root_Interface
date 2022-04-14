package com.TrakEngineering.veeder_rootinterface;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;

//import static com.TrakEngineering.veeder_rootinterface.WelcomeActivity.wifiApManager;

/**
 * Created by User on 11/8/2017.
 */

public class BackgroundServiceHotspotCheck extends BackgroundService{


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Constants.DEBUG) {
            Calendar cal = Calendar.getInstance();
            System.out.println("started wifi check " + cal.toString());
        }

        try {
            super.onStart(intent, startId);
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.d("Service", "null");
                this.stopSelf();
            } else {

                //System.out.println("Service is on...........");
                if (!CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn) {

//                    wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                    //AppConstants.colorToastBigFont(BackgroundServiceHotspotCheck.this, "Connecting to hotspot, please wait", Color.RED);
                    System.out.println("Connecting to hotspot, please wait....");
                }

            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return Service.START_STICKY;
    }
}
