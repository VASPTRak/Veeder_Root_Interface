package com.TrakEngineering.veeder_rootinterface;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.TrakEngineering.veeder_rootinterface.enity.RenameHose;
import com.TrakEngineering.veeder_rootinterface.enity.UserInfoEntity;
import com.TrakEngineering.veeder_rootinterface.enity.VR_Delivery_InfoEntity;
import com.TrakEngineering.veeder_rootinterface.enity.VR_Inventory_InfoEntity;
import com.TrakEngineering.veeder_rootinterface.server.ServerHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.TrakEngineering.veeder_rootinterface.AppConstants.Login_Email;
import static com.TrakEngineering.veeder_rootinterface.Constants.VR_polling_interval;
import static com.TrakEngineering.veeder_rootinterface.R.id.textView;
import static java.lang.System.currentTimeMillis;

public class WelcomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, ServiceConnection, SerialListener {


    final DBController controller = new DBController(WelcomeActivity.this);
    public String CompleteResponse = "", VRDeviceType = "", MacAddressForBTVeederRoot = "";

    private enum Connected {False, Pending, True}
    private BluetoothAdapter bluetoothAdapter;
    private SerialService service;
    private Connected connected = Connected.False;

    public Timer btsppTimer;
    ///////////////////////////////////////////////////////////////
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static int SelectedItemPos;
    private final ArrayList<HashMap<String, String>> serverSSIDList = new ArrayList<>();
    private final ArrayList<HashMap<String, String>> ListOfConnectedDevices = new ArrayList<>();
    private final String TAG = " WelcomeActivity ";


    //private TextView tvSSIDName;
    private TextView tv_current_baud_rate;
    private TextView tv_NFS1;
    private TextView tv_NFS2;
    private TextView tv_NFS3;
    private TextView tv_NFS4;//tv_fs1_pulse
    private LinearLayout linearHose;
    private LinearLayout linear_fs_1;
    private LinearLayout linear_fs_2;
    private LinearLayout linear_fs_3;
    private LinearLayout linear_fs_4;
    private LinearLayout layout_get_vr_readings;
    private GoogleApiClient mGoogleApiClient;
    private TextView tvLatLng;
    boolean ex_vr = false;

    private ProgressDialog loading = null;
    // android built in classes for bluetooth operations
    private BluetoothAdapter mBluetoothAdapter;
    private TextView textDateTime;

    private TextView tv_FS1_hoseName;
    private TextView tv_FS2_hoseName;
    private TextView tv_FS3_hoseName;
    private TextView tv_FS4_hoseName;
    private TextView tv_display_vr_response;

    private Button btnGo;
    private Button btnSetBaudRate;
    private Button btn_connect, btn_disConnect, btn_get_vr_readings;
    private EditText edt_mac_address;
    private EditText edt_vr_command;
    private LinearLayout linearMac;
    public ExactAlarmReceiver exact_alarm_rec = null;
    public String current_Command = "";

    //===================== USB Permission ================================//
    public UsbManager usbmanager;
    public PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";
    private boolean isUSBReceiverRegistered = false;
    //=======================================================================//

