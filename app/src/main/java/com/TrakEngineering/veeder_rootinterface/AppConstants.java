package com.TrakEngineering.veeder_rootinterface;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


/**
 * Created by Administrator on 5/19/2016.
 */
public class AppConstants {


    public static final String DEVICE_TYPE = "A";
    public static final String USER_NAME = "userName";
    public static final String ReceiveDeliveryInformation = "ReceiveDeliveryInformation";
    public static final String USER_MOBILE = "userMobile";
    public static final String USER_EMAIL = "userEmail";
    public static final String IsOdoMeterRequire = "IsOdoMeterRequire";
    public static final String IsDepartmentRequire = "IsDepartmentRequire";
    public static final String IsPersonnelPINRequire = "IsPersonnelPINRequire";
    public static final String IsPersonnelPINRequireForHub = "IsPersonnelPINRequireForHub";
    public static final String VRDeviceType = "VRDeviceType";
    public static final String MacAddressForBTVeederRoot = "MacAddressForBTVeederRoot";
    public static final String FluidSecureSiteName = "FluidSecureSiteName";
    public static final String IsOtherRequire = "IsOtherRequire";
    public static final String IsHoursRequire = "IsHoursRequire";
    public static final String OtherLabel = "OtherLabel";
    public static final String TimeOut = "TimeOut";
    public static final String HubId = "HubId";
    public static final String BLUETOOTH_VR_CONNECT_NAME = "JDY-08";  //This is the default name, we may decide to change it or dynamically load from the server.
    public static final String webIP = "https://www.fluidsecure.net/"; //live

    //public static String webIP = "http://sierravistatest.cloudapp.net/"; // new test
    public static final String RES_MESSAGE = "ResponceMessage";
    public static final String RES_DATA_SSID = "SSIDDataObj";
    public static final String RES_DATA_USER = "objUserData";
    public static final String RES_TEXT = "ResponceText";
    //private static final String webIP = "http://103.8.126.241:89/";//test
    public static final String webURL = webIP + "HandlerTrak.ashx";
    public static final String LoginURL = webIP + "LoginHandler.ashx";
    public static String FS_selected;
    public static String BaudRate = "9600";
    public static String Title = "";
    public static String HubName;
    public static String HubGeneratedpassword;
    public static String Login_Email;
    public static String Login_IMEI;
    public static String RES_DATA = "ResponceData";
    public static String FOB_KEY_PERSON = "";
    public static String FOB_KEY_VEHICLE = "";
    public static String HUB_ID = "";
    public static String VRForceReadingSave = "n";


    public static String FS2_CONNECTED_SSID;

    public static String REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC;
    public static String REPLACEBLE_WIFI_NAME_FS2;

    public static boolean NeedToRenameFS_ON_UPDATE_MAC;
    public static boolean NeedToRenameFS2;


    public static String REPLACEBLE_WIFI_NAME;
    public static String LAST_CONNECTED_SSID;
    public static String SELECTED_MACADDRESS;
    public static String CURRENT_SELECTED_SSID;
    public static String CURRENT_HOSE_SSID;
    public static String CURRENT_SELECTED_SITEID;
    public static String UPDATE_MACADDRESS;
    public static String R_HOSE_ID;
    public static String R_SITE_ID;

    public static String Current_VR_IP = "204.185.75.88";
    public static String Current_VR_Port = "10001";
    public static String VR_MAC = "50:F1:4A:AC:71:D0"; //using my test as default


    public static String WIFI_PASSWORD = "123456789";

    public static boolean RunningPoll = false;

    public static boolean NeedToRename;
    public static boolean BUSY_STATUS;


    public static boolean IS_WIFI_ON;
    public static boolean IS_DATA_ON;
    public static boolean IS_HOTSPOT_ON;

    public static ArrayList<HashMap<String, String>> DetailsServerSSIDList;
    public static ArrayList<HashMap<String, String>> DetailsListOfConnectedDevices;


