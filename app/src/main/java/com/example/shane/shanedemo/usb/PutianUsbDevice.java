package com.example.shane.shanedemo.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.example.shane.shanedemo.StringUtils;

import java.util.Arrays;


/**
 * Created by Shane on 2018/6/8.
 */
public class PutianUsbDevice {
    private static final String TAG = PutianUsbDevice.class.getSimpleName();

    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbDeviceConnection connection;
    private UsbEndpoint inEndPoint;
    private UsbEndpoint outEndPoint;
    private volatile boolean isOpen;
    private byte[] readBuffer;
    public static final int ERROR_NOT_OPEN = -2;
    private LogFunc logFunc;


    public PutianUsbDevice(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    public interface LogFunc {
        void log(String text);
    }

    public void setLogFunc(LogFunc logFunc) {
        this.logFunc = logFunc;
    }

    /**
     * 打开设备，用于后续读写数据
     *
     * @throws IllegalStateException 没有权限时抛出
     */
    public boolean open(UsbManager usbManager) throws IllegalStateException {
        if (isOpen) {
            return true;
        }
        if (!usbManager.hasPermission(usbDevice)) {
            throw new IllegalStateException("Has no permission");
        }
        if (usbDevice.getInterfaceCount() == 1) {
            usbInterface = usbDevice.getInterface(0);
        } else {
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                usbInterface = usbDevice.getInterface(i);
                log("interface " + i + ", class: " + usbInterface.getInterfaceClass()
                        + ", subClass: " + usbInterface.getInterfaceSubclass()
                        + ", protocol: " + usbInterface.getInterfaceProtocol());
            }
            // TODO: get the right one
            //usbInterface = usbDevice.getInterface(0);
        }
        if (usbInterface != null) {
            UsbDeviceConnection conn = usbManager.openDevice(usbDevice);
            if (conn != null) {
                if (conn.claimInterface(usbInterface, true)) {
                    for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                        UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                        log("endpoint type: " + endpoint.getType() + ", dir: " + endpoint.getDirection() + ", addr: " + endpoint.getAddress());
                        if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK || true) { // 普天的识别出来，type不对。。
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                inEndPoint = endpoint;
                            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                outEndPoint = endpoint;
                            }
                        }
                    }
                    if (inEndPoint == null || outEndPoint == null) {
                        log("find endpoint failed");
                        conn.releaseInterface(usbInterface);
                        conn.close();
                    } else {
                        this.connection = conn;
                        isOpen = true;
                        return true;
                    }
                } else {
                    conn.close();
                }
            }
        }
        usbInterface = null;
        inEndPoint = null;
        outEndPoint = null;
        return false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int write(byte[] outData, int length, int writeTimeOutInMillis) {
        if (isOpen()) {
            return connection.bulkTransfer(outEndPoint, outData, length, writeTimeOutInMillis);
        }
        return ERROR_NOT_OPEN;
    }

    public int write(byte[] outData, int length) {
        int ret = write(outData, length, 1000);
        log("send data: " + StringUtils.toHexStr(outData) + ", ret: " + ret);
        return ret;
    }

    public int read(byte[] inData, int readTimeOutInMillis) {
        if (isOpen()) {
            return connection.bulkTransfer(inEndPoint, inData, inData.length, readTimeOutInMillis);
        }
        return ERROR_NOT_OPEN;
    }

    public int read(byte[] inData) {
        int ret = read(inData, 1000);
        log("read data: " + StringUtils.toHexStr(inData) + ", ret:" + ret);
        return ret;
    }

    public void release() {
        isOpen = false;
        if (connection != null) {
            if (usbInterface != null) {
                connection.releaseInterface(usbInterface);
                usbInterface = null;
            }
            connection.close();
            connection = null;
        }
    }

    public long getSerial() {
        if (isOpen()) {
            byte[] cmd = new byte[]{
                    0x03, (byte) 0xB3, 0x00, (byte) 0xB3,
            };
            int retryCount = 0;
            int ret = -1;
            while (isOpen() && retryCount < 3 && (ret = write(cmd, cmd.length)) < 0) {
                retryCount++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (ret > 0) {
                retryCount = 0;
                while (isOpen() && retryCount < 3) {
                    byte[] readBuffer = getReadBuffer();
                    ret = read(readBuffer);
                    if (ret > 0) {
                        if (checkResponse(readBuffer, ret)) {
                            byte[] serialBytes = Arrays.copyOfRange(readBuffer, 2, 6);
                            return UsbUtil.toLong(serialBytes);
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    retryCount++;
                }
            }
        }
        return 0;
    }

    private byte[] getReadBuffer() {
        if (readBuffer == null) {
            readBuffer = new byte[64];
        }
        return readBuffer;
    }

    private boolean checkResponse(byte[] buffer, int length) {
        boolean ok = false;
        if (length > 1) {
            if (buffer[1] == (byte) 0xC3) {
                int len = buffer[0]; // 第一个字节表示后续包含的字节数
                if ((len + 1) <= length) {
                     ok = isCheckSumValid(buffer, len + 1);
                }
            }
        }
        log("checkResponse: " + (ok ? "ok" : "fail"));
        return ok;
    }

    public static boolean isCheckSumValid(byte[] buffer, int length) {
        if (length > 1) {
            byte sum = buffer[length - 1];  // 最后一个字节为checkSum
            byte sumCalc = 0;
            for (int i = 1; i < length - 1; i++) {
                sumCalc += buffer[i];
            }
            return sum == sumCalc;
        }
        return false;
    }

    private void log(String log) {
        Log.d(TAG, log);
        if (logFunc != null) {
            logFunc.log(log);
        }
    }





}
