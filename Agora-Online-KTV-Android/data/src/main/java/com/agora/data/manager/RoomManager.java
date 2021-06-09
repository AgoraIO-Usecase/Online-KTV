package com.agora.data.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.preference.PreferenceManager;

import com.agora.data.BaseError;
import com.agora.data.IDataRepositroy;
import com.agora.data.RoomEventCallback;
import com.agora.data.SimpleRtmChannelListener;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.observer.DataCompletableObserver;
import com.agora.data.provider.IRoomConfigProvider;
import com.agora.data.provider.IRoomProxy;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.models.ClientRoleOptions;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmMessage;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * 负责房间内数据的管理以及事件通知
 */
public final class RoomManager implements IRoomProxy {

    public static final String TAG_AUDIENCELATENCYLEVEL = "audienceLatencyLevel";

    private Logger.Builder mLogger = XLog.tag(RoomManager.class.getSimpleName());

    public static final int ERROR_REGISTER_LEANCLOUD = 1000;
    public static final int ERROR_REGISTER_LEANCLOUD_EXCEEDED_QUOTA = ERROR_REGISTER_LEANCLOUD + 1;

    private volatile static RoomManager instance;

    private Context mContext;

    private RoomManager(Context context) {
        mContext = context.getApplicationContext();
    }

    private IRoomConfigProvider roomConfig;

    private IDataRepositroy iDataRepositroy;

