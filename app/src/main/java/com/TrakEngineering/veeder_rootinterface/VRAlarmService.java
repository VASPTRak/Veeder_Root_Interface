package com.TrakEngineering.veeder_rootinterface;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.TrakEngineering.veeder_rootinterface.enity.VR_Inventory_InfoEntity;
import com.google.gson.Gson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static com.TrakEngineering.veeder_rootinterface.AppConstants.Login_Email;
import static com.TrakEngineering.veeder_rootinterface.Constants.VR_polling_interval;

public class VRAlarmService extends Service implements ServiceConnection,SerialListener {

    private static final String TAG = VRAlarmService.class.getSimpleName();
    Context ctx;
    private enum Connected {False, Pending, True}
    private BluetoothAdapter bluetoothAdapter;
    private SerialService service;
    private Connected connected = Connected.False;
    public String CompleteResponse = "", VRDeviceType = "", MacAddressForBTVeederRoot = "";
    final DBController controller = new DBController(this);

    public VRAlarmService() {
       // Log.i(TAG,"VRAlarmService");
        ctx = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {

            CommonUtils.LogMessage(TAG, "VRAlarmService started."); // #2238
            SharedPreferences sharedPrefG = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            String VRDeviceType = sharedPrefG.getString("VRDeviceType", "BT");
            String MacAddressForBTVeederRoot = sharedPrefG.getString("MacAddressForBTVeederRoot", "");
            SplashActivity.VR_BT_MAC_ADDR = MacAddressForBTVeederRoot;

            Log.i(TAG," VR_polling_interval:"+VR_polling_interval);
            CommonUtils.LogMessage(TAG, " ~VRAlarmService~ PollingInterval:" + VR_polling_interval);

            //if (!VRDeviceType.equalsIgnoreCase("BT")) { // #2238
            if (VRDeviceType.equalsIgnoreCase("BT")) {
                CommonUtils.LogMessage(TAG, "AppVersion => " + CommonUtils.getVersionCode(ctx) + "; VRDeviceType => " + VRDeviceType + "; MacAddressForBTVeederRoot => " + MacAddressForBTVeederRoot);
                CommonUtils.LogMessage(TAG, "VRAlarmService : starting VR_interface"); // #2238
                Intent vr_intent = new Intent(getApplicationContext(), VR_interface.class);
                getApplicationContext().startService(vr_intent);
            } else {
                CommonUtils.LogMessage(TAG, "AppVersion: " + CommonUtils.getVersionCode(ctx) + "; VRDeviceType: " + VRDeviceType + "; MacAddressForBTVeederRoot: " + MacAddressForBTVeederRoot);
                CheckIfDevicesIsPaired(MacAddressForBTVeederRoot);
                CodeBegins();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connect("onStartCommand");
                    }
                }, 4000);

            }

        }catch (Exception e){
            e.printStackTrace();
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
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

    //ServiceConnection
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        CommonUtils.LogMessage(TAG, "VR onServiceConnected");
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        CommonUtils.LogMessage(TAG, "VR onServiceDisconnected");
        service = null;
    }

    //SerialListener
    @Override
    public void onSerialConnect() {

        status("Connected");
        connected = Connected.True;
        send("01323030");  //Temp commented to check
    }

    @Override
    public void onSerialConnectError(Exception e) {
        stopService(new Intent(this, SerialService.class));
        status("Disconnect-4" + e.getMessage());
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("Disconnect-5 " + e.getMessage());

    }

    private void connect(String fromWhr) {
        try {
            CommonUtils.LogMessage(TAG, " temp log connect command");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(SplashActivity.VR_BT_MAC_ADDR);
            status("Connecting..." + SplashActivity.VR_BT_MAC_ADDR + " ->" + fromWhr);
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, " BT connect Exception: " + e.getMessage() + "\n", null);
        }
    }

    private void send(String str) {

        CompleteResponse = "";

        if (connected == Connected.True) {
            try {

                CommonUtils.LogMessage(TAG, "VRAlarmService: send: " + str + "\n", null);

                byte[] data = new BigInteger(str, 16).toByteArray();

                service.write(data);
            } catch (Exception e) {
                connected = Connected.False;
                onSerialIoError(e);
                CommonUtils.LogMessage(TAG, " send: catch: " + e.getMessage() + "\n", null);
            }
        } else {
            CommonUtils.LogMessage(TAG, " send else BT not connected: " + "\n", null);
        }
    }

    private void receive(byte[] data) {
        String respStr = new String(data);

        CompleteResponse = CompleteResponse + respStr;

        String etx = String.valueOf((char) 3);
        if (respStr.contains(etx)) {
            CommonUtils.LogMessage(TAG, "VRAlarmService: receive CompleteResponse: " + CompleteResponse, null);

            parseBTresponse(CompleteResponse);
            //tv_display_vr_response.setText(CompleteResponse);
        }


    }

    private void status(String str) {

        CommonUtils.LogMessage(TAG, " VR  BT status : " + str);
        if (VRDeviceType.equalsIgnoreCase("BT")) {

            Log.i(TAG, "Status:" + str);
            CommonUtils.LogMessage(TAG, " BT status : " + str + "\n", null);

            if (str.equalsIgnoreCase("Connected")) {
//                linearMac.setVisibility(View.GONE);
//                btn_disConnect.setVisibility(View.INVISIBLE);
//                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorFluid));
            } else {
//                linearMac.setVisibility(View.GONE);
//                btn_disConnect.setVisibility(View.GONE);
//                btnGo.setBackgroundColor(ContextCompat.getColor(this, R.color.pressed_start_multi));
            }
        }

    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
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

    public void parseBTresponse(String allResp) {

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

                    authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(this);
                    authEntityClass.VeederRootMacAddress = AppConstants.VR_MAC;
                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
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
            startService(new Intent(this, BackgroundService.class));

        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, " receive Response parse Ex: " + e.getMessage(), null);
        }
    }
}