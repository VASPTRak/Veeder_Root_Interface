package com.TrakEngineering.veeder_rootinterface;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Environment;
import android.support.constraint.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by VASP-LAP on 03-05-2016.
 *
 * Defines some constants used by the app.
 */
class Constants {

    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;


    static boolean deguglog = true;
    final public static String VEHICLE_NUMBER="vehicleNumber";
    final public static String ODO_METER="Odometer";
    final public static String DEPT="dept";
    final public static String PPIN="pin";
    final public static String OTHERR="other";
    final public static String HOURSS="hours";
    final public static String DATE_FORMAT="MMM dd, yyyy"; // May 24, 2016
    final public static String TIME_FORMAT="hh:mm aa";
    public static final int CONNECTION_CODE = 111;
    public static final String SHARED_PREF_NAME = "UserInfo";
    public static final String PREF_COLUMN_USER = "UserData";
    public static final String PREF_COLUMN_SITE = "SiteData";
    public static final String PREF_BAUDRATE = "BaudRate";
    public static final String PREF_VehiFuel = "SaveVehiFuelInPref";
    //Codes for the Veeder Root device
    public static final int SOH = 0x01;
    public static final int ETX = 0x03;
    public static final String VR_Inventory_Code = "201";
    public static final String VR_Delivery_Code = "202";
    public static final String VR_Leak_Code = "203";
    public static final String VR_Alarm_Code = "101";
    public static final String FS_1STATUS = "FREE";
    public static final String FS_2STATUS = "FREE";
    public static final String FS_3STATUS = "FREE";
    public static final String FS_4STATUS = "FREE";
    //The times per day of  each attempt at polling the tank moniter.
    public static int VR_polling_interval = -1;
    //if this flag is set to true, the app with show more debugging information.
    public static final boolean DEBUG = false;
    public static final ArrayList<Double> TankLevel = new ArrayList<Double>(Arrays.asList(0.0, 0.0, 0.0, 0.0));
    public static final ArrayList<Integer> TankNumbers = new ArrayList<Integer>(Arrays.asList(0, 0, 0, 0));
    public static final boolean HardcodeVR_IP = true;
    private static final int SERVER_PORT = 2901;
    private static final String SERVER_IP = "192.168.4.1";
    private static final String exrSdDir = Environment.getExternalStorageDirectory() + File.separator;
    private static final String logFolderName = "Veeder_Root_Log";
    public static final String LogPath = exrSdDir + logFolderName + File.separator + "Logs";
    public static boolean hotspotstayOn = true;
    public static double Latitude = 0;
    public static double Longitude = 0;
    public static String CurrFsPass;
    public static String FS_2Gallons = "";
    public static String FS_2Pulse = "";
    public static String CurrentSelectedHose;
    public static String AccPersonnelPIN_FS1;
    public static String AccVehicleNumber_FS1;
    public static String AccDepartmentNumber_FS1;
    public static String AccOther_FS1;
    public static int AccOdoMeter_FS1=0;
    public static int AccHours_FS1;
    public static String AccVehicleNumber;
    public static String AccDepartmentNumber;
    public static String AccPersonnelPIN;
    public static String AccOther;
    public static int AccOdoMeter;
    public static int AccHours;
    //For fs number 3
    public static String AccPersonnelPIN_FS3;
    public static String AccVehicleNumber_FS3;
    public static String AccDepartmentNumber_FS3;
    public static String AccOther_FS3;
    public static int AccOdoMeter_FS3=0;
    public static int AccHours_FS3;
    //ForFs number 4
    public static String AccPersonnelPIN_FS4;
    public static String AccVehicleNumber_FS4;
    public static String AccDepartmentNumber_FS4;
    public static String AccOther_FS4;
    public static int AccOdoMeter_FS4=0;
    public static int AccHours_FS4;
    static List<String> BusyVehicleNumberList = new ArrayList<String>();

    static AlarmManager alarm;
    static AlarmManager exat_alarm_12am;
    static AlarmManager exat_alarm_12pm;
    static AlarmManager exat_alarm_8am;
    static AlarmManager exat_alarm_8pm;
    static AlarmManager exat_alarm_6am;
    static AlarmManager exat_alarm_6pm;
    static AlarmManager exat_alarm_4pm;
    static AlarmManager exat_alarm_4am;
    public static Context global_context;
}
