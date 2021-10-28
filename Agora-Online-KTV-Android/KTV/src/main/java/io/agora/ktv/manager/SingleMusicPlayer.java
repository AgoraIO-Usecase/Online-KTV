package io.agora.ktv.manager;

import android.content.Context;

import androidx.annotation.NonNull;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.mediaplayer.IMediaPlayer;
import io.agora.rtc2.Constants;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 仅
 */
public class SingleMusicPlayer extends BaseMusicPlayer {

    public SingleMusicPlayer(Context mContext, IMediaPlayer mPlayer) {
        super(mContext, mPlayer);
        RoomManager.getInstance().getRtcEngine().setAudioProfile(Constants.AUDIO_SCENARIO_DEFAULT);
    }

//    @Override
//    public void switchRole(int role) {
//        mLogger.d("switchRole() called with: role = [%s]", role);
//        mRole = role;
//
//        ChannelMediaOptions options = new ChannelMediaOptions();
//        options.publishMediaPlayerId = mPlayer.getMediaPlayerId();
//        options.clientRoleType = role;
//        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
//            options.publishAudioTrack = true;
//            options.publishMediaPlayerAudioTrack = true;
//        } else {
//            options.publishAudioTrack = false;
//            options.publishMediaPlayerAudioTrack = false;
//        }
//        RoomManager.getInstance().getRtcEngine().updateChannelMediaOptions(options);
//    }

    @Override
    public void prepare(@NonNull MemberMusicModel music) {

        onPrepareResource();

        boolean singMyself = RoomManager.getInstance().isMainSinger();
        ResourceManager.Instance(mContext)
                .download(music, !singMyself)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MemberMusicModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull MemberMusicModel musicModel) {
                        onResourceReady(musicModel);

                        if (singMyself) {
                            open();
                        } else {
                            onMusicPlayingByListener();
                            playByListener();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        KTVUtil.logD(e.getMessage());
                        ToastUtil.toastShort(mContext, R.string.ktv_lrc_load_fail);
                    }
                });
    }
}
