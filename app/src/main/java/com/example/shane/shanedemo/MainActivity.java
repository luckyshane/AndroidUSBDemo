package com.example.shane.shanedemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.shane.shanedemo.usb.PutianUsbDevice;
import com.example.shane.shanedemo.usb.UsbUtil;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.chinamobile.lightcontrol.USB_PERMISSION_ACTION";

    private PutianUsbDevice putianUsbDevice;
    private UsbDevice targetDevice;
    private UsbManager usbManager;
    private UsbEndpoint inEndPoint;
    private UsbEndpoint outEndPoint;
    private UsbDeviceConnection usbDeviceConnection;
    private View usbBtn;
    private TextView logTv;
    private StringBuilder stringBuilder = new StringBuilder(256);

    private BroadcastReceiver usbMonitorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive " + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "device: " + device.getDeviceName() + ", " + device.getDeviceId() + ", "
                        + device.getDeviceProtocol() + ", " + device.getDeviceClass()
                        + ", vendorId: " + device.getVendorId() + ", productId: " + device.getProductId());
                ToastUtil.show("检测到USB插入");
                if (UsbUtil.isTargetPutianDevice(device)) {
                    //checkPermission(device);
                    initDevice(device);
                } else {
                    displayUSBSpotBtn(false);
                    targetDevice = null;
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                ToastUtil.show("USB拔出");
                displayUSBSpotBtn(false);
                targetDevice = null;
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        ToastUtil.show("授权成功");
                        displayUSBSpotBtn(true);
                        targetDevice = usbDevice;
                        initDevice(targetDevice);
                    } else {
                        Log.e(TAG, "permission is denied");
                        ToastUtil.show("没有权限读取USB设备");
                        displayUSBSpotBtn(false);
                        targetDevice = null;
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        startMonitorUSB();
        checkUSBDeviceConnected();
    }

    private void initView() {
        ToastUtil.init(this);
        usbBtn = findViewById(R.id.usb_btn);
        usbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readIMEIFromDevice(targetDevice);
            }
        });
        logTv = findViewById(R.id.log_tv);
        findViewById(R.id.clear_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stringBuilder = new StringBuilder();
                logTv.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopMonitorUSB();
        super.onDestroy();
    }

    private void startMonitorUSB() {
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbMonitorReceiver, usbFilter);
    }

    private void stopMonitorUSB() {
        unregisterReceiver(usbMonitorReceiver);
    }

    private UsbDevice findTargetDevice(Collection<UsbDevice> usbDeviceList) {
        if (usbDeviceList != null && !usbDeviceList.isEmpty()) {
            for (UsbDevice usbDevice : usbDeviceList) {
                if (isTargetDevice(usbDevice)) {
                    return usbDevice;
                }
            }
        }
        return null;
    }

    private boolean isTargetDevice(UsbDevice usbDevice) {
        return (4660 == usbDevice.getVendorId() && 17185 == usbDevice.getProductId());
    }

    private void readIMEIFromDevice(final UsbDevice usbDevice) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // UsbInterface usbInterface = usbDevice.getInterface(0);
                //Log.d(TAG, "connectDevice endpoint count : " + usbInterface.getEndpointCount());
                //UsbEndpoint inEndPoint = usbInterface.getEndpoint(
                //UsbEndpoint outEndpoint = usbInterface.getEndpoint(1);
                //UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
                //usbDeviceConnection.claimInterface(usbInterface, true);


                final long serial = putianUsbDevice.getSerial();
                if (serial > 0) {
                    ToastUtil.show("获取序列号成功");
                } else {
                    ToastUtil.show("获取序列号失败");
                }


                /*String cmd = "03B300B3";
                byte[] cmdBytes = StringUtils.toBytesFromHexStr(cmd);

                int ret = putianUsbDevice.write(cmdBytes, cmdBytes.length);
                //int ret = usbDeviceConnection.bulkTransfer(outEndPoint, cmdBytes, cmdBytes.length, 5000);
                Log.d(TAG, "connectDevice send ret: " + ret);
                log("Connect send " + StringUtils.toHexStr(cmdBytes) + ", ret: " + ret);
                if (ret > 0) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    byte[] readBuffer = new byte[64];
                    //ret = usbDeviceConnection.bulkTransfer(inEndPoint, readBuffer, readBuffer.length, 10000);
                    ret = putianUsbDevice.read(readBuffer);
                    log("read ret: " + ret);
                    if (ret > 0) {
                        final String responseStr = StringUtils.toHexStr(readBuffer);
                        final String imei = responseStr.substring(34, 50);
                        final String serial = responseStr.substring(4, 12);
                        log("read response: " + responseStr + ", serial: " + serial + ", imei: " + imei);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // mIMEIEditText.setText(imei);
                            }
                        });
                    }
                }*/
            }
        }).start();
    }

    private void checkUSBDeviceConnected() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.e(TAG, "usbManager null");
            return;
        }
        UsbDevice targetDevice = UsbUtil.getConnectedPutianDevice(usbManager);
        if (targetDevice != null) {
            initDevice(targetDevice);
        }
    }

    private void checkPermission(UsbDevice usbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            displayUSBSpotBtn(true);
            targetDevice = usbDevice;
            initDevice(targetDevice);
            return;
        }


    }

    private void displayUSBSpotBtn(boolean visible) {
        usbBtn.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void log(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringBuilder.append(txt);
                stringBuilder.append("\n");
                logTv.setText(stringBuilder.toString());
            }
        });
    }

    private void requestPermission(UsbDevice usbDevice) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, pendingIntent);
    }


    private void initDevice(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return;
        }
        if (putianUsbDevice != null) {
            putianUsbDevice.release();
            putianUsbDevice = null;
        }
        putianUsbDevice = new PutianUsbDevice(usbDevice);
        putianUsbDevice.setLogFunc(new PutianUsbDevice.LogFunc() {
            @Override
            public void log(String text) {
                MainActivity.this.log(text);
            }
        });
        try {
            if (putianUsbDevice.open(usbManager)) {
                displayUSBSpotBtn(true);
            } else {
                ToastUtil.show("初始化设备失败");
            }
        } catch (IllegalStateException e) {
            requestPermission(usbDevice);
        }

        /*inEndPoint = null;
        outEndPoint = null;
        int interfaceCount = usbDevice.getInterfaceCount();
        UsbInterface usbInterface = null;
        for (int i = 0; i < interfaceCount; i++) {
            usbInterface = usbDevice.getInterface(i);
            Log.d(TAG, "initDevice find interface: " + i +
                    ", class: " + usbInterface.getInterfaceClass() +
                    ", subClass: " + usbInterface.getInterfaceSubclass() +
                    ", protocol:" + usbInterface.getInterfaceProtocol());
            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                Log.d(TAG, "endpoint " + j + ", type:" + endpoint.getType() + ", direction: " + endpoint.getDirection());
                if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    inEndPoint = endpoint;
                    Log.d(TAG, "initDevice find inEndPoint: " + j);
                } else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    outEndPoint = endpoint;
                    Log.d(TAG, "initDevice find outEndPoint: " + j);
                }
            }
        }
        if (inEndPoint == null || outEndPoint == null) {
            ToastUtil.show("初始化设备失败");
            inEndPoint = null;
            outEndPoint = null;
        } else {
            usbDeviceConnection = usbManager.openDevice(usbDevice);
            usbDeviceConnection.claimInterface(usbInterface, true);
        }*/
    }





}
