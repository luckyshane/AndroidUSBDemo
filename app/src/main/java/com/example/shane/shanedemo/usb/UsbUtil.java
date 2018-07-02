package com.example.shane.shanedemo.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Shane on 2018/6/8.
 */

public class UsbUtil {


    public static boolean isTargetPutianDevice(UsbDevice usbDevice) {
        return (usbDevice.getVendorId() == 0x1234 && usbDevice.getProductId() == 0x4321);
    }

    public static boolean isSupportUsbHost(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.usb.host");
    }

    public static UsbDevice getConnectedPutianDevice(UsbManager usbManager) {
        HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
        if (deviceHashMap != null && !deviceHashMap.isEmpty()) {
            Collection<UsbDevice> devices = usbManager.getDeviceList().values();
            for (UsbDevice usbDevice : devices) {
                if (isTargetPutianDevice(usbDevice)) {
                    return usbDevice;
                }
            }
        }
        return null;
    }

    public static long toLong(byte[] bytes) {
        if (bytes == null) {
            return -1;
        }
        long result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }


}
