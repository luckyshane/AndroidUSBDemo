package com.example.shane.shanedemo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.shane.shanedemo.usb.PutianUsbDevice;
import com.example.shane.shanedemo.usb.UsbUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    /*@Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.example.shane.shanedemo", appContext.getPackageName());
    }*/

    @Test
    public void test() {
        byte[] bytes = new byte[]{
            0x49, (byte)0x96, 0x02, (byte)0xdb
        };

        assertEquals(1234567899, UsbUtil.toLong(bytes));
        assertEquals("499602DB", StringUtils.toHexStr(bytes));
    }


    @Test
    public void testChecksum() {
        String hexStr = "23C30611700F0112061075AE7A21752F000861853031488001898602B8191630065398154814971A7DCB53D2698B9B1A91B47D7189EF312B098A2BF2C9DEBD59";
        byte[] hexBytes = StringUtils.toBytesFromHexStr(hexStr);
        assertEquals(64, hexBytes.length);
        int len = hexBytes[0];
        assertEquals(0x23, len);
        assertEquals(true, PutianUsbDevice.isCheckSumValid(hexBytes, len + 1));
    }







}
