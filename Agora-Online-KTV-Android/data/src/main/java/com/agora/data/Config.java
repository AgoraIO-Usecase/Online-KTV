package com.agora.data;

public class Config {

    public static final String USER_OBJECTID = "objectId";
    public static final String USER_NAME = "name";
    public static final String USER_AVATAR = "avatar";
    public static final String USER_CREATEDAT = "createdAt";

    private static final String PROVIDER_LEANCLOUD = "LeanCloud";
    private static final String PROVIDER_FIREBASE = "FireBase";

    public static boolean isLeanCloud() {
        return PROVIDER_LEANCLOUD.equals(BuildConfig.DATA_PROVIDER);
    }

    public static boolean isFireBase() {
        return PROVIDER_FIREBASE.equals(BuildConfig.DATA_PROVIDER);
    }
}
