package io.agora.ktv.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import io.agora.ktv.manager.UserManager;
import com.agora.data.model.AgoraRoom;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.BaseActivity;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomHolder;
import io.agora.ktv.databinding.KtvActivityRoomListBinding;
import io.agora.ktv.databinding.KtvItemRoomListBinding;
import io.agora.ktv.repo.DataRepositoryImpl;
import io.agora.ktv.widget.DividerDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Remember, this room list uses the R.mipmap.cover_xxx
 *
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends BaseActivity<KtvActivityRoomListBinding> {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO};

    private BaseRecyclerViewAdapter<KtvItemRoomListBinding, AgoraRoom, RoomHolder> mAdapter;

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {
        List<String> permissionsRefused = new ArrayList<>();
        for (String s : res.keySet()) {
            if (Boolean.TRUE != res.get(s))
                permissionsRefused.add(s);
        }
        if (!permissionsRefused.isEmpty()) {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their decision.
            showPermissionAlertDialog(false);
        } else {
            // Permission is granted. Continue the action or workflow in your app.
            toNextActivity();
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    protected void initView() {

        mAdapter = new BaseRecyclerViewAdapter<>(null, new OnItemClickListener<AgoraRoom>() {
            @Override
            public void onItemClick(@NonNull AgoraRoom data, View view, int position, long id) {
                // 判断网络
                boolean hasActiveNetwork = false;
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    hasActiveNetwork = connectivityManager.getNetworkCapabilities(activeNetwork) != null;
                }else {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected())
                        hasActiveNetwork = true;
                }

                if(hasActiveNetwork) {
                    mAdapter.selectedIndex = position;
                    handlePermissionStuff();
                }else {
                    new AlertDialog.Builder(RoomListActivity.this).setMessage(R.string.ktv_network_unavailable)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                dialog.dismiss();
                                mAdapter.selectedIndex = position;
                                handlePermissionStuff();
                            }).show();
                }
            }
        }, RoomHolder.class);

        mBinding.list.setAdapter(mAdapter);
        mBinding.list.addItemDecoration(new DividerDecoration(2));
        mBinding.swipeRefreshLayout.setOnRefreshListener(this::loadRooms);

    }

    protected void initData() {
        showEmptyStatus();
        login();
    }

    @SuppressLint("CheckResult")
    private void login() {
        mBinding.swipeRefreshLayout.setRefreshing(true);
        if (!UserManager.getInstance().alreadyLoggedIn())
            DataRepositoryImpl.getInstance().login(UserManager.randomId(), UserManager.randomName()).subscribe(user -> {
                UserManager.getInstance().onLoginIn(user);
                loadRooms();
            });
    }

    private void loadRooms() {
        DataRepositoryImpl.getInstance()
                .getRooms()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<AgoraRoom>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<AgoraRoom> agoraRooms) {
                        mAdapter.setDataList(agoraRooms);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mBinding.swipeRefreshLayout.setRefreshing(false);
                        ToastUtil.toastShort(RoomListActivity.this, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        mBinding.swipeRefreshLayout.setRefreshing(false);

                        if (mAdapter.getItemCount() <= 0) {
                            showEmptyStatus();
                        } else {
                            showDataStatus();
                        }
                    }
                });
    }

    private void showEmptyStatus() {
        mBinding.viewEmptyAttRoomList.setVisibility(View.VISIBLE);
    }

    private void showDataStatus() {
        mBinding.viewEmptyAttRoomList.setVisibility(View.GONE);
    }

    /**
     * @see <a href="https://developer.android.com/images/training/permissions/workflow-runtime.svg"/>
     */
    private void handlePermissionStuff() {
        // 小于 M 无需控制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toNextActivity();
            return;
        }

        // 检查权限是否通过
        boolean needRequest = false;
        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (!needRequest) {
            toNextActivity();
            return;
        }

        dismissLoading();

        boolean requestDirectly = true;
        for (String requiredPermission : PERMISSIONS)
            if (shouldShowRequestPermissionRationale(requiredPermission)) {
                requestDirectly = false;
                break;
            }
        // 直接申请
        if (requestDirectly) requestPermissionLauncher.launch(PERMISSIONS);
            // 显示申请理由
        else showPermissionAlertDialog(true);
    }

    private void showPermissionAlertDialog(boolean canRequest) {
        if (canRequest)
            new AlertDialog.Builder(this).setMessage(R.string.ktv_permission_alert).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> requestPermissionLauncher.launch(RoomListActivity.PERMISSIONS))).show();
        else
            new AlertDialog.Builder(this).setMessage(R.string.ktv_permission_refused)
                    .setPositiveButton(android.R.string.ok, null).show();
    }

    private void toNextActivity() {
        AgoraRoom agoraRoom = null;
        try {
            agoraRoom = mAdapter.dataList.get(mAdapter.selectedIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dismissLoading();
        if (agoraRoom != null) {
            Intent intent = new Intent(this, RoomActivity.class);
            intent.putExtra(RoomActivity.TAG_ROOM, agoraRoom);
            startActivity(intent);
        }
    }
}