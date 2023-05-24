package com.TrakEngineering.veeder_rootinterface;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.veeder_rootinterface.enity.VR_Alarm_InfoEntity;
import com.TrakEngineering.veeder_rootinterface.enity.VR_Delivery_InfoEntity;
import com.TrakEngineering.veeder_rootinterface.enity.VR_Inventory_InfoEntity;
import com.TrakEngineering.veeder_rootinterface.enity.VR_Leak_InfoEntity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.TrakEngineering.veeder_rootinterface.AppConstants.Login_Email;
import static com.TrakEngineering.veeder_rootinterface.AppConstants.ReceiveDeliveryInformation;
import static com.TrakEngineering.veeder_rootinterface.Constants.VR_polling_interval;
import static java.lang.System.console;
import static java.lang.System.currentTimeMillis;

/**
 * Created by Sven Wijtmans on 12/28/2017.
 * Intended to be the class that handles communication with the Veeder-Root tank monitor
 */

public class VR_interface extends BackgroundService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = com.TrakEngineering.veeder_rootinterface.VR_interface.class.getSimpleName();


    // android built in classes for bluetooth operations
    private static BluetoothAdapter mBluetoothAdapter;
    private static String response_message;
    //String HTTP_URL = "http://192.168.43.140:80/";//for pipe
    //String HTTP_URL = "http://192.168.43.5:80/";//Other FS
    private final String HTTP_URL = "";


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    // will enable user to enter any text to be printed
    EditText myTextbox;
    String URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
    String URL_RELAY = HTTP_URL + "config?command=relay";
    private String DateTime;
    private double TankLevel = 0;
    private int TankNumber = 1;
    private TextView tvConsole;
    private BluetoothGatt mBluetoothGatt;
    private boolean flag_GotServices, flag_GotResponse;
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        //start service discovery
                        mBluetoothGatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
