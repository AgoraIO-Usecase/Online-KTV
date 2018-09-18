package io.agora.ktvkit;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class XPlay extends SurfaceView implements SurfaceHolder.Callback {

    private Surface mSurface;

    public XPlay(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public XPlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("XPlay", holder.getSurface().toString());

        mSurface = holder.getSurface();
        InitView(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public native void InitView(Object surface);

}
