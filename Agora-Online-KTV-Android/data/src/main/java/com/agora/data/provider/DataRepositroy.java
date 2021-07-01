package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import java.io.File;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class DataRepositroy implements IDataRepositroy {
    private volatile static DataRepositroy instance;

    private Context mContext;

    private final IDataRepositroy mIDataRepositroy;

    private DataRepositroy(Context context) {
        mContext = context.getApplicationContext();

        mIDataRepositroy = new DataRepositroyImpl();
    }

    public static synchronized DataRepositroy Instance(Context context) {
        if (instance == null) {
            synchronized (DataRepositroy.class) {
                if (instance == null)
                    instance = new DataRepositroy(context);
            }
        }
        return instance;
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return mIDataRepositroy.login(user);
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return mIDataRepositroy.update(user);
    }

    @Override
    public Observable<User> getUser(@NonNull String userId) {
        return mIDataRepositroy.getUser(userId);
    }

    @Override
    public Observable<List<MusicModel>> getMusics() {
        return mIDataRepositroy.getMusics();
    }

    @Override
    public Observable<MusicModel> getMusic(@NonNull String musicId) {
        return mIDataRepositroy.getMusic(musicId);
    }

    @Override
    public Completable download(@NonNull File file, @NonNull String url) {
        return mIDataRepositroy.download(file, url);
    }
}
