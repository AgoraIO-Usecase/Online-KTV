package io.agora.ktv.view;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.DataRepositoryImpl;
import com.agora.data.model.MusicModel;

import java.util.List;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.util.KTVUtil;
import io.agora.ktv.adapter.ChooseSongViewHolder;
import io.agora.ktv.databinding.KtvFragmentSongListBinding;
import io.agora.ktv.databinding.KtvItemChooseSongListBinding;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
        DataRepositoryImpl.getInstance().getMusics(searchKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<MusicModel>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(@NonNull List<MusicModel> musicModels) {
                mAdapter.setDataList(musicModels);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                KTVUtil.logE(e.getMessage());
            }

            @Override
            public void onComplete() {
                mBinding.llEmpty.setVisibility(mAdapter.getItemCount()>0 ? View.GONE : View.VISIBLE);
                mBinding.swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void doLoadMusics(View v){
        mBinding.swipeRefreshLayout.setRefreshing(true);
        loadMusics(mBinding.searchViewSecSongList.getQuery().toString().trim());
    }
}
