package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import java.io.File;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class DataRepository implements IDataRepository {
    private volatile static DataRepository instance;

    private Context mContext;

    private final IDataRepository mIDataRepository;

    private DataRepository(Context context) {
        mContext = context.getApplicationContext();

        mIDataRepository = new DataRepositoryImpl();
    }

    public static synchronized DataRepository Instance(Context context) {
        if (instance == null) {
            synchronized (DataRepository.class) {
                if (instance == null)
                    instance = new DataRepository(context);
            }
        }
        return instance;
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return mIDataRepository.login(user);
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return mIDataRepository.update(user);
    }

    @Override
    public Observable<User> getUser(@NonNull String userId) {
        return mIDataRepository.getUser(userId);
    }

    @Override
    public Observable<List<MusicModel>> getMusics(@Nullable String searchKey) {
        return mIDataRepository.getMusics(searchKey);
    }

    @Override
    public Observable<MusicModel> getMusic(@NonNull String musicId) {
        return mIDataRepository.getMusic(musicId);
    }

    @Override
    public Completable download(@NonNull File file, @NonNull String url) {
        return mIDataRepository.download(file, url);
    }
}
