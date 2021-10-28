package com.agora.data.manager;

import android.content.Context;
import android.util.Log;

import com.agora.data.model.User;
import com.agora.data.provider.DataRepository;
import com.agora.data.provider.IDataRepository;

import java.util.Random;

import io.reactivex.Observable;

public final class UserManager {

    private volatile static UserManager instance;

    public User mUser;

    private IDataRepository iDataRepository;

    private UserManager() {
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null)
                    instance = new UserManager();
            }
        }
        return instance;
    }

    public void initDataRepository(Context context){
        this.iDataRepository = DataRepository.Instance(context);
    }

    public boolean isLogin() {
        return mUser != null;
    }

    public Observable<User> loginIn() {
        if (mUser == null) {

            User mUser = new User();
            mUser.setName(randomName());
            mUser.setAvatar(randomAvatar());

            return iDataRepository
                    .login(mUser).doOnError(throwable -> Log.e("UserManager",throwable.getMessage()))
                    .doOnNext(user1 -> {
                        Log.d("UserManager","loginIn success user= "+ user1);
                        onLoginIn(user1);
                    });
        } else {
            return Observable.just(mUser);
        }
    }

    public void onLoginIn(User mUser) {
        this.mUser = mUser;
    }


    public static String randomAvatar() {
        return String.valueOf(new Random().nextInt(13) + 1);
    }

    public static String randomName() {
        return "User " + new Random().nextInt(999999);
    }
}
