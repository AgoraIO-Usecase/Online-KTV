package io.agora.baselibrary.util;

import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Keep;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.agora.baselibrary.BuildConfig;

@SuppressWarnings("unchecked")
@Keep
public class KTVUtil {
    public static void logD(String msg) {
        if (BuildConfig.DEBUG)
            Log.d("AGORA-KTV", msg);
    }

    public static void logE(String msg) {
        if (BuildConfig.DEBUG)
            Log.e("AGORA-KTV", msg);
    }

    public static void logE(Throwable e) {
        if (BuildConfig.DEBUG)
            Log.e("AGORA-KTV", e.getMessage());
    }

    public static float dp2px(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static <T> Class<T> getGenericClass(Class<?> clz, int index) {
        Type type = clz.getGenericSuperclass();
        if (type == null) return null;
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    public static Object getViewBinding(Class<?> bindingClass, LayoutInflater inflater) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class);
            return inflateMethod.invoke(null, inflater);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T getViewBinding(Class<T> bindingClass, LayoutInflater inflater, ViewGroup container) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class, ViewGroup.class, Boolean.TYPE);
            return (T) inflateMethod.invoke(null, inflater, container, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}