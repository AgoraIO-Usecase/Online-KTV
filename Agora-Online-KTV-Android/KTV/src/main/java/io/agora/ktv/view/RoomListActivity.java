package io.agora.ktv.view;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.agora.data.BaseError;
import com.agora.data.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.observer.DataObserver;
import com.agora.data.provider.DataRepositroy;
import com.agora.data.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.BaseActivity;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.MyUtil;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomHolder;
import io.agora.ktv.databinding.KtvActivityRoomListBinding;
import io.agora.ktv.databinding.KtvItemRoomListBinding;
import io.agora.ktv.widget.DividerDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Remember, this room list uses the R.mipmap.cover_xxx
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends BaseActivity<KtvActivityRoomListBinding> {
    public static String key = "ROOM";

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0)
            finish();
        else {
            super.onCreate(savedInstanceState);
            initView();
            initData();
        }
    }

    protected void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, new OnItemClickListener<AgoraRoom>() {
            @Override
            public void onItemClick(@NonNull AgoraRoom data, View view, int position, long id) {
                showLoadingDialog();
                mAdapter.selectedIndex = position;
                handlePermissionStuff();
            }
        }, RoomHolder.class);

        mBinding.list.setAdapter(mAdapter);
        mBinding.list.addItemDecoration(new DividerDecoration(2));
        mBinding.swipeRefreshLayout.setOnRefreshListener(this::loadRooms);
    }

    protected void initData() {
        UserManager.Instance().setupDataRepository(DataRepositroy.Instance(this));

        showEmptyStatus();

        mBinding.swipeRefreshLayout.post(this::login);
    }

    private void login() {
        mBinding.swipeRefreshLayout.setRefreshing(true);
        UserManager.Instance()
                .loginIn()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<User>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

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
                .compose(mLifecycleProvider.bindToLifecycle())
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
            ExampleData.updateCoverImage(ExampleData.exampleBackgrounds.indexOf(agoraRoom.getMVRes()) - 1);
            Intent intent = new Intent(this, RoomActivity.class);
            intent.putExtra(RoomActivity.TAG_ROOM, agoraRoom);
            startActivity(intent);
        }
    }
}