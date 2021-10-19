package io.agora.ktv.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.fragment.app.Fragment;

import com.agora.data.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.Arrays;

import io.agora.baselibrary.base.BaseActivity;
import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomPeopleHolder;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.bean.MusicSettingBean;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;
import io.agora.ktv.manager.BaseMusicPlayer;
import io.agora.ktv.manager.MultipleMusicPlayer;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.manager.SimpleRoomEventCallback;
import io.agora.ktv.manager.SingleMusicPlayer;
import io.agora.ktv.service.MyForegroundService;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.widget.DividerDecoration;
import io.agora.ktv.widget.LrcControlView;
import io.agora.lrcview.LrcLoadUtils;
import io.agora.lrcview.bean.LrcData;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * 房间界面
 *
 * @author chenhengfei@agora.io
 */
public class RoomActivity extends BaseActivity<KtvActivityRoomBinding> {
    public static final String TAG_ROOM = "room";

    private MusicSettingBean mSetting;

    private BaseRecyclerViewAdapter<KtvItemRoomSpeakerBinding, AgoraMember, RoomPeopleHolder> mRoomSpeakerAdapter;
    private BaseMusicPlayer mMusicPlayer;
    protected IMediaPlayer mPlayer;
    private final BaseMusicPlayer.Callback mMusicCallback = new BaseMusicPlayer.Callback() {

        @Override
        public void onPrepareResource() {
            mBinding.lrcControlView.onPrepareStatus();
        }

        @Override
        public void onResourceReady(@NonNull MemberMusicModel memberMusic) {
            setupLrc(memberMusic);
        }

        @Override
        public void onMusicOpening() {
        }

        @Override
        public void onMusicOpenCompleted(long duration) {
            mBinding.lrcControlView.getLrcView().setTotalDuration(duration);
        }

        @Override
        public void onMusicOpenError(int error) {

        }

        @Override
        public void onMusicPlaying() {
            mBinding.lrcControlView.onPlayStatus();
        }

        @Override
        public void onMusicPause() {
            mBinding.lrcControlView.onPauseStatus();
        }

        @Override
        public void onMusicStop() {

        }

        @Override
        public void onMusicCompleted() {
            mBinding.lrcControlView.getLrcView().reset();

            cutOffMusic();
        }

        @Override
        public void onMusicPositionChanged(long position) {
            mBinding.lrcControlView.getLrcView().updateTime(position);
            mBinding.lrcControlView.getPitchView().updateTime(position);
        }

    };
    private final SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {

        @SuppressLint("CheckResult")
        @Override
        public void onReceivedCountdown(int uid, int time, String musicId) {
            if (RoomManager.getInstance().mCurrentMemberMusic == null) {
                MemberMusicModel temp = new MemberMusicModel(musicId);
                temp.setType(MemberMusicModel.SingType.Chorus);
                temp.setUserId(uid);
                temp.setMusicId(musicId);
                RoomManager.getInstance().onMusicChanged(temp);
                mBinding.lrcControlView.onWaitChorusStatus();
            }
            mBinding.lrcControlView.setCountDown(time);
        }

        @Override
        public void onAudioStatusChanged(@NonNull AgoraMember member) {
            super.onAudioStatusChanged(member);

            AgoraMember mMine = RoomManager.getInstance().getMine();
            if (ObjectsCompat.equals(member, mMine)) {
                if (member.getIsSelfMuted() == 1) {
                    mBinding.btnMicAttRoom.setImageResource(R.mipmap.ktv_room_unmic);
                } else {
                    mBinding.btnMicAttRoom.setImageResource(R.mipmap.ktv_room_mic);
                }
            }
        }

        @Override
        public void onMusicChanged(@NonNull MemberMusicModel music) {
            RoomActivity.this.onMusicChanged(music);
        }

        @Override
        public void onMusicEmpty() {
            RoomActivity.this.onMusicEmpty();
        }

        @Override
        public void onMemberJoin(@NonNull AgoraMember member) {
            RoomActivity.this.onMemberJoin(member);
        }

        @Override
        public void onMemberLeave(@NonNull AgoraMember member) {
            RoomActivity.this.onMemberLeave(member);
        }

        @Override
        public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
            RoomActivity.this.onMemberJoinedChorus(music);
        }

