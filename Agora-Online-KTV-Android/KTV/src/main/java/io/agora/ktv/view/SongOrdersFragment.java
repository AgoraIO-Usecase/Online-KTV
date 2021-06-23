package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongOrdersAdapter;
import io.agora.ktv.databinding.KtvFragmentSongOrderListBinding;
import io.agora.ktv.manager.RoomManager;

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
        mAdapter = new SongOrdersAdapter(new ArrayList<>(), this);
        mDataBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        mDataBinding.list.setAdapter(mAdapter);

        mDataBinding.swipeRefreshLayout.setEnabled(false);
        mDataBinding.llEmpty.setVisibility(View.GONE);
    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        loadMusics();
    }

    private void loadMusics() {
        mAdapter.setDatas(RoomManager.Instance(requireContext()).getMusics());
    }
}
