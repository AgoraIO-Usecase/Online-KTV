package io.agora.ktv.manager;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.MusicModel;
import com.agora.data.provider.DataRepositroy;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.io.File;
import java.util.concurrent.Callable;

import io.agora.ktv.bean.MemberMusicModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * Music Resource
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
public final class MusicResourceManager {
    private Logger.Builder mLogger = XLog.tag("MusicRes");

    private Context mContext;
    private volatile static MusicResourceManager instance;

    private String resourceRoot;

    public static volatile boolean isPreparing = false;

    private MusicResourceManager(Context mContext) {
        this.mContext = mContext;
        resourceRoot = mContext.getExternalCacheDir().getPath();
    }

    public static MusicResourceManager Instance(Context mContext) {
        if (instance == null) {
            synchronized (MusicResourceManager.class) {
                if (instance == null)
                    instance = new MusicResourceManager(mContext.getApplicationContext());
            }
        }
        return instance;
    }

    public Single<MemberMusicModel> prepareMusic(final MemberMusicModel musicModel, boolean onlyLrc) {
        return DataRepositroy.Instance(mContext)
                .getMusic(musicModel.getMusicId())
                .firstOrError()
                .flatMap(new Function<MusicModel, SingleSource<MemberMusicModel>>() {
                    @Override
                    public SingleSource<MemberMusicModel> apply(@NonNull MusicModel model) throws Exception {
                        musicModel.setSong(model.getSong());
                        musicModel.setLrc(model.getLrc());

                        File fileMusic = new File(resourceRoot, musicModel.getMusicId());
                        File fileLrc = new File(resourceRoot, musicModel.getMusicId() + ".lrc");
                        musicModel.setFileMusic(fileMusic);
                        musicModel.setFileLrc(fileLrc);

                        mLogger.i("prepareMusic down %s", musicModel);
                        if (onlyLrc) {
                            return DataRepositroy.Instance(mContext).download(fileLrc, musicModel.getLrc())
                                    .toSingle(new Callable<MemberMusicModel>() {
                                        @Override
                                        public MemberMusicModel call() throws Exception {
                                            return musicModel;
                                        }
                                    });
                        } else {
                            return Completable.mergeArray(
                                    DataRepositroy.Instance(mContext).download(fileMusic, musicModel.getSong()),
                                    DataRepositroy.Instance(mContext).download(fileLrc, musicModel.getLrc()))
                                    .toSingle(new Callable<MemberMusicModel>() {
                                        @Override
                                        public MemberMusicModel call() throws Exception {
                                            return musicModel;
                                        }
                                    });
                        }
                    }
                }).doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        isPreparing = true;
                    }
                }).doOnSuccess(new Consumer<MemberMusicModel>() {
                    @Override
                    public void accept(MemberMusicModel musicModel) throws Exception {
                        isPreparing = false;
                    }
                }).doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        isPreparing = false;
                        mLogger.e("prepareMusic error", throwable);
                    }
                });
    }
}
