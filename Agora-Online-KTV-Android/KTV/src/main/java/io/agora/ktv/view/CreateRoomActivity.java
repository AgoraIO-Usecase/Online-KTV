package io.agora.ktv.view;

import android.annotation.SuppressLint;
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

import io.agora.baselibrary.base.BaseActivity;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvActivityCreateRoomBinding;
import io.agora.ktv.util.BlurTransformation;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * 创建房间
 *
 * @author chenhengfei@agora.io
 */
public class CreateRoomActivity extends BaseActivity<KtvActivityCreateRoomBinding> {

    public static Intent newIntent(Context context) {
        return new Intent(context, CreateRoomActivity.class);
    }

    AgoraRoom mRoom = new AgoraRoom();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView() {
        mBinding.titleBar.setNavigationOnClickListener(v -> finish());

        mBinding.ivChangeSong.setOnClickListener(this::randomRoomName);
        mBinding.btCreate.setOnClickListener(this::createRoom);
    }

    private void initData() {
        mRoom.radomCover();
        mBinding.ivChangeSong.callOnClick();
        Glide.with(this)
                .asDrawable()
                .load(mRoom.getCoverRes())
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(this)))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        CreateRoomActivity.this.getWindow().getDecorView().setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }


    private void randomRoomName(View v) {
        String[] names = getResources().getStringArray(R.array.ktv_roomName);
        int randomIndex = (int) (Math.random() * names.length);
//        String name = "Room " + String.valueOf(new Random().nextInt(999999));
        String name = names[randomIndex];
        mBinding.tvName.setText(name);
    }

    @SuppressLint("CheckResult")
    private void createRoom(View v) {
        User mUser = UserManager.getInstance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        String name = mBinding.tvName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            return;
        }

        mRoom.radomMV();
        mRoom.setChannelName(name);
        mRoom.setUserId(mUser.getObjectId());

        SyncManager.Instance()
                .creatRoom(mRoom)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(agoraRoom -> {
                    startActivity(RoomActivity.newIntent(CreateRoomActivity.this, agoraRoom));
                    finish();
                });
    }
}
