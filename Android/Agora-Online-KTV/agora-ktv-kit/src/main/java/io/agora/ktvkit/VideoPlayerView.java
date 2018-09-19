package io.agora.ktvkit;

import android.content.Context;
import android.util.AttributeSet;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoPlayerView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = "VideoPlayerView";

    private KTVKit mKtvKit;

    private volatile boolean mSurfaceCreated = false;

    public VideoPlayerView(Context context, KTVKit ktvKit) {
        super(context);
        getHolder().addCallback(this);

        mKtvKit = ktvKit;
    }

    public VideoPlayerView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(LOG_TAG, holder.getSurface().toString());

        if (mKtvKit == null) {
            throw new RuntimeException("Must initialized with a valid KTVKit instance, or you should call VideoPlayerView.attachToKTVKit before this view rendered");
        }

        mKtvKit.setupDisplay(holder.getSurface());

        mSurfaceCreated = true;
    }

    public void attachToKTVKit(KTVKit ktvKit) {
        if (ktvKit == null) {
            throw new RuntimeException("Must initialized with a valid KTVKit instance");
        }

        if (mKtvKit != null && ktvKit != mKtvKit) {
            Log.i(LOG_TAG, "Replace KTVKit instance");
        }

        if (mSurfaceCreated) {
            Log.i(LOG_TAG, "Must called before Surface created");
            return;
        }

        mKtvKit = ktvKit;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceCreated = false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        mKtvKit = null;
    }
}
