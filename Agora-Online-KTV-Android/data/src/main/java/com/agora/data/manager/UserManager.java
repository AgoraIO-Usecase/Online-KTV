package com.agora.data.manager;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.agora.data.model.User;
import com.agora.data.provider.DataRepository;
import com.agora.data.provider.IDataRepository;

import java.util.Random;

import io.agora.baselibrary.util.KTVUtil;
import io.reactivex.Observable;

public final class UserManager {

    private volatile static UserManager instance;

    private final MutableLiveData<User> mUserLiveData = new MutableLiveData<>();

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

    public MutableLiveData<User> getUserLiveData() {
        return mUserLiveData;
    }

    public void initDataRepository(Context context){
        this.iDataRepository = DataRepository.Instance(context);
    }

    public boolean isLogin() {
        return UserManager.getInstance().getUserLiveData().getValue() != null;
    }

    public Observable<User> loginIn() {
        User user = UserManager.getInstance().getUserLiveData().getValue();
        if (user == null) {

            User mUser = new User();
            mUser.setName(randomName());
            mUser.setAvatar(randomAvatar());

            return iDataRepository
                    .login(mUser).doOnError(throwable -> KTVUtil.logE(throwable.getMessage())).doOnNext(user1 -> {
                        KTVUtil.logD("loginIn success user= "+ user1);
                        onLoginIn(user1);
                    });
        } else {
            return Observable.just(user);
        }
    }

    public void onLoginIn(User mUser) {
        mUserLiveData.postValue(mUser);
    }


    public static String randomAvatar() {
        return String.valueOf(new Random().nextInt(13) + 1);
    }

    public static String randomName() {
        return "User " + new Random().nextInt(999999);
    }
}
