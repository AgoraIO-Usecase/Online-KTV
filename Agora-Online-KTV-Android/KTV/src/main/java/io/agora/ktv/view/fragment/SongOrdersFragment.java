package io.agora.ktv.view.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import java.util.List;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.adapter.ChosenSongViewHolder;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvFragmentSongOrderListBinding;
import io.agora.ktv.databinding.KtvItemChoosedSongListBinding;
import io.agora.ktv.manager.RoomManager;

/**
 * 已点歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongOrdersFragment extends BaseFragment<KtvFragmentSongOrderListBinding> {


    private BaseRecyclerViewAdapter<KtvItemChoosedSongListBinding, MemberMusicModel, ChosenSongViewHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(RoomManager.Instance(requireContext()).getMusics(), ChosenSongViewHolder.class);
        mBinding.list.setAdapter(mAdapter);

        RoomManager.Instance(requireContext()).getLiveDataMusics().observe(getViewLifecycleOwner(), memberMusicModels -> mAdapter.setDataList(memberMusicModels));
    }
}
