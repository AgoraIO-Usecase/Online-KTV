package io.agora.ktv.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.agora.data.manager.UserManager;
import com.agora.data.model.User;

import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class SingleMusicPlayer extends BaseMusicPlayer {

    public SingleMusicPlayer(Context mContext, int role, IMediaPlayer mPlayer) {
        super(mContext, role, mPlayer);
    }

    @Override
    public void prepare(@NonNull MemberMusicModel music) {
        User mUser = UserManager.Instance(mContext).getUserLiveData().getValue();
        if (mUser == null) {
            return;
        }

        onPrepareResource();

        if (ObjectsCompat.equals(music.getUserId(), mUser.getObjectId())) {
            ResourceManager.Instance(mContext)
                    .download(music, false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<MemberMusicModel>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@NonNull MemberMusicModel musicModel) {
                            onResourceReady(musicModel);

                            open(musicModel);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtile.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        } else {
            ResourceManager.Instance(mContext)
                    .download(music, true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<MemberMusicModel>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@NonNull MemberMusicModel musicModel) {
                            onResourceReady(musicModel);

                            onMusicPlaingByListener();
                            playByListener(musicModel);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            ToastUtile.toastShort(mContext, R.string.ktv_lrc_load_fail);
                        }
                    });
        }
    }
}
