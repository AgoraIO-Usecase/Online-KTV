package io.agora.ard.ktv.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.agora.ard.ktv.R;
import io.agora.ard.ktv.model.AGEventHandler;
import io.agora.ard.ktv.model.ConstantApp;

import io.agora.ktvkit.IKTVKitEventHandler;
import io.agora.ktvkit.KTVKit;
import io.agora.ktvkit.VideoPlayerView;

import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class LiveRoomActivity extends BaseActivity implements AGEventHandler, SeekBar.OnSeekBarChangeListener {
    private final static Logger log = LoggerFactory.getLogger(LiveRoomActivity.class);

    Button playBtn;
    Button pauseBtn;
    Button changeAudioTrackBtn;
    Button clientRoleButtion;
    Button switchMediaButton;
    Button switchMuteButton;
    Button closeButton;
    TextView voiceVolumeView;
    TextView songVolumeView;
    SeekBar voiceVolumeBar;
    SeekBar accompanyVolumeBar;
    VideoPlayerView xPlayerView;
    FrameLayout containerLayout;
    boolean isBroadcast = false;

    TextView mMediaMetaArea;

    private KTVKit mKTVKit = null;

    private final ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private Future<?> mScheduledFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mKTVKit = KTVKit.create(worker().getRtcEngine(), getApplicationContext(), new IKTVKitEventHandler() {
                @Override
                public void onPlayerStopped() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            int duration = (mKTVKit.getDuration() / 1000);

                            mMediaMetaArea.setText("Done, " + (int) Math.floor(mKTVKit.getCurrentPosition() * duration) + " " + duration);
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_live_room);
        // 相关控件的初始化
        playBtn = (Button) findViewById(R.id.play01);
        pauseBtn = (Button) findViewById(R.id.pause);
        clientRoleButtion = (Button) findViewById(R.id.change_client_role);
        changeAudioTrackBtn = (Button) findViewById(R.id.change_audio_track);
        voiceVolumeView = (TextView) findViewById(R.id.voiceTextView);
        songVolumeView = (TextView) findViewById(R.id.songTextView);
        voiceVolumeBar = (SeekBar) findViewById(R.id.voiceSeekBar);
        voiceVolumeBar.setOnSeekBarChangeListener(this);
        switchMediaButton = (Button) findViewById(R.id.switch_media_file);
        switchMuteButton = (Button) findViewById(R.id.switch_mute_status);
        accompanyVolumeBar = (SeekBar) findViewById(R.id.songSeekBar);
        accompanyVolumeBar.setOnSeekBarChangeListener(this);
        containerLayout = (FrameLayout) findViewById(R.id.xplay_view_container);
        closeButton = (Button) findViewById(R.id.close_ktv_room);

        mMediaMetaArea = (TextView) findViewById(R.id.media_meta);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // please input url which u need to open
                mKTVKit.openAndPlayVideoFile("http://compress.mv.letusmix.com/33cfa59c570ff341c8df68dc2ecbb640.mp4");
            }
        });

        // 播放暂停
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKTVKit.pause();
            }
        });

        // 音轨切换
        changeAudioTrackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKTVKit.switchAudioTrack();
            }
        });

        clientRoleButtion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBroadcast = !isBroadcast;
                // 角色切换执行的方法
                doswitchBroadCast(isBroadcast);
            }
        });

        // 切歌执行的方法
        switchMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKTVKit.stopPlayVideoFile();
                mKTVKit.openAndPlayVideoFile("http://compress.mv.letusmix.com/914184d11605138c7de8c28f2905c63a.mp4");
            }
        });

        // 离开房间执行的方法
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKTVKit.stopPlayVideoFile();
                worker().getRtcEngine().leaveChannel();
                finish();
            }
        });
        switchMuteButton.setTag(true);
        switchMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = switchMuteButton.getTag();
                if (tag != null && (boolean) tag) {
                    mKTVKit.muteLocalKtvVolume();
                    switchMuteButton.setTag(false);
                } else {
                    mKTVKit.unMuteLocalKtvVolume();
                    switchMuteButton.setTag(true);
                }
            }
        });

        mScheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        double currentPosition = mKTVKit.getCurrentPosition();

                        if (currentPosition > 0.99d && currentPosition < 1.01d) {
                            return;
                        }

                        int duration = mKTVKit.getDuration();

                        ((TextView) findViewById(R.id.media_meta)).setText((int) Math.floor((currentPosition * duration) / 1000) + " " + duration / 1000);
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // 观众角色切换
    private void doswitchBroadCast(boolean broadcaster) {
        mKTVKit.resetAudioBuffer();

        if (broadcaster) {
            removeViews();
            worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            clientRoleButtion.setText("上麦");
            doShowButtons(true);
            mKTVKit.stopPlayVideoFile();
        } else {
            addXplayView();
            worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            clientRoleButtion.setText("下麦");
            doShowButtons(false);
        }
    }

    // 初始化
    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);
        Intent i = getIntent();
        int cRole = i.getIntExtra(ConstantApp.ACTION_KEY_CROLE, 0);
        if (cRole == 0) {
            throw new RuntimeException("Should not reach here");
        }
        String roomName = i.getStringExtra(ConstantApp.ACTION_KEY_ROOM_NAME);

        doConfigEngine(cRole);

        if (isBroadcaster(cRole)) {
            addXplayView();
            isBroadcast = false;
            doShowButtons(false);
        } else {
            isBroadcast = true;
            doShowButtons(true);
            clientRoleButtion.setText("上麦");
        }

        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.profile\":{\"scenario\":%d}}", 1));
        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.headset.monitoring,true\"}"));
        worker().getRtcEngine().setParameters(String.format(Locale.US, "{\"che.audio.enable.androidlowlatencymode,true\"}"));
        worker().getRtcEngine().enableInEarMonitoring(true);
        worker().joinChannel(roomName, config().mUid);

        TextView textKtvRoomName = (TextView) findViewById(R.id.ktv_room_name);
        textKtvRoomName.setText(roomName);
    }

    private boolean isBroadcaster() {
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
        event().removeEventHandler(this);
    }

    // 控制按钮隐藏 显示
    void doShowButtons(boolean hide) {
        playBtn.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        pauseBtn.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        changeAudioTrackBtn.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        voiceVolumeView.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        songVolumeView.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        voiceVolumeBar.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        accompanyVolumeBar.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        switchMediaButton.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        switchMuteButton.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);

    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                containerLayout.addView(surfaceView);
                surfaceView.setZOrderOnTop(true);
                surfaceView.setZOrderMediaOverlay(true);
                rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
            }
        });
    }

    // 添加显示 view
    private void addXplayView() {
        xPlayerView = new VideoPlayerView(this, mKTVKit);
        xPlayerView.setZOrderOnTop(true);
        xPlayerView.setZOrderMediaOverlay(true);
        containerLayout.addView(xPlayerView);
    }

    // 移除创建的 view
    private void removeViews() {
        int index = -1;
        int count = containerLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = containerLayout.getChildAt(i);
            if ((v instanceof VideoPlayerView)) {
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
        for (int i = 0; i < count; i++) {
            View v = containerLayout.getChildAt(i);
            if (!(v instanceof VideoPlayerView)) {
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
        Log.d("heheda", "heelo");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    // 监听滑动条滚动
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar != null && seekBar.equals(voiceVolumeBar)) {
            mKTVKit.adjustVoiceVolume((double) seekBar.getProgress() / (double
                    ) seekBar.getMax());
            android.util.Log.v("zxc", "1111" + (double) seekBar.getProgress() / (double) seekBar.getMax());
        } else if (seekBar != null && seekBar.equals(accompanyVolumeBar)) {
            mKTVKit.adjustAccompanyVolume((double) seekBar.getProgress() / (double) seekBar.getMax());
            android.util.Log.v("zxc", "2222" + (double) seekBar.getProgress() / (double) seekBar.getMax());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
        }
        mScheduledExecutorService.shutdownNow();

        mKTVKit.stopPlayVideoFile();

        worker().getRtcEngine().leaveChannel();

        KTVKit.destroy();
    }
}

