package io.agora.baselibrary.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle3.LifecycleProvider;

import io.agora.baselibrary.R;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks;

/**
 * 基础类
 *
 * @author Aslan
 * @date 2018/4/11
 */
public abstract class BaseFragment extends Fragment {

    protected final LifecycleProvider<Lifecycle.Event> provider =
            AndroidLifecycle.createLifecycleProvider(this);

    protected CustomToolbar titleBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            iniBundle(bundle);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = setCusContentView(inflater, container);

        titleBar = view.findViewById(R.id.titleBar);
        if (titleBar != null) {
            titleBar.setNavigationOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationOnClickListener();
                }
            });
        }
        return view;
    }

    public abstract void iniBundle(@NonNull Bundle bundle);

    public View setCusContentView(@NonNull LayoutInflater inflater,
                                  @Nullable ViewGroup container) {
        return LayoutInflater.from(getContext()).inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        iniView(view);
        iniListener();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        iniData();
    }

    public abstract int getLayoutId();

    public abstract void iniView(View view);

    public abstract void iniListener();

    public abstract void iniData();

    public void setTitle(@StringRes int resid) {
        titleBar.setTitle(resid);
    }

    public void setTitle(CharSequence text) {
        titleBar.setTitle(text);
    }

    public void navigationOnClickListener() {
        ((BaseActivity) getActivity()).finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (this instanceof PermissionCallbacks) {
            EasyPermissions
                    .onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }
}