    public static double roundNumber(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


    public static String convertStingToBase64(String text) {
        String base64 = "";
        try {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            base64 = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        base64 = base64.replaceAll("\\n", "");

        return base64;
    }


    public static String getIMEIOnlyForBelowOS10(Context ctx) {

        String storedIMEI = "";
        try {

            TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            storedIMEI = telephonyManager.getDeviceId();


        } catch (Exception e) {

        }

        return storedIMEI;
    }


    public static boolean isMobileDataAvailable(Context ctx) {

        boolean mobileDataEnabled = false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm != null ? cm.getClass().getName() : null);
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mobileDataEnabled;
    }

    public static void disconnectWiFi(Context ctx) {


        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {

            //wifiManager.disconnect();

            wifiManager.setWifiEnabled(false);
        }

    }


    public static void dontConnectWiFi(Context ctx) {


        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {

            wifiManager.disconnect();

            wifiManager.setWifiEnabled(true);

        }

    }

    public static String getConnectedWifiName(Context context) {
        String name = "";
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager != null ? wifiManager.getConnectionInfo() : null;
        name = wifiInfo.getSSID();

        System.out.println("connected ssid--" + name);

        return name;
    }


    public static void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();


                    }
                }


        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        TextView textView = alertDialog.findViewById(android.R.id.message);
        textView.setTextSize(35);
    }


    public static void AlertDialogFinish(final Activity ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        ctx.finish();

                    }
                }

        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    public static void colorToast(Context ctx, String msg, int colr) {
        Toast toast = Toast.makeText(ctx, " " + msg + " ", Toast.LENGTH_LONG);
        toast.getView().setBackgroundColor(colr);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }


    public static void colorToastBigFont(Context ctx, String msg, int colr) {
        Toast toast = Toast.makeText(ctx, " " + msg + " ", Toast.LENGTH_LONG);
        toast.getView().setBackgroundColor(colr);
        toast.setGravity(Gravity.CENTER, 0, 0);
        ViewGroup group = (ViewGroup) toast.getView();
        TextView messageTextView = (TextView) group.getChildAt(0);
        messageTextView.setTextSize(25);
        toast.show();

    }

    public static void notificationAlert(Context context) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String message = "Successfully completed Transaction.";
        String title = "FluidSecure";
        int icon = R.mipmap.ic_launcher;
        long when = System.currentTimeMillis();
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), icon);

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(icon)
                .setLargeIcon(largeIcon)
                .setWhen(when)
                .setAutoCancel(true)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notificationManager.notify(0, notification);


    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    /*public static String getConnectedWiFidsdsdsd(Context ctx) {
        String wifiname = "";

        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();

            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            wifiname = ssid;
        }

        return wifiname;
    }*/


    public static void startWelcomeActivity(Context ctx) {
        Intent i = new Intent(ctx, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(i);
    }


    public static void ClearEdittextFielsOnBack(Context ctx) {

        if (Constants.CurrentSelectedHose.equals("FS1")) {
            Constants.AccVehicleNumber_FS1 = "";
            Constants.AccOdoMeter_FS1 = 0;
            Constants.AccDepartmentNumber_FS1 = "";
            Constants.AccPersonnelPIN_FS1 = "";
            Constants.AccOther_FS1 = "";

        } else if (Constants.CurrentSelectedHose.equals("FS2")) {

            Constants.AccVehicleNumber = "";
            Constants.AccOdoMeter = 0;
            Constants.AccDepartmentNumber = "";
            Constants.AccPersonnelPIN = "";
            Constants.AccOther = "";

        } else if (Constants.CurrentSelectedHose.equals("FS3")) {

            Constants.AccVehicleNumber_FS3 = "";
            Constants.AccOdoMeter_FS3 = 0;
            Constants.AccDepartmentNumber_FS3 = "";
            Constants.AccPersonnelPIN_FS3 = "";
            Constants.AccOther_FS3 = "";

        } else {

            Constants.AccVehicleNumber_FS4 = "";
            Constants.AccOdoMeter_FS4 = 0;
            Constants.AccDepartmentNumber_FS4 = "";
            Constants.AccPersonnelPIN_FS4 = "";
            Constants.AccOther_FS4 = "";

        }
    }

    public static String getOriginalUUID_IMEIFromFile(Context ctx) {

        String storedUUIDIMEI = "";
        try {

            String encryptedIMEI = SplashActivity.readIMEIMobileNumFromFile(ctx).trim();
            storedUUIDIMEI = AES.decrypt(encryptedIMEI, AES.credential);


        } catch (Exception e) {

        }

        return storedUUIDIMEI;
    }

    public static void WriteinFileasd(String str) {
        try {

            if (str.contains("Responce"))
                str = str.replace("Responce", "Response");

            File file = new File(Environment.getExternalStorageDirectory() + "/FSLog");

            if (!file.exists()) {
                if (file.mkdirs()) {
                    //System.out.println("Create FSLog Folder");
                } else {
                    // System.out.println("Fail to create KavachLog folder");
                }
            }

            String dt = CommonUtils.GetDateString(System.currentTimeMillis());
            File gpxfile = new File(file + "/Log_" + dt + ".txt");
            if (!gpxfile.exists()) {
                gpxfile.createNewFile();
            }

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
            String UseDate = dateFormat.format(cal.getTime());

            FileWriter fileWritter = new FileWriter(gpxfile, true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write("\n" + UseDate + "--" + str + " ");
            bufferWritter.close();

        } catch (IOException e) {


        }
    }

    public static String currentDate(String format) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat(format);//"MM/dd/yyyy HH:mm:ss a"
        String strDate = mdformat.format(calendar.getTime());
        return strDate;
    }

}
