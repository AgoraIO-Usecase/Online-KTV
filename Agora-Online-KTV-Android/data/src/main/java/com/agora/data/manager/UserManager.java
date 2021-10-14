package com.agora.data.manager;

import androidx.lifecycle.MutableLiveData;

import com.agora.data.model.User;
import com.agora.data.provider.IDataRepository;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.Random;

import io.reactivex.Observable;

public final class UserManager {
    private Logger.Builder mLogger = XLog.tag("UserManager");

    private static final String TAG = UserManager.class.getSimpleName();

    private static final String TAG_USER = "user";

    private volatile static UserManager instance;

    private final MutableLiveData<User> mUserLiveData = new MutableLiveData<>();

    private IDataRepository iDataRepository;

    private UserManager() { }

    public static UserManager Instance() {
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

    public void setupDataRepository(IDataRepository iDataRepository) {
        this.iDataRepository = iDataRepository;
    }

    public boolean isLogin() {
        return UserManager.Instance().getUserLiveData().getValue() != null;
    }

    public Observable<User> loginIn() {
        User user = UserManager.Instance().getUserLiveData().getValue();
        if (user == null) {
            user = new User();
            user.setObjectId(randomId());
            user.setAvatar(randomAvatar());
            user.setName(randomName());
        }
        onLoginIn(user);
        return Observable.just(user);
    }

    public void onLoginIn(User mUser) {
        mUserLiveData.postValue(mUser);
    }

    public static String randomId() {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }
    public static String randomAvatar() {
        return String.valueOf(new Random().nextInt(13));
    }

    public static String randomName() {
        return "User " + new Random().nextInt(999999);
    }
}