//                    displayGattServices(mBluetoothGatt.getServices());
                    mBluetoothGatt.getServices();
                    flag_GotServices = true;
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    CommonUtils.LogMessage(TAG, "onCharacteristicRead received: " + status + characteristic);

                    byte[] value = characteristic.getValue();
                    try {
                        Log.w(TAG, "Read value:" + new String(value));
                    } catch (Exception ex) {
                        Log.w(TAG, "Read value: null");
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.w(TAG, "onCharacteristicChanged received: " + characteristic);

                    byte[] value = characteristic.getValue();
                    try {

                        Log.w(TAG, "Read value: " + new String(value) + " , " + toHexString(value));
                        if (value[0] == Constants.SOH)  //logic to group together commands split over several lines
                        {
                            response_message = new String(value);
                        } else {
                            response_message += new String(value);
                        }
                        if (value[value.length - 1] == Constants.ETX) {
                            if (ValidChecksum(response_message)) {
                                flag_GotResponse = true;
                                CommonUtils.LogMessage(TAG, "VR_interface (onCharacteristicChanged): Got good response: " + response_message + "\n", null);
                                Proccess_all_response(response_message);
                            } else {
                                CommonUtils.LogMessage(TAG, "VR_interface (onCharacteristicChanged): Got bad response: " + response_message + "\n", null);
                                Log.w(TAG, "Bad Checksum");
                            }
                        }

//                        Proccess_all_response(response_message);

                    } catch (Exception ex) {
                        Log.w(TAG, "Read value: null");
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.w(TAG, "onCharacteristicWrite received: " + status + characteristic);


                    byte[] value = characteristic.getValue();
                    try {
                        Log.w(TAG, "Read value:" + new String(value));
                    } catch (Exception ex) {
                        Log.w(TAG, "Read value: null");
                        ex.printStackTrace();
                    }
                }
            };
    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static void doRestart(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    //SW 12/28: Copied over useful helpers from the DisplayMeterActivity
    @Override
    public void onConnected(Bundle bundle) { //required, left blank
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {//required, left blank
    }

    @Override
    public void onConnectionSuspended(int i) {//required, left blank

    }

    private void TransactionCompleteFunction(String type, HashMap<String, String> CurrentTank) {


        try {

            //put the information into a package
            Gson gson = new Gson();
            String jsonData = "";

            String site_Id = "";

            String userEmail = Login_Email;

            String authString = "";

            if (type.equals("inventory")) // send different information for each type of tank poll
            {
                authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveInventoryVeederTankMonitorReading");


                VR_Inventory_InfoEntity authEntityClass = new VR_Inventory_InfoEntity();

                authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(VR_interface.this);
                authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(VR_interface.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                authEntityClass.VRDateTime = FormatDate(CurrentTank.get("DateTime"));

                authEntityClass.TankNumber = (CurrentTank.get("TankNumber")); //I should perhaps check that this is the same tank we asked for
                authEntityClass.ProductCode = CurrentTank.get("ProductCode");
                authEntityClass.TankStatus = CurrentTank.get("TankStatus");

                Long i = Long.parseLong(CurrentTank.get("Volume"), 16);  //convert from hex stored as string
                double dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Volume = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("TCVolume"), 16);  //Temperature controlled volume
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.TCVolume = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("Ullage"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Ullage = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("Height"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Height = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("Water"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Water = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("Temperature"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Temperature = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("WaterVolume"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.WaterVolume = Double.toString(dbl);

                jsonData = gson.toJson(authEntityClass);
                System.out.println("AP_FS33 VR_Inventory_InfoEntity......" + jsonData);

            } else if (type.equals("delivery")) {

                String currentTankNumber = CurrentTank.get("TankNumber");

                CommonUtils.LogMessage("VR_INTERFACE", "- VR_Delivery_InfoEntity currentTankNumber " + currentTankNumber, null);
                /*
                SplashActivity obj = new SplashActivity();
                boolean receivedelivery = false;

                for (int k = 0; k <= obj.TankList.size(); k++) {
                    HashMap<String, String> z = obj.TankList.get(k);
                    String hmTankNumber = z.get("TankNumber");
                    String hmReceiveDeliveryInformation = z.get("ReceiveDeliveryInformation");
                    if (currentTankNumber.equalsIgnoreCase(hmTankNumber) && hmReceiveDeliveryInformation.equalsIgnoreCase("True")) {
                        receivedelivery = true;
                        break;
                    }

                }*/

                //receivedelivery ==
                if (true) {

                    authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveDeliveryVeederTankMonitorReading");

                    VR_Delivery_InfoEntity authEntityClass = new VR_Delivery_InfoEntity();

                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(VR_interface.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                    authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(VR_interface.this);
                    authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                    authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                    authEntityClass.VRDateTime = FormatDate(CurrentTank.get("DateTime"));
                    authEntityClass.TankNumber = (CurrentTank.get("TankNumber"));
                    authEntityClass.ProductCode = CurrentTank.get("ProductCode");
                    authEntityClass.StartDateTime = FormatDate(CurrentTank.get("StartDateTime"));
                    authEntityClass.EndDateTime = FormatDate(CurrentTank.get("EndDateTime"));

                    Long i = Long.parseLong(CurrentTank.get("StartVolume"), 16);  //convert from hex stored as string
                    double dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.StartVolume = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("StartTCVolume"), 16);  //Temperature controlled volume
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.StartTCVolume = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("StartHeight"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.StartHeight = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("StartWater"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.StartWater = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("StartTemp"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.StartTemp = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("EndVolume"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.EndVolume = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("EndTCVolume"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.EndTCVolume = Double.toString(dbl);


                    i = Long.parseLong(CurrentTank.get("EndHeight"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.EndHeight = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("EndWater"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.EndWater = Double.toString(dbl);

                    i = Long.parseLong(CurrentTank.get("EndTemp"), 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    authEntityClass.EndTemp = Double.toString(dbl);

                    jsonData = gson.toJson(authEntityClass);

                    System.out.println("AP_FS33 VR_Delivery_InfoEntity......" + jsonData);
                    CommonUtils.LogMessage("VR_INTERFACE", "- VR_Delivery_InfoEntity: " + jsonData, null);
                } else {

                    CommonUtils.LogMessage("VR_INTERFACE", "- DELIVERY SKIPPED  ", null);
                }


            } else if (type.equals("leak")) {

                authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveLeakVeederTankMonitorReading");

                VR_Leak_InfoEntity authEntityClass = new VR_Leak_InfoEntity();
                authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(VR_interface.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(VR_interface.this);
                authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                authEntityClass.VRDateTime = FormatDate(CurrentTank.get("DateTime"));
                authEntityClass.TankNumber = (CurrentTank.get("TankNumber"));
                authEntityClass.ProductCode = CurrentTank.get("ProductCode");
                authEntityClass.StartDateTime = FormatDate(CurrentTank.get("StartDateTime"));

                Long i = Long.parseLong(CurrentTank.get("Start_Vol"), 16);  //convert from hex stored as string
                double dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Start_Vol = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("Start_Temp"), 16);  //Temperature controlled volume
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.Start_Temp = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("End_Temp"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.End_Temp = Double.toString(dbl);

                i = Long.parseLong(CurrentTank.get("End_rate"), 16);
                dbl = Float.intBitsToFloat(i.intValue());
                authEntityClass.End_rate = Double.toString(dbl);

                int in = Integer.parseInt(CurrentTank.get("Hours"), 16);
                authEntityClass.Hours = Integer.toString(in);

                String[] extras_list = CurrentTank.get("Hours").split(",");
                String Extras_final = "";

                for (String entry : extras_list) {
                    i = Long.parseLong(entry, 16);
                    dbl = Float.intBitsToFloat(i.intValue());
                    Extras_final = Extras_final + "," + dbl;
                }
                authEntityClass.Extras = Extras_final;


                jsonData = gson.toJson(authEntityClass);
                System.out.println("AP_FS33 VR_Leak_InfoEntity......" + jsonData);
            } else if (type.equals("alarm")) {
                String[] auths = TransactionCompleteFunction_alarm(CurrentTank);
                jsonData = auths[0];
                authString = auths[1];

                System.out.println("AP_FS33 VR_Alarm_InfoEntity......" + jsonData);
            }


            CommonUtils.LogMessage(TAG, "VR_interface: sending data: " + jsonData + "\n", null);
            //put it into the correct format


            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);

            //compare against local database for repeats
            boolean isInsert = true;
            ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
            if (alltranz != null && alltranz.size() > 0) {

                for (int i = 0; i < alltranz.size(); i++) {
                    if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                        isInsert = false;
                        break;
                    }
                }
            }

            //If now a repeat, put it into the database
            if (isInsert) {
                controller.insertTransactions(imap);
            }
            //temporary, to clear the junk
//            controller.delete_all_transactions();
            //settransaction to FSUNIT
            //==========================


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                }
            }, 1500);

            //==========================
            //SW 1/2: don't zero out fields: they should remain constant
            /*Constants.AccVehicleNumber = "";
            Constants.AccOdoMeter = 0;
            Constants.AccDepartmentNumber = "";
            Constants.AccPersonnelPIN = "";
            Constants.AccOther = "";
            */


        } catch (Exception ex) {

            CommonUtils.LogMessage("APFS33", "AuthTestAsyncTask ", ex);
            ex.printStackTrace();
        }


        boolean isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;


        //btnStop.setVisibility(View.GONE);
        String consoleString = "";
        //tvConsole.setText("");

/*
        if (AppConstants.NeedToRename) {
            //SW 1/2 Commnted out bits I don't need now
            String userEmail = Login_Email;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.R_SITE_ID;
            rhose.HoseId = AppConstants.R_HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(this, AppConstants.NeedToRename, jsonData, authString);

        }*/

        //start a background service to send the information to the cloud
        startService(new Intent(this, BackgroundService.class));

        //linearFuelAnother.setVisibility(View.VISIBLE);


    }

    private static String make_VR_command2Demo(String Command_code, int TankNo) { //computer code response
        DecimalFormat myFormatter = new DecimalFormat("00");
        String TankString = myFormatter.format(TankNo);
        String SecurityCode = "";
        String Command_String = (char) Constants.SOH + "i" + SecurityCode + Command_code + TankString; // "i"  +
        System.out.println("VRDemo sending command " + Command_String);
        if (Constants.deguglog)
            CommonUtils.LogMessage(TAG, "VRDemo sending command " + Command_String);
        return Command_String;
    }

    //This function makes a valid input string for the VR device
    private String make_VR_command(String Command_code, int TankNo) {   //display code response
        DecimalFormat myFormatter = new DecimalFormat("00");
        String TankString = myFormatter.format(TankNo);
        String SecurityCode = "";
        String Command_String = (char) Constants.SOH + SecurityCode + Command_code + TankString; // "i"  +
        System.out.println("sending command " + Command_String);// 0x01 + 202 + 0
        CommonUtils.LogMessage(TAG, "sending command " + Command_String);
        return Command_String;
    }

    private String make_VR_command2(String Command_code, int TankNo) { //computer code response
        DecimalFormat myFormatter = new DecimalFormat("00");
        String TankString = myFormatter.format(TankNo);
        String SecurityCode = "";
        String Command_String = (char) Constants.SOH + "i" + SecurityCode + Command_code + TankString; // "i"  +
        System.out.println("sending command " + Command_String);
        CommonUtils.LogMessage(TAG, "sending command " + Command_String);
        return Command_String;
    }

    private String make_VR_command3(String Command_code, int TankNo) {  //bad
        DecimalFormat myFormatter = new DecimalFormat("00");
        String TankString = myFormatter.format(TankNo);
        String SecurityCode = "";
        String Command_String = "i" + SecurityCode + Command_code + TankString; // "i"  +
        System.out.println("sending command " + Command_String);
        CommonUtils.LogMessage(TAG, "sending command " + Command_String);
        return Command_String;
    }

    private String make_VR_command4(String Command_code, int TankNo) { //bad
        DecimalFormat myFormatter = new DecimalFormat("00");
        String TankString = myFormatter.format(TankNo);
        String SecurityCode = "";
        String Command_String = SecurityCode + Command_code + TankString; // "i"  +
        System.out.println("sending command " + Command_String);
        CommonUtils.LogMessage(TAG, "sending command " + Command_String);
        return Command_String;
    }

    //This function parses the response from the VR device
    //It assumes the SOH and ETX have already been handled
    //It returns a ArrayList<HashMap<String, String>>
    //Each element of the list is one tank
    //The HashMap stores the type of data and the value
    //As each respnose is differnt, only the follwing codes are implemented
    // 201 (Tank inventory) 202 (Tank delivery)
    //see file://trakhqfile/public/Shakil/Tutorial/TrakEng/Documentation/Veeder-root Control Commands.pdf
    //for formatting information
    //Note that some entries are still in hex when returned here
    //This includes Volume!
    private ArrayList<HashMap<String, String>> parse_VR_response(String responseString) {
        String CommandCode = responseString.substring(0, 4);
        int response_length = responseString.length();
        switch (CommandCode) {
            case "i" + Constants.VR_Inventory_Code: //It is sending the inventory report
                ArrayList<HashMap<String, String>> Final_Response = new ArrayList<HashMap<String, String>>();
                String TankNumber = responseString.substring(4, 6);  //This is the numbers queried: tthe details are in the later block
                String DateTime = responseString.substring(6, 16);

                CommonUtils.LogMessage(TAG, "inv-" + DateTime, null);

                int offset = 16;  //keeps track of the number of tanks it is querying.
                //there may be several entries, so go through them all.
                while (offset < response_length - 10) {  //The -10 here is simply to avoid off by one errors in counting the length.
                    // There are 7 charachters at the end that we do not use (2 end message, 4 checksum, one ETX
                    HashMap<String, String> Responses = new HashMap<String, String>();
                    Responses.put("DateTime", DateTime);
                    Responses.put("TankNumber", responseString.substring(offset, offset + 2));
                    Responses.put("ProductCode", responseString.substring(offset + 2, offset + 3));
                    Responses.put("TankStatus", responseString.substring(offset + 3, offset + 7)); //we probably can throw this out
                    int num_fields = Integer.parseInt(responseString.substring(offset + 7, offset + 9), 16); //Not all the following must be presnet-this fild tells us how much to expect
                    if (num_fields >= 1)
                        Responses.put("Volume", responseString.substring(offset + 9, offset + 17)); //this gives the hex data as a string: will ned to convert to float later
                    if (num_fields >= 2)
                        Responses.put("TCVolume", responseString.substring(offset + 17, offset + 25));
                    if (num_fields >= 3)
                        Responses.put("Ullage", responseString.substring(offset + 25, offset + 33));
                    if (num_fields >= 4)
                        Responses.put("Height", responseString.substring(offset + 33, offset + 41));
                    if (num_fields >= 5)
                        Responses.put("Water", responseString.substring(offset + 41, offset + 49));
                    if (num_fields >= 6)
                        Responses.put("Temperature", responseString.substring(offset + 49, offset + 57));
                    if (num_fields >= 7)
                        Responses.put("WaterVolume", responseString.substring(offset + 57, offset + 65));
                    Final_Response.add(Responses);
                    offset += 9 + 8 * num_fields;
                }
                //The data termination bits don't need to be used.
                //I could look at the checksum, but I will ignore it for now.
                //Also ignore ETX
                return Final_Response;
            //break;
            case "i" + Constants.VR_Delivery_Code: //It is sending the inventory report
                ArrayList<HashMap<String, String>> Final_Response_dc = new ArrayList<HashMap<String, String>>();
                String TankNumber_dc = responseString.substring(4, 6);  //This is the numbers queried: tthe details are in the later block
                String DateTime_dc = responseString.substring(6, 16);//This is the time of query: tthe details are in the later block
                CommonUtils.LogMessage(TAG, "del-" + DateTime_dc, null);

                int offset_dc = 16;  //keeps track of the number of tanks it is querying.
                //there may be several entries, so go through them all.
                while (offset_dc < response_length - 10) {  //The -10 here is simply to avoid off by one errors in counting the length.
                    // There are 7 charachters at the end that we do not use (2 end message, 4 checksum, one ETX


                    String Tank_num = responseString.substring(offset_dc, offset_dc + 2);
                    String Prod_Code = responseString.substring(offset_dc + 2, offset_dc + 3);
                    int deliverynum = Integer.parseInt(responseString.substring(offset_dc + 3, offset_dc + 5)); //how many times to repeat the following
                    offset_dc = offset_dc + 5;

                    for (int d_num = 0; d_num < deliverynum; d_num++) {
                        HashMap<String, String> Responses = new HashMap<String, String>();
                        Responses.put("DateTime", DateTime_dc);
                        Responses.put("TankNumber", Tank_num);
                        Responses.put("ProductCode", Prod_Code);

                        Responses.put("StartDateTime", responseString.substring(offset_dc, offset_dc + 10));
                        Responses.put("EndDateTime", responseString.substring(offset_dc + 10, offset_dc + 20));
                        int num_fields = Integer.parseInt(responseString.substring(offset_dc + 20, offset_dc + 22), 16); //Not all the following must be presnet-this fild tells us how much to expect
                        if (num_fields >= 1)
                            Responses.put("StartVolume", responseString.substring(offset_dc + 22, offset_dc + 30)); //this gives the hex data as a string: will ned to convert to float later
                        if (num_fields >= 2)
                            Responses.put("StartTCVolume", responseString.substring(offset_dc + 30, offset_dc + 38));
                        if (num_fields >= 3)
                            Responses.put("StartWater", responseString.substring(offset_dc + 38, offset_dc + 46));
                        if (num_fields >= 4)
                            Responses.put("StartTemp", responseString.substring(offset_dc + 46, offset_dc + 54));
                        if (num_fields >= 5)
                            Responses.put("EndVolume", responseString.substring(offset_dc + 54, offset_dc + 62));
                        if (num_fields >= 6)
                            Responses.put("EndTCVolume", responseString.substring(offset_dc + 62, offset_dc + 70));
                        if (num_fields >= 7)
                            Responses.put("EndWater", responseString.substring(offset_dc + 70, offset_dc + 78));
                        if (num_fields >= 8)
                            Responses.put("EndTemp", responseString.substring(offset_dc + 78, offset_dc + 86));
                        if (num_fields >= 9)
                            Responses.put("StartHeight", responseString.substring(offset_dc + 86, offset_dc + 94));
                        if (num_fields >= 10)
                            Responses.put("EndHeight", responseString.substring(offset_dc + 94, offset_dc + 102));
                        Final_Response_dc.add(Responses);
                        offset_dc += 22 + 8 * num_fields;
                    }

                }
                //The data termination bits don't need to be used.
                //I could look at the checksum, but I will ignore it for now.'
                //Also ignore ETX
                return Final_Response_dc;


            case "i" + Constants.VR_Leak_Code: //It is sending the leak report
                ArrayList<HashMap<String, String>> Final_Response_lc = new ArrayList<HashMap<String, String>>();
                String TankNumber_lc = responseString.substring(4, 6);  //This is the numbers queried: tthe details are in the later block
                String DateTime_lc = responseString.substring(6, 16);//This is the time of query: tthe details are in the later block
                CommonUtils.LogMessage(TAG, "leak-" + DateTime_lc, null);


                int offset_lc = 16;  //keeps track of the number of tanks it is querying.
                //there may be several entries, so go through them all.
                while (offset_lc < response_length - 10) {  //The -10 here is simply to avoid off by one errors in counting the length.
                    // There are 7 charachters at the end that we do not use (2 end message, 4 checksum, one ETX
                    HashMap<String, String> Responses = new HashMap<String, String>();
                    Responses.put("DateTime", DateTime_lc);
                    String tn = responseString.substring(offset_lc, offset_lc + 2);
                    Responses.put("TankNumber", responseString.substring(offset_lc, offset_lc + 2));
                    String pc = responseString.substring(offset_lc + 2, offset_lc + 3);
                    Responses.put("ProductCode", responseString.substring(offset_lc + 2, offset_lc + 3));
                    String sdt = responseString.substring(offset_lc + 3, offset_lc + 13);
                    Responses.put("StartDateTime", responseString.substring(offset_lc + 3, offset_lc + 13));
                    String hr = responseString.substring(offset_lc + 13, offset_lc + 15);
                    Responses.put("Hours", responseString.substring(offset_lc + 13, offset_lc + 15));

                    int num_fields = Integer.parseInt(responseString.substring(offset_lc + 15, offset_lc + 17), 16); //Not all the following must be presnet-this fild tells us how much to expect
                    if (num_fields >= 1)
                        Responses.put("Start_Temp", responseString.substring(offset_lc + 17, offset_lc + 25)); //this gives the hex data as a string: will ned to convert to float later
                    if (num_fields >= 2)
                        Responses.put("End_Temp", responseString.substring(offset_lc + 25, offset_lc + 33));
                    if (num_fields >= 3)
                        Responses.put("Start_Vol", responseString.substring(offset_lc + 33, offset_lc + 41));
                    if (num_fields >= 4)
                        Responses.put("End_rate", responseString.substring(offset_lc + 41, offset_lc + 49));

                    if (num_fields >= 5) {

                        String Extra_string = "";
                        try {
                            for (int extra = 0; extra <= num_fields - 5; extra++) {
                                Extra_string = Extra_string + "," + responseString.substring(offset_lc + 49 + extra * 8, offset_lc + 57 + extra * 8);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Responses.put("Extras", Extra_string);

                    }
                    Final_Response_lc.add(Responses);
                    offset_lc += 17 + 8 * num_fields;
                }
                //The data termination bits don't need to be used.
                //I could look at the checksum, but I will ignore it for now.
                //Also ignore ETX
                return Final_Response_lc;
            //break;
            default:
                //we should have proper error handling
                System.out.println("Code not recognized " + CommandCode);
        }
        return null; //we should only get here if we hit the error case
    }

    public void Proccess_all_response(String responseString) {
        CommonUtils.LogMessage(TAG, "VR_interface Response: " + responseString, null);

        String CommandCode = responseString.substring(1, 5);//he first is SOH: ignore it
        switch (CommandCode) {
            case "i" + Constants.VR_Inventory_Code: //It is sending the inventory report
                process_VR_Inventory(responseString);
                break;
            case "i" + Constants.VR_Delivery_Code:
                process_VR_Delivery(responseString);
                break;
            case "i" + Constants.VR_Leak_Code:
                process_VR_Leak(responseString);
                break;
            case "i" + Constants.VR_Alarm_Code:
                process_VR_Alarm(responseString);
                break;
            default:
                System.out.println("Code not recognized " + CommandCode);
        }
    }


    //This function takes the response from the VR unit, executes the parsing funciton, and then sets the transaction field needed in TransactionCompleteFunction
    //There's not a one-to-one correspondence between the two: most significantly, the response can be from multiple tanks
    //This processes the results of the inventory command
    private void process_VR_Inventory(String responseString) {
        try {
            System.out.println("got response " + responseString);
            ArrayList<HashMap<String, String>> Tank_Data = parse_VR_response(responseString.substring(1)); //he first is SOH: ignore it
            System.out.println("parsed VR_response as " + Tank_Data);
            //get and send tank information for each tank
            //complicated expression here should always resolve to Tank_Data.size(), but will check for nullity to avoid giving a warning
            for (int tank_no = 0; tank_no < (Tank_Data != null ? Tank_Data.size() : 0); tank_no++) {
                try {
                    HashMap<String, String> CurrentTank = Tank_Data.get(tank_no);
                    //convert from hex to float- go through long
                    Long i = Long.parseLong(CurrentTank.get("TCVolume"), 16);  //Temperature controlled volume
                    TankLevel = Float.intBitsToFloat(i.intValue());
                    DateTime = CurrentTank.get("DateTime");
                    TankNumber = Integer.parseInt(CurrentTank.get("TankNumber")); //I should perhaps check that this is the same tank we asked for

                    //set constants so the UI can be updated
                    Constants.TankLevel.set(tank_no, TankLevel);
                    Constants.TankNumbers.set(tank_no, TankNumber);

                    System.out.println("Sending tank " + TankNumber + " at level " + TankLevel + "at time" + DateTime);
                    //Does the storage and upload
                    TransactionCompleteFunction("inventory", CurrentTank);
                    // wait between each upload to make sure collisions don't occur
                    Thread.sleep(10);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            SendToast("Got inventory response", true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //This function takes the response from the VR unit, executes the parsing funciton, and then sets the transaction field needed in TransactionCompleteFunction
    //There's not a one-to-one correspondence between the two: most significantly, the response can be from multiple tanks
    //This processes the results of the delivery command
    private void process_VR_Delivery(String responseString) {
        try {
            System.out.println("got response " + responseString);
            ArrayList<HashMap<String, String>> Tank_Data = parse_VR_response(responseString.substring(1)); //he first is SOH: ignore it
            System.out.println("parsed VR_response as " + Tank_Data);
            //get and send tank information for each tank
            //complicated expression here should always resolve to Tank_Data.size(), but will check for nullity to avoid giving a warning
            for (int deliver_no = 0; deliver_no < (Tank_Data != null ? Tank_Data.size() : 0); deliver_no++) { //It gives the last few deliveries, so we will pick up duplicates. The TransactionComplete checks for duplicates, so we don't have to worry about it.
                try {
                    HashMap<String, String> CurrentTank = Tank_Data.get(deliver_no);
                    //convert from hex to float- go through long
                    Long i = Long.parseLong(CurrentTank.get("StartTCVolume"), 16);  //Temperature controlled volume
                    float startLevel = Float.intBitsToFloat(i.intValue());
                    Long j = Long.parseLong(CurrentTank.get("EndTCVolume"), 16);  //Temperature controlled volume
                    float endLevel = Float.intBitsToFloat(j.intValue());
                    double deliveryAmount = endLevel - startLevel;

                    DateTime = CurrentTank.get("DateTime");
                    TankNumber = Integer.parseInt(CurrentTank.get("TankNumber")); //I should perhaps check that this is the same tank we asked for

                    //Does the storage and upload
                    System.out.println("Sending deliver: change of " + deliveryAmount + " at level " + TankLevel + "at time" + DateTime);
                    TransactionCompleteFunction("delivery", CurrentTank);
                    // wait between each upload to make sure collisions don't occur
                    Thread.sleep(10);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            SendToast("Got Delivery response", true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CommonUtils.LogMessage(TAG, "VR_interface started."); // #2238
        //run when the class is launched.
        Do_next_VR_interface_Service();

        //hub466 delivery
        //Proccess_all_response("\u0001i20200210426000101110210423092421042309330A45902000458F8000000000004284095D45AFD00045AF080000000000428428CA425077884276515B210409083521040908480A459C9800459C20000000000042804BA645D8D00045D80800000000004281F46A425F18A242975400210402070121040207120A458190004581380000000000427EA40545BDA00045BD300000000000427C8AA9423FA9F54283EFD3210326093821032609520A452080004520200000000000427CFD2245A6D80045A6800000000000427A9A1D42068A87426B54C9210305125221030513060A456D8000456D60000000000042725A7445CE000045CDD800000000004273E3DD42334403428F25EE210226124221022612530A4510900045108000000000004270F130459EE800459F080000000000426B76BD41F9C75B4261D690210220101621022010220A4549600045493000000000004273CD964569A0004569700000000000427303A1421E8ABC423108EF210212081121021208230A457670004575D00000000000427CFA0645D1D80045D1E80000000000426D79E0423859124291F2B1210205075221020508000A458F8800458F300000000000427DCA3545AF280045AEC00000000000427CADF1424FCAF6427580CC210129080521012908170A4547E0004547400000000000428004EE45BC500045BC08000000000042781DAE421DA90242831670&&28E2\u0003");


        if (Constants.DEBUG) {
//            Calendar cal = Calendar.getInstance();
//            System.out.println("started VR_interface " + cal.toString());
//
//            System.out.println("VR send inventory " +make_VR_command(Constants.VR_Inventory_Code, TankNumber) );
//
//            System.out.println("parse_VR_response " +  parse_VR_response("i2010118011114010110000083f80000040000000404000004080000040a0000040c0000040e0000041000000&&aaaa"));//Constants.SOH+""+Constants.ETX) );
//            //System.out.println("parse_VR_response " +  parse_VR_response("i2010118011114010110000083f80000040000000404000004080000040a0000040c0000040e0000041000000&&aaaa"));//Constants.SOH+""+Constants.ETX) );
//            System.out.println("tank before: "+TankLevel);
            //process_VR_Inventory(Constants.SOH+"i2010118011114010110000083f80000040000000404000004080000040a0000040c0000040e0000041000000&&aaaa"+ Constants.ETX);
            //System.out.println("tank after: "+TankLevel);
//            TestChecksums();
//            Proccess_all_response("\u0001i20200190916132701110190912172719091218100A460DD400460B5C003F33EFFE42A986D446894C004686DE003F33845A42AA7961425ECBE842C55660190826132519082614070A45F7380045F308003F336B8242A868F4467FE000467B20003F34111A42ACFE034248A2E942B75D8B190809115119080912280A45F7080045F310003F334A6642A5AD60467EA400467A80003F34111A42A655894248829842B67CCB190729072019072907570A45CFC00045CCB8003F33421D42A1A313466C440046691C003F32A4CF429DFCBD4230383B42A9EB65190710094019071010220A45E8900045E5B0003F334A66429B595B46779000467494003F334A66429A549C423FA23142B18CD7190620153919062016110A45EE280045EBF8003F33421D4291D416467C0800467948003F32D67E4297153C424311A342B4A7CE190603151419060315480A45ECF00045EB68003F3352B04289EA5E467B90004679D0003F32CE35428BCA8C4242508442B45570190514071019051407460A460848004607C0003F33EFFE428307CA46867200468644003F33318F42779C24425812EC42C0ED79190423123719042313180A4607E8004607A4003F3429F2427ADF3A46841200468404003F32180F42723B8442579FEA42BD5B65190403094519040310220A4600AC004600B4003F32C5EB426E7F694682BC00468300003F3283B34263F9A2424ED20342BB64F502210181024101118102410460A45F4F00045F370003F8B744C42933AB346706C00466F9C003F8B702842870C2E42473CD542ACB38F181011093818101110110A460DCC00460C78003F8ABA2842A12DED4681DC004680F2003F8A7C1C42972492425EC22942BA1F7D181002111718100211540A4601500045FFF0003F8A6FB342A5C61A46776400467518003F8A253E42A11F90424F9A3042B16F6B180920133618092014100A460824004606A4003F892D0E42A8FBB4467C4400467978003F8A296142A913664257E9D742B4D1E6180911174218091118160A4603C400460244003F892D0E42AA198C467980004676B8003F892D0E42A98BE64252956942B2E51E180905115018090512220A45F4600045F180003F8834DE42ABAD3A466FD000466D20003F8907D542A9CFF14246E23F42AC4AF2180824110918082411420A460B74004609E0003F87E22442AA70AA4680B200467E6C003F88390242AB1AF2425BEE4342B871CE180814114018081412120A46195C004617B0003F87D5BA42A80EE64688280046869E003F8834DE42AA27F4426CBEE442C38F42180806094718080610220A461588004613E0003F88203042A8F55E46832A004681B2003F873CAF42A9B17742681F4142BC05E1180727074318072708280A460DC800460C38003F84B34442A8B724467E8C00467BB4003F84F99542A9816A425EBE8442B66C2803310190910165719091017310A45D6980045D3D000BC52E58042B1584146613000465E0000BC52E58042B6ED0C42347D5A42A2AC4D190829163819082917120A45E9F00045E6F800BC290D0042B05272466C6C004668E800BC290D0042B9E6F34240798C42AA0577190820100219082010400A4603440046019C00BC22130042AFD09D4678DC0046759000BBFE660042B2AC3C4251F85542B27268190812112719081212020A45F4100045F10000BBFE660042AF4BA746719800466E1000BBA60A0042B899514246B25D42AD7C4A190801162319080116570A4600C00045FE3000BBA60A0042B096834677A40046738400BB80D90042C1D1F4424EE86C42B19BBC190729143819072915120A4584680045831000BB98190042A44BE84638580046359C00BB65CA0042B979AD41FE546F428923F4190715140419071514490A45FFF00045FD9800BB93720042A09F04467634004673A400BBA1660042A61300424DF65B42B0A018190709103719070911120A45B2100045B09800BB937200429CF4C2464EDC00464CF400BB93720042A0DE39421D6415429708F4190701094219070110230A45BC280045BA9000BB937200429CC30246545C0046529800BBAAB200429CED254223D59A429A7C55190621114419062112300A45CC580045CAE000BB93720042982E4C465B040046593400BB8ECA00429CAC82422E15A8429EB5B70440005510190211221619021123090A44BB200044BB200000000000426ADD0045C9300045CA500000000000423F9E6D41C6FD4542937D29180502134718050215080A450B0000450A9000000000004285CF5C45E2D80045E0F80000000000429A8B514203AA4E42A3BB40170712105317071211080A45D3800045D120000000000042A758EA45F1000045EE48000000000042A7B66A4299F0CA42AD13B0170712102217071210500A4592E800459178000000000042A0DB0C45D3880045D128000000000042A73B184264F0204299F29B170710095517071010110A456C60004569F0000000000042A2D675459368004591E8000000000042A322F04241F64E42658EEA170710092517071009540A44E4600044E2800000000000429BC7BF456C6000456A00000000000042A2B8D941E500794241FBF3161012090316101209360A45A7F80045A6B000000000004297B53C45E7100045E568000000000042962B79427E63C842A6795D161011110416101111210A458B4000458A400000000000429636EC45A8A80045A78000000000004294F91A425BAE2A427F3B38161011102916101111010A451590004514300000000000429E9B80458B4800458A4000000000004296F681420ABFDA425BB57A160119154916011916030A45D0900045D120000000000042595E2345E9C80045EA6000000000004259DB18429819AA42A843A6&&953B\u0003");
//            Proccess_all_response("\u0001i20100200410001201100020744DB800044DAE00045072000420341ED0000000042831DB700000000&&EF32\u0003");
            Proccess_all_response("\u0001i20100200722130701100000745C9B80045C77000454C9000426601970000000042AA22D90000000002200000745433000453F80004446C0004253BD290000000042ACFAFD00000000&&E1D1\u0003");


        }
        //for all tanks
        //AppConstants.DetailsListOfConnectedDevices

        //query a tank
        // According to file://trakhqfile/public/Shakil/Tutorial/TrakEng/Documentation/Veeder-root%20Control%20Commands.pdf
        //Function 201 will get the level information
        //Function 202 will get delivery information
        try {
            //Queries the tank and gets the result as an async task.
            //it also calls TransactionCompleteFunction(); for each tank
            //new IP_VR_connect().execute();

            if (!AppConstants.RunningPoll) {
                AppConstants.RunningPoll = true;

                CommonUtils.LogMessage(TAG, "VR_interface: Starting poll + wait_time");
                Log.w(TAG, "Starting poll");
                new USB_VR_connect2().execute(); // tempararily commente out to do a long-term test.
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return Service.START_STICKY;
    }

    private void StartBTConnect() {
        flag_GotServices = false;
        String mBTname = AppConstants.BLUETOOTH_VR_CONNECT_NAME;
        String mMac = AppConstants.VR_MAC;
        try {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            BluetoothDevice mmDevice = mBluetoothAdapter.getRemoteDevice(mMac);
            mBluetoothGatt = mmDevice.connectGatt(this, true, mGattCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void SetNotify() {
        try {
            BluetoothGattService IOService = mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic IOChar = IOService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
            Log.w(TAG, "SetNotification: " + mBluetoothGatt.setCharacteristicNotification(IOChar, true));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void WriteVal(String message) {
        try {
            flag_GotResponse = false;
            //hard coded values for the BT to RS232 chip
            BluetoothGattService IOService = mBluetoothGatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic IOChar = IOService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
            IOChar.setValue(message);
            Log.w(TAG, "writing: " + mBluetoothGatt.writeCharacteristic(IOChar));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean ValidChecksum(String response_message)
    //determines if the data has been received correctly
    {
        try {
            //checksum1
            int checksum1 = 0;
            int stringsum = 0;
            int mlen = response_message.length();
            System.out.println("fullmessgae" + response_message);


            String checksumDigits = response_message.substring(mlen - 5, mlen - 1);
            System.out.println("checksumDigits" + checksumDigits);

            // Add up values of the last four characters of the string
            checksum1 += Integer.parseInt(response_message.substring(mlen - 2, mlen - 1), 16);
            checksum1 += Integer.parseInt(response_message.substring(mlen - 3, mlen - 2), 16) * 16;
            checksum1 += Integer.parseInt(response_message.substring(mlen - 4, mlen - 3), 16) * 256;
            checksum1 += Integer.parseInt(response_message.substring(mlen - 5, mlen - 4), 16) * 4096;
            System.out.println("checksum1 " + checksum1);
            // Add up the ascii values of each character of the string (except the checksum)
            for (int i = 1; i < (mlen - 5); i++) {
                stringsum += (int) response_message.charAt(i);
            }
            System.out.println("stringsum " + stringsum + "mod " + stringsum % 65536);
            // The two values must add up to 65536
            System.out.println("sum " + (checksum1 + stringsum + 1));
            return checksum1 + stringsum % 65536 + 1 == 65536;

            //checksum2: now known to be incorrect
            //response ends with 4 digit checksum then the ETX



       /* int i = Integer.parseInt(checksumDigits, 16);
//        System.out.println("i"+i);
        String binChecksum = Integer.toBinaryString(i);
        System.out.println("binChecksum" + binChecksum);

        String sumString = checksum(response_message.substring(0, response_message.length() - 5), 8);//sum the beginning of the message

        System.out.println("sumString" + sumString);
        int binsumI = Integer.parseInt(binChecksum, 2) + Integer.parseInt(sumString, 2);
//        System.out.println("binsumI"+binsumI);
        String binsumS = Integer.toBinaryString(binsumI);
//        System.out.println("binsumS"+binsumS);
        String binsumSend = binsumS.length() <= 16 ? binsumS : binsumS.substring(binsumS.length() - 16);
        System.out.println("binsumSend" + binsumSend);
        return !(binsumSend.contains("1"));//is it all 0s?:*/
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private String checksum(String inputString, int digitstokeep)
    // adds the binary representation of the inputString converted to the hex
    {
        int total = 0;
        for (int cindex = 0; cindex < inputString.length(); cindex++) {
            char c = inputString.charAt(cindex);
//            Integer a = Character.getNumericValue(c);
            String binary = Integer.toBinaryString(c);
            total += Integer.parseInt(binary, 2);
        }
        String strTotal = Integer.toBinaryString(total);
        return strTotal.substring(strTotal.length() - digitstokeep);

    }

    private void SendToast(final String message, final boolean success) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {
                if (success) {
                    AppConstants.colorToastBigFont(VR_interface.this.getApplicationContext(), message, Color.BLUE);
                } else {
                    AppConstants.colorToastBigFont(VR_interface.this.getApplicationContext(), message, Color.RED);
                }
            }
        });
    }

    private void TestChecksums() {
        //  ValidChecksum("1000504000101011110002397002348006970096830002211000572500774000722004396000331100043790053220064500661600044110005431006988006590049500009E48B");
        //  ValidChecksum("150111004191314002049006520419142801000900777040408170016210062304040911009674006470320095200115400530032010500093780061703011150001359004460301124600969300557021310500014630052802131148009816005880126181000170300462012619100100370057801100745002078005370110084401040500562122912550066950051612291304007107005171221132100155600478122114190097920056712051329001377005761205142800973500644221004241104002275006710424114900974500750032314180020860059603231504009661007060206104800868900592020611040099730059802011206002385004620201125100953600614122207570025930044812220840009902005031109075100243100613110908450099390069410050708001938007371005075400938100815082814540022880079208281536009741008930727065500249600778072707420098200082506220747002278008610622082700966100903330312291150002709004821229124401077100577062712000026870083906271306010764009410421140500271300683042114330062290075644080410102700217200675041011110097230075102071238002472005200207132000988900642121213170025880046612121357009986006211018125100234300718101813440095780085608241254002629007390824134800974500894063012540032180084406301355010415009400516101300256600680051610510099760073704211431002105006980421150100616800780911AC");
        //  ValidChecksum("1000503000101011110002860003007007140090240002211000590300802400731004112000331100044020053590066200657900044110005551007176006750047620009E4C5");
        //   ValidChecksum("1000502000101011110003373003774007300082570002211000615700842500740003711000331100044270053980069200654000044110005633007305007010046330009E4BC");
        //   ValidChecksum("1000501000101011110003707004287007420077440002211000625900858400748003552000331100044580054460071600649200044110005766007512007230044260009E499");
        //   ValidChecksum("1000430000101011110004103004908007460071230002211000655400903600751003100000331100045040055200072700641800044110005965007820007320041180009E4CE");
        //    ValidChecksum("1000429000101011110004208005074007490069570002211000658000907600753003060000331100045170055400073700639800044110005967007822007380041160009E494");
        //    ValidChecksum("1000428000101011110004328005265007510067660002211000665700919100753002945000331100045290055600073300637800044110005966007821007330041170009E492");
        //   ValidChecksum("1000427000101011110004814006040007510059910002211000676800935600753002780000331100045580056060072400633200044110006086008006007230039320009E4A7");
        //     ValidChecksum("i20200190916081901110190912172719091218100A460DD400460B5C003F33EFFE42A986D446894C004686DE003F33845A42AA7961425ECBE842C55660190826132519082614070A45F7380045F308003F336B8242A868F4467FE000467B20003F34111A42ACFE034248A2E942B75D8B190809115119080912280A45F7080045F310003F334A6642A5AD60467EA400467A80003F34111A42A655894248829842B67CCB190729072019072907570A45CFC00045CCB8003F33421D42A1A313466C440046691C003F32A4CF429DFCBD4230383B42A9EB65190710094019071010220A45E8900045E5B0003F334A66429B595B46779000467494003F334A66429A549C423FA23142B18CD7190620153919062016110A45EE280045EBF8003F33421D4291D416467C0800467948003F32D67E4297153C424311A342B4A7CE190603151419060315480A45ECF00045EB68003F3352B04289EA5E467B90004679D0003F32CE35428BCA8C4242508442B45570190514071019051407460A460848004607C0003F33EFFE428307CA46867200468644003F33318F42779C24425812EC42C0ED79190423123719042313180A4607E8004607A4003F3429F2427ADF3A46841200468404003F32180F42723B8442579FEA42BD5B65190403094519040310220A4600AC004600B4003F32C5EB426E7F694682BC00468300003F3283B34263F9A2424ED20342BB64F502210181024101118102410460A45F4F00045F370003F8B744C42933AB346706C00466F9C003F8B702842870C2E42473CD542ACB38F181011093818101110110A460DCC00460C78003F8ABA2842A12DED4681DC004680F2003F8A7C1C42972492425EC22942BA1F7D181002111718100211540A4601500045FFF0003F8A6FB342A5C61A46776400467518003F8A253E42A11F90424F9A3042B16F6B180920133618092014100A460824004606A4003F892D0E42A8FBB4467C4400467978003F8A296142A913664257E9D742B4D1E6180911174218091118160A4603C400460244003F892D0E42AA198C467980004676B8003F892D0E42A98BE64252956942B2E51E180905115018090512220A45F4600045F180003F8834DE42ABAD3A466FD000466D20003F8907D542A9CFF14246E23F42AC4AF2180824110918082411420A460B74004609E0003F87E22442AA70AA4680B200467E6C003F88390242AB1AF2425BEE4342B871CE180814114018081412120A46195C004617B0003F87D5BA42A80EE64688280046869E003F8834DE42AA27F4426CBEE442C38F42180806094718080610220A461588004613E0003F88203042A8F55E46832A004681B2003F873CAF42A9B17742681F4142BC05E1180727074318072708280A460DC800460C38003F84B34442A8B724467E8C00467BB4003F84F99542A9816A425EBE8442B66C2803310190910165719091017310A45D6980045D3D000BC52E58042B1584146613000465E0000BC52E58042B6ED0C42347D5A42A2AC4D190829163819082917120A45E9F00045E6F800BC290D0042B05272466C6C004668E800BC290D0042B9E6F34240798C42AA0577190820100219082010400A4603440046019C00BC22130042AFD09D4678DC0046759000BBFE660042B2AC3C4251F85542B27268190812112719081212020A45F4100045F10000BBFE660042AF4BA746719800466E1000BBA60A0042B899514246B25D42AD7C4A190801162319080116570A4600C00045FE3000BBA60A0042B096834677A40046738400BB80D90042C1D1F4424EE86C42B19BBC190729143819072915120A4584680045831000BB98190042A44BE84638580046359C00BB65CA0042B979AD41FE546F428923F4190715140419071514490A45FFF00045FD9800BB93720042A09F04467634004673A400BBA1660042A61300424DF65B42B0A018190709103719070911120A45B2100045B09800BB937200429CF4C2464EDC00464CF400BB93720042A0DE39421D6415429708F4190701094219070110230A45BC280045BA9000BB937200429CC30246545C0046529800BBAAB200429CED254223D59A429A7C55190621114419062112300A45CC580045CAE000BB93720042982E4C465B040046593400BB8ECA00429CAC82422E15A8429EB5B70440005510190211221619021123090A44BB200044BB200000000000426ADD0045C9300045CA500000000000423F9E6D41C6FD4542937D29180502134718050215080A450B0000450A9000000000004285CF5C45E2D80045E0F80000000000429A8B514203AA4E42A3BB40170712105317071211080A45D3800045D120000000000042A758EA45F1000045EE48000000000042A7B66A4299F0CA42AD13B0170712102217071210500A4592E800459178000000000042A0DB0C45D3880045D128000000000042A73B184264F0204299F29B170710095517071010110A456C60004569F0000000000042A2D675459368004591E8000000000042A322F04241F64E42658EEA170710092517071009540A44E4600044E2800000000000429BC7BF456C6000456A00000000000042A2B8D941E500794241FBF3161012090316101209360A45A7F80045A6B000000000004297B53C45E7100045E568000000000042962B79427E63C842A6795D161011110416101111210A458B4000458A400000000000429636EC45A8A80045A78000000000004294F91A425BAE2A427F3B38161011102916101111010A451590004514300000000000429E9B80458B4800458A4000000000004296F681420ABFDA425BB57A160119154916011916030A45D0900045D120000000000042595E2345E9C80045EA6000000000004259DB18429819AA42A843A6&&9536");
        //     ValidChecksum("i20200190916081901110190912172719091218100A460DD400460B5C003F33EFFE42A986D446894C004686DE003F33845A42AA7961425ECBE842C55660190826132519082614070A45F7380045F308003F336B8242A868F4467FE000467B20003F34111A42ACFE034248A2E942B75D8B190809115119080912280A45F7080045F310003F334A6642A5AD60467EA400467A80003F34111A42A655894248829842B67CCB190729072019072907570A45CFC00045CCB8003F33421D42A1A313466C440046691C003F32A4CF429DFCBD4230383B42A9EB65190710094019071010220A45E8900045E5B0003F334A66429B595B46779000467494003F334A66429A549C423FA23142B18CD7190620153919062016110A45EE280045EBF8003F33421D4291D416467C0800467948003F32D67E4297153C424311A342B4A7CE190603151419060315480A45ECF00045EB68003F3352B04289EA5E467B90004679D0003F32CE35428BCA8C4242508442B45570190514071019051407460A460848004607C0003F33EFFE428307CA46867200468644003F33318F42779C24425812EC42C0ED79190423123719042313180A4607E8004607A4003F3429F2427ADF3A46841200468404003F32180F42723B8442579FEA42BD5B65190403094519040310220A4600AC004600B4003F32C5EB426E7F694682BC00468300003F3283B34263F9A2424ED20342BB64F502210181024101118102410460A45F4F00045F370003F8B744C42933AB346706C00466F9C003F8B702842870C2E42473CD542ACB38F181011093818101110110A460DCC00460C78003F8ABA2842A12DED4681DC004680F2003F8A7C1C42972492425EC22942BA1F7D181002111718100211540A4601500045FFF0003F8A6FB342A5C61A46776400467518003F8A253E42A11F90424F9A3042B16F6B180920133618092014100A460824004606A4003F892D0E42A8FBB4467C4400467978003F8A296142A913664257E9D742B4D1E6180911174218091118160A4603C400460244003F892D0E42AA198C467980004676B8003F892D0E42A98BE64252956942B2E51E180905115018090512220A45F4600045F180003F8834DE42ABAD3A466FD000466D20003F8907D542A9CFF14246E23F42AC4AF2180824110918082411420A460B74004609E0003F87E22442AA70AA4680B200467E6C003F88390242AB1AF2425BEE4342B871CE180814114018081412120A46195C004617B0003F87D5BA42A80EE64688280046869E003F8834DE42AA27F4426CBEE442C38F42180806094718080610220A461588004613E0003F88203042A8F55E46832A004681B2003F873CAF42A9B17742681F4142BC05E1180727074318072708280A460DC800460C38003F84B34442A8B724467E8C00467BB4003F84F99542A9816A425EBE8442B66C2803310190910165719091017310A45D6980045D3D000BC52E58042B1584146613000465E0000BC52E58042B6ED0C42347D5A42A2AC4D190829163819082917120A45E9F00045E6F800BC290D0042B05272466C6C004668E800BC290D0042B9E6F34240798C42AA0577190820100219082010400A4603440046019C00BC22130042AFD09D4678DC0046759000BBFE660042B2AC3C4251F85542B27268190812112719081212020A45F4100045F10000BBFE660042AF4BA746719800466E1000BBA60A0042B899514246B25D42AD7C4A190801162319080116570A4600C00045FE3000BBA60A0042B096834677A40046738400BB80D90042C1D1F4424EE86C42B19BBC190729143819072915120A4584680045831000BB98190042A44BE84638580046359C00BB65CA0042B979AD41FE546F428923F4190715140419071514490A45FFF00045FD9800BB93720042A09F04467634004673A400BBA1660042A61300424DF65B42B0A018190709103719070911120A45B2100045B09800BB937200429CF4C2464EDC00464CF400BB93720042A0DE39421D6415429708F4190701094219070110230A45BC280045BA9000BB937200429CC30246545C0046529800BBAAB200429CED254223D59A429A7C55190621114419062112300A45CC580045CAE000BB93720042982E4C465B040046593400BB8ECA00429CAC82422E15A8429EB5B70440005510190211221619021123090A44BB200044BB200000000000426ADD0045C9300045CA500000000000423F9E6D41C6FD4542937D29180502134718050215080A450B0000450A9000000000004285CF5C45E2D80045E0F80000000000429A8B514203AA4E42A3BB40170712105317071211080A45D3800045D120000000000042A758EA45F1000045EE48000000000042A7B66A4299F0CA42AD13B0170712102217071210500A4592E800459178000000000042A0DB0C45D3880045D128000000000042A73B184264F0204299F29B170710095517071010110A456C60004569F0000000000042A2D675459368004591E8000000000042A322F04241F64E42658EEA170710092517071009540A44E4600044E2800000000000429BC7BF456C6000456A00000000000042A2B8D941E500794241FBF3161012090316101209360A45A7F80045A6B000000000004297B53C45E7100045E568000000000042962B79427E63C842A6795D161011110416101111210A458B4000458A400000000000429636EC45A8A80045A78000000000004294F91A425BAE2A427F3B38161011102916101111010A451590004514300000000000429E9B80458B4800458A4000000000004296F681420ABFDA425BB57A160119154916011916030A45D0900045D120000000000042595E2345E9C80045EA6000000000004259DB18429819AA42A843A6");
        //     ValidChecksum("i2010019091608180110000074680FC00467D44004558900042B8DCED3F32A4CF42AC06BC41702F76022000007461DD400461BF000461A48004272230A3F93845F42ACE2AD41FE97E1033000007463830004635D00045FFD80042890C83BC578D0042B131E100000000044000007439A000043860000461740004106718C0000000042AE7E8700000000055000007450940004507C00045F38000420270970000000042A3DD5C00000000&&B892");
        ValidChecksum("i20200190916081801110190912172719091218100A460DD400460B5C003F33EFFE42A986D446894C004686DE003F33845A42AA7961425ECBE842C55660190826132519082614070A45F7380045F308003F336B8242A868F4467FE000467B20003F34111A42ACFE034248A2E942B75D8B190809115119080912280A45F7080045F310003F334A6642A5AD60467EA400467A80003F34111A42A655894248829842B67CCB190729072019072907570A45CFC00045CCB8003F33421D42A1A313466C440046691C003F32A4CF429DFCBD4230383B42A9EB65190710094019071010220A45E8900045E5B0003F334A66429B595B46779000467494003F334A66429A549C423FA23142B18CD7190620153919062016110A45EE280045EBF8003F33421D4291D416467C0800467948003F32D67E4297153C424311A342B4A7CE190603151419060315480A45ECF00045EB68003F3352B04289EA5E467B90004679D0003F32CE35428BCA8C4242508442B45570190514071019051407460A460848004607C0003F33EFFE428307CA46867200468644003F33318F42779C24425812EC42C0ED79190423123719042313180A4607E8004607A4003F3429F2427ADF3A46841200468404003F32180F42723B8442579FEA42BD5B65190403094519040310220A4600AC004600B4003F32C5EB426E7F694682BC00468300003F3283B34263F9A2424ED20342BB64F502210181024101118102410460A45F4F00045F370003F8B744C42933AB346706C00466F9C003F8B702842870C2E42473CD542ACB38F181011093818101110110A460DCC00460C78003F8ABA2842A12DED4681DC004680F2003F8A7C1C42972492425EC22942BA1F7D181002111718100211540A4601500045FFF0003F8A6FB342A5C61A46776400467518003F8A253E42A11F90424F9A3042B16F6B180920133618092014100A460824004606A4003F892D0E42A8FBB4467C4400467978003F8A296142A913664257E9D742B4D1E6180911174218091118160A4603C400460244003F892D0E42AA198C467980004676B8003F892D0E42A98BE64252956942B2E51E180905115018090512220A45F4600045F180003F8834DE42ABAD3A466FD000466D20003F8907D542A9CFF14246E23F42AC4AF2180824110918082411420A460B74004609E0003F87E22442AA70AA4680B200467E6C003F88390242AB1AF2425BEE4342B871CE180814114018081412120A46195C004617B0003F87D5BA42A80EE64688280046869E003F8834DE42AA27F4426CBEE442C38F42180806094718080610220A461588004613E0003F88203042A8F55E46832A004681B2003F873CAF42A9B17742681F4142BC05E1180727074318072708280A460DC800460C38003F84B34442A8B724467E8C00467BB4003F84F99542A9816A425EBE8442B66C2803310190910165719091017310A45D6980045D3D000BC52E58042B1584146613000465E0000BC52E58042B6ED0C42347D5A42A2AC4D190829163819082917120A45E9F00045E6F800BC290D0042B05272466C6C004668E800BC290D0042B9E6F34240798C42AA0577190820100219082010400A4603440046019C00BC22130042AFD09D4678DC0046759000BBFE660042B2AC3C4251F85542B27268190812112719081212020A45F4100045F10000BBFE660042AF4BA746719800466E1000BBA60A0042B899514246B25D42AD7C4A190801162319080116570A4600C00045FE3000BBA60A0042B096834677A40046738400BB80D90042C1D1F4424EE86C42B19BBC190729143819072915120A4584680045831000BB98190042A44BE84638580046359C00BB65CA0042B979AD41FE546F428923F4190715140419071514490A45FFF00045FD9800BB93720042A09F04467634004673A400BBA1660042A61300424DF65B42B0A018190709103719070911120A45B2100045B09800BB937200429CF4C2464EDC00464CF400BB93720042A0DE39421D6415429708F4190701094219070110230A45BC280045BA9000BB937200429CC30246545C0046529800BBAAB200429CED254223D59A429A7C55190621114419062112300A45CC580045CAE000BB93720042982E4C465B040046593400BB8ECA00429CAC82422E15A8429EB5B70440005510190211221619021123090A44BB200044BB200000000000426ADD0045C9300045CA500000000000423F9E6D41C6FD4542937D29180502134718050215080A450B0000450A9000000000004285CF5C45E2D80045E0F80000000000429A8B514203AA4E42A3BB40170712105317071211080A45D3800045D120000000000042A758EA45F1000045EE48000000000042A7B66A4299F0CA42AD13B0170712102217071210500A4592E800459178000000000042A0DB0C45D3880045D128000000000042A73B184264F0204299F29B170710095517071010110A456C60004569F0000000000042A2D675459368004591E8000000000042A322F04241F64E42658EEA170710092517071009540A44E4600044E2800000000000429BC7BF456C6000456A00000000000042A2B8D941E500794241FBF3161012090316101209360A45A7F80045A6B000000000004297B53C45E7100045E568000000000042962B79427E63C842A6795D161011110416101111210A458B4000458A400000000000429636EC45A8A80045A78000000000004294F91A425BAE2A427F3B38161011102916101111010A451590004514300000000000429E9B80458B4800458A4000000000004296F681420ABFDA425BB57A160119154916011916030A45D0900045D120000000000042595E2345E9C80045EA6000000000004259DB18429819AA42A843A6&&9537");

    }

    private String FormatDate(String rawdatetime) {
        //chages a YYMMDDHHMM time to yyyy-mm-dd hh:mm:ss
        //assumes year starts with '20' and sec is '00'
        String format_date = "20";
        format_date += rawdatetime.substring(0, 2);
        format_date += "-";
        format_date += rawdatetime.substring(2, 4);
        format_date += "-";
        format_date += rawdatetime.substring(4, 6);
        format_date += " ";
        format_date += rawdatetime.substring(6, 8);
        format_date += ":";
        format_date += rawdatetime.substring(8, 10);
        format_date += ":00";
        return format_date;
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private class MyHandler extends Handler {
        private final WeakReference<Service> mActivity;

        MyHandler(Service activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    Toast.makeText(mActivity.get(), data, Toast.LENGTH_LONG).show();
//                    mActivity.get().display.append(data);
                    byte[] value = data.getBytes();// a bit redundant, as I switch back and forth beween string and bytes, but the byte code was already written.
                    try {

                        Log.w(TAG, "Read value: " + new String(value) + " , " + toHexString(value));
                        if (value[0] == Constants.SOH)  //logic to group together commands split over several lines
                        {
                            response_message = new String(value);
                        } else {
                            response_message += new String(value);
                        }
                        if (value[value.length - 1] == Constants.ETX) {
                            if (ValidChecksum(response_message)) {
                                flag_GotResponse = true;
                                CommonUtils.LogMessage(TAG, "VR_interface: Got good response: " + response_message + "\n", null);
                                Proccess_all_response(response_message);
                            } else {
                                CommonUtils.LogMessage(TAG, "VR_interface: Got bad response: " + response_message + "\n", null);
                                Log.w(TAG, "Bad Checksum");
                            }
                        }

//                        Proccess_all_response(response_message);

                    } catch (Exception ex) {
                        Log.w(TAG, "Read value: null");
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class IP_VR_connect extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {
                String VR_IP = AppConstants.Current_VR_IP;
                String VR_PORT = AppConstants.Current_VR_Port;
                TelnetConnection tc = new TelnetConnection(VR_IP, Integer.parseInt(VR_PORT));

                //first get the inventory
                String send_message = make_VR_command(Constants.VR_Inventory_Code, 0); //Tank 0 is all
                tc.WriteLine(send_message);
                int attempts = 0;
                String vrresponse = "";
                vrresponse = tc.Read(); //a bit ugly to have this line twice, but it prevents unnessecary sleeps.
                while (!(vrresponse.length() > 0) & attempts < 10) {  //try several times to get a response, and wait 10 milliseconds between attempts.
                    attempts++;
                    Thread.sleep(10);
                    vrresponse = tc.Read();
                }
                if (!(vrresponse.length() > 0)) {
                    AppConstants.colorToastBigFont(VR_interface.this, "  Inventory Poll failed  ", Color.RED);
                }
                process_VR_Inventory(vrresponse);


                //then get the deliveries
                send_message = make_VR_command(Constants.VR_Delivery_Code, 0); //Tank 0 is all
                tc.WriteLine(send_message);
                attempts = 0;
                vrresponse = "";
                vrresponse = tc.Read();
                while (!(vrresponse.length() > 0) & attempts < 10) {  //try several times to get a response, and wait 10 milliseconds between attempts.
                    attempts++;
                    Thread.sleep(10);
                    vrresponse = tc.Read();
                }
                if (!(vrresponse.length() > 0)) {
                    AppConstants.colorToastBigFont(VR_interface.this, "  Delivery Poll failed  ", Color.RED);
                }
                process_VR_Delivery(vrresponse);


            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {
        }
    }



    class USB_VR_connect extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            String response = "";
            try {
                double seconds_attempt = 0;
                int num_attepts = 0;
                int maxTime = 10;  //seconds
                int sleepTime_ms = 100;  //ms
                int maxAttempts = 5;

                String send_message = make_VR_command(Constants.VR_Inventory_Code, 0); //Tank 0 is all
                usbService.write(send_message.getBytes());
//                WriteVal(send_message);

                seconds_attempt = 0;
//                num_attepts = 0;
                while (!flag_GotResponse) { //wait until we are connected
                    Thread.sleep(sleepTime_ms);
                    seconds_attempt += sleepTime_ms * 1.0 / 1000;
                    if (seconds_attempt > maxTime) {  //wait , then try again
                        CommonUtils.LogMessage(TAG, "VR_interface: could not get inventory response", null);

                        seconds_attempt = 0;
                        num_attepts++;
                        if (num_attepts > maxAttempts) //tried 5 times, give up
                        {
                            CommonUtils.LogMessage(TAG, "VR_interface: could not get inventory response giving up");
                            SendToast("Could not get inventory response", false);
                            break;
                        } else {
                            usbService.write(send_message.getBytes());
//                            WriteVal(send_message);
                        }
                    }
                }

                //then get the deliveries
                send_message = make_VR_command(Constants.VR_Delivery_Code, 0); //Tank 0 is all
                usbService.write(send_message.getBytes());

                seconds_attempt = 0;
                num_attepts = 0;
                while (!flag_GotResponse) { //wait until we are connected
                    Thread.sleep(sleepTime_ms);
                    seconds_attempt += sleepTime_ms * 1.0 / 1000;
                    if (seconds_attempt > maxTime) { //wait , then try again
                        Log.w(TAG, "could not get deliver response");

                        seconds_attempt = 0;
                        num_attepts++;
                        if (num_attepts > maxAttempts) {//tried 5 times, give up
                            CommonUtils.LogMessage(TAG, "VR_interface: could not get deliver response giving up");
                            SendToast("Could not get delivery response", false);
                            break;
                        } else {
                            usbService.write(send_message.getBytes());
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            AppConstants.RunningPoll = false;
            return response;
        }

        @Override
        protected void onPostExecute(String resp) {
        }
    }

    private void process_VR_Leak(String responseString) {
        try {
            System.out.println("got response " + responseString);
            ArrayList<HashMap<String, String>> Tank_Data = parse_VR_response(responseString.substring(1)); //he first is SOH: ignore it
            System.out.println("parsed VR_response as " + Tank_Data);
            //get and send tank information for each tank
            //complicated expression here should always resolve to Tank_Data.size(), but will check for nullity to avoid giving a warning
            for (int leak_no = 0; leak_no < (Tank_Data != null ? Tank_Data.size() : 0); leak_no++) { //It gives the last few deliveries, so we will pick up duplicates. The TransactionComplete checks for duplicates, so we don't have to worry about it.
                try {
                    HashMap<String, String> CurrentTank = Tank_Data.get(leak_no);
                    //convert from hex to float- go through long
                    DateTime = CurrentTank.get("DateTime");

                    //Does the storage and upload
                    System.out.println("Sending leak: at level " + TankLevel + "at time" + DateTime);
                    TransactionCompleteFunction("leak", CurrentTank);   //  removed for now as VASP can't receive.
                    // wait between each upload to make sure collisions don't occur
                    Thread.sleep(10);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            SendToast("Got Delivery response", true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //This method starts the VR interface background service, and sets it to activate every specified interval
    private void Do_next_VR_interface_Service() {

        if (VR_polling_interval < 1) {
            VR_polling_interval = 4;
        }
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        //VR_polling_interval=24; //temproray for testing.

        int target_h = (hour / (24 / VR_polling_interval) + 1) * (24 / VR_polling_interval);  //uses int division to get the next hour that satisfies the interval

        CommonUtils.LogMessage("VR_INTERFACE", "- Do_next_VR_interface_Service target_h= " + target_h + " --- VR_polling_interval=" + VR_polling_interval, null);

        Calendar target_time = Calendar.getInstance();
        target_time.set(year, month, day, target_h, 0, 0);
       /* if (target_h % 24 == 0) //handle rollover
        {
            target_time.add(Calendar.DAY_OF_MONTH, 1);
        }*/

        Intent name = new Intent(this, VR_interface.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, FLAG_CANCEL_CURRENT);

        Constants.alarm.cancel(pintent); //remove previous alarms so we don't have multiple firing.

        Log.w(TAG, "now:" + cal);
        Log.w(TAG, "target:" + target_time);
        Long wait_time = (target_time.getTimeInMillis() - cal.getTimeInMillis());
/*        if (wait_time < 60 * 60 * 1000) {
            wait_time = (long) (60 * 60 * 1000);
        }*/
        Log.w(TAG, "waiting :" + wait_time);
        CommonUtils.LogMessage(TAG, "VR_interface waiting :" + wait_time);

        Constants.alarm.setRepeating(AlarmManager.RTC_WAKEUP, wait_time + currentTimeMillis(), (long) (24.0 / VR_polling_interval) * 60 * 60 * 1000, pintent);  //by defualt, 6 hours
    }

    static class USB_VR_connect2Demo extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            try {

                String send_message = make_VR_command2Demo(Constants.VR_Inventory_Code, 0); //Tank 0 is all

            } catch (Exception e) {
                e.printStackTrace();
                if (Constants.deguglog) CommonUtils.LogMessage(TAG, "VRDemo Ex: " + e.getMessage());
            }

            return response_message;
        }

        @Override
        protected void onPostExecute(String resp) {
            if (Constants.deguglog) CommonUtils.LogMessage(TAG, "VRDemo Response:" + resp);
        }
    }

    class USB_VR_connect2 extends AsyncTask<String, Void, String> {

        public FT311UARTInterface uartInterface;
        public SharedPreferences sharePrefSettings;
        public handler_thread handlerThread;
        byte[] writeBuffer;
        byte[] readBuffer;
        char[] readBufferToChar;
        int[] actualNumBytes;
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                byte[] value = new byte[actualNumBytes[0]];

                System.arraycopy(readBuffer, 0, value, 0, actualNumBytes[0]);
                try {

                    Log.w(TAG, "Read value: " + new String(value) + " , " + toHexString(value));
                    CommonUtils.LogMessage(TAG, "Read value: " + new String(value) + " , " + toHexString(value));
                    if (value[0] == Constants.SOH)  //logic to group together commands split over several lines
                    {
                        response_message = new String(value);
                    } else {
                        response_message += new String(value);
                    }
                    if (value[value.length - 1] == Constants.ETX) {
                        if (ValidChecksum(response_message)) {
                            flag_GotResponse = true;
                            CommonUtils.LogMessage(TAG, "Got good response: " + response_message + "\n", null);
                            Proccess_all_response(response_message);
                        } else {
                            CommonUtils.LogMessage(TAG, "Got bad response: " + response_message + "\n", null);
                            Log.w(TAG, "Bad Checksum ");
                        }
                    }
                } catch (Exception
                        ex) {
                    Log.e(TAG, "process message failed");
                }

            }
        };
        int baudRate = Integer.parseInt(AppConstants.BaudRate); /* baud rate */
        byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
        byte dataBit = 8; /* 8:8bit, 7: 7bit */
        byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
        byte flowControl = 0;
        byte status;
        boolean handle_thread_running;

        @Override
        protected String doInBackground(String... params) {
            String response = "";
            try {
                handle_thread_running = true;
                double seconds_attempt = 0;
                int num_attepts = 0;
                int maxTime = 10;  //seconds
                int sleepTime_ms = 100;  //ms
                int maxAttempts = 5;

                String send_message = make_VR_command2(Constants.VR_Inventory_Code, 0); //Tank 0 is all

                writeBuffer = new byte[64];
                readBuffer = new byte[4096];
                readBufferToChar = new char[4096];
                actualNumBytes = new int[1];

                sharePrefSettings = getSharedPreferences("UARTLBPref", 0);
                savePreference();
                uartInterface = new FT311UARTInterface(Constants.global_context, sharePrefSettings);
                uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
                handlerThread = new handler_thread(handler);
                handlerThread.start();

                flag_GotResponse = false;

                int numBytes = send_message.length();
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) send_message.charAt(i);
                }
                uartInterface.SendData(numBytes, writeBuffer);
                // usbService.write(send_message.getBytes());
//                WriteVal(send_message);

                seconds_attempt = 0;
                num_attepts = 0;
                while (!flag_GotResponse) { //wait until we are connected
                    Thread.sleep(sleepTime_ms);
                    seconds_attempt += sleepTime_ms * 1.0 / 1000;
                    if (seconds_attempt > maxTime) {  //wait , then try again
                        Log.w(TAG, "could not get inventory response");

                        seconds_attempt = 0;
                        num_attepts++;
                        maxAttempts = 5;
                        if (num_attepts > maxAttempts) //tried 5 times, give up
                        {
                            CommonUtils.LogMessage(TAG, "could not get inventory response giving up");
                            SendToast("Could not get inventory response", false);
                            doRestart(Constants.global_context); //disabled for timing test
                            break;
                        } else {
                            numBytes = send_message.length();
                            for (int i = 0; i < numBytes; i++) {
                                writeBuffer[i] = (byte) send_message.charAt(i);
                            }
                            uartInterface.ResumeAccessory();
                            Thread.sleep(50);
                            uartInterface.SendData(numBytes, writeBuffer);
//                            WriteVal(send_message);
                        }
                    }
                }


                flag_GotResponse = false;
                //then get the deliveries
                send_message = make_VR_command2(Constants.VR_Delivery_Code, 0); //Tank 0 is all
                numBytes = send_message.length();
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) send_message.charAt(i);
                }
                uartInterface.SendData(numBytes, writeBuffer);

                seconds_attempt = 0;
                num_attepts = 0;
                while (!flag_GotResponse) { //wait until we are connected
                    Thread.sleep(sleepTime_ms);
                    seconds_attempt += sleepTime_ms * 1.0 / 1000;
                    if (seconds_attempt > maxTime) { //wait , then try again
                        Log.w(TAG, "could not get deliver response");

                        seconds_attempt = 0;
                        num_attepts++;
                        if (num_attepts > maxAttempts) {//tried 5 times, give up
                            CommonUtils.LogMessage(TAG, "could not get deliver response giving up");
                            SendToast("Could not get delivery response", false);
                            break;
                        } else {
                            numBytes = send_message.length();
                            for (int i = 0; i < numBytes; i++) {
                                writeBuffer[i] = (byte) send_message.charAt(i);
                            }
                            uartInterface.ResumeAccessory();
                            Thread.sleep(50);
                            uartInterface.SendData(numBytes, writeBuffer);
                        }

                    }
                }


                flag_GotResponse = false;
                //then get the leak
                send_message = make_VR_command2(Constants.VR_Leak_Code, 0); //Tank 0 is all
                numBytes = send_message.length();
                for (int i = 0; i < numBytes; i++) {
                    writeBuffer[i] = (byte) send_message.charAt(i);
                }
                uartInterface.SendData(numBytes, writeBuffer);

                seconds_attempt = 0;
                num_attepts = 0;
                while (!flag_GotResponse) { //wait until we are connected
                    Thread.sleep(sleepTime_ms);
                    seconds_attempt += sleepTime_ms * 1.0 / 1000;
                    if (seconds_attempt > maxTime) { //wait , then try again
                        Log.w(TAG, "could not get leak response");

                        seconds_attempt = 0;
                        num_attepts++;
                        if (num_attepts > maxAttempts) {//tried 5 times, give up
                            CommonUtils.LogMessage(TAG, "could not get leak response giving up");
                            SendToast("Could not get leak response", false);
                            break;
                        } else {
                            numBytes = send_message.length();
                            for (int i = 0; i < numBytes; i++) {
                                writeBuffer[i] = (byte) send_message.charAt(i);
                            }
                            uartInterface.ResumeAccessory();
                            Thread.sleep(50);
                            uartInterface.SendData(numBytes, writeBuffer);
                        }

                    }
                }


                handle_thread_running = false;

                uartInterface.DestroyAccessory(true);  //close when done
                uartInterface = null; //clear the old

            } catch (Exception e) {
                e.printStackTrace();
            }
            AppConstants.RunningPoll = false;

            return response;
        }

        protected void savePreference() {
            sharePrefSettings.edit().putString("configed", "TRUE").commit();
            sharePrefSettings.edit().putInt("baudRate", baudRate).commit();
            sharePrefSettings.edit().putInt("stopBit", stopBit).commit();
            sharePrefSettings.edit().putInt("dataBit", dataBit).commit();
            sharePrefSettings.edit().putInt("parity", parity).commit();
            sharePrefSettings.edit().putInt("flowControl", flowControl).commit();

        }

        /* usb input data handler */
        private class handler_thread extends Thread {
            Handler mHandler;

            /* constructor */
            handler_thread(Handler h) {
                mHandler = h;
            }

            public void run() {
                Message msg;

                while (handle_thread_running) {

                    status = uartInterface.ReadData(4096, readBuffer, actualNumBytes);

                    if (status == 0x00 && actualNumBytes[0] > 0) {
                        msg = mHandler.obtainMessage();
                        mHandler.sendMessage(msg);
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }


        @Override
        protected void onPostExecute(String resp) {
        }
    }

    private String[] TransactionCompleteFunction_alarm(HashMap<String, String> CurrentTank) {
        String[] auths = new String[2];

        try {
            //put the information into a package
            Gson gson = new Gson();
            String jsonData;


            String userEmail = Login_Email;

            String authString;

            authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveAlarmVeederTankMonitorReading");

            VR_Alarm_InfoEntity authEntityClass = new VR_Alarm_InfoEntity();
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(VR_interface.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
            authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(VR_interface.this);
            authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
            authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss"); //FormatDate(DateTime);
            authEntityClass.VRDateTime = FormatDate(CurrentTank.get("DateTime"));
            authEntityClass.TankNumber = (CurrentTank.get("TankNumber"));
            authEntityClass.Alarm_Category = CurrentTank.get("Alarm_Category");
            authEntityClass.Alarm_Type = CurrentTank.get("Alarm_Type");


            jsonData = gson.toJson(authEntityClass);

            auths[0] = jsonData;
            auths[1] = authString;
        } catch (Exception ex) {

            CommonUtils.LogMessage("APFS33", "AuthTestAsyncTask ", ex);
            ex.printStackTrace();
        }
        return auths;

    }

    private void process_VR_Alarm(String responseString) {
        try {
            System.out.println("got response " + responseString);
            ArrayList<HashMap<String, String>> Alarm_Data = parse_VR_response_alarm(responseString.substring(1)); //he first is SOH: ignore it
            System.out.println("parsed VR_response as " + Alarm_Data);
            //get and send tank information for each tank
            //complicated expression here should always resolve to Tank_Data.size(), but will check for nullity to avoid giving a warning
            for (int alarm_no = 0; alarm_no < (Alarm_Data != null ? Alarm_Data.size() : 0); alarm_no++) {
                try {
                    HashMap<String, String> CurrentAlarm = Alarm_Data.get(alarm_no);
                    //convert from hex to float- go through long
                    DateTime = CurrentAlarm.get("DateTime");


                    //Does the storage and upload
                    System.out.println("Sending alarm at time" + DateTime);
                    TransactionCompleteFunction("alarm", CurrentAlarm);   //  removed for now as VASP can't receive.
                    // wait between each upload to make sure collisions don't occur
                    Thread.sleep(10);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            SendToast("Got Alarm response", true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private ArrayList<HashMap<String, String>> parse_VR_response_alarm(String responseString) {
        int response_length = responseString.length();
        ArrayList<HashMap<String, String>> Final_Response_ac = new ArrayList<HashMap<String, String>>();
        String TankNumber_ac = responseString.substring(4, 6);  //This is the numbers queried: tthe details are in the later block
        String DateTime_ac = responseString.substring(6, 16);//This is the time of query: tthe details are in the later block
        int offset_ac = 16;  //keeps track of the number of tanks it is querying.
        //there may be several entries, so go through them all.
        while (offset_ac < response_length - 10) {  //The -10 here is simply to avoid off by one errors in counting the length.
            // There are 7 charachters at the end that we do not use (2 end message, 4 checksum, one ETX
            HashMap<String, String> Responses = new HashMap<String, String>();
            Responses.put("DateTime", DateTime_ac);
            Responses.put("Alarm_Category", responseString.substring(offset_ac, offset_ac + 2));
            Responses.put("Alarm_Type", responseString.substring(offset_ac + 2, offset_ac + 4));
            Responses.put("TankNumber", responseString.substring(offset_ac + 4, offset_ac + 6));
            Final_Response_ac.add(Responses);
            offset_ac += 6;
        }
        //The data termination bits don't need to be used.
        //I could look at the checksum, but I will ignore it for now.
        //Also ignore ETX
        return Final_Response_ac;
    }
}