        @Override
        public void onMemberChorusReady(@NonNull MemberMusicModel music) {
            super.onMemberChorusReady(music);
            RoomActivity.this.onMemberChorusReady(music);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
        initData();
    }

    /**
     * 1. Init and attach recyclerview adapter
     * 2. Animation stuff
     */
    protected void initView() {
        mRoomSpeakerAdapter = new BaseRecyclerViewAdapter<>(Arrays.asList(new AgoraMember[8]), new OnItemClickListener<AgoraMember>() {
            @Override
            public void onItemClick(View view, int position, long id) {
                RoomActivity.this.requestSeatOn();
            }
        }, RoomPeopleHolder.class);

        mBinding.recyclerViewAttRoom.addItemDecoration(new DividerDecoration(4, 24, 8));
        mBinding.recyclerViewAttRoom.setAdapter(mRoomSpeakerAdapter);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
            mBinding.recyclerViewAttRoom.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // FIXME what is this for ?????
        Intent intent = new Intent(this, MyForegroundService.class);
        intent.setAction(MyForegroundService.ACTION_START_FOREGROUND_SERVICE);
        this.startService(intent);
    }

    /**
     * 1. Add Room event call back
     */
    protected void initListener() {
        RoomManager.getInstance().addRoomEventCallback(mRoomEventCallback);

        mBinding.toolBarAttRoom.setNavigationOnClickListener(this::showLeaveConfirmDialog);
        mBinding.btnMicAttRoom.setOnClickListener(this::toggleMic);
        mBinding.btnChangeCoverAttRoom.setOnClickListener(this::showBackgroundPicDialog);
        mBinding.btnOrderSongAttRoom.setOnClickListener(this::showChooseSongDialog);
        mBinding.btnChorusAttRoom.setOnClickListener(this::showChooseSongDialog);

        mBinding.lrcControlView.setOnLrcClickListener(new LrcControlView.OnLrcActionListener() {
            @Override
            public void onProgressChanged(long time) {
                mMusicPlayer.seek(time);
            }

            @Override
            public void onStartTrackingTouch() {

            }

            @Override
            public void onStopTrackingTouch() {

            }

            @Override
            public void onSwitchOriginalClick() {
                toggleOriginal();
            }

            @Override
            public void onMenuClick() {
                showMusicMenuDialog();
            }

            @Override
            public void onPlayClick() {
                toggleStart();
            }

            @Override
            public void onChangeMusicClick() {
                showChangeMusicDialog();
            }

            @Override
            public void onStartSing() {
                changeSingType();
            }

            @Override
            public void onJoinChorus() {
                joinChorus();
            }

            @Override
            public void onWaitTimeOut() {
                changeSingType();
            }

            @Override
            public void onCountTime(int time) {
                RoomManager.getInstance().sendSyncCountdown(time);
            }
        });

        ExampleData.getMvImage().observe(this, index -> mBinding.lrcControlView.setLrcViewBackground(ExampleData.exampleBackgrounds.get(index)));
    }

    /**
     * 1. Init current setting for agora SDK
     * 2. Check current User data is valid
     * 3. Config data base on the TAG_ROOM
     * 4. Try to Engine related
     */
    protected void initData() {
        // Step 1
        mSetting = new MusicSettingBean(false, 100, 100, new MusicSettingDialog.Callback() {
            @Override
            public void onEarChanged(boolean isEar) {
                RoomManager.getInstance().getRtcEngine().enableInEarMonitoring(isEar);
            }

            @Override
            public void onMicVolChanged(int vol) {
                mMusicPlayer.setMicVolume(vol);
            }

            @Override
            public void onMusicVolChanged(int vol) {
                mMusicPlayer.setMusicVolume(vol);
            }

            @Override
            public void onEffectChanged(int effect) {
                KTVUtil.logD("effect:"+effect);
                RoomManager.getInstance().getRtcEngine().setAudioEffectPreset(getEffectIndex(effect));
            }
        });
        // Step 2
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mUser == null) {
            ToastUtil.toastShort(this, "please login in");
            finish();
            return;
        }

