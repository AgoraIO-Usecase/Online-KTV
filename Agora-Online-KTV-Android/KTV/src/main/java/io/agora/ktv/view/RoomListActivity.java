package io.agora.ktv.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.agora.data.BaseError;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.observer.DataObserver;
import com.agora.data.sync.SyncManager;

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
import io.agora.ktv.widget.DividerDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends BaseActivity<KtvActivityRoomListBinding> {

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private BaseRecyclerViewAdapter<KtvItemRoomListBinding, AgoraRoom, RoomHolder> mAdapter;

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {

        List<String> permissionsRefused = new ArrayList<>();

        // 检查是否所有权限通过
        for (String s : res.keySet()) {
            if (Boolean.TRUE != res.get(s))
                permissionsRefused.add(s);
        }

        if (!permissionsRefused.isEmpty()) {
            showPermissionAlertDialog(false);
        } else {
            toRoomActivity();
        }
    });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();


        UserManager.getInstance().initDataRepository(this);
        showEmptyStatus();
        mBinding.swipeRefreshLayout.setRefreshing(true);
        refreshData();
    }

    private void initView() {

        mAdapter = new BaseRecyclerViewAdapter<>(null, new OnItemClickListener<AgoraRoom>() {
            @Override
            public void onItemClick(@NonNull AgoraRoom data, View view, int position, long viewType) {
                mAdapter.selectedIndex = position;
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

                // 网络判断
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    handlePermissionStuff();
                } else {
                    showNetworkErrorDialog();
                }
            }
        }, RoomHolder.class);

        mBinding.list.setAdapter(mAdapter);
        mBinding.list.addItemDecoration(new DividerDecoration(2));
        mBinding.swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
            mBinding.list.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mBinding.btnCreateAttRoomList.setOnClickListener(this::gotoCreateRoom);
    }

    private void refreshData() {
        if (!UserManager.getInstance().isLogin()) {
            login();
        } else {
            loadRooms();
        }
    }

    private void login() {
        UserManager.getInstance()
                .loginIn()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DataObserver<User>() {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        e.printStackTrace();
                        ToastUtil.toastLong(RoomListActivity.this, e.getMessage());
                        mBinding.swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void handleSuccess(@NonNull User user) {
                        loadRooms();
                    }
                });
    }

    private void loadRooms() {
        SyncManager.Instance()
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
                        e.printStackTrace();
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

    private void gotoCreateRoom(View v) {
        Intent intent = CreateRoomActivity.newIntent(RoomListActivity.this);
        startActivity(intent);
    }

    /**
     * @see <a href="https://developer.android.com/images/training/permissions/workflow-runtime.svg"/>
     */
    private void handlePermissionStuff() {
        // 小于 M 无需控制
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toRoomActivity();
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
            toRoomActivity();
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

    private void showNetworkErrorDialog() {
        new AlertDialog.Builder(RoomListActivity.this).setMessage(R.string.ktv_network_unavailable)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    handlePermissionStuff();
                }).show();
    }

    private void toRoomActivity() {
        AgoraRoom itemRoom = mAdapter.getItemData(mAdapter.selectedIndex);
        if (itemRoom != null) {
            Intent intent = RoomActivity.newIntent(RoomListActivity.this, itemRoom);
            startActivity(intent);
        }
    }
}
