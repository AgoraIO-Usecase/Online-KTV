package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.model.User;

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
}
