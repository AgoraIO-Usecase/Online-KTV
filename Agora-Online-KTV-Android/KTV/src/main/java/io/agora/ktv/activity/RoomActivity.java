package io.agora.ktv.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.agora.data.SimpleRoomEventCallback2;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.sync.RoomManager;

import java.util.ArrayList;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomSpeakerAdapter;
import io.agora.ktv.databinding.KtvActivityRoomBinding;
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

    private AgoraRoom.MusicStatus mMusicStatus = AgoraRoom.MusicStatus.IDLE;

    private SimpleRoomEventCallback2 mRoomEventCallback = new SimpleRoomEventCallback2() {
        @Override
        public void onRoleChanged(boolean isMine, @NonNull AgoraMember member) {
            super.onRoleChanged(isMine, member);

            if (member.getRole() == AgoraMember.Role.Speaker) {
                mRoomSpeakerAdapter.addItem(member);
            } else {
                mRoomSpeakerAdapter.deleteItem(member);
            }
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
    }

    @Override
    protected void iniData() {
        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            ToastUtile.toastShort(this, "please login in");
            finish();
            return;
        }

        showOnSeatStatus();
        showMusicIDLEStatus();

        AgoraRoom mRoom = getIntent().getExtras().getParcelable(TAG_ROOM);

        mDataBinding.tvName.setText(mRoom.getName());

        AgoraMember owner = new AgoraMember();
        owner.setRole(AgoraMember.Role.Owner);
        mRoomSpeakerAdapter.addItem(owner);

        RoomManager.Instance(this)
                .joinRoom(mRoom)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<AgoraMember>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull AgoraMember agoraMember) {
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

    }

    private void showMusicMenuDialog() {
        new MusicSettingDialog().show(getSupportFragmentManager());
    }

    private void toggleStart() {

    }

    private void changeRole() {

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

        RoomManager.Instance(this)
                .changeRole(mMine, AgoraMember.Role.Speaker.getValue())
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

    @Override
    protected void onDestroy() {
        RoomManager.Instance(this).removeRoomEventCallback(mRoomEventCallback);
        super.onDestroy();
    }
}
