package io.agora.ktv.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import io.agora.ktv.manager.SimpleRoomEventCallback;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import io.agora.ktv.bean.MusicModel;
import com.agora.data.model.User;
import io.agora.ktv.manager.RoomManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.MusicPlayer;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomSpeakerAdapter;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
import io.agora.ktv.view.dialog.MusicSettingDialog;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.view.dialog.RoomMVDialog;
import io.agora.ktv.view.dialog.UserSeatMenuDialog;
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

    private SimpleRoomEventCallback mRoomEventCallback = new SimpleRoomEventCallback() {

        @Override
        public void onMemberLeave(@NonNull AgoraMember member) {
            super.onMemberLeave(member);
            if (ObjectsCompat.equals(member, RoomManager.Instance(RoomActivity.this).getOwner())) {
                RoomActivity.this.doLeave();
                return;
            }

            mRoomSpeakerAdapter.deleteItem(member);
        }

        @Override
        public void onRoleChanged(boolean isMine, @NonNull AgoraMember member) {
            super.onRoleChanged(isMine, member);

            if (member.getRole() == AgoraMember.Role.Speaker) {
                mRoomSpeakerAdapter.addItem(member);
            } else {
                mRoomSpeakerAdapter.deleteItem(member);
            }
        }

        @Override
        public void onAudioStatusChanged(boolean isMine, @NonNull AgoraMember member) {
            super.onAudioStatusChanged(isMine, member);
        }

        @Override
        public void onMusicChanged(@NonNull MusicModel music) {
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

        mCacheDir = getApplicationContext().getExternalCacheDir().getPath();
        extractAsset("qinghuaci.m4a");
        extractAsset("send_it.m4a");

        showOnSeatStatus();
        showMusicIDLEStatus();

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);
        mDataBinding.tvName.setText(mRoom.getName());

        mMusicPlayer = new MusicPlayer(getApplicationContext(), RoomManager.Instance(this).getRtcEngine(), mDataBinding.lrcView);
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
                        doLeave();
                    }
                });
    }

    private String mCacheDir = "";

    private void extractAsset(String f) {
        String filePath = mCacheDir + "/" + f;
        File tmpf = new File(filePath);
        if (tmpf.exists()) {
            return;
        }
        try {
            InputStream is = getAssets().open(f);
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int byteRead;
            while ((byteRead = is.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onJoinRoom() {
        AgoraMember owner = RoomManager.Instance(this).getOwner();
        assert owner != null;
        mRoomSpeakerAdapter.addItem(owner);

        RoomManager.Instance(this).loadMemberStatus();
        RoomManager.Instance(this)
                .getMusicsFromRemote()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new SingleObserver<List<MusicModel>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull List<MusicModel> musicModels) {
                        if (musicModels.isEmpty()) {
                            onMusicEmpty();
                        } else {
                            onMusicChanged(musicModels.get(0));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
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
        boolean newValue = mMine.getIsSelfAudioMuted() == 0;
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
        if (mMusicPlayer.getAudioTracksCount() >= 2) {
            if (mMusicPlayer.getAudioTrackIndex() == 0) {
                mMusicPlayer.selectAudioTrack(1);
            } else {
                mMusicPlayer.selectAudioTrack(0);
            }
        }
    }

    private void showMusicMenuDialog() {
        new MusicSettingDialog().show(getSupportFragmentManager());
    }

    private void changeMusic() {

    }

    private void toggleStart() {
        if (mMusicPlayer.isPlaying()) {
            mMusicPlayer.pause();
        } else {
            mMusicPlayer.resume();
        }
    }

    private void showMusicIDLEStatus() {
        mDataBinding.llNoSing.setVisibility(View.VISIBLE);
        mDataBinding.rlSing.setVisibility(View.GONE);
    }

    private void showMusicStartStatus() {
        mDataBinding.llNoSing.setVisibility(View.GONE);
        mDataBinding.rlSing.setVisibility(View.VISIBLE);
    }

    private void showMusicPauseStatus() {
        mDataBinding.llNoSing.setVisibility(View.VISIBLE);
        mDataBinding.rlSing.setVisibility(View.VISIBLE);
    }

    private void showOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.VISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.VISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.VISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.GONE);
    }

    private void showNotOnSeatStatus() {
        mDataBinding.ivMic.setVisibility(View.INVISIBLE);
        mDataBinding.ivBackgroundPicture.setVisibility(View.INVISIBLE);
        mDataBinding.llChooseSong.setVisibility(View.INVISIBLE);
        mDataBinding.tvNoOnSeat.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(@NonNull AgoraMember data, View view, int position, long id) {
        AgoraMember mMine = RoomManager.Instance(this).getMine();
        if (mMine == null) {
            return;
        }

        if (mMine.getRole() != AgoraMember.Role.Owner) {
            return;
        }

        if (data.getRole() == AgoraMember.Role.Owner) {
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

    private String getLrcText(String fileName) {
        String lrcText = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            lrcText = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lrcText;
    }

    private void onMusicChanged(@NonNull MusicModel music) {
        mDataBinding.llNoSing.setVisibility(View.GONE);
        mDataBinding.rlSing.setVisibility(View.VISIBLE);

        mDataBinding.tvMusicName.setText(music.getName());
        mDataBinding.lrcView.setTotalDuration(1000);
        mDataBinding.lrcView.loadLrc(getLrcText(music.getMusicLrcFile()), null);

        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        if (ObjectsCompat.equals(music.getMusicId(), mUser.getObjectId())) {
            mMusicPlayer.play(mCacheDir + "/" + music.getMusicFile());
        }
    }

    private void onMusicEmpty() {
        mDataBinding.llNoSing.setVisibility(View.VISIBLE);
        mDataBinding.rlSing.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        RoomManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        super.onDestroy();
    }
}
