package com.TrakEngineering.veeder_rootinterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.text.Html;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.TrakEngineering.veeder_rootinterface.enity.AuthEntityClass;
import com.TrakEngineering.veeder_rootinterface.enity.UserInfoEntity;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by VASP-LAP on 08-09-2015.
 */
class CommonUtils {
    private static File mypath; /*'---------------------------------------------------------------------------------------- Implemet logger functionality here....*/

    public static void LogMessage(String TAG, String TheMessage, Exception ex) {
        String logmessage = getTodaysDateInString();
        try {
            File logFileFolder = new File(Constants.LogPath);
            if (!logFileFolder.exists()) logFileFolder.mkdirs(); /*Delete file if it is more than 7 days old*/
            String OldFileToDelete = logFileFolder + "/Log_" + GetDateString(System.currentTimeMillis() - 604800000) + ".txt";
            File fd = new File(OldFileToDelete);
            if (fd.exists()) {
                fd.delete();
            }
            String LogFileName = logFileFolder + "/Log_" + GetDateString(System.currentTimeMillis()) + ".txt"; /*if(!new File(LogFileName).exists()) { new File(LogFileName).createNewFile(); }*/

            if (!new File(LogFileName).exists()) {
                File newFile = new File(LogFileName);
                newFile.createNewFile();
            }

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LogFileName, true)));

            logmessage = logmessage + " - " + TheMessage;

            if (ex != null){ logmessage = logmessage + TAG + ":" + ex.getMessage();}

            out.println(logmessage);
            out.close();
        } catch (Exception e1) {
            logmessage = logmessage + e1.getMessage();
            Log.d(TAG, logmessage);
        }
    }


    public static void LogMessage(String TAG, String TheMessage) {
        LogMessage(TAG, TheMessage, null);
    }

    private static String getTodaysDateInString() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return (df.format(c.getTime()));
    }


    public static String getTodaysDateInStringPrint(String ServerDate001) {

        String outputDateStr = null;
        try {
        DateFormat inputFormat = new SimpleDateFormat("mm/dd/yyyy hh:mm:ss a");
        DateFormat outputFormat = new SimpleDateFormat("hh:mm MMM dd,yyyy");
        Date date = inputFormat.parse(ServerDate001);
        outputDateStr = outputFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return outputDateStr;
    }

    //this method counts the number of nonzero elements in an array
    public static int NumNonZero(ArrayList<Integer> integerarray) {
        Integer nnz = 0;
        for (int i = 0; i < integerarray.size(); i++) {
            if (integerarray.get(i) > 0) nnz++;
        }
        return nnz;
    }


    public static String GetDateString(Long dateinms) {
        try {
            Time myDate = new Time();
            myDate.set(dateinms);
            return myDate.format("%Y-%m-%d");
        } catch (Exception e1) {
            return "";
        }
    } // Create logger functionality

    //----------------------------------------------------------------------------

    public static void showCustomMessageDilaog(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialogBus.setTitle("KavachGPS Would Like to Access Your Details");
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge);
        dialogBus.show();

        TextView edt_message = dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = dialogBus.findViewById(R.id.btnAllow);
        edt_message.setText(message);

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();

                //editVehicleNumber.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);


            }
        });

    }

    public static void showMessageDilaog(final Activity context, String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title

        //alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    public static void showMessageDilaogFinish(final Activity context, String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        context.finish();
                        dialog.cancel();
                    }
                });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }


    public static void showNoInternetDialog(final Activity context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title
        alertDialogBuilder.setTitle("No Internet");
        alertDialogBuilder
                .setMessage(Html.fromHtml(context.getResources().getString(R.string.no_internet)))
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        context.finish();
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }


    public static void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman != null ? conman.getClass().getName() : null);
        final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Class connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());


        Class[] cArg = new Class[2];
        cArg[0] = String.class;
        cArg[1] = Boolean.TYPE;
        Method setMobileDataEnabledMethod;

        setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", cArg);

        Object[] pArg = new Object[2];
        pArg[0] = context.getPackageName();
        pArg[1] = false;

        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(connectivityManager, pArg);
    }


    public static Boolean isMobileDataEnabled(Activity activity) {
        Object connectivityService = activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) connectivityService;

        try {
            Class<?> c = Class.forName(cm != null ? cm.getClass().getName() : null);
            Method m = c.getDeclaredMethod("getMobileDataEnabled");
            m.setAccessible(true);
            return (Boolean) m.invoke(cm);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isWiFiEnabled(Context ctx) {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(WIFI_SERVICE);

        return wifiManager != null && wifiManager.isWifiEnabled();

    }

    public static boolean isHotspotEnabled(Context ctx) {

        final WifiManager wifiManager = (WifiManager) ctx.getSystemService(WIFI_SERVICE);
        final int apState;
        try {
            apState = (Integer) (wifiManager != null ? wifiManager.getClass().getMethod("getWifiApState").invoke(wifiManager) : null);
            if (apState == 13) {
                return true;  // hotspot Enabled
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void SaveBaudRateInPref(Activity activity, String data, String valueType) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_BAUDRATE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(valueType, data);
        editor.apply();
    }

    public static void SaveDataInPref(Activity activity, String data, String valueType) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_COLUMN_SITE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(valueType, data);
        editor.apply();
    }

    public static void SaveUserInPref(Activity activity, String userName, String userMobile, String userEmail, String IsOdoMeterRequire,
                                      String IsDepartmentRequire, String IsPersonnelPINRequire, String IsOtherRequire, String IsHoursRequire, String OtherLabel, String TimeOut, String HubId, String IsPersonnelPINRequireForHub, String FluidSecureSiteName,
                                      String ReceiveDeliveryInformation, String VRDeviceType, String MacAddressForBTVeederRoot) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(AppConstants.ReceiveDeliveryInformation, ReceiveDeliveryInformation);
        editor.putString(AppConstants.USER_NAME, userName);
        editor.putString(AppConstants.USER_MOBILE, userMobile);
        editor.putString(AppConstants.USER_EMAIL, userEmail);
        editor.putString(AppConstants.IsOdoMeterRequire, IsOdoMeterRequire);
        editor.putString(AppConstants.IsDepartmentRequire, IsDepartmentRequire);
        editor.putString(AppConstants.IsPersonnelPINRequire, IsPersonnelPINRequire);
        editor.putString(AppConstants.FluidSecureSiteName, FluidSecureSiteName);
        //editor.putString(AppConstants.IsPersonnelPINRequireForHub, IsPersonnelPINRequireForHub);
        editor.putString(AppConstants.IsOtherRequire, IsOtherRequire);
        editor.putString(AppConstants.IsHoursRequire, IsHoursRequire);
        editor.putString(AppConstants.OtherLabel, OtherLabel);
        editor.putString(AppConstants.TimeOut, TimeOut);
        editor.putString(AppConstants.HubId, HubId);
        editor.putString(AppConstants.IsPersonnelPINRequireForHub, IsPersonnelPINRequireForHub);
        editor.putString(AppConstants.VRDeviceType, VRDeviceType);
        editor.putString(AppConstants.MacAddressForBTVeederRoot, MacAddressForBTVeederRoot);
        editor.apply();
    }

    public static void SaveVehiFuelInPref_FS1(Activity activity, String TransactionId_FS1, String VehicleId_FS1, String PhoneNumber_FS1, String PersonId_FS1, String PulseRatio_FS1, String MinLimit_FS1, String FuelTypeId_FS1, String ServerDate_FS1, String IntervalToStopFuel_FS1, String PrintDate_FS1, String Company_FS1, String Location_FS1, String PersonName_FS1, String BluetoothCardReader_FS1, String PrinterName_FS1, String vehicleNumber_FS1, String accOther_FS1) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("TransactionId_FS1", TransactionId_FS1);
        editor.putString("VehicleId_FS1", VehicleId_FS1);
        editor.putString("VehicleId_FS1", VehicleId_FS1);
        editor.putString("PhoneNumber_FS1", PhoneNumber_FS1);
        editor.putString("PersonId_FS1", PersonId_FS1);
        editor.putString("PulseRatio_FS1", PulseRatio_FS1);
        editor.putString("MinLimit_FS1", MinLimit_FS1);
        editor.putString("FuelTypeId_FS1", FuelTypeId_FS1);
        editor.putString("ServerDate_FS1", ServerDate_FS1);
        editor.putString("IntervalToStopFuel_FS1", IntervalToStopFuel_FS1);
        editor.putString("PrintDate_FS1", PrintDate_FS1);
        editor.putString("Company_FS1", Company_FS1);
        editor.putString("Location_FS1", Location_FS1);
        editor.putString("PersonName_FS1", PersonName_FS1);
        editor.putString("BluetoothCardReader_FS1", BluetoothCardReader_FS1);
        editor.putString("PrinterName_FS1", PrinterName_FS1);
        editor.putString("vehicleNumber_FS1", vehicleNumber_FS1);
        editor.putString("accOther_FS1", accOther_FS1);


        editor.apply();
    }

    public static void SaveVehiFuelInPref(Activity activity, String TransactionId, String VehicleId, String PhoneNumber, String PersonId, String PulseRatio, String MinLimit, String FuelTypeId, String ServerDate, String IntervalToStopFuel, String PrintDate, String Company, String Location, String PersonName, String BluetoothCardReader, String PrinterName, String vehicleNumber, String accOther) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("TransactionId", TransactionId);
        editor.putString("VehicleId", VehicleId);
        editor.putString("PhoneNumber", PhoneNumber);
        editor.putString("PersonId", PersonId);
        editor.putString("PulseRatio", PulseRatio);
        editor.putString("MinLimit", MinLimit);
        editor.putString("FuelTypeId", FuelTypeId);
        editor.putString("ServerDate", ServerDate);
        editor.putString("IntervalToStopFuel", IntervalToStopFuel);
        editor.putString("PrintDate", PrintDate);
        editor.putString("Company", Company);
        editor.putString("Location", Location);
        editor.putString("PersonName", PersonName);
        editor.putString("BluetoothCardReader", BluetoothCardReader);
        editor.putString("PrinterName", PrinterName);
        editor.putString("vehicleNumber", vehicleNumber);
        editor.putString("accOther", accOther);

        editor.apply();
    }

    public static void SaveVehiFuelInPref_FS3(Activity activity, String TransactionId_FS3, String VehicleId_FS3, String PhoneNumber_FS3, String PersonId_FS3, String PulseRatio_FS3, String MinLimit_FS3, String FuelTypeId_FS3, String ServerDate_FS3, String IntervalToStopFuel_FS3, String PrintDate_FS3, String Company_FS3, String Location_FS3, String PersonName_FS3, String BluetoothCardReader_FS3, String PrinterName_FS3, String vehicleNumber_FS3, String accOther_FS3) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("TransactionId_FS3", TransactionId_FS3);
        editor.putString("VehicleId_FS3", VehicleId_FS3);
        editor.putString("VehicleId_FS3", VehicleId_FS3);
        editor.putString("PhoneNumber_FS3", PhoneNumber_FS3);
        editor.putString("PersonId_FS3", PersonId_FS3);
        editor.putString("PulseRatio_FS3", PulseRatio_FS3);
        editor.putString("MinLimit_FS3", MinLimit_FS3);
        editor.putString("FuelTypeId_FS3", FuelTypeId_FS3);
        editor.putString("ServerDate_FS3", ServerDate_FS3);
        editor.putString("IntervalToStopFuel_FS3", IntervalToStopFuel_FS3);
        editor.putString("PrintDate_FS3", PrintDate_FS3);
        editor.putString("Company_FS3", Company_FS3);
        editor.putString("Location_FS3", Location_FS3);
        editor.putString("PersonName_FS3", PersonName_FS3);
        editor.putString("BluetoothCardReader_FS3", BluetoothCardReader_FS3);
        editor.putString("PrinterName_FS3", PrinterName_FS3);
        editor.putString("vehicleNumber_FS3", vehicleNumber_FS3);
        editor.putString("accOther_FS3", accOther_FS3);


        editor.apply();
    }

    public static void SaveVehiFuelInPref_FS4(Activity activity, String TransactionId_FS4, String VehicleId_FS4, String PhoneNumber_FS4, String PersonId_FS4, String PulseRatio_FS4, String MinLimit_FS4, String FuelTypeId_FS4, String ServerDate_FS4, String IntervalToStopFuel_FS4, String PrintDate_FS4, String Company_FS4, String Location_FS4, String PersonName_FS4, String BluetoothCardReader_FS4, String PrinterName_FS4, String vehicleNumber_FS4, String accOther_FS4) {

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("TransactionId_FS4", TransactionId_FS4);
        editor.putString("VehicleId_FS4", VehicleId_FS4);
        editor.putString("VehicleId_FS4", VehicleId_FS4);
        editor.putString("PhoneNumber_FS4", PhoneNumber_FS4);
        editor.putString("PersonId_FS4", PersonId_FS4);
        editor.putString("PulseRatio_FS4", PulseRatio_FS4);
        editor.putString("MinLimit_FS4", MinLimit_FS4);
        editor.putString("FuelTypeId_FS4", FuelTypeId_FS4);
        editor.putString("ServerDate_FS4", ServerDate_FS4);
        editor.putString("IntervalToStopFuel_FS4", IntervalToStopFuel_FS4);
        editor.putString("PrintDate_FS4", PrintDate_FS4);
        editor.putString("Company_FS4", Company_FS4);
        editor.putString("Location_FS4", Location_FS4);
        editor.putString("PersonName_FS4", PersonName_FS4);
        editor.putString("BluetoothCardReader_FS4", BluetoothCardReader_FS4);
        editor.putString("PrinterName_FS4", PrinterName_FS4);
        editor.putString("vehicleNumber_FS4", vehicleNumber_FS4);
        editor.putString("accOther_FS4", accOther_FS4);


        editor.apply();
    }

    public static AuthEntityClass getWiFiDetails(Activity activity, String wifiSSID) {


        AuthEntityClass authEntityClass = new AuthEntityClass();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        String dataSite = sharedPref.getString(Constants.PREF_COLUMN_SITE, "");


        try {
            if (dataSite != null) {
                JSONArray jsonArray = new JSONArray(dataSite);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    Gson gson = new Gson();
                    authEntityClass = gson.fromJson(jsonObject.toString(), AuthEntityClass.class);

                }
            }
        } catch (Exception ex) {

            String TAG = "CommonUtils";
            CommonUtils.LogMessage(TAG, "", ex);
        }

        return authEntityClass;

    }

    public static String getVersionCode(Context ctx) {

        String versioncode = "";
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            versioncode = pInfo.versionName;

        } catch (Exception q) {
            q.printStackTrace();
        }

        return versioncode;
    }

    public static UserInfoEntity getCustomerDetails(Activity activity) {

        UserInfoEntity userInfoEntity = new UserInfoEntity();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        userInfoEntity.PersonName = sharedPref.getString(AppConstants.USER_NAME, "");
        userInfoEntity.PhoneNumber = sharedPref.getString(AppConstants.USER_MOBILE, "");
        userInfoEntity.PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");
        userInfoEntity.FluidSecureSiteName = sharedPref.getString(AppConstants.FluidSecureSiteName, "");

        return userInfoEntity;
    }



    /*
    This shold not be needed- These are the services for the 4 hoses of the HUB app
    public static UserInfoEntity getCustomerDetails_backgroundService(BackgroundService_AP activity) {

        UserInfoEntity userInfoEntity = new UserInfoEntity();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        userInfoEntity.PersonName = sharedPref.getString(AppConstants.USER_NAME, "");
        userInfoEntity.PhoneNumber = sharedPref.getString(AppConstants.USER_MOBILE, "");
        userInfoEntity.PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");


        return userInfoEntity;
    }


    public static UserInfoEntity getCustomerDetails_backgroundService_PIPE(BackgroundService_AP_PIPE activity) {

        UserInfoEntity userInfoEntity = new UserInfoEntity();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        userInfoEntity.PersonName = sharedPref.getString(AppConstants.USER_NAME, "");
        userInfoEntity.PhoneNumber = sharedPref.getString(AppConstants.USER_MOBILE, "");
        userInfoEntity.PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");


        return userInfoEntity;
    }

    public static UserInfoEntity getCustomerDetails_backgroundService_FS3(BackgroundService_FS_UNIT_3 activity) {

        UserInfoEntity userInfoEntity = new UserInfoEntity();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        userInfoEntity.PersonName = sharedPref.getString(AppConstants.USER_NAME, "");
        userInfoEntity.PhoneNumber = sharedPref.getString(AppConstants.USER_MOBILE, "");
        userInfoEntity.PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");


        return userInfoEntity;
    }

    public static UserInfoEntity getCustomerDetails_backgroundService_FS4(BackgroundService_FS_UNIT_4 activity) {

        UserInfoEntity userInfoEntity = new UserInfoEntity();

        SharedPreferences sharedPref = activity.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        userInfoEntity.PersonName = sharedPref.getString(AppConstants.USER_NAME, "");
        userInfoEntity.PhoneNumber = sharedPref.getString(AppConstants.USER_MOBILE, "");
        userInfoEntity.PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");


        return userInfoEntity;
    }
*/


}
