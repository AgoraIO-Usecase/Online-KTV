package io.agora.ktv.manager;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.DataRepositoryImpl;
import com.agora.data.ExampleData;
import com.agora.data.model.MusicModel;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.ktv.bean.MemberMusicModel;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.observable.ObservableEmpty;

/**
 * Music Resource
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/06/01
 */
public final class ResourceManager {
    private final Logger.Builder mLogger = XLog.tag("MusicRes");

    private volatile static ResourceManager instance;
    private final String resourceRoot;

    private ResourceManager(Context mContext) {
        resourceRoot = mContext.getExternalCacheDir().getPath();
    }

    public static ResourceManager Instance(Context mContext) {
        if (instance == null) {
            synchronized (ResourceManager.class) {
                if (instance == null)
                    instance = new ResourceManager(mContext.getApplicationContext());
            }
        }
        return instance;
    }

    public Single<MemberMusicModel> download(final MemberMusicModel musicModel, boolean onlyLrc) {
        return Single.create((SingleOnSubscribe<MusicModel>) emitter ->
                DataRepositoryImpl.getInstance().getMusic(musicModel.getMusicId()).subscribe(new Observer<MusicModel>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                KTVUtil.logD("onSubscribe");
            }

            @Override
            public void onNext(@NonNull MusicModel musicModel) {
                KTVUtil.logD("onNext");
                emitter.onSuccess(musicModel);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                KTVUtil.logD(e.getMessage());
                emitter.onError(e);
            }

            @Override
            public void onComplete() {
                KTVUtil.logD("onComplete");
            }
        })).flatMap((Function<MusicModel, SingleSource<MemberMusicModel>>) model -> {
            musicModel.setSong(model.getSong());
            musicModel.setLrc(model.getLrc());

            File fileMusic = new File(resourceRoot, musicModel.getMusicId());
            File fileLrc;

            if (model.getLrc().endsWith("zip")) {
                fileLrc = new File(resourceRoot, musicModel.getMusicId() + ".zip");
            } else if (model.getLrc().endsWith("xml")) {
                fileLrc = new File(resourceRoot, musicModel.getMusicId() + ".xml");
            } else if (model.getLrc().endsWith("lrc")) {
                fileLrc = new File(resourceRoot, musicModel.getMusicId() + ".lrc");
            } else {
                return Single.error(new Throwable("未知歌词格式"));
            }

            musicModel.setFileMusic(fileMusic);
            musicModel.setFileLrc(fileLrc);

            mLogger.i("prepareMusic down %s", musicModel);
            if (onlyLrc) {
                Completable mCompletable = DataRepositoryImpl.getInstance().download(fileLrc, musicModel.getLrc());
                if (model.getLrc().endsWith("zip")) {
                    mCompletable = mCompletable.andThen(Completable.create(new CompletableOnSubscribe() {
                        @Override
                        public void subscribe(@NonNull CompletableEmitter emitter) throws Exception {
                            File fileLrcNew = new File(resourceRoot, musicModel.getMusicId() + ".xml");
                            unzipLrc(fileLrc, fileLrcNew);
                            musicModel.setFileLrc(fileLrcNew);
                            emitter.onComplete();
                        }
                    }));
                }

                return mCompletable.andThen(Single.just(musicModel));
            } else {
                Completable mCompletable = DataRepositoryImpl.getInstance().download(fileLrc, musicModel.getLrc());
                if (model.getLrc().endsWith("zip")) {
                    mCompletable = mCompletable.andThen(Completable.create(new CompletableOnSubscribe() {
                        @Override
                        public void subscribe(@NonNull CompletableEmitter emitter) throws Exception {
                            File fileLrcNew = new File(resourceRoot, musicModel.getMusicId() + ".xml");
                            unzipLrc(fileLrc, fileLrcNew);
                            musicModel.setFileLrc(fileLrcNew);
                            emitter.onComplete();
                        }
                    }));
                }

                return Completable.mergeArray(
                        DataRepositoryImpl.getInstance().download(fileMusic, musicModel.getSong()),
                        mCompletable)
                        .toSingle(new Callable<MemberMusicModel>() {
                            @Override
                            public MemberMusicModel call() throws Exception {
                                return musicModel;
                            }
                        });
            }
        });
    }

    private void unzipLrc(File src, File des) throws Exception {
        mLogger.i("prepareMusic unzipLrc %s", des);

        ZipInputStream inZip = new ZipInputStream(new FileInputStream(src));
        ZipEntry zipEntry;
        String szName = null;

        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                continue;
            } else {
                des.createNewFile();
                FileOutputStream out = new FileOutputStream(des);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = inZip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inZip.close();
    }
}
