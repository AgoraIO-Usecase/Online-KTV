package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.agora.data.provider.AgoraObject;
import com.agora.data.provider.DataRepository;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import java.util.List;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.adapter.ChooseSongViewHolder;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.view.dialog.RoomChooseSongDialog;
import io.agora.ktv.widget.DividerDecoration;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 歌单列表
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/15
 */
public class SongsFragment extends BaseFragment<KtvFragmentSongListBinding> implements OnItemClickListener<MusicModel> {
    private BaseRecyclerViewAdapter<KtvItemChooseSongListBinding, MusicModel, ChooseSongViewHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        initListener();
        mBinding.tvSearch.callOnClick();
    }

    private void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(null,this, ChooseSongViewHolder.class);
        mBinding.recyclerViewFgSong.setAdapter(mAdapter);
        mBinding.recyclerViewFgSong.addItemDecoration(new DividerDecoration(1,20,8));
    }

    private void initListener() {
        mBinding.llEmpty.setOnClickListener(this::doLoadMusics);
        mBinding.tvSearch.setOnClickListener(this::doLoadMusics);

        mBinding.refreshLayoutFgSong.setOnRefreshListener(() -> {
            String filter = mBinding.searchViewSecSongList.getQuery().toString().trim();
            loadMusics(filter);
        });
    }

    private void loadMusics(String searchKey) {
        DataRepository.Instance(requireContext())
                .getMusics(searchKey)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<MusicModel>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<MusicModel> musicModels) {
                        onLoadMusics(musicModels);
                        mBinding.refreshLayoutFgSong.setRefreshing(false);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ToastUtil.toastShort(requireContext(), e.getMessage());
                        mBinding.refreshLayoutFgSong.setRefreshing(false);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void onLoadMusics(List<MusicModel> list) {
        if (list.isEmpty()) {
            mBinding.llEmpty.setVisibility(View.VISIBLE);
        } else {
            mBinding.llEmpty.setVisibility(View.GONE);
        }
        mAdapter.setDataList(list);
    }

    @Override
    public void onItemClick(@NonNull MusicModel data, View view, int position, long viewType) {

        if(!(view instanceof Button)) return;

        AgoraRoom mRoom = RoomManager.Instance(requireContext()).getRoom();
        if (mRoom == null) {
            return;
        }

        User mUser = UserManager.getInstance().getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        showLoading();
        MemberMusicModel model = new MemberMusicModel(data);
        model.setRoomId(mRoom);
        model.setUserId(mUser.getObjectId());
        if(RoomChooseSongDialog.isChorus)
            model.setType(MemberMusicModel.SingType.Chorus);
        else
            model.setType(MemberMusicModel.SingType.Single);

        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .collection(MemberMusicModel.TABLE_NAME)
                .add(model.toHashMap(), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {
                        view.setEnabled(true);
                        MemberMusicModel musicModel = result.toObject(MemberMusicModel.class);
                        musicModel.setId(result.getId());

                        RoomManager.Instance(requireContext()).onMusicAdd(musicModel);
                        mAdapter.notifyItemChanged(position);
                        dismissLoading();
                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        ToastUtil.toastShort(requireContext(), exception.getMessage());
                        dismissLoading();
                    }
                });
    }

    private void doLoadMusics(View v){
        mBinding.refreshLayoutFgSong.setRefreshing(true);
        loadMusics(mBinding.searchViewSecSongList.getQuery().toString().trim());
    }
}
