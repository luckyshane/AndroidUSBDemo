package com.example.shane.shanedemo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * 全局toast，避免重复弹出
 */
public class ToastUtil {

    private static ToastUtil toastUtil;
    private Toast mToast;

    private ToastUtil(Context context){
        if(mToast == null) {
            mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        }
    }

    public static void init(Context context){
        if (toastUtil == null) {
            synchronized (ToastUtil.class) {
                if (toastUtil == null) {
                    toastUtil = new ToastUtil(context);
                }
            }
        }
    }

    public static void show(String text) {
        if (toastUtil != null && toastUtil.mToast != null) {
            toastUtil.mToast.setText(text);
            toastUtil.mToast.setDuration(Toast.LENGTH_SHORT);
            toastUtil.mToast.show();
        }else{
            Log.i("ToastUtil", "未初始化....");
        }
    }

    public void cancel() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    public void onBackPressed(Activity activity) {
        cancel();
        activity.onBackPressed();
    }
}
