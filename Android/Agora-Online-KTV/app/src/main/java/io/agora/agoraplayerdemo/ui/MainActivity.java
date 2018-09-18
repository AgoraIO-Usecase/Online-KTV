package io.agora.agoraplayerdemo.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import io.agora.agoraplayerdemo.model.ConstantApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.agora.agoraplayerdemo.R;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.AgoraVideoFrame;

public class MainActivity extends BaseActivity {
//    Button playBtn;
//    Button pauseBtn;
//    Button changeAudioBtn;
//    private static final String LOG_TAG = MainActivity.class.getSimpleName();
//    private RtcEngine mRtcEngine;
//    private final IKTVKitEventHandler mRtcEventHandler = new IKTVKitEventHandler() { // Tutorial Step 1
//        @Override
//        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    setupRemoteVideo(uid);
//                }
//            });
//        }
//
//        @Override
//        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    onRemoteUserLeft();
//                }
//            });
//        }
//
//        @Override
//        public void onUserMuteVideo(final int uid, final boolean muted) { // Tutorial Step 10
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    onRemoteUserVideoMuted(uid, muted);
//                }
//            });
//        }
//    };

        // private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

//    // Used to load the 'native-lib' library on application startup.
//    static {
//
//        System.loadLibrary("apm-plugin-native-lib");
//    }
//    private void initAgoraEngineAndJoinChannel() {
//        initializeAgoraEngine();     // Tutorial Step 1
//        setupVideoProfile();         // Tutorial Step 2
//        joinChannel();               // Tutorial Step 4
//    }
//    // Tutorial Step 1
//    private void initializeAgoraEngine() {
//        try {
//            mRtcEngine = RtcEngine.create(getBaseContext(),"aab8b8f5a8cd4469a63042fcfafe7063",mRtcEventHandler);
//            mRtcEngine.setExternalVideoSource(true,false,true);
//            mRtcEngine.setRecordingAudioFrameParameters(16000,2,2,640);
//            Log.i(LOG_TAG,"yyyyyyyyy test");
////            Log.e(stringFromJNI(),LOG_TAG);
//
//        } catch (Exception e) {
//            Log.e(LOG_TAG, Log.getStackTraceString(e));
//
//            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
//        }
//    }
//        // Tutorial Step 2
//        private void setupVideoProfile() {
//            mRtcEngine.enableVideo();
//            mRtcEngine.enableAudio();
//            mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false);
//
//        }
//    private boolean checkSelfPermissions() {
//        return checkSelfPermission(Manifest.permission.RECORD_AUDIO, ConstantApp.PERMISSION_REQ_ID_RECORD_AUDIO) &&
//                checkSelfPermission(Manifest.permission.CAMERA, ConstantApp.PERMISSION_REQ_ID_CAMERA) &&
//                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ConstantApp.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
//    }
//    public boolean checkSelfPermission(String permission, int requestCode) {
//        Log.d(LOG_TAG,"checkSelfPermission " + permission + " " + requestCode);
//        if (ContextCompat.checkSelfPermission(this,
//                permission)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{permission},
//                    requestCode);
//            return false;
//        }
//
//        return true;
//    }
//    // Tutorial Step 4
//    private void joinChannel() {
//        mRtcEngine.joinChannel(null, "agora123456", "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
//    }
//
//    // Tutorial Step 5
//    private void setupRemoteVideo(int uid) {
//        Log.e(LOG_TAG,String.valueOf(uid));
//
//
//    }
//
//    // Tutorial Step 6
//    private void leaveChannel() {
//        mRtcEngine.leaveChannel();
//    }
//
//    // Tutorial Step 7
//    private void onRemoteUserLeft() {
//
//    }
//
//    // Tutorial Step 10
//    private void onRemoteUserVideoMuted(int uid, boolean muted) {
//
//    }

    @Override
    protected void initUIandEvent() {
        EditText textRoomName = (EditText) findViewById(R.id.room_name);
        textRoomName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmpty = s.toString().isEmpty();
                findViewById(R.id.button_join).setEnabled(!isEmpty);
            }
        });
    }

    @Override
    protected void deInitUIandEvent() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        checkSelfPermissions();
//        //cpy mp4 to sdcard
//        File file  = new File("/sdcard/1080.mp4");
//        try {
//            copyAssets(getBaseContext(),"1080.mp4",file,"777");
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        //set the jni methods
//        setCallBack();
//        initAgoraEngineAndJoinChannel();
//        setContentView(R.layout.activity_main);
//        playBtn = (Button)findViewById(R.id.play01);
//        pauseBtn = (Button)findViewById(R.id.pause);
//        playBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //please input url which u need to open
//                Open("/sdcard/1080.mp4");
//            }
//        });
//        pauseBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PlayOrPause();
//            }
//        });
//
//        changeAudioBtn = (Button)findViewById(R.id.ChangeAudio);
//        changeAudioBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ChangeAudio();
//            }
//        });


    }
//    //transefer data to SDK
//    public static void copyAssets(Context context, String assetsFilename, File file, String mode)
//            throws IOException, InterruptedException {
//        AssetManager manager = context.getAssets();
//        final InputStream is = manager.open(assetsFilename);
//        copyFile(file, is, mode);
//    }
//
//    /**
//     * copy file to destination
//     */
//    private static void copyFile(File file, InputStream is, String mode)
//            throws IOException, InterruptedException {
//        final String abspath = file.getAbsolutePath();
//        final FileOutputStream out = new FileOutputStream(file);
//        byte buf[] = new byte[1024];
//        int len;
//        while ((len = is.read(buf)) > 0) {
//            out.write(buf, 0, len);
//        }
//
//        out.close();
//        is.close();
//
//        Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();
//    }

//     public void renderVideoFrame(final  byte[] data, int width,int height){
//
//        Log.d(LOG_TAG,"receive data length"+data.length +width+height);
//        AgoraVideoFrame f = new AgoraVideoFrame();
//        f.format = AgoraVideoFrame.FORMAT_I420;
//        f.timeStamp = System.currentTimeMillis();
//        f.buf = data;
//        f.stride = width;
//        f.height = height;
//        boolean ret =  mRtcEngine.pushExternalVideoFrame(f);
//        Log.d(LOG_TAG,"status" + String.valueOf(ret));
//
//    }

    public void onClickJoin(View view) {
        // show dialog to choose role
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.msg_choose_role);
        builder.setNegativeButton(R.string.label_audience, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.forwardToLiveRoom(Constants.CLIENT_ROLE_AUDIENCE);
            }
        });
        builder.setPositiveButton(R.string.label_broadcaster, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.forwardToLiveRoom(Constants.CLIENT_ROLE_BROADCASTER);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void forwardToLiveRoom(int cRole) {
        final EditText v_room = (EditText) findViewById(R.id.room_name);
        String room = v_room.getText().toString();

        Intent i = new Intent(MainActivity.this, LiveRoomActivity.class);
        i.putExtra(ConstantApp.ACTION_KEY_CROLE, cRole);
        i.putExtra(ConstantApp.ACTION_KEY_ROOM_NAME, room);

        startActivity(i);
    }


//    public native String stringFromJNI();
//    public native void Open(String url);
//    public native void PlayOrPause();
//    public native void setCallBack();
//    public native void ChangeAudio();


}
