package io.agora.ktv.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;


public class BlurTransformation extends BitmapTransformation {
    private static final float DEFAULT_DOWN_SAMPLING = 0.5f;

    private final Context context;

    public BlurTransformation(Context context) {
        this.context = context;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int scaledWidth = (int) (toTransform.getWidth() * DEFAULT_DOWN_SAMPLING);
        int scaledHeight = (int) (toTransform.getHeight() * DEFAULT_DOWN_SAMPLING);
        Bitmap bitmap = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        BitmapResource obtain = BitmapResource.obtain(
                blurBitmap(
                        context,
                        toTransform,
                        bitmap,
                        Color.argb(90, 255, 255, 255)
                ), pool
        );
        if (obtain == null) return null;
        return obtain.get();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update("blur transformation".getBytes());
    }

    @Nullable
    private Bitmap blurBitmap(Context context, Bitmap source, Bitmap bitmap, @ColorInt int overlayColor) {
        if (source == null) return bitmap;
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(DEFAULT_DOWN_SAMPLING, DEFAULT_DOWN_SAMPLING);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(source, 0f, 0f, paint);
        canvas.drawColor(overlayColor);
        try {
            return blur(context, bitmap);
        } catch (RSRuntimeException e) {
            e.printStackTrace();
            return bitmap;
        }
    }


    private Bitmap blur(Context context, Bitmap bitmap) {
        RenderScript rs = null;
        Allocation input = null;
        Allocation output = null;
        ScriptIntrinsicBlur blur = null;
        try {
            rs = RenderScript.create(context);
            rs.setMessageHandler(new RenderScript.RSMessageHandler());
            input = Allocation.createFromBitmap(
                    rs,
                    bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT
            );
            output = Allocation.createTyped(rs, input.getType());
            blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            blur.setInput(input);
            blur.setRadius(25f);
            blur.forEach(output);
            output.copyTo(bitmap);
        } finally {
            if (rs != null) {
                rs.destroy();
            }
            if (input != null) {
                input.destroy();
            }
            if (output != null) {
                output.destroy();
            }
            if (blur != null) {
                blur.destroy();
            }
        }
        return bitmap;
    }
}