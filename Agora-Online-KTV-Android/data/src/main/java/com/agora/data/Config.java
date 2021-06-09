package com.agora.data;

public class Config {

    public static final String USER_OBJECTID = "objectId";
    public static final String USER_NAME = "name";
    public static final String USER_AVATAR = "avatar";
    public static final String USER_CREATEDAT = "createdAt";

    public static final String MEMBER_OBJECTID = "objectId";
    public static final String MEMBER_ROLE = "role";
    public static final String MEMBER_CHANNELNAME = "channelName";
    public static final String MEMBER_ANCHORID = "anchorId";
    public static final String MEMBER_ROOMID = "roomId";
    public static final String MEMBER_USERID = "userId";
    public static final String MEMBER_IS_SPEAKER = "isSpeaker";
    public static final String MEMBER_ISMUTED = "isMuted";
    public static final String MEMBER_ISSELFMUTED = "isSelfMuted";
    public static final String MEMBER_STREAMID = "streamId";
    public static final String MEMBER_CREATEDAT = "createdAt";

    public static final String ROOM_OBJECTID = "objectId";
    public static final String ROOM_CREATEDAT = "createdAt";

    public static final String ACTION_OBJECTID = "objectId";
    public static final String ACTION_MEMBERID = "memberId";
    public static final String ACTION_ROOMID = "roomId";
    public static final String ACTION_ACTION = "action";
    public static final String ACTION_STATUS = "status";
    public static final String ACTION_CREATEDAT = "createdAt";

    private static final String PROVIDER_LEANCLOUD = "LeanCloud";
    private static final String PROVIDER_FIREBASE = "FireBase";

    public static boolean isLeanCloud() {
        return PROVIDER_LEANCLOUD.equals(BuildConfig.DATA_PROVIDER);
    }

    public static boolean isFireBase() {
        return PROVIDER_FIREBASE.equals(BuildConfig.DATA_PROVIDER);
    }
}
