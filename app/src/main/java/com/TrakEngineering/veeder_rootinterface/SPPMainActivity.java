package com.TrakEngineering.veeder_rootinterface;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public class SPPMainActivity extends AppCompatActivity implements ServiceConnection, SerialListener {


    private enum Connected {False, Pending, True}

    private BluetoothAdapter bluetoothAdapter;

    private Button btn_exe_cmd, btn_connect, btn_clr_response;
    private EditText edt_cmd, edt_mac_address;
    private TextView tv_status, tv_response;
    private static final String TAG = SPPMainActivity.class.getSimpleName();
    private SerialService service;
    private String newline = "\r\n";

    private Connected connected = Connected.False;

    private String deviceAddress = "";//00:14:03:05:F2:9B";//""10:52:1C:85:72:1E";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spp_main);


        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_exe_cmd = (Button) findViewById(R.id.btn_exe_cmd);

        btn_clr_response = (Button) findViewById(R.id.btn_clr_response);
        edt_mac_address = (EditText) findViewById(R.id.edt_mac_address);
        edt_cmd = (EditText) findViewById(R.id.edt_cmd);
        tv_response = (TextView) findViewById(R.id.tv_response);
        tv_status = (TextView) findViewById(R.id.tv_status);

        requestMultiplePermissions();


        edt_cmd.setText("");
        edt_mac_address.setText(deviceAddress);

        status("Disconnect");


        btn_clr_response.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_response.setText("");
                btn_clr_response.setVisibility(View.GONE);
            }
        });

        btn_exe_cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int nu = tv_response.getText().toString().length();
                if (tv_response.getText().toString().length() >= 5) {
                    btn_clr_response.setVisibility(View.VISIBLE);
                } else {
                    btn_clr_response.setVisibility(View.GONE);
                }
                send(edt_cmd.getText().toString().trim());

            }
        });


        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String str = edt_mac_address.getText().toString().trim();
                if (str != null && !str.isEmpty()) {
                    deviceAddress = str;
                    if (connected == Connected.True) {
                        status("Disconnect");
                        disconnect();
                    } else {
                        connect();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (deviceAddress != null && !deviceAddress.isEmpty())
            edt_mac_address.setText(deviceAddress);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        SPPMainActivity.this.stopService(new Intent(this, SerialService.class));
        super.onDestroy();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        requestMultiplePermissions();
    }

    private void requestMultiplePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        SPPMainActivity.this.startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
        SPPMainActivity.this.bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("Connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(SPPMainActivity.this.getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(String str) {

        if (connected != Connected.True) {
            Toast.makeText(SPPMainActivity.this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv_response.append(spn);

            byte[] data = new BigInteger(str,16).toByteArray();

            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
            tv_response.append(e.getMessage());
        }
    }

    private void receive(byte[] data) {

        SpannableStringBuilder spn = new SpannableStringBuilder(new String(data) + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorResponse)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_response.append(spn);
    }

    private void status(String str) {

        Log.i(TAG, "Status:" + str);
        //tv_status.setText(str);
        SpannableStringBuilder spn = new SpannableStringBuilder(str);
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv_status.setText(spn);
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);

        SPPMainActivity.this.runOnUiThread(this::connect);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }

    @Override
    public void onSerialConnect() {
        status("Connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("Disconnect");
        e.printStackTrace();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("Disconnect");
        e.printStackTrace();
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
}