    private final IRtcEngineEventHandler mIRtcEngineEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onLocalVideoStateChanged(int localVideoState, int error) {
            super.onLocalVideoStateChanged(localVideoState, error);
            Member member = getMine();
            if (member == null) {
                return;
            }

            if (localVideoState == Constants.LOCAL_VIDEO_STREAM_STATE_ENCODING) {
                member.setIsSDKVideoMuted(0);
            } else if (localVideoState == Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED || localVideoState == Constants.LOCAL_VIDEO_STREAM_STATE_FAILED) {
                member.setIsSDKVideoMuted(1);
            }
            mainThreadDispatch.onSDKVideoStatusChanged(member);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            long streamId = uid & 0xffffffffL;
            Member member = getMemberByStramId(streamId);
            if (member == null) {
                return;
            }

            if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                member.setIsSDKVideoMuted(0);
            } else if (state == Constants.REMOTE_VIDEO_STATE_STOPPED || state == Constants.REMOTE_VIDEO_STATE_FAILED) {
                member.setIsSDKVideoMuted(1);
            }
            mainThreadDispatch.onSDKVideoStatusChanged(member);
        }
    };

    private final RtmChannelListener mRtmChannelListener = new SimpleRtmChannelListener() {
        @Override
        public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember) {
            super.onMessageReceived(rtmMessage, rtmChannelMember);
            if (isLeaving) {
                return;
            }

            String uid = rtmChannelMember.getUserId();
            Member member = getMemberById(uid);
            if (member == null) {
                return;
            }
            onRoomMessageReceived(member, rtmMessage.getText());
        }
    };

    public synchronized static RoomManager Instance(Context context) {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null)
                    instance = new RoomManager(context);
            }
        }
        return instance;
    }

    /**
     * 正在退出房间，防止回调处理。
     */
    public volatile static boolean isLeaving = false;

    private volatile Room mRoom;
    private volatile Member mMine;
    private volatile Member mOwner;

    /**
     * Member表中objectId和Member的键值对
     */
    private final Map<String, Member> membersMap = new ConcurrentHashMap<>();

    /**
     * RTC的UID和Member的键值对
     */
    private final Map<Long, Member> streamIdMap = new ConcurrentHashMap<>();

    private final MainThreadDispatch mainThreadDispatch = new MainThreadDispatch();

    public void setupRoomConfig(IRoomConfigProvider roomConfig) {
        this.roomConfig = roomConfig;
        RtcManager.Instance(mContext).setupRoomConfig(roomConfig);
    }

    public void setupDataRepositroy(IDataRepositroy iDataRepositroy) {
        this.iDataRepositroy = iDataRepositroy;
    }

    /**
     * 先加入RTC房间，获取userId，然后加入到业务服务器。
     */
    public Completable joinRoom() {
        mLogger.d("joinRoom() called");

        Room room = getRoom();
        if (room == null) {
            return Completable.complete();
        }

        Member member = getMine();
        if (member == null) {
            return Completable.complete();
        }

        int userId = 0;
        if (member.getStreamId() != null) {
            userId = member.getStreamIntId();
        }

        String objectId = room.getObjectId();
        return RtcManager.Instance(mContext).joinChannel(objectId, userId).flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull Integer uid) throws Exception {
                Long streamId = uid & 0xffffffffL;
                member.setStreamId(streamId);
                return iDataRepositroy.joinRoom(room, member).concatMapCompletable(new Function<Member, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull Member member) throws Exception {
                        mMine = member;
                        if (ObjectsCompat.equals(room.getAnchorId(), mMine.getUserId())) {
                            mOwner = mMine;
                        }

                        mLogger.d("joinRoom() doOnComplete called member= [%s]", mMine);
                        return Completable.complete();
                    }
                });
            }
        });
    }

    public Completable requestConnect(@NonNull Action.ACTION action) {
        mLogger.d("requestConnect() called with: action = [" + action + "]");
        return iDataRepositroy
                .requestConnect(mMine, action);
    }

    public Completable toggleSelfAudio() {
        mLogger.d("toggleSelfAudio() called");
        Member member = getMine();
        if (member == null) {
            return Completable.complete();
        }

        int newValue = mMine.getIsSelfMuted() == 0 ? 1 : 0;
        return iDataRepositroy
                .muteSelfVoice(mMine, newValue)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        mMine.setIsSelfMuted(newValue);

                        RtcManager.Instance(mContext).getRtcEngine().muteLocalAudioStream(newValue == 1);
                        onAudioStatusChanged(true, mMine);
                    }
                });
    }

    public Completable toggleTargetAudio(@NonNull Member member) {
        mLogger.d("toggleTargetAudio() called with: member = [" + member + "]");
        if (ObjectsCompat.equals(member, getMine())) {
            return toggleSelfAudio();
        }

        int newValue = member.getIsMuted() == 0 ? 1 : 0;
        return iDataRepositroy
                .muteVoice(member, newValue)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        member.setIsMuted(newValue);

                        RtcManager.Instance(mContext).getRtcEngine().muteRemoteAudioStream(member.getStreamIntId(), newValue != 0);
                        onAudioStatusChanged(false, member);
                    }
                });
    }

    public Completable seatOff(@NonNull Member member, @NonNull Member.Role role) {
        mLogger.d("seatOff() called with: member = [" + member + "], role = [" + role + "]");
        return iDataRepositroy
                .seatOff(member, role)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        member.setIsSpeaker(0);
                        member.setRole(role);

                        if (isMine(member)) {
                            onRoleChanged(true, member);
                        } else {
                            onRoleChanged(false, member);
                        }
                    }
                });
    }

    public void addRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.mainThreadDispatch.addRoomEventCallback(callback);
    }

    public void removeRoomEventCallback(@NonNull RoomEventCallback callback) {
        this.mainThreadDispatch.removeRoomEventCallback(callback);
    }

    public void onJoinRoom(Room room, Member member) {
        mLogger.d("onJoinRoom() called room= [%s] member= [%s]", room, member);
        this.mRoom = room;
        this.mMine = member;
        this.mOwner = new Member(room.getAnchorId());
        isLeaving = false;

        RtcManager.Instance(mContext).addHandler(mIRtcEngineEventHandler);
        RTMManager.Instance(mContext).addChannelListeners(mRtmChannelListener);
    }

    private void leaveRoom(boolean fromUser) {
        mLogger.d("leaveRoom() called with: fromUser = [%s]", fromUser);
        if (isLeaving) {
            return;
        }

        isLeaving = true;

        RoomManager.Instance(mContext).stopLivePlay();
        RtcManager.Instance(mContext).leaveChannel();
        iDataRepositroy
                .leaveRoom(mRoom, mMine)
                .subscribe(new DataCompletableObserver(mContext) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                    }

                    @Override
                    public void handleSuccess() {
                    }
                });

        onLeaveRoom(fromUser);
    }

    public void leaveRoom() {
        leaveRoom(true);
    }

    private void onLeaveRoom(boolean fromUser) {
        mLogger.d("onLeaveRoom() called");
        RtcManager.Instance(mContext).removeHandler(mIRtcEngineEventHandler);
        RTMManager.Instance(mContext).removeChannelListeners(mRtmChannelListener);

        if (fromUser) {
            if (isOwner()) {
                mainThreadDispatch.onRoomClosed(mRoom, fromUser);
            }
        } else {
            mainThreadDispatch.onRoomClosed(mRoom, fromUser);
        }

        this.mRoom = null;
        this.mMine = null;
        this.mOwner = null;
        this.membersMap.clear();
        this.streamIdMap.clear();
    }

    public void onRoomUpdated(Room room) {
        mLogger.d("onRoomUpdated() called with: room = [" + room + "]");
        this.mRoom = room;
    }

    public void onLoadRoomMembers(@NonNull List<Member> members) {
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            if (isMine(member)) {
                member = mMine;
                members.set(i, mMine);
            }

            if (ObjectsCompat.equals(mRoom.getAnchorId(), member.getUserId())) {
                mOwner = member;
            }

            member.setRoomId(mRoom);
            membersMap.put(member.getObjectId(), member);
            if (member.getStreamId() != null) {
                streamIdMap.put(member.getStreamId(), member);
            }
        }
    }

    public Maybe<Room> getRoom(Room room) {
        mLogger.d("getRoom() called with: room = [" + room + "]");
        return iDataRepositroy
                .getRoom(room)
                .doOnSuccess(new Consumer<Room>() {
                    @Override
                    public void accept(Room room) throws Exception {
                        onRoomUpdated(room);
                    }
                });
    }

    public List<Member> getMembers() {
        return new ArrayList<>(membersMap.values());
    }

    @Nullable
    @Override
    public Member getMine() {
        return mMine;
    }

    @Nullable
    @Override
    public Room getRoom() {
        return mRoom;
    }

    @Nullable
    @Override
    public Member getOwner() {
        return mOwner;
    }

    public void updateMine(@NonNull Member mine) {
        this.mMine = mine;
    }

    public void startLivePlay() {
        mLogger.d("startLivePlay() called");
        if (roomConfig.isNeedAudio()) {
            RtcManager.Instance(mContext).getRtcEngine().enableLocalAudio(true);
        }

        if (roomConfig.isNeedVideo()) {
            RtcManager.Instance(mContext).getRtcEngine().enableLocalVideo(true);
            RtcManager.Instance(mContext).getRtcEngine().startPreview();
        }

        RtcManager.Instance(mContext).getRtcEngine().setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
    }

    public void stopLivePlay() {
        mLogger.d("stopLivePlay() called");
        if (roomConfig.isNeedAudio()) {
            RtcManager.Instance(mContext).getRtcEngine().enableLocalAudio(false);
        }

        if (roomConfig.isNeedVideo()) {
            RtcManager.Instance(mContext).getRtcEngine().enableLocalVideo(false);
            RtcManager.Instance(mContext).getRtcEngine().stopPreview();
        }

        int audienceLatencyLevel = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(TAG_AUDIENCELATENCYLEVEL, Constants.AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY);
        ClientRoleOptions mClientRoleOptions = new ClientRoleOptions();
        mClientRoleOptions.audienceLatencyLevel = audienceLatencyLevel;
        RtcManager.Instance(mContext).getRtcEngine().setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE, mClientRoleOptions);
    }

    public Maybe<Member> getMember(String userId) {
        return iDataRepositroy
                .getMember(mRoom.getObjectId(), userId)
                .doOnSuccess(new Consumer<Member>() {
                    @Override
                    public void accept(Member member) throws Exception {
                        member.setRoomId(mRoom);
                    }
                });
    }

    public Completable agreeInvite(@NonNull Member member) {
        mLogger.d("agreeInvite() called with: member = [" + member + "]");
        return iDataRepositroy
                .agreeInvite(member)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        startLivePlay();
                        onInviteAgree(member);
                    }
                });
    }

    public Completable refuseInvite(@NonNull Member member) {
        mLogger.d("refuseInvite() called with: member = [" + member + "]");
        return iDataRepositroy
                .refuseInvite(member)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        onInviteRefuse(member);
                    }
                });
    }

    public Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        mLogger.d("agreeRequest() called with: member = [" + member + "], action = [" + action + "]");
        return iDataRepositroy
                .agreeRequest(member, action)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        Member memberLocal = getMemberById(member.getObjectId());
                        if (memberLocal == null) {
                            return;
                        }

                        if (action == Action.ACTION.HandsUp) {
                            memberLocal.setIsSpeaker(1);
                            memberLocal.setRole(Member.Role.Speaker);
                        } else if (action == Action.ACTION.RequestLeft) {
                            memberLocal.setRole(Member.Role.Left);
                        } else if (action == Action.ACTION.RequestRight) {
                            memberLocal.setRole(Member.Role.Right);
                        }
                        onRequestAgreed(memberLocal, action);
                    }
                });
    }

    public Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        mLogger.d("refuseRequest() called with: member = [" + member + "], action = [" + action + "]");
        return iDataRepositroy
                .refuseRequest(member, action)
                .doOnComplete(new io.reactivex.functions.Action() {
                    @Override
                    public void run() throws Exception {
                        onRequestRefused(member);
                    }
                });
    }

    @Override
    public boolean isMembersContainsKey(@NonNull String memberId) {
        return membersMap.containsKey(memberId);
    }

    @Override
    public Member getMemberById(@NonNull String memberId) {
        return membersMap.get(memberId);
    }

    @Override
    public Member getMemberByStramId(long streamId) {
        return streamIdMap.get(streamId);
    }

    @Override
    public Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId) {
        return iDataRepositroy.getMember(roomId, userId);
    }

    @Override
    public boolean isOwner(@NonNull Member member) {
        return ObjectsCompat.equals(member.getUserId(), mRoom.getAnchorId());
    }

    @Override
    public boolean isOwner() {
        return isOwner(mMine);
    }

    @Override
    public boolean isOwner(String userId) {
        return ObjectsCompat.equals(userId, mRoom.getAnchorId().getObjectId());
    }

    @Override
    public boolean isMine(@NonNull Member member) {
        return ObjectsCompat.equals(member, mMine);
    }

    @Override
    public void onMemberJoin(@NonNull Member member) {
        mLogger.d("onMemberJoin() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        if (!TextUtils.isEmpty(member.getObjectId())) {
            membersMap.put(member.getObjectId(), member);
        }

        if (member.getStreamId() != null) {
            streamIdMap.put(member.getStreamId(), member);
        }

        mainThreadDispatch.onMemberJoin(member);
    }

    @Override
    public void onMemberLeave(@NonNull Member member) {
        mLogger.d("onMemberLeave() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        if (!TextUtils.isEmpty(member.getObjectId())) {
            membersMap.remove(member.getObjectId());
        }

        if (member.getStreamId() != null) {
            streamIdMap.remove(member.getStreamId());
        }

        mainThreadDispatch.onMemberLeave(member);

        if (isOwner(member)) {
            leaveRoom(false);
        }
    }

    @Override
    public void onRoleChanged(boolean isMine, @NonNull Member member) {
        mLogger.d("onRoleChanged() called with: isMine = [" + isMine + "], member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        Member old = getMemberById(member.getObjectId());
        if (old == null) {
            return;
        }
        old.setIsSpeaker(member.getIsSpeaker());
        old.setRole(member.getRole());

        if (isMine(old)) {
            if (old.getIsSpeaker() == 1) {
                startLivePlay();
            } else {
                stopLivePlay();
            }

            if (old.getRole() == Member.Role.Listener) {
                stopLivePlay();
            } else {
                startLivePlay();
            }
        }
        mainThreadDispatch.onRoleChanged(isMine, old);
    }

    @Override
    public void onAudioStatusChanged(boolean isMine, @NonNull Member member) {
        mLogger.d("onAudioStatusChanged() called with: isMine = [" + isMine + "], member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        Member old = getMemberById(member.getObjectId());
        if (old == null) {
            return;
        }
        old.setIsMuted(member.getIsMuted());
        old.setIsSelfMuted(member.getIsSelfMuted());
        mainThreadDispatch.onAudioStatusChanged(isMine, old);
    }

    @Override
    public void onReceivedRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        mLogger.d("onReceivedRequest() called with: member = [" + member + "], action = [" + action + "]");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onReceivedRequest(member, action);
    }

    @Override
    public void onRequestAgreed(@NonNull Member member, @NonNull Action.ACTION action) {
        mLogger.d("onRequestAgreed() called with: member = [" + member + "], action = [" + action + "]");
        if (isLeaving) {
            return;
        }

        if (action == Action.ACTION.HandsUp) {
            member.setIsSpeaker(1);
            member.setRole(Member.Role.Speaker);
        } else if (action == Action.ACTION.RequestLeft) {
            member.setRole(Member.Role.Left);
        } else if (action == Action.ACTION.RequestRight) {
            member.setRole(Member.Role.Right);
        }

        if (isMine(member)) {
            startLivePlay();
        }
        mainThreadDispatch.onRequestAgreed(member);
    }

    @Override
    public void onRequestRefused(@NonNull Member member) {
        mLogger.d("onHandUpRefuse() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onRequestRefuse(member);
    }

    @Override
    public void onReceivedInvite(@NonNull Member member) {
        mLogger.d("onReceivedInvite() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onReceivedInvite(member);
    }

    @Override
    public void onInviteAgree(@NonNull Member member) {
        mLogger.d("onInviteAgree() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onInviteAgree(member);
    }

    @Override
    public void onInviteRefuse(@NonNull Member member) {
        mLogger.d("onInviteRefuse() called with: member = [" + member + "]");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onInviteRefuse(member);
    }

    @Override
    public void onEnterMinStatus() {
        mLogger.d("onEnterMinStatus() called");
        if (isLeaving) {
            return;
        }

        mainThreadDispatch.onEnterMinStatus();
    }

    @Override
    public void onRoomError(int error) {
        mLogger.d("onRoomError() called with: error = [" + error + "]");
        mainThreadDispatch.onRoomError(error);
    }

    @Override
    public void onRoomMessageReceived(@NonNull Member member, @NonNull String message) {
        mLogger.d("onRoomMessageReceived() called with: member = [" + member + "], message = [" + message + "]");
        mainThreadDispatch.onRoomMessageReceived(member, message);
    }
}
