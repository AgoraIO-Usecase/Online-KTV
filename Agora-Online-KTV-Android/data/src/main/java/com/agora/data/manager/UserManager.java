package com.agora.data.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.agora.data.model.User;
import com.agora.data.provider.IDataRepositroy;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;

import java.util.Random;

import io.reactivex.Observable;

public final class UserManager {
    private Logger.Builder mLogger = XLog.tag("UserManager");

    private static final String TAG = UserManager.class.getSimpleName();

    private static final String TAG_USER = "user";

    private volatile static UserManager instance;

    private MutableLiveData<User> mUserLiveData = new MutableLiveData<>();

    private IDataRepositroy iDataRepositroy;

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

    public void setupDataRepositroy(IDataRepositroy iDataRepositroy) {
        this.iDataRepositroy = iDataRepositroy;
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

//            String userValue = PreferenceManager.getDefaultSharedPreferences(context)
//                    .getString(TAG_USER, null);
//            User mUser = null;
//            if (TextUtils.isEmpty(userValue)) {
//                mUser = new User();
//                mUser.setName(randomName());
//                mUser.setAvatar(randomAvatar());
//            } else {
//                mUser = new Gson().fromJson(userValue, User.class);
//            }
//            user = mUser;
//
//            mLogger.d("loginIn() called");
//            return iDataRepositroy
//                    .login(mUser).doOnError(new Consumer<Throwable>() {
//                        @Override
//                        public void accept(Throwable throwable) throws Exception {
//                            mLogger.e("loginIn faile ", throwable);
//                        }
//                    }).doOnNext(new Consumer<User>() {
//                        @Override
//                        public void accept(User user) throws Exception {
//                            mLogger.i("loginIn success user= %s", user);
                            onLoginIn(user);
//                        }
//                    });
        }
        return Observable.just(user);
    }

    public void onLoginIn(User mUser) {
        mUserLiveData.postValue(mUser);

//        PreferenceManager.getDefaultSharedPreferences(mContext)
//                .edit()
//                .putString(TAG_USER, new Gson().toJson(mUser))
//                .apply();
    }
//
//    public void onLoginOut(User mUser) {
//        mUserLiveData.postValue(null);
//
//        PreferenceManager.getDefaultSharedPreferences(mContext)
//                .edit()
//                .remove(TAG_USER)
//                .apply();
//    }

//    public void update(Context context, User mUser) {
//        mUserLiveData.postValue(mUser);
//
//        PreferenceManager.getDefaultSharedPreferences(context)
//                .edit()
//                .putString(TAG_USER, new Gson().toJson(mUser))
//                .apply();
//    }

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
