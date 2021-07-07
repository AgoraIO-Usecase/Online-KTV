package io.agora.ktv.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomSpeakerAdapter;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
import io.agora.ktv.manager.MusicPlayer;
import io.agora.ktv.manager.MusicResourceManager;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.manager.SimpleRoomEventCallback;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.view.dialog.UserSeatMenuDialog;
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
    private MusicPlayer mMusicPlayer;

    private MusicPlayer.Callback mMusicCallback = new MusicPlayer.Callback() {

        @Override
        public void onMusicOpening() {
            mDataBinding.ivChangeSong.setEnabled(false);
        }

        @Override
        public void onMusicOpenCompleted() {
            mDataBinding.ivChangeSong.setEnabled(true);
        }

        @Override
        public void onMusicOpenError(int error) {

        }

        @Override
        public void onMusicPlaing() {
            mDataBinding.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_pause);
        }

        @Override
        public void onMusicPause() {
            mDataBinding.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_play);
        }

        @Override
        public void onMusicStop() {

        }

        @Override
        public void onMusicCompleted() {
            changeMusic();
        }

        @Override
        public void onMusicPreparing() {
            mDataBinding.tvLoad.setVisibility(View.VISIBLE);
            mDataBinding.tvLoad.setText(R.string.ktv_lrc_loading);
        }

        @Override
        public void onMusicPrepared() {
            mDataBinding.tvLoad.setVisibility(View.GONE);
        }
    };

    private SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {

        @Override
        public void onRoomError(int error, String msg) {
            super.onRoomError(error, msg);
            showRoomErrorDialog(msg);
        }

        @Override
        public void onRoomInfoChanged(@NonNull AgoraRoom room) {
            super.onRoomInfoChanged(room);
            mDataBinding.rlSing.setBackgroundResource(room.getMVRes());
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

            if (member.getRole() == AgoraMember.Role.Speaker) {
                mRoomSpeakerAdapter.addItem(member);

                AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
                if (ObjectsCompat.equals(member, mMine)) {
                    showOnSeatStatus();
                    mMusicPlayer.switchRole(Constants.CLIENT_ROLE_BROADCASTER);

                    RoomManager.Instance(RoomActivity.this).getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                }
            } else if (member.getRole() == AgoraMember.Role.Listener) {
                mRoomSpeakerAdapter.deleteItem(member);

                AgoraMember mMine = RoomManager.Instance(RoomActivity.this).getMine();
                if (ObjectsCompat.equals(member, mMine)) {
                    showNotOnSeatStatus();

                    int role = Constants.CLIENT_ROLE_AUDIENCE;
                    mMusicPlayer.switchRole(role);
                    RoomManager.Instance(RoomActivity.this).getRtcEngine().setClientRole(role);
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
        public void onMusicChanged(@NonNull MemberMusicModel music) {
            RoomActivity.this.onMusicChanged(music);
        }

        @Override
        public void onMusicEmpty() {
            RoomActivity.this.onMusicEmpty();
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
        mDataBinding.switchOriginal.setOnClickListener(this);
        mDataBinding.ivMusicMenu.setOnClickListener(this);
        mDataBinding.ivMusicStart.setOnClickListener(this);
        mDataBinding.ivChangeSong.setOnClickListener(this);
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

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);
        mDataBinding.tvName.setText(mRoom.getChannelName());

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
                        onJoinRoom();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtile.toastShort(RoomActivity.this, "加入房间失败");
                        doLeave();
                    }
                });
    }

    private void onJoinRoom() {
        AgoraRoom mRoom = RoomManager.Instance(this).getRoom();
        assert mRoom != null;
        mDataBinding.rlSing.setBackgroundResource(mRoom.getMVRes());

        mMusicPlayer = new MusicPlayer(getApplicationContext(), RoomManager.Instance(this).getRtcEngine(), mDataBinding.lrcView);
        mMusicPlayer.registerPlayerObserver(mMusicCallback);

        AgoraMember owner = RoomManager.Instance(this).getOwner();
        assert owner != null;
        mRoomSpeakerAdapter.addItem(owner);

        if (RoomManager.Instance(this).isOwner()) {
            showOnSeatStatus();
        } else {
            showNotOnSeatStatus();
        }

        RoomManager.Instance(this).loadMemberStatus();
        syncMusics();
    }

    private void preperMusic(final MemberMusicModel musicModel, boolean isSinger) {
        mMusicCallback.onMusicPreparing();
        MusicResourceManager.Instance(this)
                .prepareMusic(musicModel, !isSinger)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new SingleObserver<MemberMusicModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull MemberMusicModel musicModel) {
                        mMusicCallback.onMusicPrepared();

//                        File root = getExternalCacheDir();
//                        musicModel.setFileLrc(new File(root, "突然好想你.xml"));
//                        musicModel.setFileMusic(new File(root, "突然好想你.mp3"));
                        if (isSinger) {
                            mMusicPlayer.play(musicModel);
                        } else {
                            mMusicPlayer.playWithDisplay(musicModel);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mDataBinding.tvLoad.setText(R.string.ktv_lrc_load_fail);
                        ToastUtile.toastShort(RoomActivity.this, R.string.ktv_lrc_load_fail);
                    }
                });
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
            showChooseSongDialog();
        } else if (v == mDataBinding.ivChorus) {

        } else if (v == mDataBinding.switchOriginal) {
            toggleOriginal();
        } else if (v == mDataBinding.ivMusicMenu) {
            showMusicMenuDialog();
        } else if (v == mDataBinding.ivMusicStart) {
            toggleStart();
        } else if (v == mDataBinding.ivChangeSong) {
            changeMusic();
        }
    }

    private void showChooseSongDialog() {
        new RoomChooseSongDialog().show(getSupportFragmentManager());
    }

    private void showBackgroundPicDialog() {
        new RoomMVDialog().show(getSupportFragmentManager());
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
            mDataBinding.switchOriginal.setChecked(true);
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

        mDataBinding.ivMusicMenu.setEnabled(false);
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
        mDataBinding.ivMusicMenu.setEnabled(true);
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

        if (mMusicPlayer == null) {
            return;
        }

        mDataBinding.ivChangeSong.setEnabled(false);
        mMusicPlayer.stop()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        SyncManager.Instance()
                                .getRoom(mRoom.getId())
                                .collection(MemberMusicModel.TABLE_NAME)
                                .document(musicModel.getId())
                                .delete(new SyncManager.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        mDataBinding.ivChangeSong.setEnabled(true);
                                    }

                                    @Override
                                    public void onFail(AgoraException exception) {
                                        mDataBinding.ivChangeSong.setEnabled(true);
                                        ToastUtile.toastShort(RoomActivity.this, exception.getMessage());
                                        finish();
                                    }
                                });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mDataBinding.ivChangeSong.setEnabled(true);
                    }
                });
    }

    private void toggleStart() {
        if (mMusicPlayer == null) {
            return;
        }

        if (mMusicPlayer.isPlaying() == false) {
            return;
        }

        if (mMusicPlayer.isPaused()) {
            mMusicPlayer.resume();
        } else {
            mMusicPlayer.pause();
        }
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
        mDataBinding.rlSing.setVisibility(View.GONE);
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

    private void onMusicChanged(@NonNull MemberMusicModel music) {
        mDataBinding.llNoSing.setVisibility(View.GONE);
        mDataBinding.rlSing.setVisibility(View.VISIBLE);
        mDataBinding.tvMusicName.setText(music.getName());

        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            mDataBinding.rlMusicMenu.setVisibility(View.VISIBLE);
            mDataBinding.switchOriginal.setChecked(true);

            preperMusic(music, true);
        } else {
            preperMusic(music, false);
            mDataBinding.rlMusicMenu.setVisibility(View.GONE);
        }
    }

    private void onMusicEmpty() {
        mDataBinding.llNoSing.setVisibility(View.VISIBLE);
        mDataBinding.rlSing.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        RoomManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        if (mMusicPlayer != null) {
            mMusicPlayer.unregisterPlayerObserver();
            mMusicPlayer.destory();
            mMusicPlayer = null;
        }
        super.onDestroy();
    }
}