    @Override
    protected void onResume() {
        super.onResume();

        InitializeVRService();  //Begining of VR Code

        Log.i(TAG,"surelockcheck onResume");
        AppConstants.VRForceReadingSave = "n";

        //Hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Constants.global_context = getBaseContext();

        //new GetConnectedDevicesIP().execute();
        new GetSSIDUsingLocationOnResume().execute();
        UpdateFSUI_seconds();

        if (VRDeviceType.equalsIgnoreCase("BT")) {

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        }


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ex_vr = true;
                tv_display_vr_response.setText("");
            }
        }, 60000);


    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"surelockcheck onPause");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"surelockcheck onCreate"+VR_polling_interval);

        setContentView(R.layout.activity_welcome);
        //tvSSIDName = findViewById(R.id.tvSSIDName);
        tvLatLng = findViewById(R.id.tvLatLng);
        btnGo = findViewById(R.id.btnGo);

        linearMac = (LinearLayout) findViewById(R.id.linearMac);
        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_disConnect = (Button) findViewById(R.id.btn_disConnect);
        edt_mac_address = (EditText) findViewById(R.id.edt_mac_address);

        InItGUI();

        SharedPreferences sharedPrefG = WelcomeActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        VRDeviceType = sharedPrefG.getString("VRDeviceType", "BT");
        MacAddressForBTVeederRoot = sharedPrefG.getString("MacAddressForBTVeederRoot", "");
        CommonUtils.LogMessage(TAG, "App Version: " + CommonUtils.getVersionCode(WelcomeActivity.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " ");

        InitAlarm();

        try {

            //Register Broadcast For Alaram readings..
            exact_alarm_rec = new ExactAlarmReceiver();
            IntentFilter infilter = new IntentFilter("GetExactVRReadings");
            registerReceiver(exact_alarm_rec, infilter);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (VRDeviceType.equalsIgnoreCase("BT")) {
            layout_get_vr_readings.setVisibility(View.VISIBLE);
            SplashActivity.VR_BT_MAC_ADDR = MacAddressForBTVeederRoot;
            CheckIfDevicesIsPaired(MacAddressForBTVeederRoot);
            /*SharedPreferences sharedPrefBT = WelcomeActivity.this.getSharedPreferences("storeVRBTMAc", Context.MODE_PRIVATE);
            SplashActivity.VR_BT_MAC_ADDR = sharedPrefBT.getString("mac", "");
            edt_mac_address.setText(SplashActivity.VR_BT_MAC_ADDR);//"00:14:03:05:F2:9B"*/

            requestMultiplePermissions();
            status("Disconnect-1");
            //////////////////////////////////////////////////////////////////////////
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if (VR_polling_interval < 1) {
                VR_polling_interval = 6;
            }
            int target_h = (hour / (24 / VR_polling_interval) + 1) * (24 / VR_polling_interval);  //uses int division to get the next hour that satisfies the interval

            Calendar target_time = Calendar.getInstance();
            target_time.set(year, month, day, target_h, 0, 0);
            Long wait_time = (target_time.getTimeInMillis() - cal.getTimeInMillis());
            long _period = (long) (24 / VR_polling_interval) * 60 * 60 * 1000;


            //////////////////////////////////////////////////////////////////////////
            btsppTimer = new Timer();
            btsppTimer.schedule(new TimerTask() {
                @Override
                public void run() {

                    Log.i(TAG,"LOOP START--");
                    try {

                        if (connected == Connected.True) {

                            btnGo.setBackgroundColor(ContextCompat.getColor(WelcomeActivity.this, R.color.colorFluid));
                            // send("01323030");//200 VR_Inventory_Code

                        } else {
                            btnGo.setBackgroundColor(ContextCompat.getColor(WelcomeActivity.this, R.color.pressed_start_multi));
                            // connect("re-try");// re-try connect
                            //ReestablishConnection();//Recreate app to maintain SPP connection

                        }



                    } catch (Exception e) {
                        CommonUtils.LogMessage(TAG, "SPP timer Exception: " + e.getMessage() + "\n", null);
                    }
                }
            }, 10000,3600000); //wait_time, _period 3600000 * 24
            //30min= 1800000
            //60min= 3600000


        } else {
            linearMac.setVisibility(View.GONE);
            btn_disConnect.setVisibility(View.GONE);

            //====================== USB Permission ================================//
            try {
                usbmanager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
                mPermissionIntent = PendingIntent.getBroadcast(WelcomeActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                UsbAccessory[] accessories = usbmanager.getAccessoryList();
                UsbAccessory accessory = (accessories == null ? null : accessories[0]);
                if (accessory != null) {
                    if (!usbmanager.hasPermission(accessory)) {
                        CommonUtils.LogMessage(TAG, "<WelcomeActivity: Registering the USB Receiver.>");
                        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                        registerReceiver(mUsbReceiver, filter);
                        isUSBReceiverRegistered = true;
                        CommonUtils.LogMessage(TAG, "<WelcomeActivity: Requesting USB_PERMISSION.>");
                        usbmanager.requestPermission(accessory, mPermissionIntent);
                        return;
                    } else {
                        CommonUtils.LogMessage(TAG, "<WelcomeActivity: USB_PERMISSION is allowed.>");
                    }
                } else {
                    CommonUtils.LogMessage(TAG, "<WelcomeActivity: UsbAccessory is null.>");
                }
            } catch (Exception ex) {
                CommonUtils.LogMessage(TAG, "WelcomeActivity: Exception while checking USB_PERMISSION: ", ex);
            }
            //==========================================================================//
        }

        tvLatLng.setVisibility(View.GONE);

        SelectedItemPos = -1;

        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setIcon(R.drawable.fuel_secure_lock);

        float density = getResources().getDisplayMetrics().density;
        Constants.global_context = this;


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        TextView tvVersionNum = findViewById(R.id.tvVersionNum);
        tvVersionNum.setText("Version " + CommonUtils.getVersionCode(WelcomeActivity.this));

        SharedPreferences sharedPref = WelcomeActivity.this.getSharedPreferences(Constants.PREF_BAUDRATE, Context.MODE_PRIVATE);
        AppConstants.BaudRate = sharedPref.getString(Constants.PREF_BAUDRATE, "9600");
        tv_current_baud_rate.setText(AppConstants.BaudRate);

        new GetConnectedDevicesIP().execute(); //getListOfConnectedDevice();

       /* if (!VRDeviceType.equalsIgnoreCase("BT")) {
            startService(new Intent(this, VR_interface.class)); //if not BT

        }*/

        // set User Information
        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);
        if (!userInfoEntity.FluidSecureSiteName.isEmpty()) {
            AppConstants.Title = "HUB Name : " + userInfoEntity.FluidSecureSiteName;
        } else {
            AppConstants.Title = "HUB Name : " + userInfoEntity.PersonName;//+ "\nMobile : " + userInfoEntity.PhoneNumber + "\nEmail : " + userInfoEntity.PersonEmail
        }
        AppConstants.HubName = userInfoEntity.PersonName;
        TextView tvTitle = findViewById(textView);
        tvTitle.setText(AppConstants.Title);

        // Display current date time u
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, textDateTime);
        myThread = new Thread(myRunnableThread);
        myThread.start();
        //end current date time----------------------------------------------

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AppConstants.BUSY_STATUS)
                    new ChangeBusyStatus().execute();

            }
        }, 2000);

        btn_get_vr_readings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetVRReadingsManually();

            }
        });

        btnSetBaudRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomDilaogEnterpin(WelcomeActivity.this, "Enter Security Pin", "tt");
            }
        });

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String str = edt_mac_address.getText().toString().trim();
                if (str != null && !str.isEmpty()) {

                    SplashActivity.VR_BT_MAC_ADDR = str.toUpperCase();
                    if (connected == WelcomeActivity.Connected.True) {
                        status("Disconnect-2");
                        disconnect();
                    } else {
                        connect("btn-click");
                    }

                    SharedPreferences sharedPref = WelcomeActivity.this.getSharedPreferences("storeVRBTMAc", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("mac", SplashActivity.VR_BT_MAC_ADDR);
                    editor.commit();
                }
            }
        });

        btn_disConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    status("Disconnect-3");
                    disconnect();
                } catch (Exception e) {

                }
            }
        });

        edt_vr_command.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnterpinForEditCommand(WelcomeActivity.this, "Enter Security Pin", "tt");
            }
        });

        CommonUtils.LogMessage(TAG, "*** WelcomeActivity:onCreate***" + "\n", null);

        startService(new Intent(WelcomeActivity.this, BackgroundService.class));

        Do_next_BT_Service();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            CommonUtils.LogMessage(TAG, "<WelcomeActivity: USB Permission Allowed.>");
                        } else {
                            CommonUtils.LogMessage(TAG, "<WelcomeActivity: USB Permission Denied.>");
                        }
                    }
                    proceedAfterUSBPermission();
                }
            } catch (Exception ex) {
                CommonUtils.LogMessage(TAG, "WelcomeActivity: mUsbReceiver onReceive Exception: ", ex);
                proceedAfterUSBPermission();
            }
        }
    };

    private void proceedAfterUSBPermission() {
        if (isUSBReceiverRegistered) {
            isUSBReceiverRegistered = false;
            try {
                CommonUtils.LogMessage(TAG, "<WelcomeActivity: Unregistering the USB Receiver.>");
                unregisterReceiver(mUsbReceiver);
            } catch (Exception ex) {
                CommonUtils.LogMessage(TAG, "WelcomeActivity: unregisterReceiver (USB) Exception: ", ex);
            }
        }
        CommonUtils.LogMessage(TAG, "<WelcomeActivity: Restarting the activity.>");
        recreate();
    }

    @Override
    protected void onStop() {
        super.onStop();

        CommonUtils.LogMessage(TAG, "in onStop");
        Log.i(TAG,"surelockcheck onStop");
        if (loading != null) {
            loading.dismiss();
            Constants.hotspotstayOn = true;
            loading = null;
        }
    }

    private void UpdateFSUI_seconds() {

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // System.out.println("FS UI Update here");
                                int FS_Count = CommonUtils.NumNonZero(Constants.TankNumbers);
                                //int FS_Count = serverSSIDList.size();

                                if (FS_Count > 0) {

                                    //FS Visibility on Dashboard
                                    if (FS_Count == 1) {
                                        tv_FS1_hoseName.setText("Tank " + Constants.TankNumbers.get(0));

                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.INVISIBLE);
                                        linear_fs_3.setVisibility(View.INVISIBLE);
                                        linear_fs_4.setVisibility(View.INVISIBLE);

                                    } else if (FS_Count == 2) {


                                        //------------
                                        tv_FS1_hoseName.setText("Tank " + Constants.TankNumbers.get(0));
                                        tv_FS2_hoseName.setText("Tank " + Constants.TankNumbers.get(1));

                                        // System.out.println("MacAddress" + serverSSIDList.get(0).get("MacAddress").toString());


                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.INVISIBLE);
                                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = 0; // In dp
                                        linear_fs_3.setLayoutParams(params);

                                        linear_fs_4.setVisibility(View.INVISIBLE);
                                        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = 0; // In dp
                                        linear_fs_4.setLayoutParams(params1);

                                    } else if (FS_Count == 3) {

                                        tv_FS1_hoseName.setText("Tank " + Constants.TankNumbers.get(0));
                                        tv_FS2_hoseName.setText("Tank " + Constants.TankNumbers.get(1));
                                        tv_FS3_hoseName.setText("Tank " + Constants.TankNumbers.get(2));


                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.VISIBLE);
                                       /* LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = match_parent; // In dp
                                        linear_fs_3.setLayoutParams(params);*/

                                        linear_fs_4.setVisibility(View.INVISIBLE);
                                       /* LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = 0; // In dp
                                        linear_fs_4.setLayoutParams(params1);*/


                                    } else {

                                        tv_FS1_hoseName.setText("Tank " + Constants.TankNumbers.get(0));
                                        tv_FS2_hoseName.setText("Tank " + Constants.TankNumbers.get(1));
                                        tv_FS3_hoseName.setText("Tank " + Constants.TankNumbers.get(2));
                                        tv_FS4_hoseName.setText("Tank " + Constants.TankNumbers.get(3));

                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.VISIBLE);
                                        /*LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = match_parent; // In dp
                                        linear_fs_3.setLayoutParams(params);*/

                                        linear_fs_4.setVisibility(View.VISIBLE);
                                        /*LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = match_parent; // In dp
                                        linear_fs_4.setLayoutParams(params1);*/

                                    }
                                } else {
                                    try {
                                        FS_Count = Constants.TankList.size();
                                        if (FS_Count > 0) {

                                            //FS Visibility on Dashboard
                                            if (FS_Count == 1) {
                                                tv_FS1_hoseName.setText("Tank " + Constants.TankList.get(0).get("TankNumber"));

                                                linear_fs_1.setVisibility(View.VISIBLE);
                                                linear_fs_2.setVisibility(View.INVISIBLE);
                                                linear_fs_3.setVisibility(View.INVISIBLE);
                                                linear_fs_4.setVisibility(View.INVISIBLE);

                                            } else if (FS_Count == 2) {

                                                tv_FS1_hoseName.setText("Tank " + Constants.TankList.get(0).get("TankNumber"));
                                                tv_FS2_hoseName.setText("Tank " + Constants.TankList.get(1).get("TankNumber"));

                                                linear_fs_1.setVisibility(View.VISIBLE);
                                                linear_fs_2.setVisibility(View.VISIBLE);

                                                linear_fs_3.setVisibility(View.INVISIBLE);
                                                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                                params.height = 0; // In dp
                                                linear_fs_3.setLayoutParams(params);

                                                linear_fs_4.setVisibility(View.INVISIBLE);
                                                LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                                params1.height = 0; // In dp
                                                linear_fs_4.setLayoutParams(params1);

                                            } else if (FS_Count == 3) {
                                                tv_FS1_hoseName.setText("Tank " + Constants.TankList.get(0).get("TankNumber"));
                                                tv_FS2_hoseName.setText("Tank " + Constants.TankList.get(1).get("TankNumber"));
                                                tv_FS3_hoseName.setText("Tank " + Constants.TankList.get(2).get("TankNumber"));

                                                linear_fs_1.setVisibility(View.VISIBLE);
                                                linear_fs_2.setVisibility(View.VISIBLE);
                                                linear_fs_3.setVisibility(View.VISIBLE);
                                                linear_fs_4.setVisibility(View.INVISIBLE);

                                            } else {
                                                tv_FS1_hoseName.setText("Tank " + Constants.TankList.get(0).get("TankNumber"));
                                                tv_FS2_hoseName.setText("Tank " + Constants.TankList.get(1).get("TankNumber"));
                                                tv_FS3_hoseName.setText("Tank " + Constants.TankList.get(2).get("TankNumber"));
                                                tv_FS4_hoseName.setText("Tank " + Constants.TankList.get(3).get("TankNumber"));

                                                linear_fs_1.setVisibility(View.VISIBLE);
                                                linear_fs_2.setVisibility(View.VISIBLE);
                                                linear_fs_3.setVisibility(View.VISIBLE);
                                                linear_fs_4.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    } catch (Exception ex) {
                                        CommonUtils.LogMessage(TAG, "TankList is empty." + "\n", ex);
                                    }
                                }

                                //===Display Dashboard every Second=====
                                DisplayDashboardEveSecond();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        t.start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.first, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
/*        switch (item.getItemId()) {
            case R.id.mClose:
                finish();
                return true;

            default:*/
        return super.onOptionsItemSelected(item);
//        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLastLocation != null) {
                System.out.println("rrr" + mLastLocation.getLatitude());
                System.out.println("rrr" + mLastLocation.getLongitude());


                LocationManager locationManager = (LocationManager) WelcomeActivity.this.getSystemService(Context.LOCATION_SERVICE);
                boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


                double longitude = 0;
                double latitude = 0;
                if (!statusOfGPS) {
                    latitude = 0;
                    longitude = 0;
                } else {
                    latitude = mLastLocation.getLatitude();
                    // AcceptVehicleActivity.CurrentLat = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                    // AcceptVehicleActivity.CurrentLng = mLastLocation.getLongitude();
                }

                if (latitude == 0 && longitude == 0) {
                    //AppConstants.AlertDialogFinish(WelcomeActivity.this, "Unable to get current location.\nPlease try again later!");
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void InItGUI() {

        textDateTime = findViewById(R.id.textDateTime);
      /*  tv_fs1_Qty = findViewById(R.id.tv_fs1_Qty);
        tv_fs2_Qty = findViewById(R.id.tv_fs2_Qty);
        tv_fs3_Qty = findViewById(R.id.tv_fs3_Qty);
        tv_fs4_Qty = findViewById(R.id.tv_fs4_Qty);*/
        tv_FS2_hoseName = findViewById(R.id.tv_FS2_hoseName);
        tv_FS1_hoseName = findViewById(R.id.tv_FS1_hoseName);
        tv_FS3_hoseName = findViewById(R.id.tv_FS3_hoseName);
        tv_FS4_hoseName = findViewById(R.id.tv_FS4_hoseName);

        tv_current_baud_rate = findViewById(R.id.tv_current_baud_rate);
        tv_NFS1 = findViewById(R.id.tv_NFS1);
        tv_NFS2 = findViewById(R.id.tv_NFS2);
        tv_NFS3 = findViewById(R.id.tv_NFS3);
        tv_NFS4 = findViewById(R.id.tv_NFS4);

       /* tv_fs1QTN = findViewById(R.id.tv_fs1QTN);
        tv_fs2QTN = findViewById(R.id.tv_fs2QTN);
        tv_fs3QTN = findViewById(R.id.tv_fs3QTN);
        tv_fs4QTN = findViewById(R.id.tv_fs4QTN);*/

        ImageView imgFuelLogo = findViewById(R.id.imgFuelLogo);
        linearHose = findViewById(R.id.linearHose);
        linear_fs_1 = findViewById(R.id.linear_fs_1);
        linear_fs_2 = findViewById(R.id.linear_fs_2);
        linear_fs_3 = findViewById(R.id.linear_fs_3);
        linear_fs_4 = findViewById(R.id.linear_fs_4);

        layout_get_vr_readings = findViewById(R.id.layout_get_vr_readings);
        edt_vr_command = findViewById(R.id.edt_vr_command);
        btn_get_vr_readings = findViewById(R.id.btn_get_vr_readings);
        tv_display_vr_response = findViewById(R.id.tv_display_vr_response);

        btnSetBaudRate = findViewById(R.id.btnSetBaudRate);

        btnSetBaudRate.setClickable(true);
    }

    public void selectHoseAction(View v) {
        refreshWiFiList();

    }

    //This function fires when the 'Poll Tanks' button is clicked.
    @SuppressWarnings("SameParameterValue")
    public void goButtonAction(View view) {
        //get information stored when page is refreshed, and load into locat variables
        //There is only one tank, so we can choose to get '0'

        try {  //try to load in data from cloud
            AppConstants.CURRENT_SELECTED_SSID = serverSSIDList.get(0).get("WifiSSId");
            AppConstants.CURRENT_HOSE_SSID = serverSSIDList.get(0).get("HoseId");
            AppConstants.CURRENT_SELECTED_SITEID = serverSSIDList.get(0).get("SiteId");
            AppConstants.SELECTED_MACADDRESS = serverSSIDList.get(0).get("MacAddress");
            if (!Constants.HardcodeVR_IP) {
                AppConstants.Current_VR_IP = serverSSIDList.get(0).get("VR_IP");
                AppConstants.Current_VR_Port = serverSSIDList.get(0).get("VR_Port");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }


    @Override
    public void onClick(View view) {

    }

    public void onChangeWifiAction(View view) {
        try {

            refreshWiFiList();


        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "onChangeWifiAction :", ex);
        }
    }

    private void refreshWiFiList() {
        new GetSSIDUsingLocation().execute();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.CONNECTION_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                String messageData = data.getStringExtra("MESSAGE");

                if (messageData.equalsIgnoreCase("true")) {
                    //no vehicle
                    //Intent intent = new Intent(WelcomeActivity.this, AcceptVehicleActivity.class);
                    //startActivity(intent);
                }
            }
        }

        /////////////////////////////////////////////

        switch (requestCode) {

            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("Splash", "User agreed to make required location settings changes.");

                        AppConstants.colorToast(getApplicationContext(), "Please wait...", Color.BLACK);


                        goButtonAction(null);

                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("Splash", "User chose not to make required location settings changes.");

                        AppConstants.colorToastBigFont(getApplicationContext(), "Please On GPS to connect WiFi", Color.BLUE);

                        break;
                }
                break;
        }
    }

    private boolean isNotNULL(String value) {

        boolean flag = true;
        if (value == null) {
            flag = false;
        } else if (value.trim().isEmpty()) {
            flag = false;
        } else if (value != null && value.trim().equalsIgnoreCase("null")) {
            flag = false;
        }

        return flag;
    }

    private void alertSelectHoseList(String errMsg) {


        final Dialog dialog = new Dialog(WelcomeActivity.this);
        dialog.setTitle("Fuel Secure");
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_hose_list);
        //dialog.getWindow().getAttributes().windowAnimations = R.style.DialogSlideAnimation;

        TextView tvNoFuelSites = dialog.findViewById(R.id.tvNoFuelSites);
        ListView lvHoseNames = dialog.findViewById(R.id.lvHoseNames);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        if (!errMsg.trim().isEmpty())
            tvNoFuelSites.setText(errMsg);

        if (serverSSIDList != null && serverSSIDList.size() > 0) {

            lvHoseNames.setVisibility(View.VISIBLE);
            tvNoFuelSites.setVisibility(View.GONE);

        } else {
            lvHoseNames.setVisibility(View.GONE);
            tvNoFuelSites.setVisibility(View.VISIBLE);
        }

        SimpleAdapter adapter = new SimpleAdapter(WelcomeActivity.this, serverSSIDList, R.layout.item_hose, new String[]{"item"}, new int[]{R.id.tvSingleItem});
        lvHoseNames.setAdapter(adapter);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        lvHoseNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //OnHoseSelected_OnClick(Integer.toString(position));

                new GetConnectedDevicesIP().execute();//Refreshed donnected devices list on hose selection.
                String IpAddress = "";
                SelectedItemPos = position;
                String selSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
                String IsBusy = serverSSIDList.get(SelectedItemPos).get("IsBusy");
                String selMacAddress = serverSSIDList.get(SelectedItemPos).get("MacAddress");
                String selSiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
                String hoseID = serverSSIDList.get(SelectedItemPos).get("HoseId");
                AppConstants.CURRENT_SELECTED_SSID = selSSID;
                AppConstants.CURRENT_HOSE_SSID = hoseID;
                AppConstants.CURRENT_SELECTED_SITEID = selSiteId;
                AppConstants.SELECTED_MACADDRESS = selMacAddress;
                String IsHoseNameReplaced = serverSSIDList.get(SelectedItemPos).get("IsHoseNameReplaced");
                String ReplaceableHoseName = serverSSIDList.get(SelectedItemPos).get("ReplaceableHoseName");
                if (!Constants.HardcodeVR_IP) {
                    AppConstants.Current_VR_IP = serverSSIDList.get(SelectedItemPos).get("VR_IP");
                    AppConstants.Current_VR_Port = serverSSIDList.get(SelectedItemPos).get("VR_Port");
                }
                //For now, run the VR activity every time the hose list is selected.
                //Intent vr_intent = new Intent(WelcomeActivity.this, VR_interface.class);
                //startService(vr_intent);

                //Rename SSID while mac address updation
                if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                    AppConstants.NeedToRenameFS_ON_UPDATE_MAC = false;
                    AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = "";
                } else {
                    AppConstants.NeedToRenameFS_ON_UPDATE_MAC = true;
                    AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = ReplaceableHoseName;
                }

                if (selMacAddress.trim().equals("")) {  //MacAddress on server is null

                    if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                        loading = new ProgressDialog(WelcomeActivity.this);
                        loading.setCancelable(true);
                        loading.setMessage("Updating mac address please wait..");
                        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        loading.setCancelable(false);
                        loading.show();

                        //Do not enable hotspot.
                        Constants.hotspotstayOn = false;

                        //AppConstants.colorToast(WelcomeActivity.this, "Updating mac address please wait..", Color.RED);
//                        wifiApManager.setWifiApEnabled(null, false);  //Hotspot disabled

                        // Toast.makeText(getApplicationContext(),"Enabled WIFI connecting to "+AppConstants.CURRENT_SELECTED_SSID,Toast.LENGTH_LONG).show();

                        WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        if (!wifiManagerMM.isWifiEnabled()) {
                            wifiManagerMM.setWifiEnabled(true);
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //new ChangeSSIDofHubStation().execute(); //Connect to selected (SSID) and Rename UserName and password of Fs unit
//                                new WiFiConnectTask().execute(); //1)Connect to selected (SSID) wifi network and 2)change the ssid and password settings to connect to Hub's hotspot 3)Update MackAddress
                            }
                        }, 1000);


                    } else {
                        AppConstants.colorToastBigFont(WelcomeActivity.this, "Can't update mac address,Hose is busy please retry later.", Color.RED);
                    }

                } else {

                    try {
                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                            String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                            if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (IpAddress.equals("")) {
                        //tvSSIDName.setText("Can't select this Hose not connected");
                        btnGo.setVisibility(View.GONE);

                    } else {

                        //Selected position
                        //Toast.makeText(getApplicationContext(), "FS Position" + position, Toast.LENGTH_SHORT).show();
                        AppConstants.FS_selected = String.valueOf(position);
                        if (String.valueOf(position).equalsIgnoreCase("0")) {

                            if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                //Rename SSID from cloud
                                if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                } else {
                                }

                                Constants.AccPersonnelPIN = "";
                                //tvSSIDName.setText(selSSID);
                                Constants.CurrentSelectedHose = "FS1";
                                btnGo.setVisibility(View.VISIBLE);
                            } else {
                                //tvSSIDName.setText("Hose in use.\nPlease try again later");
                                btnGo.setVisibility(View.GONE);

                            }
                        } else if (String.valueOf(position).equalsIgnoreCase("1")) {
                            if (Constants.FS_2STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                //Rename SSID from cloud
                                if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                    AppConstants.NeedToRenameFS2 = false;
                                    AppConstants.REPLACEBLE_WIFI_NAME_FS2 = "";
                                } else {
                                    AppConstants.NeedToRenameFS2 = true;
                                    AppConstants.REPLACEBLE_WIFI_NAME_FS2 = ReplaceableHoseName;
                                }

                                Constants.AccPersonnelPIN = "";
                                //tvSSIDName.setText(selSSID);
                                AppConstants.FS2_CONNECTED_SSID = selSSID;
                                Constants.CurrentSelectedHose = "FS2";
                                btnGo.setVisibility(View.VISIBLE);
                            } else {
                                //tvSSIDName.setText("Hose in use.\nPlease try again later");
                                btnGo.setVisibility(View.GONE);
                            }

                        } else if (String.valueOf(position).equalsIgnoreCase("2")) {


                            if (Constants.FS_3STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                //Rename SSID from cloud
                                if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                } else {
                                }

                                Constants.AccPersonnelPIN = "";
                                //tvSSIDName.setText(selSSID);
                                Constants.CurrentSelectedHose = "FS3";
                                btnGo.setVisibility(View.VISIBLE);
                            } else {
                                //tvSSIDName.setText("Hose in use.\nPlease try again later");
                                btnGo.setVisibility(View.GONE);
                            }


                        } else if (String.valueOf(position).equalsIgnoreCase("3")) {


                            if (Constants.FS_4STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                                //Rename SSID from cloud
                                if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                } else {
                                }

                                Constants.AccPersonnelPIN = "";
                                //tvSSIDName.setText(selSSID);
                                Constants.CurrentSelectedHose = "FS4";
                                btnGo.setVisibility(View.VISIBLE);
                            } else {
                                //tvSSIDName.setText("Hose in use.\nPlease try again later");
                                btnGo.setVisibility(View.GONE);
                            }
                        } else {

                            //tvSSIDName.setText("Can't select this Hose for current version");
                            btnGo.setVisibility(View.GONE);
                        }
                    }

                }
                dialog.dismiss();

            }
        });

        dialog.show();
    }


    private int getExistingNetworkId(String SSID) {

        SSID = "\"" + SSID + "\"";

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID != null && existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }


    //=========================Stop button functionality for each hose==============

    private void DisplayDashboardEveSecond() {

        if (!VRDeviceType.equalsIgnoreCase("BT")) {
            if (AppConstants.RunningPoll) {
                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.pressed_start_multi));
            } else {
                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFluid));
            }
        }


