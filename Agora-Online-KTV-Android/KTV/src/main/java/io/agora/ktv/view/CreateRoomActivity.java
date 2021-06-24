package io.agora.ktv.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.sync.SyncManager;

import java.util.Random;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvActivityCreateRoomBinding;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


/**
 * 创建房间
 *
 * @author chenhengfei@agora.io
 */
public class CreateRoomActivity extends DataBindBaseActivity<KtvActivityCreateRoomBinding> implements View.OnClickListener {
    private static final String TAG_ROOM = "room";

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, CreateRoomActivity.class);
        return intent;
    }

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.ktv_activity_create_room;
    }

    @Override
    protected void iniView() {
    }

    @Override
    protected void iniListener() {
        mDataBinding.ivChangeSong.setOnClickListener(this);
        mDataBinding.btCreate.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
        radomRoomName();
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.ivChangeSong) {
            radomRoomName();
        } else if (v == mDataBinding.btCreate) {
            createRoom();
        }
    }

    private void radomRoomName() {
        String name = "Room " + String.valueOf(new Random().nextInt(999999));
        mDataBinding.tvName.setText(name);
    }

    private void createRoom() {
        User mUser = UserManager.Instance(this).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        String name = mDataBinding.tvName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            return;
        }

        AgoraRoom mRoom = new AgoraRoom();
        mRoom.radomCover();
        mRoom.radomMV();
        mRoom.setChannelName(name);
        mRoom.setUserId(mUser.getObjectId());

        mDataBinding.btCreate.setEnabled(false);
        SyncManager.Instance()
                .creatRoom(mRoom)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AgoraRoom>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull AgoraRoom agoraRoom) {
                        startActivity(RoomActivity.newIntent(CreateRoomActivity.this, agoraRoom));
                        finish();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtile.toastShort(CreateRoomActivity.this, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.btCreate.setEnabled(true);
                    }
                });
    }
}
