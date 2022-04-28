package io.agora.ktv.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.agora.baselibrary.base.BaseActivity;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
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
import io.agora.ktv.util.BlurTransformation;
import io.agora.ktv.view.dialog.MoreDialog;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.view.dialog.UserSeatMenuDialog;
import io.agora.ktv.widget.DividerDecoration;
import io.agora.ktv.widget.LrcControlView;
import io.agora.lrcview.LrcLoadUtils;
import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryData;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 房间界面
 *
 * @author chenhengfei@agora.io
 */
public class RoomActivity extends BaseActivity<KtvActivityRoomBinding> implements OnItemClickListener<AgoraMember> {
    public static final String TAG_ROOM = "room";

    private List<LrcEntryData> toneList;

    public static Intent newIntent(Context context, AgoraRoom mRoom) {
        Intent intent = new Intent(context, RoomActivity.class);
        intent.putExtra(TAG_ROOM, mRoom);
        return intent;
    }

    private BaseRecyclerViewAdapter<KtvItemRoomSpeakerBinding, AgoraMember, RoomPeopleHolder> mRoomSpeakerAdapter;
    private BaseMusicPlayer mMusicPlayer;

    protected IMediaPlayer mPlayer;

    private MusicSettingBean mSetting;
    private final BaseMusicPlayer.Callback mMusicCallback = new BaseMusicPlayer.Callback() {

        @Override
        public void onPrepareResource() {
            mBinding.lrcControlView.onPrepareStatus();
        }

        @Override
        public void onResourceReady(@NonNull MemberMusicModel music) {
            File lrcFile = music.getFileLrc();
            LrcData data = LrcLoadUtils.parse(lrcFile);
            toneList = data.entrys;
            mBinding.lrcControlView.getLrcView().setLrcData(data);
            mBinding.lrcControlView.getPitchView().setLrcData(data);
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
        public void onMusicPlaing() {
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

            changeMusic();
        }

        @Override
        public void onMusicPositionChanged(long position) {
            mBinding.lrcControlView.getLrcView().updateTime(position);
            mBinding.lrcControlView.getPitchView().updateTime(position);
            double pitch = getMusicPitch(position);
            if(pitch > 0){
                mBinding.lrcControlView.setMusicPitch(pitch);
            }
        }

        @Override
        public void onReceivedCountdown(int time) {
            mBinding.lrcControlView.setCountDown(time);
        }
    };

    private double getMusicPitch(long position) {
        double pitch = 0;
        for(LrcEntryData entry : this.toneList){
            if(position > entry.getStartTime() && position < entry.tones.get(entry.tones.size()-1).end){
                for(LrcEntryData.Tone item: entry.tones){
                    if(position > item.begin && position < item.end){
                        pitch = item.pitch;
                        break;
                    }
                }
            }
        }
        return pitch;
    }

    private final SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {

        @Override
        public void onLocalPitch(double pitch) {
            super.onLocalPitch(pitch);
            mBinding.lrcControlView.setLocalPitch(pitch);
            mBinding.lrcControlView.getPitchView().updateLocalPitch(pitch);
//            mMusicPlayer.writePitchToLog(pitch);
        }

        @Override
        public void onRoomError(int error, String msg) {
            super.onRoomError(error, msg);
            showRoomErrorDialog(msg);
        }

        @Override
        public void onRoomInfoChanged(@NonNull AgoraRoom room) {
            super.onRoomInfoChanged(room);
            mBinding.lrcControlView.setLrcViewBackground(room.getMVRes());
        }

        @Override
        public void onMemberLeave(@NonNull AgoraMember member) {
            super.onMemberLeave(member);
            if (ObjectsCompat.equals(member, RoomManager.Instance(RoomActivity.this).getOwner())) {
                RoomActivity.this.doLeave();
                return;
            }

            RoomActivity.this.onMemberLeave(member);

            if (RoomManager.Instance(RoomActivity.this).isOwner()) {
                MemberMusicModel musicModel = RoomManager.Instance(RoomActivity.this).getMusicModel();
                if (musicModel != null && ObjectsCompat.equals(member.getUserId(), musicModel.getUserId())) {
                    changeMusic();
                }
            }
        }

        @Override
        public void onRoleChanged(@NonNull AgoraMember member) {
            super.onRoleChanged(member);

            if (member.getRole() == AgoraMember.Role.Owner
                    || member.getRole() == AgoraMember.Role.Speaker) {
                RoomActivity.this.onMemberJoin(member);

                AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
                if (ObjectsCompat.equals(member, mMine)) {
                    showOnSeatStatus();

                    int role = Constants.CLIENT_ROLE_BROADCASTER;
                    if (mMusicPlayer != null) {
                        mMusicPlayer.switchRole(role);
                    }
                    RoomManager.Instance(RoomActivity.this).getRtcEngine().setClientRole(role);
                }
            } else if (member.getRole() == AgoraMember.Role.Listener) {
                RoomActivity.this.onMemberLeave(member);

                if (RoomManager.Instance(RoomActivity.this).isOwner()) {
                    if (RoomManager.Instance(RoomActivity.this).isMainSinger(member)) {
                        changeMusic();
                    }
                }

                AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
                if (ObjectsCompat.equals(member, mMine)) {
                    showNotOnSeatStatus();

                    int role = Constants.CLIENT_ROLE_AUDIENCE;
                    if (mMusicPlayer != null) {
                        mMusicPlayer.switchRole(role);
                    }
                    RoomManager.Instance(RoomActivity.this).getRtcEngine().setClientRole(role);

                    if (RoomManager.Instance(RoomActivity.this).isFollowSinger()) {
                        if (mMusicPlayer != null) {
                            mMusicPlayer.stop();

                            MemberMusicModel mMusicModel = RoomManager.Instance(RoomActivity.this).getMusicModel();
                            if (mMusicModel != null) {
                                mMusicPlayer.playByListener(mMusicModel);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onAudioStatusChanged(@NonNull AgoraMember member) {
            super.onAudioStatusChanged(member);

            AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
            if (ObjectsCompat.equals(member, mMine)) {
                if (member.getIsSelfMuted() == 1) {
                    mBinding.btnMicAttRoom.setImageResource(R.drawable.ktv_ic_mic_disable);
                } else {
                    mBinding.btnMicAttRoom.setImageResource(R.drawable.ktv_ic_mic_enable);
                }
            }
        }

        @Override
        public void onVideoStatusChanged(@NonNull AgoraMember member) {
            super.onVideoStatusChanged(member);
            // TODO
            for (int i = 0; i < mRoomSpeakerAdapter.dataList.size(); i++) {
                AgoraMember currentMember = mRoomSpeakerAdapter.dataList.get(i);
                if (currentMember != null && currentMember.getId().equals(member.getId())){
                    mRoomSpeakerAdapter.dataList.set(i, member);
                    mRoomSpeakerAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        @Override
        public void onMusicDelete(@NonNull MemberMusicModel music) {
            super.onMusicDelete(music);
            RoomActivity.this.onMusicDelete(music);
        }

        @Override
        public void onMusicChanged(@NonNull MemberMusicModel music) {
            super.onMusicChanged(music);
            RoomActivity.this.onMusicChanged(music);
        }

        @Override
        public void onMusicEmpty() {
            super.onMusicEmpty();
            RoomActivity.this.onMusicEmpty();
        }

        @Override
        public void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {
            super.onMemberApplyJoinChorus(music);
            RoomActivity.this.onMemberApplyJoinChorus(music);
        }

        @Override
        public void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
            super.onMemberJoinedChorus(music);
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

    private void initView() {
        mRoomSpeakerAdapter = new BaseRecyclerViewAdapter<>(Arrays.asList(new AgoraMember[8]), this, RoomPeopleHolder.class);
        mBinding.recyclerViewAttRoom.addItemDecoration(new DividerDecoration(4,24,8));
        mBinding.recyclerViewAttRoom.setAdapter(mRoomSpeakerAdapter);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
            mBinding.recyclerViewAttRoom.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // FIXME what is this for ?????
        Intent intent = new Intent(this, MyForegroundService.class);
        intent.setAction(MyForegroundService.ACTION_START_FOREGROUND_SERVICE);
        this.startService(intent);
    }

    private void initListener() {
        RoomManager.Instance(this).addRoomEventCallback(mRoomEventCallback);

        mBinding.toolBarAttRoom.setNavigationOnClickListener(this::showLeaveConfirmDialog);
        mBinding.btnMicAttRoom.setOnClickListener(this::toggleMic);
        mBinding.btnChangeCoverAttRoom.setOnClickListener(this::showBackgroundPicDialog);
        mBinding.btnMoreAttRoom.setOnClickListener(this::showMoreDialog);
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
                if (mMusicPlayer != null) {
                    mMusicPlayer.sendCountdown(time);
                }
            }
        });
    }

    private void initData() {
        mSetting = new MusicSettingBean(false, 100, 100, 0, new MusicSettingDialog.Callback() {
            @Override
            public void onEarChanged(boolean isEar) {
                RoomManager.Instance(RoomActivity.this).getRtcEngine().enableInEarMonitoring(isEar);
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
                RoomManager.Instance(RoomActivity.this).getRtcEngine().setAudioEffectPreset(getEffectIndex(effect));
            }

            @Override
            public void onToneChanged(int newToneValue) {
                mMusicPlayer.setAudioMixingPitch(newToneValue);
            }
        });

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            ToastUtil.toastShort(this, "please login in");
            finish();
            return;
        }

        showNotOnSeatStatus();
        mBinding.lrcControlView.setRole(LrcControlView.Role.Listener);

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);
        mBinding.toolBarAttRoom.setTitle(mRoom.getChannelName());

        Glide.with(this)
                .asDrawable()
                .load(mRoom.getCoverRes())
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(this)))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        RoomActivity.this.getWindow().getDecorView().setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        showLoading(false);
        RoomManager.Instance(this)
                .joinRoom(mRoom)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        dismissLoading();
                        onJoinRoom();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dismissLoading();
                        ToastUtil.toastShort(RoomActivity.this, R.string.ktv_join_error);
                        doLeave();
                    }
                });
    }

    private void onJoinRoom() {
        AgoraRoom mRoom = RoomManager.Instance(this).getRoom();
        assert mRoom != null;

        AgoraMember owner = RoomManager.Instance(this).getOwner();
        assert owner != null;
        RoomActivity.this.onMemberJoin(owner);

        if (RoomManager.Instance(this).isOwner()) {
            long liveTimeLeft = mRoom.getCreatedAt().getTime() + (10 * 60 * 1000) - System.currentTimeMillis();
            if (liveTimeLeft <= 0) {
                ToastUtil.toastShort(RoomActivity.this, R.string.ktv_use_overtime);
                doLeave();
                return;
            }

            startOnTrialTimer(liveTimeLeft);
        }

        mPlayer = RoomManager.Instance(this).getRtcEngine().createMediaPlayer();

        mBinding.lrcControlView.setLrcViewBackground(mRoom.getMVRes());

        if (RoomManager.Instance(this).isOwner()) {
            showOnSeatStatus();
        } else {
            showNotOnSeatStatus();
        }

        RoomManager.Instance(this).loadMemberStatus();
        syncMusics();
    }

    private void changeSingType() {
        AgoraRoom mRoom = RoomManager.Instance(RoomActivity.this).getRoom();
        if (mRoom == null) {
            return;
        }

        MemberMusicModel music = RoomManager.Instance(RoomActivity.this).getMusicModel();
        if (music == null) {
            return;
        }

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .document(music.getId())
                .update(MemberMusicModel.COLUMN_TYPE, MemberMusicModel.SingType.Single.value, new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {

                    }

                    @Override
                    public void onFail(AgoraException exception) {

                    }
                });
    }

