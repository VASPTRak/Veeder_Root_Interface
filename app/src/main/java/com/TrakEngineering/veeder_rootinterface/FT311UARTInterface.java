//User must modify the below package with their package name
package com.TrakEngineering.veeder_rootinterface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/******************************FT311 GPIO interface class******************************************/
public class FT311UARTInterface extends BackgroundService {

    private final static String TAG = com.TrakEngineering.veeder_rootinterface.FT311UARTInterface.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.TrakEngineering.veeder_rootinterface.USB_PERMISSION";
    public static String ManufacturerString = "mManufacturer=FTDI";
    public static String ModelString1 = "mModel=FTDIUARTDemo";
    public static String ModelString2 = "mModel=Android Accessory FT312D";
    public static String VersionString = "mVersion=1.0";
    final int maxnumbytes = 65536;
    public UsbManager usbmanager;
    public UsbAccessory usbaccessory;
    public PendingIntent mPermissionIntent;
    public ParcelFileDescriptor filedescriptor = null;
    public FileInputStream inputstream = null;
    public FileOutputStream outputstream = null;
    public boolean mPermissionRequestPending = false;
    public read_thread readThread;
    public boolean datareceived = false;
    public boolean READ_ENABLE = false;
    public boolean accessory_attached = false;
    public SharedPreferences intsharePrefSettings;
    private byte[] usbdata;
    private byte[] writeusbdata;
    private byte[] readBuffer; /*circular buffer*/
    private int readcount;
    private int totalBytes;
    private int writeIndex;
    private int readIndex;
    private byte status;
    private IntentFilter filter;
    private Context localcontext;
    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            Toast.makeText(Constants.global_context, "Allow USB Permission", Toast.LENGTH_SHORT).show();
                            OpenAccessory(accessory);
                        } else {
                            Toast.makeText(Constants.global_context, "Deny USB Permission", Toast.LENGTH_SHORT).show();
                            Log.d("LED", "permission denied for accessory " + accessory);

                        }
                        mPermissionRequestPending = false;
                    }
                } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                    saveDetachPreference();
                    DestroyAccessory(true);
                    //CloseAccessory();
                } else {
                    Log.d("LED", "....");
                }
            } catch (Exception ex) {
                CommonUtils.LogMessage(TAG, "FT311UARTInterface: mUsbReceiver onReceive Exception: ", ex);
            }
        }
    };

    /*constructor*/
    public FT311UARTInterface(Context context, SharedPreferences sharePrefSettings) {
        super();
        intsharePrefSettings = sharePrefSettings;
        /*shall we start a thread here or what*/
        usbdata = new byte[1024];
        writeusbdata = new byte[256];
        /*128(make it 256, but looks like bytes should be enough)*/
        readBuffer = new byte[maxnumbytes];

        readIndex = 0;
        writeIndex = 0;
        /***********************USB handling******************************************/

        usbmanager = (UsbManager) Constants.global_context.getSystemService(Context.USB_SERVICE);
        // Log.d("LED", "usbmanager" +usbmanager);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);

        inputstream = null;
        outputstream = null;

        context.registerReceiver(mUsbReceiver, filter);

        localcontext = context;
        Looper.prepare();
    }

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        try {
            /*prepare the baud rate buffer*/
            writeusbdata[0] = (byte) baud;
            writeusbdata[1] = (byte) (baud >> 8);
            writeusbdata[2] = (byte) (baud >> 16);
            writeusbdata[3] = (byte) (baud >> 24);

            /*data bits*/
            writeusbdata[4] = dataBits;
            /*stop bits*/
            writeusbdata[5] = stopBits;
            /*parity*/
            writeusbdata[6] = parity;
            /*flow control*/
            writeusbdata[7] = flowControl;

            /*send the UART configuration packet*/
            SendPacket(8);
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: SetConfig Exception: ", ex);
        }
    }

    /*write data*/
    public byte SendData(int numBytes, byte[] buffer) {
        try {
            status = 0x00; /*success by default*/
            /*
             * if num bytes are more than maximum limit
             */
            if (numBytes < 1) {
                /*return the status with the error in the command*/
                return status;
            }

            /*check for maximum limit*/
            if (numBytes > 256) {
                numBytes = 256;
            }

            /*prepare the packet to be sent*/
            // CommonUtils.LogMessage(TAG, "write"+String.format("%02x",buffer[count]));
            System.arraycopy(buffer, 0, writeusbdata, 0, numBytes);

            if (numBytes != 64) {
                SendPacket(numBytes);
            } else {
                byte temp = writeusbdata[63];
                SendPacket(63);
                writeusbdata[0] = temp;
                SendPacket(1);
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: SendData Exception: ", ex);
        }
        return status;
    }

    /*read data*/
    public byte ReadData(int numBytes, byte[] buffer, int[] actualNumBytes) {
        try {
            status = 0x00; /*success by default*/

            /*should be at least one byte to read*/
            if ((numBytes < 1) || (totalBytes == 0)) {
                actualNumBytes[0] = 0;
                status = 0x01;
                return status;
            }

            /*check for max limit*/
            if (numBytes > totalBytes)
                numBytes = totalBytes;

            /*update the number of bytes available*/
            totalBytes -= numBytes;

            actualNumBytes[0] = numBytes;

            /*copy to the user buffer*/
            for (int count = 0; count < numBytes; count++) {
                buffer[count] = readBuffer[readIndex];
                //Log.w("UART", String.format("%02x",buffer[count]));
                //CommonUtils.LogMessage(TAG, "read"+String.format("%02x",buffer[count]));
                readIndex++;
                /*shouldnt read more than what is there in the buffer,
                 * 	so no need to check the overflow
                 */
                readIndex %= maxnumbytes;
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: ReadData Exception: ", ex);
        }
        return status;
    }

    /*method to send on USB*/
    private int SendPacket(int numBytes) {
        try {
            if (outputstream != null) {
                outputstream.write(writeusbdata, 0, numBytes);
                return 1;
            }
        } catch (IOException e) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: SendPacket Exception: ", e);
            return -1;
        }
        return 0;
    }

    /*resume accessory*/
    public int ResumeAccessory() {
        try {
            // Intent intent = getIntent();
            if (inputstream != null && outputstream != null) {
                return 1;
            }

            UsbAccessory[] accessories = usbmanager.getAccessoryList();

            if (accessories != null) {
//			Looper.prepare();
                Toast.makeText(Constants.global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();
//			Looper.loop();
            } else {
                // return 2 for accessory detached case
                //Log.e(">>@@","ResumeAccessory RETURN 2 (accessories == null)");
                accessory_attached = false;
                return 2;
            }

            UsbAccessory accessory = (accessories == null ? null : accessories[0]);
            if (accessory != null) {
                if (!accessory.toString().contains(ManufacturerString)) {
//				Looper.prepare();
                    Toast.makeText(Constants.global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
//				Looper.loop();
                    CommonUtils.LogMessage(TAG, "FT311UARTInterface: Manufacturer is not matched!");
                    return 1;
                }

                if (!accessory.toString().contains(ModelString1) && !accessory.toString().contains(ModelString2)) {
//				Looper.prepare();
                    Toast.makeText(Constants.global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
//				Looper.loop();
                    CommonUtils.LogMessage(TAG, "FT311UARTInterface: Model is not matched!");
                    return 1;
                }

                if (!accessory.toString().contains(VersionString)) {
//				Looper.prepare();
                    Toast.makeText(Constants.global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
//				Looper.loop();
                    CommonUtils.LogMessage(TAG, "FT311UARTInterface: Version is not matched!");
                    return 1;
                }

//			Looper.prepare();
                Toast.makeText(Constants.global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();
                CommonUtils.LogMessage(TAG, "FT311UARTInterface: Manufacturer, Model & Version are matched!");
//			Looper.loop();
                accessory_attached = true;

                if (usbmanager.hasPermission(accessory)) {
                    OpenAccessory(accessory);
                } else {
                    synchronized (mUsbReceiver) {
                        if (!mPermissionRequestPending) {
//						Looper.prepare();
                            Toast.makeText(Constants.global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
//						Looper.loop();
                            CommonUtils.LogMessage(TAG, "FT311UARTInterface: Requesting USB_PERMISSION.");
                            usbmanager.requestPermission(accessory,
                                    mPermissionIntent);
                            mPermissionRequestPending = true;
                        }
                    }
                }
            } else {
                CommonUtils.LogMessage(TAG, "FT311UARTInterface: No UART2 permission");
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: ResumeAccessory Exception: ", ex);
        }
        return 0;
    }

    /*destroy accessory*/
    public void DestroyAccessory(boolean bConfiged) {
        try {
            if (bConfiged) {
                READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
                writeusbdata[0] = 0;  // send dummy data for instream.read going
                SendPacket(1);
            } else {
                SetConfig(Integer.parseInt(AppConstants.BaudRate), (byte) 1, (byte) 8, (byte) 0, (byte) 0);  // send default setting data for config
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
                writeusbdata[0] = 0;  // send dummy data for instream.read going
                SendPacket(1);
                if (accessory_attached) {
                    saveDefaultPreference();
                }
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CloseAccessory();
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: DestroyAccessory Exception: ", ex);
        }
    }

    /*********************helper routines*************************************************/
    public void OpenAccessory(UsbAccessory accessory) {
        try {
            filedescriptor = usbmanager.openAccessory(accessory);
            if (filedescriptor != null) {
                usbaccessory = accessory;

                FileDescriptor fd = filedescriptor.getFileDescriptor();

                inputstream = new FileInputStream(fd);
                outputstream = new FileOutputStream(fd);
                /*check if any of them are null*/
                if (inputstream == null || outputstream == null) {
                    return;
                }

                if (!READ_ENABLE) {
                    READ_ENABLE = true;
                    readThread = new read_thread(inputstream);
                    CommonUtils.LogMessage(TAG, "Connected to UART1");
                    readThread.start();
                }
                CommonUtils.LogMessage(TAG, "FT311UARTInterface: Connected to UART2");
            } else {
                CommonUtils.LogMessage(TAG, "FT311UARTInterface: Could not connect to UART");
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: OpenAccessory Exception: ", ex);
        }
    }

    private void CloseAccessory() {
        try {
            try {
                if (filedescriptor != null)
                    filedescriptor.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (inputstream != null)
                    inputstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (outputstream != null)
                    outputstream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            /*FIXME, add the notfication also to close the application*/

            filedescriptor = null;
            inputstream = null;
            outputstream = null;

            localcontext.unregisterReceiver(mUsbReceiver);
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: CloseAccessory Exception: ", ex);
        }
    }

    protected void saveDetachPreference() {
        try {
            if (intsharePrefSettings != null) {
                intsharePrefSettings.edit()
                        .putString("configed", "FALSE")
                        .commit();
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: saveDetachPreference Exception: ", ex);
        }
    }

    protected void saveDefaultPreference() {
        try {
            if (intsharePrefSettings != null) {
                intsharePrefSettings.edit().putString("configed", "TRUE").commit();
                intsharePrefSettings.edit().putInt("baudRate", Integer.parseInt(AppConstants.BaudRate)).commit();
                intsharePrefSettings.edit().putInt("stopBit", 1).commit();
                intsharePrefSettings.edit().putInt("dataBit", 8).commit();
                intsharePrefSettings.edit().putInt("parity", 0).commit();
                intsharePrefSettings.edit().putInt("flowControl", 0).commit();
            }
        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "FT311UARTInterface: saveDefaultPreference Exception: ", ex);
        }
    }

    /*usb input data handler*/
    private class read_thread extends Thread {
        FileInputStream instream;

        read_thread(FileInputStream stream) {
            instream = stream;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        public void run() {
            while (READ_ENABLE) {
                while (totalBytes > (maxnumbytes - 1024)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if (instream != null) {
                        readcount = instream.read(usbdata, 0, 1024);

                        //printUSBdata
                        ArrayList<String> txts = new ArrayList<String>();
                        for (int i = 0; i < readcount; i++) {
                            txts.add(String.format("%02x", usbdata[i]));
                        }
                        String msg = TextUtils.join(" ", txts);
                        Log.d(TAG, "rawUSB " + msg);



                        if (readcount > 0) {
                            for (int count = 0; count < readcount; count++) {
                                readBuffer[writeIndex] = usbdata[count];
                                writeIndex++;
                                writeIndex %= maxnumbytes;
                            }

                            if (writeIndex >= readIndex)
                                totalBytes = writeIndex - readIndex;
                            else
                                totalBytes = (maxnumbytes - readIndex) + writeIndex;

//					    		Log.e(">>@@","totalBytes:"+totalBytes);
                        }
                    }
                } catch (Exception ex) {
                    CommonUtils.LogMessage(TAG, "FT311UARTInterface: read_thread inner Exception: ", ex);
                }
            }
        }
    }
}