        // Step 3
        AgoraRoom mRoom = (AgoraRoom) getIntent().getExtras().getSerializable(TAG_ROOM);
        mBinding.toolBarAttRoom.setTitle(mRoom.getId());
        Glide.with(this)
                .asDrawable()
                .load(mRoom.getCoverRes())
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        RoomActivity.this.getWindow().getDecorView().setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        // just for local database version
        mBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        // Step 4
        RoomManager.getInstance().initEngine(this)
                .andThen(RoomManager.getInstance().joinRoom(mRoom))
                .andThen(RoomManager.getInstance().joinRTC())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        showLoadingDialog(false);
                    }

                    @Override
                    public void onComplete() {
                        KTVUtil.logD("onComplete");
                        dismissLoading();
                        onJoinRoom();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        KTVUtil.logD("onError");
                        ToastUtil.toastLong(RoomActivity.this, getString(R.string.ktv_join_error, e.getMessage()));
                        e.printStackTrace();
                        dismissLoading();
                        finish();
                    }
                });
    }

    private void onJoinRoom() {
        mPlayer = RoomManager.getInstance().getRtcEngine().createMediaPlayer();
        showNotOnSeatStatus();
    }

    /**
     * 仅当合唱变独唱时调用
     */
    private void changeSingType() {
        MemberMusicModel musicModel = RoomManager.getInstance().mCurrentMemberMusic;
        if (musicModel != null) {
            musicModel.setType(MemberMusicModel.SingType.Single);
            onMusicChanged(musicModel);
        }
    }

    private void joinChorus() {
        AgoraMember mMember = RoomManager.getInstance().getMine();

        if (mMember == null) return;

        if (mMember.getRole() == Constants.CLIENT_ROLE_AUDIENCE) {
            ToastUtil.toastShort(this, R.string.ktv_need_up);
        } else {
            RoomManager.getInstance().startSyncRequestChorus();
        }

    }

