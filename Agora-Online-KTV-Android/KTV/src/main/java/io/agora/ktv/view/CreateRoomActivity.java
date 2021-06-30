package io.agora.ktv.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.sync.SyncManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.Random;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvActivityCreateRoomBinding;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import jp.wasabeef.glide.transformations.BlurTransformation;


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

    AgoraRoom mRoom = new AgoraRoom();

    @Override
    protected void iniData() {
        mRoom.radomCover();

        radomRoomName();

        Glide.with(this)
                .asDrawable()
                .load(mRoom.getCoverRes())
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mDataBinding.root.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
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
        String[] names = getResources().getStringArray(R.array.ktv_roomName);
        int randomIndex = (int) (Math.random() * names.length);
        String name = names[randomIndex];
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
