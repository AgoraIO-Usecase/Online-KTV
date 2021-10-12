package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.agora.data.ExampleData;
import com.agora.data.model.MusicModel;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.adapter.ChooseSongViewHolder;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;

/**
 * 歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongsFragment extends BaseFragment<KtvFragmentSongListBinding> {
    private BaseRecyclerViewAdapter<KtvItemChooseSongListBinding, MusicModel, ChooseSongViewHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        initListener();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, ChooseSongViewHolder.class);
        mBinding.list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mBinding.list.setAdapter(mAdapter);

    }

    private void initListener() {
        mBinding.llEmpty.setOnClickListener(v-> {
            mBinding.swipeRefreshLayout.setRefreshing(true);
            loadMusics("");
        });
        mBinding.swipeRefreshLayout.setOnRefreshListener(() -> {
            String filter = mBinding.searchViewSecSongList.getQuery().toString().trim();
            loadMusics(filter);
        });
    }

    private void loadMusics(String searchKey) {
        mAdapter.dataList.clear();
        if(searchKey.isEmpty())
            mAdapter.dataList.addAll(ExampleData.exampleSongs);
        else{
            int size = ExampleData.exampleSongs.size();
            MusicModel music;
            for (int i = 0; i < size; i++) {
                music = ExampleData.exampleSongs.get(i);
                if(music.getName().contains(searchKey))
                    mAdapter.dataList.add(music);
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