    private void joinChorus() {
        AgoraRoom mRoom = RoomManager.Instance(this).getRoom();
        if (mRoom == null) {
            return;
        }

        MemberMusicModel musicModel = RoomManager.Instance(this).getMusicModel();
        if (musicModel == null) {
            return;
        }

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        AgoraMember mMember = RoomManager.Instance(this).getMine();
        if (mMember == null) {
            return;
        }

        if (mMember.getRole() == AgoraMember.Role.Listener) {
            ToastUtil.toastShort(this, R.string.ktv_need_up);
            return;
        }

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .document(musicModel.getId())
                .update(MemberMusicModel.COLUMN_APPLYUSERID, mUser.getObjectId(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {

                    }

                    @Override
                    public void onFail(AgoraException exception) {

                    }
                });
    }

    private CountDownTimer timerOnTrial;

    private void startOnTrialTimer(long liveTimeLeft) {
        timerOnTrial = new CountDownTimer(liveTimeLeft, 999) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                ToastUtil.toastShort(RoomActivity.this, R.string.ktv_use_overtime);
                doLeave();
            }
        }.start();
    }

    private void stopOnTrialTimer() {
        if (timerOnTrial != null) {
            timerOnTrial.cancel();
            timerOnTrial = null;
        }
    }

    private void syncMusics() {
        RoomManager.Instance(this)
                .getMusicOrderList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<MemberMusicModel>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull List<MemberMusicModel> musicModels) {
                        if (musicModels.isEmpty()) {
                            RoomManager.Instance(RoomActivity.this).onMusicEmpty();
                        } else {
                            RoomManager.Instance(RoomActivity.this).onMusicChanged(musicModels.get(0));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtil.toastShort(RoomActivity.this, R.string.ktv_sync_music_error);
                        e.printStackTrace();
                    }
                });
    }

    private AlertDialog mAlertDialogRoomError;

    private void showRoomErrorDialog(String msg) {
        if (mAlertDialogRoomError != null && mAlertDialogRoomError.isShowing()) {
            return;
        }

        mAlertDialogRoomError = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setNegativeButton(R.string.ktv_done, (dialog, which) -> doLeave())
                .show();
    }

    private void showChooseSongDialog(View v) {
        boolean isChorus = false;
        if (v.getId() == R.id.btn_chorus_att_room)
            isChorus = true;
        new RoomChooseSongDialog(isChorus).show(getSupportFragmentManager(), RoomChooseSongDialog.TAG);
    }
    private void showLeaveConfirmDialog(View v){
        new AlertDialog.Builder(this)
                .setTitle(R.string.ktv_leave_title)
                .setMessage(R.string.ktv_leave_msg)
                .setPositiveButton(R.string.ktv_confirm, (dialog, which) -> doLeave())
                .setNegativeButton(R.string.ktv_cancel, null)
                .show();
    }

    private void showBackgroundPicDialog(View v) {
        AgoraRoom room = RoomManager.Instance(this).getRoom();
        if (room == null) {
            return;
        }

        new RoomMVDialog().show(getSupportFragmentManager(), Integer.parseInt(room.getMv()) - 1);
    }
    private void showMoreDialog(View v) {
        new MoreDialog(mSetting).show(getSupportFragmentManager(), MoreDialog.TAG);
    }

    private void doLeave() {
        RoomManager.Instance(this)
                .leaveRoom()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
        finish();
    }

    private void toggleMic(View v) {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        mBinding.btnMicAttRoom.setEnabled(false);
        boolean newValue = mMine.getIsSelfMuted() == 0;
        RoomManager.Instance(this)
                .toggleSelfAudio(newValue)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        mBinding.btnMicAttRoom.setEnabled(true);

                        ChannelMediaOptions options = new ChannelMediaOptions();
                        options.publishAudioTrack = !newValue;
                        RoomManager.Instance(RoomActivity.this).getRtcEngine().updateChannelMediaOptions(options);

                        mBinding.btnMicAttRoom.setImageResource(newValue ? R.drawable.ktv_ic_mic_disable : R.drawable.ktv_ic_mic_enable);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    private void toggleOriginal() {
        if (mMusicPlayer == null) {
            return;
        }

        if (mMusicPlayer.hasAccompaniment()) {
            mMusicPlayer.toggleOrigle();
        } else {
            mBinding.lrcControlView.setSwitchOriginalChecked(true);
            ToastUtil.toastShort(this, R.string.ktv_error_cut);
        }
    }

    private boolean isEar = false;
    private int volMic = 100;
    private int volMusic = 100;

    private void showMusicMenuDialog() {
        if (mPlayer != null)
            new MusicSettingDialog(mSetting).show(getSupportFragmentManager(), MusicSettingDialog.TAG);
    }

    private int getEffectIndex(int index) {
        switch (index) {
            case 0:
                return Constants.AUDIO_EFFECT_OFF;
            case 1:
                return Constants.ROOM_ACOUSTICS_KTV;
            case 2:
                return Constants.ROOM_ACOUSTICS_VOCAL_CONCERT;
            case 3:
                return Constants.ROOM_ACOUSTICS_STUDIO;
            case 4:
                return Constants.ROOM_ACOUSTICS_PHONOGRAPH;
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

    private void showChangeMusicDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ktv_room_change_music_title)
                .setMessage(R.string.ktv_room_change_music_msg)
                .setNegativeButton(R.string.ktv_cancel, null)
                .setPositiveButton(R.string.ktv_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeMusic();
                    }
                })
                .show();
    }

    private void changeMusic() {
        AgoraRoom mRoom = RoomManager.Instance(this).getRoom();
        if (mRoom == null) {
            return;
        }

        MemberMusicModel musicModel = RoomManager.Instance(this).getMusicModel();
        if (musicModel == null) {
            return;
        }

        if (mMusicPlayer != null) {
            mMusicPlayer.selectAudioTrack(1);
            mMusicPlayer.stop();
        }

        mBinding.lrcControlView.setEnabled(false);
        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .document(musicModel.getId())
                .delete(new SyncManager.Callback() {
                    @Override
                    public void onSuccess() {
                        mBinding.lrcControlView.setEnabled(true);
                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        mBinding.lrcControlView.setEnabled(true);
                        ToastUtil.toastShort(RoomActivity.this, exception.getMessage());
                    }
                });
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

    @Override
    public void onItemClick(@NonNull AgoraMember data, View view, int position, long viewType) {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        if (mMine.getRole() == AgoraMember.Role.Owner) {
            if (ObjectsCompat.equals(mMine, data)) {
                return;
            }
        } else if (!ObjectsCompat.equals(mMine, data)) {
            return;
        }

        new UserSeatMenuDialog(data).show(getSupportFragmentManager(), UserSeatMenuDialog.TAG);
    }

    @Override
    public void onItemClick(View view, int position, long id) {
        requestSeatOn();
    }

    private void requestSeatOn() {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        if (mMine.getRole() != AgoraMember.Role.Listener) {
            return;
        }

        mBinding.recyclerViewAttRoom.setEnabled(false);
        RoomManager.Instance(this)
                .changeRole(mMine, AgoraMember.Role.Speaker.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        mBinding.recyclerViewAttRoom.setEnabled(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mBinding.recyclerViewAttRoom.setEnabled(true);
                        ToastUtil.toastShort(RoomActivity.this, e.getMessage());
                    }
                });
    }

    private void onMusicDelete(@NonNull MemberMusicModel music) {

    }

    private void onMusicChanged(@NonNull MemberMusicModel music) {
        mBinding.lrcControlView.setMusic(music);

        User mUser = UserManager.getInstance().mUser;
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            mBinding.lrcControlView.setRole(LrcControlView.Role.Singer);
        } else {
            mBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        }

        if (mMusicPlayer != null) {
            mMusicPlayer.stop();
            mMusicPlayer.destroy();
        }

        int role = Constants.CLIENT_ROLE_BROADCASTER;
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine != null) {
            if (mMine.getRole() == AgoraMember.Role.Owner ||
                    mMine.getRole() == AgoraMember.Role.Speaker) {
                role = Constants.CLIENT_ROLE_BROADCASTER;
            } else if (mMine.getRole() == AgoraMember.Role.Listener) {
                role = Constants.CLIENT_ROLE_AUDIENCE;
            }
        }

        if (music.getType() == MemberMusicModel.SingType.Single) {
            mBinding.lrcControlView.onPrepareStatus();
            mMusicPlayer = new SingleMusicPlayer(this, role, mPlayer);
        } else if (music.getType() == MemberMusicModel.SingType.Chorus) {
            mBinding.lrcControlView.onWaitChorusStatus();
            if (mMine != null && music.getUser1Id() != null && music.getUser1Id().equals(mMine.getUserId())) {
                role = Constants.CLIENT_ROLE_BROADCASTER;
            }
            mMusicPlayer = new MultipleMusicPlayer(this, role, mPlayer);
        }
        mMusicPlayer.registerPlayerObserver(mMusicCallback);
        mMusicPlayer.prepare(music);
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

    private void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {

    }

    private void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        mBinding.lrcControlView.onMemberJoinedChorus();
    }

    private void onMemberChorusReady(@NonNull MemberMusicModel music) {

    }

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

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, MyForegroundService.class);
        intent.setAction(MyForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
        stopService(intent);

        if (mPlayer != null) {
            mPlayer.destroy();
            mPlayer = null;
        }

        dismissLoading();
        stopOnTrialTimer();

        RoomManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        if (mMusicPlayer != null) {
            mMusicPlayer.unregisterPlayerObserver();
            mMusicPlayer.destroy();
            mMusicPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

    }
}
