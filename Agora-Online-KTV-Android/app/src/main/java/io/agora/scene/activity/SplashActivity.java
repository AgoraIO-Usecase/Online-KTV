package io.agora.scene.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.scene.R;
import io.agora.scene.databinding.ActivitySplashBinding;

/**
 * 闪屏界面
 *
 * @author chenhengfei@agora.io
 */
public class SplashActivity extends DataBindBaseActivity<ActivitySplashBinding> implements View.OnClickListener {

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void iniView() {

    }

    @Override
    protected void iniListener() {
        mDataBinding.tvLivecast.setOnClickListener(this);
        mDataBinding.tvMerry.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.tvLivecast) {
            Intent intent = new Intent(this, io.agora.interactivepodcast.activity.RoomListActivity.class);
            startActivity(intent);
        } else if (v == mDataBinding.tvMerry) {
            Intent intent = new Intent(this, io.agora.marriageinterview.activity.RoomListActivity.class);
            startActivity(intent);
        }
    }
}
