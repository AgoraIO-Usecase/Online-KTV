package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.ExampleData;
import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.ktv.adapter.ChooseSongViewHolder;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.widget.DividerDecoration;

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
        mBinding.tvSearch.callOnClick();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null, ChooseSongViewHolder.class);
        mBinding.list.setAdapter(mAdapter);
    }

    private void initListener() {
        mBinding.llEmpty.setOnClickListener(this::doLoadMusics);
        mBinding.tvSearch.setOnClickListener(this::doLoadMusics);

        mBinding.swipeRefreshLayout.setOnRefreshListener(() -> {
            String filter = mBinding.searchViewSecSongList.getQuery().toString().trim();
            loadMusics(filter);
        });
    }

    private void loadMusics(String searchKey) {
        new Thread(() -> {
            mAdapter.dataList.clear();
            if(searchKey.isEmpty())
                mAdapter.dataList.addAll(ExampleData.exampleSongs);
            else{
                int size = ExampleData.exampleSongs.size();
                MusicModel music;
                for (int i = 0; i < size; i++) {
                    music = ExampleData.exampleSongs.get(i);
                    if(music.getName().contains(searchKey) || music.getSinger().contains(searchKey))
                        mAdapter.dataList.add(music);
                }
            }
            mBinding.getRoot().post(() -> {
                mAdapter.notifyDataSetChanged();
                mBinding.llEmpty.setVisibility(mAdapter.getItemCount()>0 ? View.GONE : View.VISIBLE);
                mBinding.swipeRefreshLayout.setRefreshing(false);
            });
        }).start();
    }

    private void doLoadMusics(View v){
        mBinding.swipeRefreshLayout.setRefreshing(true);
        loadMusics(mBinding.searchViewSecSongList.getQuery().toString().trim());
    }
}
