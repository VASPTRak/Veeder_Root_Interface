package com.TrakEngineering.veeder_rootinterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            SharedPreferences sharedPrefG = context.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            String VRDeviceType = sharedPrefG.getString("VRDeviceType", "BT");
            if (!VRDeviceType.equalsIgnoreCase("BT")) {
                Intent vr_intent = new Intent(context, VR_interface.class);
                context.startService(vr_intent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
