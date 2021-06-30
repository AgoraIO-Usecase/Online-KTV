package io.agora.ktv.view;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.data.BaseError;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.User;
import com.agora.data.observer.DataObserver;
import com.agora.data.provider.AgoraObject;
import com.agora.data.provider.DataRepositroy;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import java.util.List;

import io.agora.baselibrary.base.DataBindBaseActivity;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.adapter.RoomListAdapter;
import io.agora.ktv.databinding.KtvActivityRoomListBinding;
import io.agora.ktv.widget.SpaceItemDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 房间列表
 *
 * @author chenhengfei@agora.io
 */
public class RoomListActivity extends DataBindBaseActivity<KtvActivityRoomListBinding> implements View.OnClickListener,
        OnItemClickListener<AgoraRoom>, EasyPermissions.PermissionCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private static final int TAG_PERMISSTION_REQUESTCODE = 1000;
    private static final String[] PERMISSTION = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    private RoomListAdapter mAdapter;

    @Override
    protected void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.ktv_activity_room_list;
    }

    @Override
    protected void iniView() {
        mAdapter = new RoomListAdapter(null, this);
        mDataBinding.list.setLayoutManager(new GridLayoutManager(this, 2));
        mDataBinding.list.setAdapter(mAdapter);
        mDataBinding.list.addItemDecoration(new SpaceItemDecoration(this));
    }

    @Override
    protected void iniListener() {
        mDataBinding.swipeRefreshLayout.setOnRefreshListener(this);
        mDataBinding.btCrateRoom.setOnClickListener(this);
    }

    @Override
    protected void iniData() {
        UserManager.Instance(this).setupDataRepositroy(DataRepositroy.Instance(this));

        showEmptyStatus();
        login();
    }

    private void login() {
        UserManager.Instance(this)
                .loginIn()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataObserver<User>(this) {
                    @Override
                    public void handleError(@NonNull BaseError e) {

                    }

                    @Override
                    public void handleSuccess(@NonNull User user) {
                        mDataBinding.swipeRefreshLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                mDataBinding.swipeRefreshLayout.setRefreshing(true);
                                loadRooms();
                            }
                        });
                    }
                });
    }

    private void loadRooms() {
        mAdapter.clear();

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
                        mAdapter.setDatas(agoraRooms);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);
                        ToastUtile.toastShort(RoomListActivity.this, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.swipeRefreshLayout.setRefreshing(false);

                        if (mAdapter.getItemCount() <= 0) {
                            showEmptyStatus();
                        } else {
                            showDataStatus();
                        }
                    }
                });
    }

    private void showEmptyStatus() {
        mDataBinding.llEmpty.setVisibility(View.VISIBLE);
    }

    private void showDataStatus() {
        mDataBinding.llEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (!UserManager.Instance(this).isLogin()) {
            login();
            return;
        }

        if (v.getId() == R.id.btCrateRoom) {
            gotoCreateRoom();
        }
    }

    private void gotoCreateRoom() {
        if (EasyPermissions.hasPermissions(this, PERMISSTION)) {
            Intent intent = CreateRoomActivity.newIntent(RoomListActivity.this);
            startActivity(intent);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.ktv_error_permisstion),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onItemClick(@NonNull AgoraRoom data, View view, int position, long id) {
        if (!EasyPermissions.hasPermissions(this, PERMISSTION)) {
            EasyPermissions.requestPermissions(this, getString(R.string.ktv_error_permisstion),
                    TAG_PERMISSTION_REQUESTCODE, PERMISSTION);
            return;
        }

        mDataBinding.list.setEnabled(false);
        SyncManager.Instance()
                .getRoom(data.getId())
                .get(new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject LCObject) {
                        AgoraRoom mRoom = LCObject.toObject(AgoraRoom.class);
                        mRoom.setId(LCObject.getId());

                        Intent intent = RoomActivity.newIntent(RoomListActivity.this, mRoom);
                        startActivity(intent);
                        mDataBinding.list.setEnabled(true);
                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        mAdapter.deleteItem(position);
                        mDataBinding.list.setEnabled(true);
                        ToastUtile.toastShort(RoomListActivity.this, "房间不存在");
                    }
                });
    }

    @Override
    public void onRefresh() {
        loadRooms();
    }
}
