package io.agora.baselibrary.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class ToastUtil {
    private static Toast mToast;

    public static void toastShort(Context context, String msg) {
        if(mToast != null) mToast.cancel();
        mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void toastShort(Context context, @StringRes int msg) {
        if(mToast != null) mToast.cancel();
        mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void toastLong(Context context, String msg) {
        if(mToast != null) mToast.cancel();
        mToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        mToast.show();
    }
}