//    private CountDownTimer timerOnTrial;
//
//    private void startOnTrialTimer(long liveTimeLeft) {
//        timerOnTrial = new CountDownTimer(liveTimeLeft, 999) {
//            public void onTick(long millisUntilFinished) {
//            }
//
//            public void onFinish() {
//                ToastUtil.toastShort(RoomActivity.this, R.string.ktv_use_overtime);
//                finish();
//            }
//        }.start();
//    }
//
//    private void stopOnTrialTimer() {
//        if (timerOnTrial != null) {
//            timerOnTrial.cancel();
//            timerOnTrial = null;
//        }
//    }

    private void toggleMic(View v) {
        AgoraMember temp = RoomManager.getInstance().getMine();
        if (temp != null) {
            boolean desireMute = temp.getIsSelfMuted() == 0;
            RoomManager.getInstance()
                    .toggleSelfAudio(desireMute)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onComplete() {
                            mBinding.btnMicAttRoom.setImageResource(desireMute ? R.mipmap.ktv_room_unmic : R.mipmap.ktv_room_mic);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {

                        }
                    });
        }
    }

    private void toggleOriginal() {
        if (mMusicPlayer == null) {
            return;
        }

        if (mMusicPlayer.hasAccompaniment()) {
            mMusicPlayer.toggleOriginal();
        } else {
            mBinding.lrcControlView.setSwitchOriginalChecked(true);
            ToastUtil.toastShort(this, R.string.ktv_error_cut);
        }
    }

    private int getEffectIndex(int index) {
        switch (index) {
            case 0:
                return Constants.AUDIO_EFFECT_OFF;
            case 1:
                return Constants.AUDIO_REVERB_FX_KTV;
            case 2:
                return Constants.AUDIO_REVERB_FX_VOCAL_CONCERT;
            case 3:
                return Constants.AUDIO_REVERB_FX_STUDIO;
            case 4:
                return Constants.AUDIO_REVERB_FX_PHONOGRAPH;
            case 5:
                return Constants.ROOM_ACOUSTICS_SPACIAL;
            case 6:
                return Constants.ROOM_ACOUSTICS_ETHEREAL;
            case 7:
                return Constants.STYLE_TRANSFORMATION_POPULAR;
            case 8:
                return Constants.STYLE_TRANSFORMATION_RNB;
        }
        return Constants.AUDIO_EFFECT_OFF;
    }

    private void cutOffMusic() {
        RoomManager.getInstance().onMusicEmpty();
    }

    private void toggleStart() {
        if (mMusicPlayer == null) {
            return;
        }

        mMusicPlayer.togglePlay();
    }

    private void showOnSeatStatus() {
        mBinding.groupOnSeatAttRoom.setVisibility(View.VISIBLE);
        mBinding.tvNoOnSeat.setVisibility(View.GONE);
    }

    private void showNotOnSeatStatus() {
        mBinding.groupOnSeatAttRoom.setVisibility(View.GONE);
        mBinding.tvNoOnSeat.setVisibility(View.VISIBLE);
    }

    /**
     * 1. Show current avatar in RecyclerView.
     * 2. Show control buttons.
     * 3. Start Sync Member.
     * 4. Enable Speaker.
     */
    private void requestSeatOn() {
        AgoraMember mMine = RoomManager.getInstance().getMine();
        if (mMine != null && mMine.getRole() == Constants.CLIENT_ROLE_AUDIENCE) {
            // Step 1
            onMemberJoin(mMine);
            // Step 2
            showOnSeatStatus();

            int roleNew = Constants.CLIENT_ROLE_BROADCASTER;

            RoomManager.getInstance().changeCurrentRole(roleNew);
            // Step 3
            RoomManager.getInstance().startSyncMember();
            // Step 4
            RoomManager.getInstance().toggleSelfAudio(false);
        }
    }

    /**
     * 1. Reset lrcControlView music and role.
     * 2. Stop former music player.
     * 3.1. Create new SingleMusicPlayer and prepare to play the music.
     * 3.2. Tell LrcView on wait chorus status.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void onMusicChanged(@NonNull MemberMusicModel music) {
        KTVUtil.logD(music.toString());
        // Step 1
        mBinding.lrcControlView.setMusic(music);

        if (RoomManager.getInstance().isMainSinger()) {
            mBinding.lrcControlView.setRole(LrcControlView.Role.Singer);
        } else {
            mBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        }

        // Step 2
        if (mMusicPlayer != null) {
            mMusicPlayer.stop();
            mMusicPlayer.destroy();
        }

        // Step 3
        // 听众 和 上麦者 —— 没有 播放器
        if (music.getType() == MemberMusicModel.SingType.Chorus) {
            mBinding.lrcControlView.onWaitChorusStatus();
        } else {
            mBinding.lrcControlView.onPrepareStatus();

            mMusicPlayer = new SingleMusicPlayer(this, mPlayer);
            mMusicPlayer.registerPlayerObserver(mMusicCallback);
            mMusicPlayer.prepare(music);
            if( RoomManager.getInstance().isMainSinger()) {
                ChannelMediaOptions options = new ChannelMediaOptions();
                options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
                options.publishMediaPlayerAudioTrack = true;
                RoomManager.getInstance().getRtcEngine().updateChannelMediaOptions(options);
            }
        }

    }

    private void onMusicEmpty() {
        mBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        mBinding.lrcControlView.onIdleStatus();

        if (mMusicPlayer != null) {
            mMusicPlayer.stop();
            mMusicPlayer.destroy();
            mMusicPlayer = null;
        }
    }

    private void setupLrc(MemberMusicModel music) {
        File lrcFile = music.getFileLrc();
        LrcData data = LrcLoadUtils.parse(lrcFile);
        mBinding.lrcControlView.getLrcView().setLrcData(data);
        mBinding.lrcControlView.getPitchView().setLrcData(data);
    }

    //<editor-fold desc="Member stuff">
    private void onMemberJoin(@NonNull AgoraMember member) {
        for (int i = 0; i < mRoomSpeakerAdapter.getItemCount(); i++) {
            if (mRoomSpeakerAdapter.getItemData(i) == null) {
                mRoomSpeakerAdapter.dataList.set(i, member);
                mRoomSpeakerAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void onMemberLeave(@NonNull AgoraMember member) {
        for (int i = 0; i < mRoomSpeakerAdapter.getItemCount(); i++) {
            AgoraMember temp = mRoomSpeakerAdapter.getItemData(i);
            if (temp != null && temp.equals(member)) {
                mRoomSpeakerAdapter.dataList.set(i, null);
                mRoomSpeakerAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        mBinding.lrcControlView.onMemberJoinedChorus();

        mMusicPlayer = new MultipleMusicPlayer(this, mPlayer);
        mMusicPlayer.registerPlayerObserver(mMusicCallback);
        ((MultipleMusicPlayer) mMusicPlayer).onMemberJoinedChorus(music);
    }

    private void onMemberChorusReady(@NonNull MemberMusicModel music) {

    }
    //</editor-fold>

    //<editor-fold desc="Vary Dialogs">

    private void showLeaveConfirmDialog(View v) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ktv_leave_title)
                .setMessage(R.string.ktv_leave_msg)
                .setPositiveButton(R.string.ktv_confirm, (dialog, which) -> finish())
                .setNegativeButton(R.string.ktv_cancel, null)
                .show();
    }

    private void showChooseSongDialog(View v) {
        if (RoomManager.getInstance().mCurrentMemberMusic != null) {
            ToastUtil.toastShort(this, "Already have a song.");
            return;
        }

        boolean isChorus = false;
        if (v.getId() == R.id.btn_chorus_att_room)
            isChorus = true;
        new RoomChooseSongDialog(isChorus).show(getSupportFragmentManager(), RoomChooseSongDialog.TAG);
    }

    private void showBackgroundPicDialog(View v) {
        AgoraRoom room = RoomManager.getInstance().getRoom();
        if (room != null) {
            new RoomMVDialog().show(getSupportFragmentManager(), RoomMVDialog.TAG);
        }
    }

    private void showMusicMenuDialog() {
        if (mPlayer != null)
            new MusicSettingDialog(mSetting).show(getSupportFragmentManager(), MusicSettingDialog.TAG);
    }

    private void showChangeMusicDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ktv_room_change_music_title)
                .setMessage(R.string.ktv_room_change_music_msg)
                .setNegativeButton(R.string.ktv_cancel, null)
                .setPositiveButton(R.string.ktv_confirm, (dialog, which) -> cutOffMusic())
                .show();
    }
    //</editor-fold>


    //<editor-fold desc="Finish Stuff">
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, MyForegroundService.class);
        intent.setAction(MyForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
        stopService(intent);

        dismissLoading();
//        stopOnTrialTimer();

        if (mPlayer != null) {
            mPlayer.destroy();
            mPlayer = null;
        }

        if (mMusicPlayer != null) {
            mMusicPlayer.unregisterPlayerObserver();
            mMusicPlayer.destroy();
            mMusicPlayer = null;
        }
        RoomManager.getInstance().leaveRoom();
    }
    //</editor-fold>

    @Override
    public void onBackPressed() {
        // dismiss dialog
        Fragment tempFragment = null;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BaseBottomSheetDialogFragment)
                tempFragment = fragment;
        }
        if (tempFragment != null)
            ((BaseBottomSheetDialogFragment<?>) tempFragment).dismiss();
        else showLeaveConfirmDialog(mBinding.toolBarAttRoom);
    }
}
