package com.agora.data.manager;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.agora.data.DataRepositoryImpl;
import com.agora.data.model.User;

import java.util.Random;

import io.reactivex.disposables.Disposable;

public final class UserManager {

    private volatile static UserManager instance;

    private final MutableLiveData<User> mUserLiveData = new MutableLiveData<>();

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



    public LiveData<User> getUserLiveData() {
        return mUserLiveData;
    }

    public @NonNull User getUser(){
        return mUserLiveData.getValue();
    }

    public boolean alreadyLoggedIn() {
        return UserManager.Instance().getUserLiveData().getValue() != null;
    }

    public Disposable loginIn() {
        return DataRepositoryImpl.getInstance().login(randomId(), randomName()).subscribe(this::onLoginIn);
    }

    public void onLoginIn(User mUser) {
        mUserLiveData.setValue(mUser);
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
