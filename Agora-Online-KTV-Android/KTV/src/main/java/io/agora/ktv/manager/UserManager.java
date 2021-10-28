package io.agora.ktv.manager;

import com.agora.data.model.User;

import java.util.Random;

public final class UserManager {

    private volatile static UserManager instance;

    public User mUser = null;

    private UserManager() { }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null)
                    instance = new UserManager();
            }
        }
        return instance;
    }

    public boolean alreadyLoggedIn() {
        return mUser != null;
    }

    public void onLoginIn(User mUser) {
        this.mUser = mUser;
    }

    public static int randomId() {
        return new Random().nextInt(Integer.MAX_VALUE);
    }
    public static String randomAvatar() {
        return String.valueOf(new Random().nextInt(14));
    }
    public static String randomName() {
        return "User " + new Random().nextInt(999999);
    }
}
