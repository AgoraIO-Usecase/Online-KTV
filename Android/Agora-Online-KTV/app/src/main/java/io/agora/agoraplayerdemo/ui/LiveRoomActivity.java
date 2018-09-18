package io.agora.agoraplayerdemo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import io.agora.agoraplayerdemo.Model.AGEventHandler;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.AgoraVideoFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

import java.util.Locale;

import io.agora.agoraplayerdemo.Model.ConstantApp;
import io.agora.agoraplayerdemo.R;
import io.agora.rtc.video.VideoCanvas;




public class LiveRoomActivity extends BaseActivity implements  AGEventHandler,SeekBar.OnSeekBarChangeListener {
    private final static  Logger log= LoggerFactory.getLogger(LiveRoomActivity.class);
    SurfaceView containnerView;
    Button playBtn;
    Button pauseBtn;
    Button changeAudioBtn;
    Button clientRoleButtion;
    Button switchAudioButton;
    Button closeButton;
    TextView voiceView;
    TextView songView;
    SeekBar voiceBar;
    SeekBar songBar;
    XPlay  xplayView;
    FrameLayout containerLayout;
    boolean isBroadcast = false;
    static {

        System.loadLibrary("apm-plugin-native-lib");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCallBack();
        initAgoraObserver();
        setContentView(R.layout.activity_live_room);
        //相关控件的初始化
        playBtn = (Button)findViewById(R.id.play01);
        pauseBtn = (Button)findViewById(R.id.pause);
        clientRoleButtion = (Button)findViewById(R.id.clientRole);
        changeAudioBtn = (Button)findViewById(R.id.ChangeAudio);
        voiceView = (TextView)findViewById(R.id.voiceTextView);
        songView = (TextView)findViewById(R.id.songTextView);
        voiceBar = (SeekBar)findViewById(R.id.voiceSeekBar);
        voiceBar.setOnSeekBarChangeListener(this);
        switchAudioButton = (Button)findViewById(R.id.switchAudio);
        songBar = (SeekBar)findViewById(R.id.songSeekBar);
        songBar.setOnSeekBarChangeListener(this);
        containerLayout = (FrameLayout)findViewById(R.id.xplay_view_container);
        closeButton = (Button)findViewById(R.id.room_close);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //please input url which u need to open
                closeSong();
                Open("http://compress.mv.letusmix.com/33cfa59c570ff341c8df68dc2ecbb640.mp4");
            }
        });
        //播放暂停
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayOrPause();
            }
        });
        //音轨切换
        changeAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeAudio();
            }
        });
        clientRoleButtion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBroadcast = !isBroadcast;
                //角色切换执行的方法
                doswitchBroadCast(isBroadcast);
            }
        });
        //切歌执行的方法
        switchAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSong();
                Open("http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4");
            }
        });
        //离开房间执行的方法
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                worker().getRtcEngine().leaveChannel();
                closeSong();
                finish();
            }
        });


    }
    //观众角色切换
    private void doswitchBroadCast(Boolean broadCaster){
        destroyAudioBuf();
        if (broadCaster)
        {
            removeViews();
            worker().getRtcEngine().setClientRole(2);
            clientRoleButtion.setText("上麦");
            doShowButtons(true);
            closeSong();

        }else{
            addXplayView();
            worker().getRtcEngine().setClientRole(1);
            clientRoleButtion.setText("下麦");
            doShowButtons(false);

        }
    }
    //渲染回调
    public void renderVideoFrame(final  byte[] data, int width,int height){

        AgoraVideoFrame f = new AgoraVideoFrame();
        f.format = AgoraVideoFrame.FORMAT_I420;
        f.timeStamp = System.currentTimeMillis();
        f.buf = data;
        f.stride = width;
        f.height = height;
        boolean ret =  rtcEngine().pushExternalVideoFrame(f);

    }
    //播放完成之后的回调
    public void videoComplete(){
        log.debug("视频播放完成");
    }
    //视频时长回调
    public void totalMSCallBack(int time){
        log.debug("视频播放时长 %d",time);
    }
    //获取视频进度
    public native double PlayPos();
    //初始化
    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);
        Intent i = getIntent();
        int cRole = i.getIntExtra(ConstantApp.ACTION_KEY_CROLE,0);
        if (cRole == 0)
        {
            throw new RuntimeException("Should not reach here");
        }
        String roomName = i.getStringExtra(ConstantApp.ACTION_KEY_ROOM_NAME);
        doConfigEngine(cRole);
        if (isBroadcaster(cRole)){
            addXplayView();
            isBroadcast = false;
            doShowButtons(false);
        }else
        {
            isBroadcast = true;
            doShowButtons(true);
            clientRoleButtion.setText("上麦");

        }
        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.profile\":{\"scenario\":%d}}", 1));
        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.headset.monitoring,true\"}"));
        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.enable.androidlowlatencymode,true\"}"));
        worker().getRtcEngine().enableInEarMonitoring(true);
        worker().joinChannel(roomName, config().mUid);
        TextView textRoomName = (TextView)findViewById(R.id.room_name);
        textRoomName.setText(roomName);
    }
    private boolean isBroadcaster(){
        return isBroadcaster(config().mClientRole);
    }
    private boolean isBroadcaster(int cRole) {
        return cRole == Constants.CLIENT_ROLE_BROADCASTER;
    }
    private void doConfigEngine(int cRole) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        if (prefIndex > ConstantApp.VIDEO_PROFILES.length - 1) {
            prefIndex = ConstantApp.DEFAULT_PROFILE_IDX;
        }
        int vProfile = ConstantApp.VIDEO_PROFILES[prefIndex];

        worker().configEngine(cRole, vProfile);
    }

    @Override
    protected void deInitUIandEvent() {


    }
    //控制按钮隐藏 显示
    void doShowButtons(boolean hide){

            playBtn.setVisibility(hide ? playBtn.INVISIBLE : playBtn.VISIBLE);
            pauseBtn.setVisibility(hide ? pauseBtn.INVISIBLE:pauseBtn.VISIBLE);
            changeAudioBtn.setVisibility(hide? changeAudioBtn.INVISIBLE:changeAudioBtn.VISIBLE);
            voiceView.setVisibility(hide ? voiceView.INVISIBLE:voiceView.VISIBLE);
            songView.setVisibility(hide ? songView.INVISIBLE : songView.VISIBLE);
            voiceBar.setVisibility(hide ? voiceBar.INVISIBLE : voiceBar.VISIBLE);
            songBar.setVisibility(hide ? songBar.INVISIBLE : songBar.VISIBLE);
            switchAudioButton.setVisibility(hide ? switchAudioButton.INVISIBLE : switchAudioButton.VISIBLE);

    }
    //初始化音频观察期对象
    public native void initAgoraObserver();
    //打开播放器
    public native void Open(String url);
    //暂停播放
    public native void PlayOrPause();
    //设置视频渲染回调
    public native void setCallBack();
    //切音轨
    public native void ChangeAudio();
    //调节人声音量
    public native void voiceSeek(double pos);
    //调节伴奏音量
    public native void songSeek(double pos);
    //关闭mv
    public native void closeSong();
    //清空音频缓冲区缓存
    public native void destroyAudioBuf();
    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);

    }
    private void doRenderRemoteUi(final int uid){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                   if (isFinishing())
               {
                   return;
               }
                SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                containerLayout.addView(surfaceView);
                surfaceView.setZOrderOnTop(true);
                surfaceView.setZOrderMediaOverlay(true);
                rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceView,VideoCanvas.RENDER_MODE_ADAPTIVE,uid));
            }
        });
    }
    //添加显示view
    private void addXplayView(){
        xplayView = new XPlay(this);
        xplayView.setZOrderOnTop(true);
        xplayView.setZOrderMediaOverlay(true);
        containerLayout.addView(xplayView);

    }
    //移除创建的view
    private void removeViews(){

        int index = -1;
        int count = containerLayout.getChildCount();
        for (int i = 0; i < count ; i++)
        {
            View v =   containerLayout.getChildAt(i);
            if ((v instanceof XPlay))
            {
                index = i;
                break;
            }
        }
        if (index != -1) {
            containerLayout.removeViewAt(index);
        }
    }
    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {

    }

    @Override
    public void onUserOffline(int uid, int reason) {
        int index = -1;
        int count = containerLayout.getChildCount();
        for (int i = 0; i < count ; i++)
        {
           View v =   containerLayout.getChildAt(i);
           if (!(v instanceof XPlay))
           {
               index = i;
               break;
           }
        }
        if (index != -1) {
            containerLayout.removeViewAt(index);
        }
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {


        Log.d("heheda","heelo");

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    //监听滑动条滚动
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(seekBar!=null && seekBar.equals(voiceBar)){

            voiceSeek((double)seekBar.getProgress()/(double
                    )seekBar.getMax());
            android.util.Log.v("zxc","1111"+ (double)seekBar.getProgress()/(double)seekBar.getMax());

        } else if (seekBar!=null && seekBar.equals(songBar)){

            songSeek((double) seekBar.getProgress()/(double)seekBar.getMax());
            android.util.Log.v("zxc","2222"+ (double)seekBar.getProgress()/(double)seekBar.getMax());

        }

    }
}

