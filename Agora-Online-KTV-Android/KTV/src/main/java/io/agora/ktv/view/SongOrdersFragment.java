package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongOrdersAdapter;
import com.agora.data.model.MusicModel;
import io.agora.ktv.databinding.KtvFragmentSongOrderListBinding;

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

        loadMusics();
    }

    private void loadMusics() {
        mAdapter.addItem(new MusicModel("Music 1"));
        mAdapter.addItem(new MusicModel("Music 2"));
    }
}
