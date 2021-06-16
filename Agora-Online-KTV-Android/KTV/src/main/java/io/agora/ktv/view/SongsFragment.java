package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.RoomManager;
import com.agora.data.sync.SyncManager;

import java.util.ArrayList;

import io.agora.baselibrary.base.DataBindBaseFragment;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.ktv.R;
import io.agora.ktv.adapter.SongsAdapter;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;

/**
 * 歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongsFragment extends DataBindBaseFragment<KtvFragmentSongListBinding> implements OnItemClickListener<MusicModel> {

    public static SongsFragment newInstance() {
        SongsFragment mFragment = new SongsFragment();
        return mFragment;
    }

    private SongsAdapter mAdapter;

    @Override
    public void iniBundle(@NonNull Bundle bundle) {

    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_fragment_song_list;
    }

    @Override
    public void iniView(View view) {

    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        mAdapter = new SongsAdapter(new ArrayList<>(), this);
        mDataBinding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        mDataBinding.list.setAdapter(mAdapter);

        loadMusics();
    }

    private void loadMusics() {
        onLoadMusics();
    }

    private void onLoadMusics() {
        mAdapter.addItem(new MusicModel("Music 1"));
        mAdapter.addItem(new MusicModel("Music 2"));
    }

    @Override
    public void onItemClick(@NonNull MusicModel data, View view, int position, long id) {
        AgoraRoom mRoom = RoomManager.Instance(requireContext()).getRoom();
        if (mRoom == null) {
            return;
        }

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MusicModel.TABLE_NAME)
                .add(data.toHashMap(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {

                    }

                    @Override
                    public void onFail(AgoraException exception) {

                    }
                });
    }
}
