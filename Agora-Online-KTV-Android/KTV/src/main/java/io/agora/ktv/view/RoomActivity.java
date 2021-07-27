package io.agora.ktv.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomSpeakerAdapter;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
import io.agora.ktv.manager.BaseMusicPlayer;
import io.agora.ktv.manager.MultipleMusicPlayer;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.manager.SimpleRoomEventCallback;
import io.agora.ktv.manager.SingleMusicPlayer;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.view.dialog.UserSeatMenuDialog;
import io.agora.ktv.view.dialog.WaitingDialog;
import io.agora.ktv.widget.LrcControlView;
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
public class RoomActivity extends DataBindBaseActivity<KtvActivityRoomBinding> implements View.OnClickListener, OnItemClickListener<AgoraMember> {
    private static final String TAG_ROOM = "room";

    public static Intent newIntent(Context context, AgoraRoom mRoom) {
        Intent intent = new Intent(context, RoomActivity.class);
        intent.putExtra(TAG_ROOM, mRoom);
        return intent;
    }

    private RoomSpeakerAdapter mRoomSpeakerAdapter;
    private BaseMusicPlayer mMusicPlayer;

    private final BaseMusicPlayer.Callback mMusicCallback = new BaseMusicPlayer.Callback() {

        @Override
        public void onPrepareResource() {
            mDataBinding.lrcControlView.onPrepareStatus();
        }

        @Override
        public void onResourceReady(@NonNull MemberMusicModel music) {
            File lrcFile = music.getFileLrc();
            mDataBinding.lrcControlView.getLrcView().loadLrc(lrcFile);
        }

        @Override
        public void onMusicOpening() {
        }

        @Override
        public void onMusicOpenCompleted(long duration) {
            mDataBinding.lrcControlView.getLrcView().setTotalDuration(duration);
        }

        @Override
        public void onMusicOpenError(int error) {

        }

        @Override
        public void onMusicPlaing() {
            mDataBinding.lrcControlView.onPlayStatus();
        }

        @Override
        public void onMusicPause() {
            mDataBinding.lrcControlView.onPauseStatus();
        }

        @Override
        public void onMusicStop() {

        }

        @Override
        public void onMusicCompleted() {
            mDataBinding.lrcControlView.getLrcView().reset();

            changeMusic();
        }

        @Override
        public void onMusicPositionChanged(long position) {
            mDataBinding.lrcControlView.getLrcView().updateTime(position);
        }

        @Override
        public void onReceivedCountdown(int time) {
            mDataBinding.lrcControlView.setCountDown(time);
        }
    };

    private final SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {

        @Override
        public void onRoomError(int error, String msg) {
            super.onRoomError(error, msg);
            showRoomErrorDialog(msg);
        }

        @Override
        public void onRoomInfoChanged(@NonNull AgoraRoom room) {
            super.onRoomInfoChanged(room);
            mDataBinding.lrcControlView.setLrcViewBackground(room.getMVRes());
        }

        @Override
        public void onMemberLeave(@NonNull AgoraMember member) {
            super.onMemberLeave(member);
            if (ObjectsCompat.equals(member, RoomManager.Instance(RoomActivity.this).getOwner())) {
                RoomActivity.this.doLeave();
                return;
            }

            mRoomSpeakerAdapter.deleteItem(member);

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
                mRoomSpeakerAdapter.addItem(member);

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
                mRoomSpeakerAdapter.deleteItem(member);

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
        public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
            super.onAudioStatusChanged(isMine, member);

            AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
            if (ObjectsCompat.equals(member, mMine)) {
                if (member.getIsSelfMuted() == 1) {
                    mDataBinding.ivMic.setImageResource(R.mipmap.ktv_room_unmic);
                } else {
                    mDataBinding.ivMic.setImageResource(R.mipmap.ktv_room_mic);
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

        @Override
        public void onCountDown(int time) {
            super.onCountDown(time);
            mDataBinding.lrcControlView.setCountDown(time);
        }
    };

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.ktv_activity_room;
    }

    @Override
    protected void iniView() {
        mRoomSpeakerAdapter = new RoomSpeakerAdapter(new ArrayList<>(), this);
        mDataBinding.rvSpeakers.setLayoutManager(new GridLayoutManager(this, 4));
        mDataBinding.rvSpeakers.setAdapter(mRoomSpeakerAdapter);
    }

    @Override
    protected void iniListener() {
        RoomManager.Instance(this).addRoomEventCallback(mRoomEventCallback);
        mDataBinding.ivLeave.setOnClickListener(this);
        mDataBinding.ivMic.setOnClickListener(this);
        mDataBinding.ivBackgroundPicture.setOnClickListener(this);
        mDataBinding.llChooseSong.setOnClickListener(this);
        mDataBinding.ivChorus.setOnClickListener(this);

        mDataBinding.lrcControlView.setOnLrcClickListener(new LrcControlView.OnLrcActionListener() {
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

    @Override
    protected void iniData() {
        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            ToastUtile.toastShort(this, "please login in");
            finish();
            return;
        }

        showNotOnSeatStatus();
        mDataBinding.lrcControlView.setRole(LrcControlView.Role.Listener);

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);
        mDataBinding.tvName.setText(mRoom.getChannelName());

        showJoinRoomDialog();
        RoomManager.Instance(this)
                .joinRoom(mRoom)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        closeJoinRoomDialog();
                        onJoinRoom();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        closeJoinRoomDialog();
                        ToastUtile.toastShort(RoomActivity.this, "加入房间失败");
                        doLeave();
                    }
                });
    }

