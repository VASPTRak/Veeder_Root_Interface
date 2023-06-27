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

import com.TrakEngineering.veeder_rootinterface.enity.VR_Delivery_InfoEntity;
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
    public String current_Command = "";

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
            CommonUtils.LogMessage(TAG, " ~VRAlarmService~ PollingInterval: " + VR_polling_interval);

            if (!VRDeviceType.equalsIgnoreCase("BT")) {
                CommonUtils.LogMessage(TAG, "AppVersion => " + CommonUtils.getVersionCode(ctx) + "; VRDeviceType => " + VRDeviceType);
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
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    //SerialListener
    @Override
    public void onSerialConnect() {

        status("Connected");
        connected = Connected.True;
        CommonUtils.LogMessage(TAG, "VRAlarmService: Sending commands from onSerialConnect.");
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
            //CommonUtils.LogMessage(TAG, " temp log connect command");
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

        current_Command = str;
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
        if (respStr.contains(etx)) { //  coming at the end of the response.
            CommonUtils.LogMessage(TAG, "VRAlarmService: Command: " + current_Command + "; Received CompleteResponse: \n" + CompleteResponse, null);

            if (current_Command.equalsIgnoreCase(AppConstants.BT_Level_Command)) {
                CommonUtils.LogMessage(TAG, "VRAlarmService: Parsing LEVEL Response", null);
                parseBTresponse(CompleteResponse);

            } else if (current_Command.equalsIgnoreCase(AppConstants.BT_Delivery_Command)) {
                CommonUtils.LogMessage(TAG, "VRAlarmService: Parsing DELIVERY Response", null);
                ParseBTDeliveryResponse(CompleteResponse); // To Parse Delivery response.
            }
            //tv_display_vr_response.setText(CompleteResponse);
        }


    }

    private void status(String str) {

        CommonUtils.LogMessage(TAG, " VRAlarmService: BT status : " + str);
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

            int num = 0;
            for (int i = 0; i < nLine.length; i++) {
                String line = nLine[i].trim();

                if (line.contains("TANK")) { // first instance of TANK word
                    num = i;
                    try {
                        VR_DTime = nLine[i - 12];

                        String[] vals = VR_DTime.split(" "); // To remove extra space between date and time
                        ArrayList<String> dateList = new ArrayList<>();

                        for (String data : vals) {
                            if (!data.isEmpty()) {
                                dateList.add(data);
                            }
                        }

                        if (dateList.size() > 2) {
                            VR_DTime = dateList.get(0) + " " + dateList.get(1) + " " + dateList.get(2);
                        }
                        if (VR_DTime.contains("\r")) {
                            VR_DTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                        }
                    } catch (Exception e) {
                        VR_DTime = AppConstants.currentDate("yyyy-MM-dd HH:mm:ss");
                    }
                    break;
                }
            }

            StringBuilder sbMainData = new StringBuilder();
            for (int i = num; i < nLine.length; i++) { // To get only main data (i.e. Tank, Product, Start-End Readings)
                String line = nLine[i].trim();

                if (line.contains("TANK") || line.contains("START") || line.contains("END")) {
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
                                TankNumber = ""; // Code to get only latest reading for single tank
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

            for (int i = 0; i < deliveryDataList.size(); i++) {
                String tankNumber = deliveryDataList.get(i).get("TankNumber");
                if (TankNumberListToReceiveDelivery.contains(tankNumber) || TankMonitorNumberListToReceiveDelivery.contains(tankNumber)) {

                    Gson gson = new Gson();
                    String jsonData = "";
                    String authString = "";
                    String userEmail = Login_Email;

                    authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getOriginalUUID_IMEIFromFile(this) + ":" + userEmail + ":" + "SaveDeliveryVeederTankMonitorReading");

                    VR_Delivery_InfoEntity authEntityClass = new VR_Delivery_InfoEntity();

                    authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
                    authEntityClass.IMEI_UDID = AppConstants.getOriginalUUID_IMEIFromFile(this);
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

                    CommonUtils.LogMessage(TAG, "VRAlarmService: DELIVERY jsonData: " + jsonData, null);
                } else {
                    CommonUtils.LogMessage(TAG, "VRAlarmService: DELIVERY Skipped for Tank: " + tankNumber, null);
                }
            }
            startService(new Intent(this, BackgroundService.class));

        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, "VRAlarmService: parseBTDeliveryResponse Exception: " + e.getMessage(), null);
        }
    }
}