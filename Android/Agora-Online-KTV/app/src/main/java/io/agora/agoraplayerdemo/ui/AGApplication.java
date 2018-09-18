package io.agora.agoraplayerdemo.ui;

import android.app.Application;

import io.agora.agoraplayerdemo.Model.WorkerThread;

/**
 * Created by zhanxiaochao on 2018/9/5.
 */

public class AGApplication extends Application {
    private WorkerThread mWorKerThread;
    public synchronized void initWorkerThread(){
        if (mWorKerThread == null){
            mWorKerThread = new WorkerThread(getApplicationContext());
            mWorKerThread.start();
            mWorKerThread.waitForReady();
        }
    }
    public synchronized WorkerThread getWorkerThread(){
        return mWorKerThread;
    }
    public synchronized void deInitWorkerThread(){
        mWorKerThread.exit();
        try{
            mWorKerThread.join();
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        mWorKerThread = null;
    }

}
