package io.agora.baselibrary.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;

import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle3.LifecycleProvider;

import io.agora.baselibrary.R;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 基础
 *
 * @author chenhengfei@agora.io
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected final LifecycleProvider<Lifecycle.Event> mLifecycleProvider = AndroidLifecycle.createLifecycleProvider(this);

    protected Toolbar titleBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            iniBundle(bundle);
        }

        setCusContentView();

        titleBar = findViewById(R.id.titleBar);
        if (titleBar != null) {
            titleBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationOnClickListener();
                }
            });
        }

        iniView();
        iniListener();
        iniData();
    }

    public void navigationOnClickListener() {
        finish();
    }

    protected void setCusContentView() {
        setContentView(getLayoutId());
    }

    protected abstract void iniBundle(@NonNull Bundle bundle);

    protected abstract int getLayoutId();

    protected abstract void iniView();

    protected abstract void iniListener();

    protected abstract void iniData();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (this instanceof EasyPermissions.PermissionCallbacks) {
            EasyPermissions
                    .onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }
}