    private WaitingDialog dialogJoinRoom = null;

    private void showJoinRoomDialog() {
        if (dialogJoinRoom != null && dialogJoinRoom.isShowing()) {
            return;
        }

        dialogJoinRoom = new WaitingDialog();
        dialogJoinRoom.show(getSupportFragmentManager(), getString(R.string.ktv_dialog_join_msg), new WaitingDialog.Callback() {
            @Override
            public void onTimeout() {

            }
        });
    }

    private void closeJoinRoomDialog() {
        if (dialogJoinRoom == null || dialogJoinRoom.isShowing() == false) {
            return;
        }

        dialogJoinRoom.dismiss();
    }

    private void onJoinRoom() {
        AgoraRoom mRoom = RoomManager.Instance(this).getRoom();
        assert mRoom != null;

        AgoraMember owner = RoomManager.Instance(this).getOwner();
        assert owner != null;
        mRoomSpeakerAdapter.addItem(owner);

        if (RoomManager.Instance(this).isOwner()) {
            long liveTimeLeft = mRoom.getCreatedAt().getTime() + (10 * 60 * 1000) - System.currentTimeMillis();
            if (liveTimeLeft <= 0) {
                ToastUtile.toastShort(RoomActivity.this, "试用时间已到");
                doLeave();
                return;
            }

            startOnTrialTimer(liveTimeLeft);
        }

        mDataBinding.lrcControlView.setLrcViewBackground(mRoom.getMVRes());

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

        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        AgoraMember mMember = RoomManager.Instance(this).getMine();
        if (mMember == null) {
            return;
        }

        if (mMember.getRole() == AgoraMember.Role.Listener) {
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
                ToastUtile.toastShort(RoomActivity.this, "试用时间已到");
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
                .compose(mLifecycleProvider.bindToLifecycle())
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
                        ToastUtile.toastShort(RoomActivity.this, "同步歌曲失败");
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
                .setNegativeButton(R.string.ktv_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doLeave();
                    }
                })
                .show();
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.ivLeave) {
            doLeave();
        } else if (v == mDataBinding.ivMic) {
            toggleMic();
        } else if (v == mDataBinding.ivBackgroundPicture) {
            showBackgroundPicDialog();
        } else if (v == mDataBinding.llChooseSong) {
            RoomChooseSongDialog.mSingType = MemberMusicModel.SingType.Single;
            showChooseSongDialog();
        } else if (v == mDataBinding.ivChorus) {
            RoomChooseSongDialog.mSingType = MemberMusicModel.SingType.Chorus;
            showChooseSongDialog();
        }
    }

    private void showChooseSongDialog() {
        new RoomChooseSongDialog().show(getSupportFragmentManager());
    }

    private void showBackgroundPicDialog() {
        AgoraRoom room = RoomManager.Instance(this).getRoom();
        if (room == null) {
            return;
        }

        new RoomMVDialog().show(getSupportFragmentManager(), Integer.parseInt(room.getMv()) - 1);
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

    private void toggleMic() {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        mDataBinding.ivMic.setEnabled(false);
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
                        mDataBinding.ivMic.setEnabled(true);

                        ChannelMediaOptions options = new ChannelMediaOptions();
                        options.publishAudioTrack = !newValue;
                        RoomManager.Instance(RoomActivity.this).getRtcEngine().updateChannelMediaOptions(options);

                        mDataBinding.ivMic.setImageResource(newValue ? R.mipmap.ktv_room_unmic : R.mipmap.ktv_room_mic);
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
            mDataBinding.lrcControlView.setSwitchOriginalChecked(true);
            ToastUtile.toastShort(this, "该歌曲无法进行人声切换");
        }
    }

    private boolean isEar = false;
    private int volMic = 100;
    private int volMusic = 100;

    private void showMusicMenuDialog() {
        if (mMusicPlayer == null) {
            return;
        }

        new MusicSettingDialog().show(getSupportFragmentManager(), isEar, volMic, volMusic, new MusicSettingDialog.Callback() {
            @Override
            public void onEarChanged(boolean isEar) {
                RoomActivity.this.isEar = isEar;
                RoomManager.Instance(RoomActivity.this).getRtcEngine().enableInEarMonitoring(isEar);
            }

            @Override
            public void onMicVolChanged(int vol) {
                RoomActivity.this.volMic = vol;
                mMusicPlayer.setMicVolume(vol);
            }

            @Override
            public void onMusicVolChanged(int vol) {
                RoomActivity.this.volMusic = vol;
                mMusicPlayer.setMusicVolume(vol);
            }
        });
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
            mMusicPlayer.stop();
        }

        mDataBinding.lrcControlView.setEnabled(false);
        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .document(musicModel.getId())
                .delete(new SyncManager.Callback() {
                    @Override
                    public void onSuccess() {
                        mDataBinding.lrcControlView.setEnabled(true);
                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        mDataBinding.lrcControlView.setEnabled(true);
                        ToastUtile.toastShort(RoomActivity.this, exception.getMessage());
                    }
                });
    }

    private void toggleStart() {
        if (mMusicPlayer == null) {
            return;
        }

        mMusicPlayer.toggleStart();
    }

    private void showOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.VISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.VISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.VISIBLE);
        mDataBinding.ivChorus.setVisibility(View.VISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.GONE);
    }

    private void showNotOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.INVISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.INVISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.INVISIBLE);
        mDataBinding.ivChorus.setVisibility(View.INVISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(@NonNull AgoraMember data, View view, int position, long id) {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        if (mMine.getRole() == AgoraMember.Role.Owner) {
            if (ObjectsCompat.equals(mMine, data)) {
                return;
            }
        } else if (ObjectsCompat.equals(mMine, data) == false) {
            return;
        }

        new UserSeatMenuDialog().show(getSupportFragmentManager(), data);
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

        mDataBinding.rvSpeakers.setEnabled(false);
        RoomManager.Instance(this)
                .changeRole(mMine, AgoraMember.Role.Speaker.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.rvSpeakers.setEnabled(true);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mDataBinding.rvSpeakers.setEnabled(true);
                        ToastUtile.toastShort(RoomActivity.this, e.getMessage());
                    }
                });
    }

    private void onMusicDelete(@NonNull MemberMusicModel music) {
        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUser1Id(), mUser.getObjectId())) {
            if (mMusicPlayer != null) {
                mMusicPlayer.stop();
            }
        }
    }

    private void onMusicChanged(@NonNull MemberMusicModel music) {
        mDataBinding.lrcControlView.setMusic(music);

        if (music.getType() == MemberMusicModel.SingType.Single) {
            RoomManager.Instance(this).getRtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        } else if (music.getType() == MemberMusicModel.SingType.Chorus) {
            RoomManager.Instance(this).getRtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING_2);
        }

        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            mDataBinding.lrcControlView.setRole(LrcControlView.Role.Singer);
        } else {
            mDataBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        }

        if (mMusicPlayer != null) {
            mMusicPlayer.destory();
            mMusicPlayer = null;
        }

        if (music.getType() == MemberMusicModel.SingType.Single) {
            mDataBinding.lrcControlView.onPrepareStatus();
            mMusicPlayer = new SingleMusicPlayer(this);
            mMusicPlayer.prepare(music);
        } else if (music.getType() == MemberMusicModel.SingType.Chorus) {
            mDataBinding.lrcControlView.onWaitChorusStatus();
            mMusicPlayer = new MultipleMusicPlayer(this);
            mMusicPlayer.prepare(music);
        }

        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine != null) {
            if (mMine.getRole() == AgoraMember.Role.Owner ||
                    mMine.getRole() == AgoraMember.Role.Speaker) {
                int role = Constants.CLIENT_ROLE_BROADCASTER;
                mMusicPlayer.switchRole(role);
            } else if (mMine.getRole() == AgoraMember.Role.Listener) {
                int role = Constants.CLIENT_ROLE_AUDIENCE;
                mMusicPlayer.switchRole(role);
            }
        }

        mMusicPlayer.registerPlayerObserver(mMusicCallback);
    }

    private void onMusicEmpty() {
        mDataBinding.lrcControlView.setRole(LrcControlView.Role.Listener);
        mDataBinding.lrcControlView.onIdleStatus();

        if (mMusicPlayer != null) {
            mMusicPlayer.destory();
            mMusicPlayer = null;
        }
    }

    private void onMemberApplyJoinChorus(@NonNull MemberMusicModel music) {

    }

    private void onMemberJoinedChorus(@NonNull MemberMusicModel music) {
        mDataBinding.lrcControlView.onMemberJoinedChorus();
    }

    private void onMemberChorusReady(@NonNull MemberMusicModel music) {

    }

    @Override
    protected void onDestroy() {
        closeJoinRoomDialog();
        stopOnTrialTimer();

        RoomManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        if (mMusicPlayer != null) {
            mMusicPlayer.unregisterPlayerObserver();
            mMusicPlayer.destory();
            mMusicPlayer = null;
        }
        super.onDestroy();
    }
}