//        tv_fs1_Qty.setText(String.valueOf((int) Math.floor(Constants.TankLevel.get(0))));
        linear_fs_1.setBackgroundResource(R.color.colorPrimary);
        tv_NFS1.setTextColor(getResources().getColor(R.color.white));
        tv_FS1_hoseName.setTextColor(getResources().getColor(R.color.white));
//        tv_fs1QTN.setTextColor(getResources().getColor(R.color.white));
//        tv_fs1_Qty.setTextColor(getResources().getColor(R.color.white));

//        tv_fs2_Qty.setText(String.valueOf((int) Math.floor(Constants.TankLevel.get(1))));
        linear_fs_2.setBackgroundResource(R.color.colorPrimary);
        tv_NFS2.setTextColor(getResources().getColor(R.color.white));
        tv_FS2_hoseName.setTextColor(getResources().getColor(R.color.white));
//        tv_fs2QTN.setTextColor(getResources().getColor(R.color.white));
//        tv_fs2_Qty.setTextColor(getResources().getColor(R.color.white));

//        tv_fs3_Qty.setText(String.valueOf((int) Math.floor(Constants.TankLevel.get(2))));
        linear_fs_3.setBackgroundResource(R.color.colorPrimary);
        tv_NFS3.setTextColor(getResources().getColor(R.color.white));
        tv_FS3_hoseName.setTextColor(getResources().getColor(R.color.white));
