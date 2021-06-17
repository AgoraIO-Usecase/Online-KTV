package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.agora.ktv.bean.MusicModel;
import io.agora.ktv.manager.RoomManager;

import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongOrdersAdapter;
import io.agora.ktv.databinding.KtvFragmentSongOrderListBinding;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 已点歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongOrdersFragment extends DataBindBaseFragment<KtvFragmentSongOrderListBinding> {

    public static SongOrdersFragment newInstance() {
        SongOrdersFragment mFragment = new SongOrdersFragment();
        return mFragment;
    }

    private SongOrdersAdapter mAdapter;

    @Override
    public void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_fragment_song_order_list;
    }

    @Override
    public void iniView(View view) {

    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        mAdapter = new SongOrdersAdapter(new ArrayList<>(), this);
        mDataBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        mDataBinding.list.setAdapter(mAdapter);

        mDataBinding.swipeRefreshLayout.setEnabled(false);
        mDataBinding.llEmpty.setVisibility(View.GONE);

        loadMusics();
    }

    private void loadMusics() {
        RoomManager.Instance(requireContext())
                .getMusicsFromRemote()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new SingleObserver<List<MusicModel>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull List<MusicModel> musicModels) {
                        mAdapter.setDatas(musicModels);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }
}
