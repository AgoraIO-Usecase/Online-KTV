package io.agora.ktv.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


/**
 * 闪屏界面
 *
 * @author chenhengfei@agora.io
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
        }else{
            super.onCreate(savedInstanceState);
            initData();
        }
    }

    private void initData() {
        Intent intent = getIntent();
        if (!this.isTaskRoot() && intent != null) {
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                finish();
                return;
            }
        }

        startActivity(new Intent(this, RoomListActivity.class));
        finish();
    }
}