//        tv_fs3QTN.setTextColor(getResources().getColor(R.color.white));
//        tv_fs3_Qty.setTextColor(getResources().getColor(R.color.white));

//        tv_fs4_Qty.setText(String.valueOf((int) Math.floor(Constants.TankLevel.get(3))));
        linear_fs_4.setBackgroundResource(R.color.colorPrimary);
        tv_NFS4.setTextColor(getResources().getColor(R.color.white));
        tv_FS4_hoseName.setTextColor(getResources().getColor(R.color.white));
//        tv_fs4QTN.setTextColor(getResources().getColor(R.color.white));
//        tv_fs4_Qty.setTextColor(getResources().getColor(R.color.white));
        //}

    }

    @Override
    public void onBackPressed() {

        finish();

    }


    public class GetSSIDUsingLocation extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage("Please wait...");
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String parm1 = AppConstants.getOriginalUUID_IMEIFromFile(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "Other";
                String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;


                System.out.println("parm1----" + parm1);
                System.out.println("parm2----" + parm2);

                String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, parm2);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();


                //------------------------------

            } catch (Exception e) {
                System.out.println("Ex" + e.getMessage());
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            tvLatLng.setText("Current Location :" + Constants.Latitude + "," + Constants.Longitude);

            System.out.println("GetSSIDUsingLocation...." + result);

            try {

                serverSSIDList.clear();
                //AppConstants.DetailsServerSSIDList.clear();

                String errMsg = "";

                if (result != null && !result.isEmpty()) {

                    JSONObject jsonObjectSite = new JSONObject(result);
                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                    if (ResponseMessageSite.equalsIgnoreCase("success")) {
                        VR_polling_interval = -1;
                        JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                        if (Requests.length() > 0) {

                            for (int i = 0; i < Requests.length(); i++) {
                                JSONObject c = Requests.getJSONObject(i);

                                //commented out the ones we are not using
                                String SiteId = c.getString("SiteId");
                                //String SiteNumber = c.getString("SiteNumber");
                                // String SiteName = c.getString("SiteName");
                                // String SiteAddress = c.getString("SiteAddress");
                                // String Latitude = c.getString("Latitude");
                                //String Longitude = c.getString("Longitude");
                                String HoseId = c.getString("HoseId");
                                // String HoseNumber = c.getString("HoseNumber");
                                String WifiSSId = c.getString("WifiSSId");
                                // String UserName = c.getString("UserName");
                                String Password = c.getString("Password");
                                String ResponceMessage = c.getString("ResponceMessage");
                                String ResponceText = c.getString("ResponceText");
                                String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                                String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                                String MacAddress = c.getString("MacAddress");
                                String IsBusy = c.getString("IsBusy");
                                //String IsUpgrade = c.getString("IsUpgrade");
                                String ScheduleTankReading = c.getString("ScheduleTankReading");


                                try {
                                    VR_polling_interval = Math.max(VR_polling_interval, Integer.parseInt(ScheduleTankReading));
                                } catch (Exception e) {

                                }


                                //Current Fs wifi password
                                Constants.CurrFsPass = Password;

                                HashMap<String, String> map = new HashMap<>();
                                map.put("SiteId", SiteId);
                                map.put("HoseId", HoseId);
                                map.put("WifiSSId", WifiSSId);
                                map.put("ReplaceableHoseName", ReplaceableHoseName);
                                map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                                map.put("item", WifiSSId);
                                map.put("MacAddress", MacAddress);
                                map.put("IsBusy", IsBusy);
                                if (!Constants.HardcodeVR_IP) {
                                    String VR_IP = c.getString("VR_IP");
                                    String VR_Port = c.getString("VR_Port");
                                    map.put("VR_IP", VR_IP);
                                    map.put("VR_Port", VR_Port);
                                }

                                if (ResponceMessage.equalsIgnoreCase("success")) {
                                    if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                        serverSSIDList.add(map);
                                        AppConstants.DetailsServerSSIDList = serverSSIDList;
                                    }
                                } else {
                                    errMsg = ResponceText;
//                                    AppConstants.AlertDialogFinish(WelcomeActivity.this, ResponceText);
                                }
                            }


                        }
                        //HoseList Alert
                        alertSelectHoseList(tvLatLng.getText().toString() + "\n" + errMsg);


                        JSONArray Requests2 = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_USER);
                        if (Requests2.length() > 0) {
                            for (int i = 0; i < Requests.length(); i++) {
                                JSONObject c = Requests.getJSONObject(i);
                                JSONArray tanks = c.getJSONArray("tanksObj");
                                if (tanks.length() > 0) {
                                    for (int j = 0; j < tanks.length(); i++) {
                                        JSONObject d = tanks.getJSONObject(j);
                                        String ScheduleTankReading = d.getString("ScheduleTankReading");
                                        try {
                                            VR_polling_interval = Math.max(VR_polling_interval, Integer.parseInt(ScheduleTankReading));
                                        } catch (Exception e) {

                                        }
                                    }
                                }
                            }
                        }


                    } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {
                        String ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);


//                        AppConstants.AlertDialogBox(WelcomeActivity.this, ResponseTextSite);


                    }
                } else {
                   // AppConstants.AlertDialogFinish(WelcomeActivity.this, "Unable to connect server. Please try again later!");
                }


            } catch (Exception e) {

                CommonUtils.LogMessage(TAG, " GetSSIDUsingLocation :" + result, e);
            }

            if (VR_polling_interval < 1) VR_polling_interval = 4;
            CommonUtils.LogMessage(TAG, " VR_polling_interval -:" + VR_polling_interval);

        }

    }

    public class GetSSIDUsingLocationOnResume extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage("Please wait...");
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String parm1 = AppConstants.getOriginalUUID_IMEIFromFile(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "Other";
                String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;


                System.out.println("parm1----" + parm1);
                System.out.println("parm2----" + parm2);

                String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, parm2);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                System.out.println("Ex" + e.getMessage());
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            tvLatLng.setText("Current Location :" + Constants.Latitude + "," + Constants.Longitude);

            System.out.println("GetSSIDUsingLocation...." + result);

            try {

                serverSSIDList.clear();
                //AppConstants.DetailsServerSSIDList.clear();

                String errMsg = "";

                if (result != null && !result.isEmpty()) {

                    JSONObject jsonObjectSite = new JSONObject(result);
                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                    if (ResponseMessageSite.equalsIgnoreCase("success")) {
                        VR_polling_interval = -1;
                        JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                        if (Requests.length() > 0) {

                            for (int i = 0; i < Requests.length(); i++) {
                                JSONObject c = Requests.getJSONObject(i);

                                //commented out the ones we are not using
                                String SiteId = c.getString("SiteId");
                                //String SiteNumber = c.getString("SiteNumber");
                                //String SiteName = c.getString("SiteName");
                                //String SiteAddress = c.getString("SiteAddress");
                                //String Latitude = c.getString("Latitude");
                                //String Longitude = c.getString("Longitude");
                                String HoseId = c.getString("HoseId");
                                // String HoseNumber = c.getString("HoseNumber");
                                String WifiSSId = c.getString("WifiSSId");
                                //String UserName = c.getString("UserName");
                                String Password = c.getString("Password");
                                String ResponceMessage = c.getString("ResponceMessage");
                                String ResponceText = c.getString("ResponceText");
                                String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                                String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                                String MacAddress = c.getString("MacAddress");
                                String IsBusy = c.getString("IsBusy");
                                //String IsUpgrade = c.getString("IsUpgrade");
                                String ScheduleTankReading = c.getString("ScheduleTankReading");

                                try {
                                    VR_polling_interval = Math.max(VR_polling_interval, Integer.parseInt(ScheduleTankReading));
                                } catch (Exception e) {
                                }

                                String BluetoothCardReaderHF = c.getString("BluetoothCardReaderHF");
                                try {
                                    String vrmac = c.getString("VeederRootMacAddress");
                                    if (!vrmac.equalsIgnoreCase("null"))
                                        AppConstants.VR_MAC = vrmac;
                                } catch (Exception ex) {
                                    System.out.println("server did not give VR mac address");
                                }


                                //Current Fs wifi password
                                Constants.CurrFsPass = Password;

                                HashMap<String, String> map = new HashMap<>();
                                map.put("SiteId", SiteId);
                                map.put("HoseId", HoseId);
                                map.put("WifiSSId", WifiSSId);
                                map.put("ReplaceableHoseName", ReplaceableHoseName);
                                map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                                map.put("item", WifiSSId);
                                map.put("MacAddress", MacAddress);
                                map.put("IsBusy", IsBusy);
                                //commeting out this for now, as we will se hardcoded host and port
                                if (!Constants.HardcodeVR_IP) {
                                    String VR_IP = c.getString("VR_IP");
                                    String VR_Port = c.getString("VR_Port");
                                    map.put("VR_IP", VR_IP);
                                    map.put("VR_Port", VR_Port);
                                }

                                try {
                                    String VR_MAC = c.getString("VeederRootMacAddress");
                                    map.put("VR_MAC", VR_MAC);
                                } catch (Exception ex) {
                                    System.out.println("server did not give VR mac address");
                                }

                                if (ResponceMessage.equalsIgnoreCase("success")) {
                                    if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                        serverSSIDList.add(map);
                                        AppConstants.DetailsServerSSIDList = serverSSIDList;

                                        //#73--Only one FS unit display
                                        /*if (serverSSIDList != null && serverSSIDList.size() == 0) {

                                            tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                            OnHoseSelected_OnClick(Integer.toString(0));

                                        }*/

                                    }
                                } else {
                                    errMsg = ResponceText;
//                                    AppConstants.AlertDialogFinish(WelcomeActivity.this, ResponceText);
                                }
                            }


                        }
                        //HoseList Alert
                        //alertSelectHoseList(tvLatLng.getText().toString() + "\n" + errMsg);
                        JSONObject Requests2 = jsonObjectSite.getJSONObject("objUserData");

                        JSONArray tanks = Requests2.getJSONArray("tanksObj");
                        if (tanks.length() > 0) {
                            for (int j = 0; j < tanks.length(); j++) {
                                JSONObject d = tanks.getJSONObject(j);
                                String ScheduleTankReading = d.getString("ScheduleTankReading");
                                try {
                                    VR_polling_interval = Math.max(VR_polling_interval, Integer.parseInt(ScheduleTankReading));
                                } catch (Exception e) {
                                }
                            }

                        }

                    } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {
//                        String ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);
//
//
//                        AppConstants.AlertDialogBox(WelcomeActivity.this, ResponseTextSite);


                    }
                } else {
                   // AppConstants.AlertDialogFinish(WelcomeActivity.this, "Unable to connect server. Please try again later!");
                    CommonUtils.LogMessage(TAG, " server response is empty. Restarting the app.");
                    RestartApplication();
                }

            } catch (Exception e) {

                CommonUtils.LogMessage(TAG, " GetSSIDUsingLocation :" + result, e);
            }
            if (VR_polling_interval < 1) VR_polling_interval = 4;

        }
    }

    private void RestartApplication() {
        Intent i = new Intent(WelcomeActivity.this, SplashActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    public class GetConnectedDevicesIP extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;


        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(WelcomeActivity.this);
            dialog.setMessage("Fetching connected device info..");
            dialog.setCancelable(false);
            dialog.show();

        }

        protected String doInBackground(String... arg0) {

            ListOfConnectedDevices.clear();

            String resp = "";

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    BufferedReader br = null;
                    boolean isFirstLine = true;

                    /*try {
                        br = new BufferedReader(new FileReader("/proc/net/arp"));
                        String line;

                        while ((line = br.readLine()) != null) {
                            if (isFirstLine) {
                                isFirstLine = false;
                                continue;
                            }

                            String[] splitted = line.split(" +");

                            if (splitted != null && splitted.length >= 4) {

                                String ipAddress = splitted[0];
                                String macAddress = splitted[3];
                                System.out.println("IPAddress" + ipAddress);
                                boolean isReachable = InetAddress.getByName(
                                        splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                                if (isReachable) {
                                    Log.d("Device Information", ipAddress + " : "
                                            + macAddress);
                                }

                                if (ipAddress != null || macAddress != null) {

                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("ipAddress", ipAddress);
                                    map.put("macAddress", macAddress);

                                    ListOfConnectedDevices.add(map);

                                }
                                AppConstants.DetailsListOfConnectedDevices = ListOfConnectedDevices;

                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }*/
                }
            });
            thread.start();


            return resp;


        }


        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);


            dialog.dismiss();

        }

    }

    public class ChangeBusyStatus extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";

            String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(WelcomeActivity.this) + ":" + userEmail + ":" + "UpgradeIsBusyStatus");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.CURRENT_SELECTED_SITEID;


            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {

                // pd.dismiss();
                System.out.println("eeee" + result);

            } catch (Exception e) {
                System.out.println("eeee" + e);
            }
        }
    }

    private void CustomDilaogEnterpin(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_debugwindow);
        dialogBus.show();

        final EditText edt_code = (EditText) dialogBus.findViewById(R.id.edt_code);
        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btn_cancel);
        edt_message.setText(Html.fromHtml(title));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String code = edt_code.getText().toString().trim();

                if (code != null && !code.isEmpty()) {

                    try {

                        if (code.equalsIgnoreCase("5432154321")) {
                            dialogBus.dismiss();
                            CustomDilaogSetBaudRate(WelcomeActivity.this, "Set New Baud rate", "tt");

                        } else {
                            //Wrong pin try using correct one..
                            Toast.makeText(WelcomeActivity.this, "Wrong Pin Entered", Toast.LENGTH_SHORT).show();
                            dialogBus.dismiss();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }

            }
        });

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }
            }
        };

        dialogBus.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 20000);

    }

    private void CustomDilaogSetBaudRate(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_debugwindow);
        dialogBus.show();

        final EditText edt_code = (EditText) dialogBus.findViewById(R.id.edt_code);
        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btn_cancel);
        edt_message.setText(Html.fromHtml(title));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String code = edt_code.getText().toString().trim();

                if (code != null && !code.isEmpty()) {
                    //Toast.makeText(AcceptVehicleActivity_new.this, "Done", Toast.LENGTH_SHORT).show();
                    CommonUtils.SaveBaudRateInPref(WelcomeActivity.this, code, Constants.PREF_BAUDRATE);
                    dialogBus.dismiss();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    WelcomeActivity.this.finish();
                    System.exit(0);
                    //finish();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }

            }
        });

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }
            }
        };

        dialogBus.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 20000);

    }


    private void EnterpinForEditCommand(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_debugwindow);
        dialogBus.show();

        final EditText edt_code = (EditText) dialogBus.findViewById(R.id.edt_code);
        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btn_cancel);
        edt_message.setText(Html.fromHtml(title));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String code = edt_code.getText().toString().trim();

                if (code != null && !code.isEmpty()) {

                    try {

                        if (code.equalsIgnoreCase("5432154321")) {
                            dialogBus.dismiss();
                            SetCommand(WelcomeActivity.this, "Set New Command", "tt");

                        } else {
                            //Wrong pin try using correct one..
                            Toast.makeText(WelcomeActivity.this, "Wrong Pin Entered", Toast.LENGTH_SHORT).show();
                            dialogBus.dismiss();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }

            }
        });

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }
            }
        };

        dialogBus.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 20000);

    }

    private void SetCommand(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_debugwindow);
        dialogBus.show();

        final EditText edt_code = (EditText) dialogBus.findViewById(R.id.edt_code);
        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btn_cancel);
        edt_message.setText(Html.fromHtml(title));

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String code = edt_code.getText().toString().trim();

                if (code != null && !code.isEmpty()) {
                    //Toast.makeText(AcceptVehicleActivity_new.this, "Done", Toast.LENGTH_SHORT).show();
                    edt_vr_command.setText(code);
                    dialogBus.dismiss();

                    /*try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    WelcomeActivity.this.finish();
                    System.exit(0);*/
                    //finish();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }

            }
        });

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (dialogBus.isShowing()) {
                    dialogBus.dismiss();
                }
            }
        };

        dialogBus.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 20000);

    }

    @Override
    public void onStart() {
        super.onStart();

        if (VRDeviceType.equalsIgnoreCase("BT"))
            this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CommonUtils.LogMessage(TAG, "in onDestroy");
        Log.i(TAG,"surelockcheck onDestroy");
        if (VRDeviceType.equalsIgnoreCase("BT")){

            if (connected != Connected.False)
                disconnect();

        }


        try {
            if (exact_alarm_rec != null) {
                exact_alarm_rec = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Log.i(TAG,"surelockcheck onPostResume");

        if (VRDeviceType.equalsIgnoreCase("BT")) {
            layout_get_vr_readings.setVisibility(View.VISIBLE);
            requestMultiplePermissions();
        }

    }

    private void requestMultiplePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                            CodeBegins();
                        } else if (report.isAnyPermissionPermanentlyDenied()) { // check for permanent denial of any permission
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                            Toast.makeText(getApplicationContext(), "Permissions is denied permanently by user!", Toast.LENGTH_SHORT).show();
                        } else {
                            finish();
                        }

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    private void CodeBegins() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.disable()) {
            bluetoothAdapter.enable();
        }
        GetPairedDevicesList();
        startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
        bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void connect(String fromWhr) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(SplashActivity.VR_BT_MAC_ADDR);
            status("Connecting..." + SplashActivity.VR_BT_MAC_ADDR + " -> " + fromWhr);
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, " BT connect Exception: " + e.getMessage() + "\n", null);
        }
    }

    private void send(String str) {

        current_Command = str;
        CompleteResponse = "";

        if (connected == Connected.True) {

            try {

                CommonUtils.LogMessage(TAG, " send: " + str + "\n", null);

                byte[] data = new BigInteger(str, 16).toByteArray();

                service.write(data);
            } catch (Exception e) {
                connected = Connected.False;
                onSerialIoError(e);
                CommonUtils.LogMessage(TAG, " catch: " + e.getMessage() + "\n", null);
            }
        } else {
            CommonUtils.LogMessage(TAG, " send else BT not connected: " + "\n", null);
        }
    }

    private void receive(byte[] data) {
        String respStr = new String(data);

        CompleteResponse = CompleteResponse + respStr;

        String etx = String.valueOf((char) 3);
        if (respStr.contains(etx)) { //  coming at the end of the response.
            CommonUtils.LogMessage(TAG, "Command: " + current_Command + "; Received CompleteResponse: \n" + CompleteResponse, null);

            if (current_Command.equalsIgnoreCase(AppConstants.BT_Level_Command)) {
                CommonUtils.LogMessage(TAG, "Parsing LEVEL Response", null);
                parseBTLevelResponse(CompleteResponse);
                tv_display_vr_response.setText(CompleteResponse);

                if (!AppConstants.ReceiveDeliveryInformation) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.LogMessage(TAG, "Rescheduling alarms for further readings.");
                            InitializeVRInitAlarmService();
                        }
                    }, 30000);
                }

            } else if (current_Command.equalsIgnoreCase(AppConstants.BT_Delivery_Command)) {
                CommonUtils.LogMessage(TAG, "Parsing DELIVERY Response", null);
                ParseBTDeliveryResponse(CompleteResponse); // To Parse Delivery response.
                tv_display_vr_response.setText(CompleteResponse);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CommonUtils.LogMessage(TAG, "Rescheduling alarms for further readings");
                        InitializeVRInitAlarmService();
                    }
                }, 30000);
            }
        }
    }

    private void InitializeVRInitAlarmService() {

        CommonUtils.LogMessage(TAG, "InitializeVRService : starting VRInitAlarmService"); // #2238
        this.startService(new Intent(WelcomeActivity.this, VRInitAlarmService.class));
    }

    private void status(String str) {

        if (VRDeviceType.equalsIgnoreCase("BT")) {

            layout_get_vr_readings.setVisibility(View.VISIBLE);
            Log.i(TAG, "Status:" + str);
            CommonUtils.LogMessage(TAG, "BT status : " + str + "\n", null);

            if (str.equalsIgnoreCase("Connected")) {
                linearMac.setVisibility(View.GONE);
                btn_disConnect.setVisibility(View.INVISIBLE);
                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFluid));
            } else {
                linearMac.setVisibility(View.GONE);
                btn_disConnect.setVisibility(View.GONE);
                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.pressed_start_multi));
            }
        }

    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);


    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }

    @Override
    public void onSerialConnect() {
        status("Connected");
        connected = Connected.True;
        CommonUtils.LogMessage(TAG, "Sending commands from onSerialConnect.");
        CommonUtils.LogMessage(TAG, "Sending command to get Levels");
        send(AppConstants.BT_Level_Command);  //Temp commented to check

        if (AppConstants.ReceiveDeliveryInformation) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Send BT command to get Deliveries
                    CommonUtils.LogMessage(TAG, "Sending command to get Deliveries");
                    send(AppConstants.BT_Delivery_Command);
                }
            }, 30000);
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        stopService(new Intent(this, SerialService.class));
        status("Disconnect-4 (SerialConnectError): " + e.getMessage());

    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("Disconnect-5 (SerialIoError): " + e.getMessage());

    }

    private void GetPairedDevicesList() {

        // Get paired devices.
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, "DeviceName:" + deviceName + "\n" + "MacAddress:" + deviceHardwareAddress);

            }
        }
    }

    public void parseBTLevelResponse(String allResp) {

        try {
            /*
            allResp = "\u0001200\n" +
                    "Sarasota Sheriff\n" +
                    "16501 Honore Ave\n" +
                    "Nokomis Fl.\n" +
                    "\n" +
                    "\n" +
                    "MAY 04, 2021 05:38 AM\n" +
                    "\n" +
                    "TANK  PRODUCT                GALLONS   INCHES   WATER  DEG F   ULLAGE\n" +
                    "  1   Diesel                     125    13.42     0.0   80.8      386\n" +
                    "  2   Ethanol Free               154    15.60     0.0   80.2      357\n" +
                    "  3   Premium                    343    29.12     0.6   80.6      168\n" +
                    "  4   Regular                   3881    51.87     0.9   79.5     6119\n" +
                    "\n" +
                    "\u0003";


             */


            String nLine[] = allResp.split("\n");


            int num = 0;
            for (int i = 0; i < nLine.length; i++) {
                String line = nLine[i].trim();

                if (line.contains("TANK") && line.contains("PRODUCT")) {
                    num = i + 1;
                    break;
                }
            }

            for (int i = num; i < nLine.length; i++) {
                String levels = nLine[i].trim();

                String vals[] = levels.split(" ");

                ArrayList<String> TankList = new ArrayList<>();

                for (int k = 0; k < vals.length; k++) {
                    String data = vals[k];
                    if (!data.isEmpty()) {
                        TankList.add(data);
                    }
                }

                String VR_DTime = nLine[6];
                if (VR_DTime.contains("\r")) {
                    VR_DTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                }

                if (TankList.size() >= 7) {
                    System.out.println("data-" + TankList.toString());

                    int tankSize = TankList.size();
                    String _TANK = TankList.get(0);
                    String _PRODUCT = TankList.get(1);
                    String _GALLONS = TankList.get(tankSize - 5);
                    String _INCHES = TankList.get(tankSize - 4);
                    String _WATER = TankList.get(tankSize - 3);
                    String _DEG_F = TankList.get(tankSize - 2);
                    String _ULLAGE = TankList.get(tankSize - 1);

                    System.out.println("end of tank details...");
////////////////////////////////////////////////////////////////////////////////////

                    Gson gson = new Gson();
                    String jsonData = "";
                    String userEmail = Login_Email;

                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveInventoryVeederTankMonitorReading");


                    VR_Inventory_InfoEntity authEntityClass = new VR_Inventory_InfoEntity();

                    authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(WelcomeActivity.this);
                    authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(WelcomeActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                    authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                    authEntityClass.VRDateTime = VR_DTime;

                    authEntityClass.TankNumber = _TANK; //I should perhaps check that this is the same tank we asked for
                    authEntityClass.ProductCode = _PRODUCT;
                    authEntityClass.TankStatus = "0";
                    authEntityClass.Volume = _GALLONS;
                    authEntityClass.TCVolume = _GALLONS;
                    authEntityClass.Ullage = _ULLAGE;
                    authEntityClass.Height = _INCHES;
                    authEntityClass.Water = _WATER;
                    authEntityClass.Temperature = _DEG_F;
                    authEntityClass.WaterVolume = _WATER;
                    authEntityClass.ForceReadingSave = AppConstants.VRForceReadingSave;
                    jsonData = gson.toJson(authEntityClass);

                    HashMap<String, String> imap = new HashMap<>();
                    imap.put("jsonData", jsonData);
                    imap.put("authString", authString);
                    controller.insertTransactions(imap);

                    CommonUtils.LogMessage(TAG, " receive jsonData: " + jsonData, null);
                }


            }
            startService(new Intent(WelcomeActivity.this, BackgroundService.class));

        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, " receive Response parse Ex: " + e.getMessage(), null);
        }
    }

    private void Do_next_BT_Service() {

        try {
            CommonUtils.LogMessage(TAG, "Do_next_BT_Service:  VR_polling_interval=" + VR_polling_interval, null);

            if (VR_polling_interval < 1) {
                VR_polling_interval = 6;
            }
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            int target_h = (hour / (24 / VR_polling_interval) + 1) * (24 / VR_polling_interval);  //uses int division to get the next hour that satisfies the interval

            Calendar target_time = Calendar.getInstance();
            target_time.set(year, month, day, target_h, 0, 0);

            Intent name = new Intent(this, BackgroundService.class);
            PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, FLAG_CANCEL_CURRENT);
            Constants.alarm.cancel(pintent); //remove previous alarms so we don't have multiple firing.
            Long wait_time = (target_time.getTimeInMillis() - cal.getTimeInMillis());
            Constants.alarm.setRepeating(AlarmManager.RTC_WAKEUP, wait_time + currentTimeMillis(), (long) (24.0 / VR_polling_interval) * 60 * 60 * 1000, pintent);  //by defualt, 6 hours

        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, "Do_next_BT_Service:  Exception=" + e.getMessage(), null);
        }
    }

    public void CheckIfDevicesIsPaired(String MacAddressForBTVeederRoot) {
        try {
            // Get paired devices.
            boolean Ispaired = false;
            String deviceName = "";
            String deviceHardwareAddress = "";

            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    deviceName = device.getName();
                    deviceHardwareAddress = device.getAddress(); // MAC address
                    if (deviceHardwareAddress.equalsIgnoreCase(MacAddressForBTVeederRoot)) {
                        Ispaired = true;
                    }
                }
            }

            if (!Ispaired) {
                Log.i(TAG, "Device Not found in List of paired device. DeviceName:" + deviceName + "\n" + "MacAddress:" + deviceHardwareAddress);
                CommonUtils.LogMessage(TAG, "Device Not found in List of paired device. DeviceName:" + deviceName + "\n" + "MacAddress:" + deviceHardwareAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void GetVRReadingsManually() {
        //For VR-Manuall Readings...
        try {
            current_Command = "";
            CommonUtils.LogMessage(TAG, "Get VR Readings Manually");
            AppConstants.VRForceReadingSave = "y";
            String vr_command = edt_vr_command.getText().toString().trim().replace(" ", "");
            if (!vr_command.isEmpty()) {

                //If VR-Connected execute command
                if (connected == Connected.True) {
                    CommonUtils.LogMessage(TAG, "Sending commands from GetVRReadingsManually: BT_Status: Connected");
                    CommonUtils.LogMessage(TAG, "Sending command to get Levels");
                    send(vr_command);//vr_command

                    if (AppConstants.ReceiveDeliveryInformation) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Send BT command to get Deliveries
                                CommonUtils.LogMessage(TAG, "Sending command to get Deliveries");
                                send(AppConstants.BT_Delivery_Command);
                            }
                        }, 30000);
                    }
                } else {

                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }

                    connect("re-try manually");

                    // Commented below code, sending command from onSerialConnect() after connection attempt.
                    //Wait while connecting..
                    /*new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.LogMessage(TAG, " send from GetVRReadingsManually: re-try manually");
                            send(vr_command);//vr_command

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // Send BT command to get Deliveries
                                    send(AppConstants.BT_Delivery_Command);
                                }
                            },30000);
                        }
                    }, 6000);*/
                }

            } else {
                Toast.makeText(getApplicationContext(), "Please enter command", Toast.LENGTH_SHORT).show();
            }

            //Clear response text view in 60 sec
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    tv_display_vr_response.setText("");
                    AppConstants.VRForceReadingSave = "n";
                }
            }, 60000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void GetVRReadingsExactAlarm() {

        //For VR-ExactAlarm Readings...
        try {

            //If VR-Connected execute command
           /* if (connected == Connected.True) {
                send("01323030");
            } else { }*/

                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                    CommonUtils.LogMessage(TAG, "Re-establishConnection BT Enabled");
                }

                btnGo.setBackgroundColor(ContextCompat.getColor(WelcomeActivity.this, R.color.pressed_start_multi));
                CommonUtils.LogMessage(TAG, "Re-establishConnection Disconnect");
                disconnect();
                Thread.sleep(5000);
                CommonUtils.LogMessage(TAG, "Re-establishConnection Re-connect ");;
                connect("re-try VR-ExactAlarm");
               /* Thread.sleep(5000);
                send("01323030");*/

            //Clear response text view in 60 sec
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ex_vr = true;
                    tv_display_vr_response.setText("");
                    // startService(new Intent(WelcomeActivity.this, BackgroundService.class));
                }
            }, 60000);

        } catch (Exception e) {
            ex_vr = true;
            e.printStackTrace();
            CommonUtils.LogMessage(TAG, "GetVRReadingsExactAlarm Ex: " + e.toString());
        }
    }



    private void InitAlarm() {

        try {

            Constants.alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            /*Constants.exat_alarm_12am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_12pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_8am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_8pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_6am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_6pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_4am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Constants.exat_alarm_4pm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);*/

        } catch (Exception e) {
            e.printStackTrace();
            CommonUtils.LogMessage(TAG, "InitAlarm Ex: " + e.toString());
        }

    }

    /*private void RemoveAllPreviousSetAlarms() {
        try {
            //Remove previous alarms so we don't have multiple firing.
            Intent intent = new Intent(this, ExactAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);
            Constants.exat_alarm_12am.cancel(pendingIntent);
            Constants.exat_alarm_12pm.cancel(pendingIntent);
            Constants.exat_alarm_8am.cancel(pendingIntent);
            Constants.exat_alarm_8pm.cancel(pendingIntent);
            Constants.exat_alarm_6am.cancel(pendingIntent);
            Constants.exat_alarm_6pm.cancel(pendingIntent);
            Constants.exat_alarm_4am.cancel(pendingIntent);
            Constants.exat_alarm_4pm.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
            CommonUtils.LogMessage(TAG, "RemoveAllPreviousSetAlarms Ex: " + e.toString());
        }
    }*/

    public class ExactAlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            CommonUtils.LogMessage(TAG, "ExactAlarm: onReceive " + ex_vr);
            if (ex_vr) {
                ex_vr = false;
                Log.i(TAG,"surelockcheck ExactAlarm onReceive");
                AppConstants.VRForceReadingSave = "n";

                if (!VRDeviceType.equalsIgnoreCase("BT")) {
                    CommonUtils.LogMessage(TAG, "AppVersion => " + CommonUtils.getVersionCode(WelcomeActivity.this) + "; VRDeviceType => " + VRDeviceType);
                    CommonUtils.LogMessage(TAG, "ExactAlarm : starting VR_interface"); // #2238
                    Intent vr_intent = new Intent(WelcomeActivity.this, VR_interface.class);
                    startService(vr_intent);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ex_vr = true;
                            tv_display_vr_response.setText("");
                        }
                    }, 30000);
                } else {
                    CommonUtils.LogMessage(TAG, "AppVersion => " + CommonUtils.getVersionCode(WelcomeActivity.this) + "; VRDeviceType => " + VRDeviceType + "; MacAddressForBTVeederRoot => " + MacAddressForBTVeederRoot);
                    CommonUtils.LogMessage(TAG, "ExactAlarm : GetVRReadingsExactAlarm"); // #2238
                    GetVRReadingsExactAlarm();
                }
            }
        }
    }



    private void ReestablishConnection() {

        try {

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
                CommonUtils.LogMessage(TAG, "Re-establishConnection BT Enabled: ");
            }

            if (connected == Connected.True) {
                btnGo.setBackgroundColor(ContextCompat.getColor(WelcomeActivity.this, R.color.colorFluid));

            } else {
                btnGo.setBackgroundColor(ContextCompat.getColor(WelcomeActivity.this, R.color.pressed_start_multi));
               CommonUtils.LogMessage(TAG, "Re-establishConnection Disconnect: ");
               disconnect();
               Thread.sleep(5000);
                CommonUtils.LogMessage(TAG, "Re-establishConnection Re-connect ");
                connect("re-establish connection");

            }

            //Application restart code
            /*PackageManager packageManager = getApplicationContext().getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(getApplicationContext().getPackageName());
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            getApplicationContext().startActivity(mainIntent);
            Runtime.getRuntime().exit(0);

            System.exit(0);*/

        } catch (Exception e) {
            e.printStackTrace();
            CommonUtils.LogMessage(TAG, "Re-establishConnection Exception: " + e.getMessage(), null);
        }

    }

    public void getAppToForeground(){

        try {

            boolean foregroud = new ForegroundCheckTask().execute(getApplicationContext()).get();
            System.out.println("JobService foregroud"+foregroud);

            if (!foregroud){

                    CommonUtils.LogMessage(TAG ," getAppToForeground fun");

                    PackageManager packageManager = getApplicationContext().getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage(getApplicationContext().getPackageName());
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    getApplicationContext().startActivity(mainIntent);
                    Runtime.getRuntime().exit(0);

                    System.exit(0);

            }else{
                CommonUtils.LogMessage(TAG ," App already in foreground");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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

    ///New VR Code beginning !!
    private void InitializeVRService() {

        Intent myIntent = new Intent(WelcomeActivity.this, VRInitAlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, myIntent, PendingIntent.FLAG_IMMUTABLE);

        // Set the alarm to start at 11:10 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 10);

        if (calendar.after(Calendar.getInstance())) {
            CommonUtils.LogMessage(TAG, "InitializeVRService : starting VRInitAlarmService"); // #2238
            this.startService(new Intent(WelcomeActivity.this, VRInitAlarmService.class));
        }

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                60000 * 60 * 24, pendingIntent);
    }

    public void ParseBTDeliveryResponse(String response) {

        try {

            /*String allResp = "^A\n" +
                    "I20200\n" +
                    "06/06/23  2:34 PM\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "                                                                 Volume=GALLONS\n" +
                    "                                                                  Height=INCHES\n" +
                    "DELIVERY REPORT                                                 Temp=FAHRENHEIT\n" +
                    "\n" +
                    "\n" +
                    "TANK  2:UNLEADED            \n" +
                    "\n" +
                    "                                   Fuel    FuelTC     Water      Fuel      Fuel\n" +
                    "        Date / Time              Volume    Volume    Height      Temp    Height\n" +
                    "\n" +
                    " START: 06/06/23  9:46 AM          4626      4622      0.00     61.42     44.37\n" +
                    "   END: 06/06/23 10:04 AM          7891      7869      0.00     63.94     68.83\n" +
                    "AMOUNT:                            3265      3247\n" +
                    "\n" +
                    " START: 06/02/23 12:40 PM          4897      4894      0.00     61.00     46.31\n" +
                    "   END: 06/02/23  1:03 PM          8418      8400      0.00     63.11     73.56\n" +
                    "AMOUNT:                            3521      3506\n" +
                    "\n" +
                    "\n" +
                    "TANK  3:DIESEL T3           \n" +
                    "\n" +
                    "                                   Fuel    FuelTC     Water      Fuel      Fuel\n" +
                    "        Date / Time              Volume    Volume    Height      Temp    Height\n" +
                    "\n" +
                    " START: 06/06/23  9:44 AM          5944      5938      0.00     62.21     53.78\n" +
                    "   END: 06/06/23 10:07 AM          8515      8497      0.00     64.77     74.48\n" +
                    "AMOUNT:                            2571      2559\n" +
                    "\n" +
                    " START: 06/02/23 12:38 PM          6665      6657      0.00     62.68     59.09\n" +
                    "   END: 06/02/23 12:52 PM          8246      8232      0.00     63.78     71.96\n" +
                    "AMOUNT:                            1581      1575\n" +
                    "\n" +
                    "\n" +
                    "TANK  4:DIESEL T4           \n" +
                    "\n" +
                    "                                   Fuel    FuelTC     Water      Fuel      Fuel\n" +
                    "        Date / Time              Volume    Volume    Height      Temp    Height\n" +
                    "\n" +
                    " START: 06/06/23  9:44 AM          5929      5924      0.00     61.76     53.67\n" +
                    "   END: 06/06/23 10:07 AM          8568      8553      0.00     63.88     74.99\n" +
                    "AMOUNT:                            2639      2629\n" +
                    "\n" +
                    " START: 06/02/23 12:38 PM          6695      6689      0.00     62.00     59.31\n" +
                    "   END: 06/02/23 12:52 PM          8222      8208      0.00     63.82     71.75\n" +
                    "AMOUNT:                            1527      1519\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "^C";*/

            String[] nLine = response.split("\n");
            String VR_DTime = "";

            //================ Date-Time ==================
            int num = 0;
            for (int i = 0; i < nLine.length; i++) {
                String line = nLine[i].trim();

                if (line.contains("I20200")) {
                    num = i;
                    try {
                        VR_DTime = nLine[i + 1]; // Send as it is to the server

                        if (VR_DTime.contains("\r")) {
                            VR_DTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                        }
                    } catch (Exception e) {
                        VR_DTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                    }
                    break;
                }
            }
            //==============================================

            StringBuilder sbMainData = new StringBuilder();
            for (int i = num; i < nLine.length; i++) { // To get only main data (i.e. Tank, Product, Start-End Readings)
                String line = nLine[i].trim();

                if (line.contains("TANK") || line.contains("START") || line.contains("END")) { // || (nLine[i + 1].trim().contains("INCREASE")) || line.contains("AMOUNT") // Test before release
                    sbMainData.append(line).append("\n");
                }
            }

            ArrayList<HashMap<String,String>> deliveryDataList = new ArrayList<>();

            String[] dataLines = sbMainData.toString().split("\n");

            HashMap<String,String> map = null;
            String TankNumber = "", Product = "";

            for (String dataLine : dataLines) {

                String line = dataLine.trim();

                if (line.contains("TANK")) {
                    TankNumber = "";
                    Product = "";
                    String[] vals = line.split(":");

                    if (vals.length > 1) {
                        String tank = vals[0];
                        TankNumber = tank.substring(4).trim();

                        Product = vals[1].trim();
                    }
                } else {
                    if (!TankNumber.isEmpty()) {

                        if (line.contains("START")) {
                            map = new HashMap<>();
                            map.put("TankNumber", TankNumber);
                            map.put("Product", Product);
                            map.put("VRDateTime", VR_DTime);

                            String[] vals = line.split(" ");
                            ArrayList<String> startDataList = new ArrayList<>();

                            for (String data : vals) {
                                if (!data.isEmpty()) {
                                    startDataList.add(data);
                                }
                            }

                            if (startDataList.size() > 8) {
                                int dataSize = startDataList.size();
                                map.put("StartDateTime", startDataList.get(1) + " " + startDataList.get(2) + " " + startDataList.get(3));
                                map.put("StartVolume", startDataList.get(dataSize - 5));
                                map.put("StartTCVolume", startDataList.get(dataSize - 4));
                                map.put("StartWater", startDataList.get(dataSize - 3));
                                map.put("StartTemp", startDataList.get(dataSize - 2));
                                map.put("StartHeight", startDataList.get(dataSize - 1));
                            }
                        } else if (line.contains("END")) {

                            String[] vals = line.split(" ");
                            ArrayList<String> endDataList = new ArrayList<>();

                            for (String data : vals) {
                                if (!data.isEmpty()) {
                                    endDataList.add(data);
                                }
                            }

                            if (endDataList.size() > 8) {
                                int dataSize = endDataList.size();
                                map.put("EndDateTime", endDataList.get(1) + " " + endDataList.get(2) + " " + endDataList.get(3));
                                map.put("EndVolume", endDataList.get(dataSize - 5));
                                map.put("EndTCVolume", endDataList.get(dataSize - 4));
                                map.put("EndWater", endDataList.get(dataSize - 3));
                                map.put("EndTemp", endDataList.get(dataSize - 2));
                                map.put("EndHeight", endDataList.get(dataSize - 1));
                            }

                            if (map != null) {
                                deliveryDataList.add(map);
                                //TankNumber = ""; // Code to get only latest reading for single tank
                                // Commented above line as per #2238 => JOHN 6-30-23  Save all the deliveries
                            }
                        }
                    }
                }
            }

            // Get tank numbers and tank monitor numbers which have the "ReceiveDeliveryInformation" flag set to true
            ArrayList<String> TankNumberListToReceiveDelivery = new ArrayList<>();
            ArrayList<String> TankMonitorNumberListToReceiveDelivery = new ArrayList<>();
            for (int i = 0; i < Constants.TankList.size(); i++) {
                String tankNumber = Constants.TankList.get(i).get("TankNumber");
                String TankMonitorNumber = Constants.TankList.get(i).get("TankMonitorNumber");
                String receiveDeliveryInformation = Constants.TankList.get(i).get("ReceiveDeliveryInformation");

                if (receiveDeliveryInformation != null && receiveDeliveryInformation.equalsIgnoreCase("True")) {
                    TankNumberListToReceiveDelivery.add(tankNumber);
                    TankMonitorNumberListToReceiveDelivery.add(TankMonitorNumber);
                }
            }

            // Sort readings by START DateTime
            try {
                deliveryDataList.sort(new HashMapComparator("StartDateTime"));
            } catch (Exception e) {
                Log.e(TAG, "ParseBTDeliveryResponse: Exception while sorting readings: " + e.getMessage());
                CommonUtils.LogMessage(TAG, "ParseBTDeliveryResponse: Exception while sorting readings: " + e.getMessage(), null);
            }

            for (int i = 0; i < deliveryDataList.size(); i++) {
                String tankNumber = deliveryDataList.get(i).get("TankNumber");
                if (TankNumberListToReceiveDelivery.contains(tankNumber) || TankMonitorNumberListToReceiveDelivery.contains(tankNumber)) {

                    Gson gson = new Gson();
                    String jsonData = "";
                    String authString = "";
                    String userEmail = Login_Email;

                    authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveDeliveryVeederTankMonitorReading");

                    VR_Delivery_InfoEntity authEntityClass = new VR_Delivery_InfoEntity();

                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(WelcomeActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                    authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(WelcomeActivity.this);
                    authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                    authEntityClass.AppDateTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                    authEntityClass.VRDateTime = deliveryDataList.get(i).get("VRDateTime");
                    authEntityClass.TankNumber = tankNumber;
                    authEntityClass.ProductCode = deliveryDataList.get(i).get("Product");
                    authEntityClass.StartDateTime = deliveryDataList.get(i).get("StartDateTime");
                    authEntityClass.EndDateTime = deliveryDataList.get(i).get("EndDateTime");
                    authEntityClass.StartVolume = deliveryDataList.get(i).get("StartVolume");
                    authEntityClass.EndVolume = deliveryDataList.get(i).get("EndVolume");
                    authEntityClass.StartTCVolume = deliveryDataList.get(i).get("StartTCVolume");
                    authEntityClass.EndTCVolume = deliveryDataList.get(i).get("EndTCVolume");
                    authEntityClass.StartWater = deliveryDataList.get(i).get("StartWater");
                    authEntityClass.EndWater = deliveryDataList.get(i).get("EndWater");
                    authEntityClass.StartTemp = deliveryDataList.get(i).get("StartTemp");
                    authEntityClass.EndTemp = deliveryDataList.get(i).get("EndTemp");
                    authEntityClass.StartHeight = deliveryDataList.get(i).get("StartHeight");
                    authEntityClass.EndHeight = deliveryDataList.get(i).get("EndHeight");

                    jsonData = gson.toJson(authEntityClass);

                    HashMap<String, String> imap = new HashMap<>();
                    imap.put("jsonData", jsonData);
                    imap.put("authString", authString);
                    controller.insertTransactions(imap);

                    CommonUtils.LogMessage(TAG, "DELIVERY jsonData: " + jsonData, null);
                } else {
                    CommonUtils.LogMessage(TAG, "DELIVERY Skipped for Tank: " + tankNumber, null);
                }
            }
            startService(new Intent(this, BackgroundService.class));

        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, "parseBTDeliveryResponse Exception: " + e.getMessage(), null);
        }
    }
}