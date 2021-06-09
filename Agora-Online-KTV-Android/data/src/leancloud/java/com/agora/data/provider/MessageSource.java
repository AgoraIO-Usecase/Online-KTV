package com.agora.data.provider;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.BaseError;
import com.agora.data.Config;
import com.agora.data.EnumActionSerializer;
import com.agora.data.EnumRoleSerializer;
import com.agora.data.manager.RoomManager;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.observer.DataMaybeObserver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.types.AVNull;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

class MessageSource extends BaseMessageSource {

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Member.Role.class, new EnumRoleSerializer())
            .registerTypeAdapter(Action.ACTION.class, new EnumActionSerializer())
            .create();

    private Context mContext;

    /**
     * 申请举手用户列表
     */
    private final Map<String, RequestMember> requestMembers = new ConcurrentHashMap<>();

    public MessageSource(@NonNull Context context, @NonNull IRoomProxy iRoomProxy, @NonNull IConfigSource mIConfigSource) {
        super(iRoomProxy, mIConfigSource);
        this.mContext = context;
    }

    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());
        AVObject userAVObject = AVObject.createWithoutData(mIConfigSource.getUserTableName(), member.getUserId().getObjectId());

        AVQuery<AVObject> avQuery = AVQuery.getQuery(mIConfigSource.getMemberTableName())
                .whereEqualTo(Config.MEMBER_USERID, userAVObject)
                .whereEqualTo(Config.MEMBER_ROOMID, roomAVObject);

        return avQuery.countInBackground()
                .subscribeOn(Schedulers.io())
                .concatMap(new Function<Integer, ObservableSource<Member>>() {
                    @Override
                    public ObservableSource<Member> apply(@NonNull Integer integer) throws Exception {
                        if (integer <= 0) {
                            AVObject avObject = new AVObject(mIConfigSource.getMemberTableName());
                            avObject.put(Config.MEMBER_ROOMID, roomAVObject);
                            avObject.put(Config.MEMBER_USERID, userAVObject);

                            avObject.put(Config.MEMBER_STREAMID, member.getStreamId());
                            avObject.put(Config.MEMBER_IS_SPEAKER, member.getIsSpeaker());
                            avObject.put(Config.MEMBER_ROLE, member.getRole().getValue());
                            avObject.put(Config.MEMBER_ISMUTED, member.getIsMuted());
                            avObject.put(Config.MEMBER_ISSELFMUTED, member.getIsSelfMuted());
                            return avObject.saveInBackground()
                                    .flatMap(new Function<AVObject, ObservableSource<Member>>() {
                                        @Override
                                        public ObservableSource<Member> apply(@NonNull AVObject avObject) throws Exception {
                                            Member memberTemp = mGson.fromJson(avObject.toJSONObject().toJSONString(), Member.class);
                                            memberTemp.setUserId(member.getUserId());
                                            memberTemp.setRoomId(member.getRoomId());
                                            return Observable.just(memberTemp);
                                        }
                                    });
                        } else {
                            avQuery.include(Config.MEMBER_ROOMID);
                            avQuery.include(Config.MEMBER_ROOMID + "." + Config.MEMBER_ANCHORID);
                            avQuery.include(Config.MEMBER_USERID);
                            return avQuery.getFirstInBackground()
                                    .flatMap(new Function<AVObject, ObservableSource<Member>>() {
                                        @Override
                                        public ObservableSource<Member> apply(@NonNull AVObject avObject) throws Exception {
                                            AVObject userObject = avObject.getAVObject(Config.MEMBER_USERID);
                                            AVObject roomObject = avObject.getAVObject(Config.MEMBER_ROOMID);
                                            AVObject ancherObject = roomObject.getAVObject(Config.MEMBER_ANCHORID);

                                            User user = mGson.fromJson(userObject.toJSONObject().toJSONString(), User.class);
                                            Room room = mGson.fromJson(roomObject.toJSONObject().toJSONString(), Room.class);
                                            User ancher = mGson.fromJson(ancherObject.toJSONObject().toJSONString(), User.class);
                                            room.setAnchorId(ancher);

                                            Member memberTemp = mGson.fromJson(avObject.toJSONObject().toJSONString(), Member.class);
                                            memberTemp.setUserId(user);
                                            memberTemp.setRoomId(room);
                                            return Observable.just(memberTemp);
                                        }
                                    });
                        }
                    }
                }).doOnComplete(() -> {
                    registerMemberChanged();

                    //todo：如果同时订阅，会导致前一个订阅收不到回调，所以这里做了延迟
                    new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
                                registerAnchorActionStatus();
                            } else {
                                registerMemberActionStatus();
                            }
                        }
                    }, 500L);
                });
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        unregisterAnchorActionStatus();
        unregisterMemberActionStatus();
        unregisterMemberChanged();

        if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
            AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

            //删除Member。
            AVQuery<AVObject> avQueryMember = AVQuery.getQuery(mIConfigSource.getMemberTableName())
                    .whereEqualTo(Config.MEMBER_ROOMID, roomAVObject);

            //删除Action
            AVQuery<AVObject> avQueryAction = AVQuery.getQuery(mIConfigSource.getActionTableName())
                    .whereEqualTo(Config.ACTION_ROOMID, roomAVObject);

            //删除房间
            AVObject avObjectRoom = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

            return Observable.concat(avQueryMember.deleteAllInBackground(), avQueryAction.deleteAllInBackground(), avObjectRoom.deleteInBackground()).concatMapCompletable(new Function<AVNull, CompletableSource>() {
                @Override
                public CompletableSource apply(@NonNull AVNull avNull) throws Exception {
                    return Completable.complete();
                }
            }).subscribeOn(Schedulers.io());
        } else {
            AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());
            AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());

            //删除Action
            AVQuery<AVObject> avQueryAction = AVQuery.getQuery(mIConfigSource.getActionTableName())
                    .whereEqualTo(Config.ACTION_ROOMID, roomAVObject)
                    .whereEqualTo(Config.ACTION_MEMBERID, memberAVObject);

            //删除Member
            AVObject avObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());

            return avQueryAction.deleteAllInBackground()
                    .concatWith(avObject.deleteInBackground()).concatMapCompletable(new Function<AVNull, CompletableSource>() {
                        @Override
                        public CompletableSource apply(@NonNull AVNull avNull) throws Exception {
                            return Completable.complete();
                        }
                    }).subscribeOn(Schedulers.io());
        }
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        AVObject avObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        avObject.put(Config.MEMBER_ISMUTED, muted);
        return avObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .concatMapCompletable(new Function<AVObject, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                        return Completable.complete();
                    }
                });
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        AVObject avObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        avObject.put(Config.MEMBER_ISSELFMUTED, muted);
        return avObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .concatMapCompletable(new Function<AVObject, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                        return Completable.complete();
                    }
                });
    }

    @Override
    public Completable requestConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        AVObject avObject = new AVObject(mIConfigSource.getActionTableName());
        avObject.put(Config.ACTION_MEMBERID, memberAVObject);
        avObject.put(Config.ACTION_ROOMID, roomAVObject);
        avObject.put(Config.ACTION_ACTION, action.getValue());
        avObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());
        return avObject.saveInBackground()
                .concatMapCompletable(new Function<AVObject, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                        return Completable.complete();
                    }
                });
    }

    @Override
    public Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        AVObject memberObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        //更新Member表
        memberObject.put(Config.MEMBER_IS_SPEAKER, 1);
        if (action == Action.ACTION.RequestLeft) {
            memberObject.put(Config.MEMBER_ROLE, Member.Role.Left.getValue());
        } else if (action == Action.ACTION.RequestRight) {
            memberObject.put(Config.MEMBER_ROLE, Member.Role.Right.getValue());
        }

        //更新Action表
        AVObject actionObject = new AVObject(mIConfigSource.getActionTableName());
        actionObject.put(Config.ACTION_MEMBERID, memberObject);
        actionObject.put(Config.ACTION_ROOMID, roomAVObject);
        actionObject.put(Config.ACTION_ACTION, action.getValue());
        actionObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());

        return Completable.concatArray(memberObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        }), actionObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        })).subscribeOn(Schedulers.io()).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                requestMembers.remove(member.getObjectId());
            }
        });
    }

    @Override
    public Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        //更新Action表
        AVObject actionObject = new AVObject(mIConfigSource.getActionTableName());
        actionObject.put(Config.ACTION_MEMBERID, memberAVObject);
        actionObject.put(Config.ACTION_ROOMID, roomAVObject);
        actionObject.put(Config.ACTION_ACTION, action.getValue());
        actionObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());

        return actionObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        }).subscribeOn(Schedulers.io()).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                requestMembers.remove(member.getObjectId());
            }
        });
    }

    @Override
    public Completable inviteConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        AVObject avObject = new AVObject(mIConfigSource.getActionTableName());
        avObject.put(Config.ACTION_MEMBERID, memberAVObject);
        avObject.put(Config.ACTION_ROOMID, roomAVObject);
        avObject.put(Config.ACTION_ACTION, action.getValue());
        avObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());

        return avObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        });
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        //更新Member表
        memberAVObject.put(Config.MEMBER_IS_SPEAKER, 1);
        memberAVObject.put(Config.MEMBER_ROLE, member.getRole().getValue());

        //更新Action表
        AVObject actionObject = new AVObject(mIConfigSource.getActionTableName());
        actionObject.put(Config.ACTION_MEMBERID, memberAVObject);
        actionObject.put(Config.ACTION_ROOMID, roomAVObject);
        actionObject.put(Config.ACTION_ACTION, Action.ACTION.Invite.getValue());
        actionObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());

        return Completable.concatArray(memberAVObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        }), actionObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), member.getRoomId().getObjectId());

        //更新Action表
        AVObject actionObject = new AVObject(mIConfigSource.getActionTableName());
        actionObject.put(Config.ACTION_MEMBERID, memberAVObject);
        actionObject.put(Config.ACTION_ROOMID, roomAVObject);
        actionObject.put(Config.ACTION_ACTION, Action.ACTION.Invite.getValue());
        actionObject.put(Config.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());

        return actionObject.saveInBackground().concatMapCompletable(new Function<AVObject, CompletableSource>() {
            @Override
            public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                return Completable.complete();
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable seatOff(@NonNull Member member, @NonNull Member.Role role) {
        AVObject memberObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());
        memberObject.put(Config.MEMBER_IS_SPEAKER, 0);
        memberObject.put(Config.MEMBER_ROLE, role.getValue());
        return memberObject.saveInBackground()
                .subscribeOn(Schedulers.io())
                .concatMapCompletable(new Function<AVObject, CompletableSource>() {
                    @Override
                    public CompletableSource apply(@NonNull AVObject avObject) throws Exception {
                        return Completable.complete();
                    }
                });
    }

    @Override
    public Observable<List<RequestMember>> getRequestList() {
        return Observable.just(new ArrayList<>(requestMembers.values()));
    }

    @Override
    public int getHandUpListCount() {
        return requestMembers.size();
    }

    private AVLiveQueryHelp<Action> mAVLiveQueryHelpAction = new AVLiveQueryHelp<>(Action.class);
    private AVLiveQueryHelp<Member> mAVLiveQueryHelpMember = new AVLiveQueryHelp<>(Member.class);

    /**
     * 作为房主，需要监听房间中Action变化。
     */
    private void registerAnchorActionStatus() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), room.getObjectId());

        AVQuery<AVObject> query = AVQuery.getQuery(mIConfigSource.getActionTableName());
        query.whereEqualTo(Config.ACTION_ROOMID, roomAVObject);
        Log.i(AVLiveQueryHelp.TAG, String.format("%s registerObserve roomId= %s", mIConfigSource.getActionTableName(), room.getObjectId()));
        mAVLiveQueryHelpAction.registerObserve(query, new AVLiveQueryHelp.AttributeListener<Action>() {
            @Override
            public void onCreated(Action item) {
                if (item.getAction() == Action.ACTION.HandsUp
                        || item.getAction() == Action.ACTION.RequestLeft
                        || item.getAction() == Action.ACTION.RequestRight) {
                    if (item.getStatus() == Action.ACTION_STATUS.Ing.getValue()) {
                        Member member = item.getMemberId();
                        member = iRoomProxy.getMemberById(member.getObjectId());
                        if (member == null) {
                            return;
                        }

                        if (requestMembers.containsKey(member.getObjectId())) {
                            return;
                        }

                        requestMembers.put(member.getObjectId(), new RequestMember(member, item.getAction()));
                        iRoomProxy.onReceivedRequest(member, item.getAction());
                    }
                } else if (item.getAction() == Action.ACTION.Invite) {
                    if (item.getStatus() == Action.ACTION_STATUS.Agree.getValue()) {
                        Member member = item.getMemberId();
                        member = iRoomProxy.getMemberById(member.getObjectId());
                        if (member == null) {
                            return;
                        }

                        iRoomProxy.onInviteAgree(member);
                    } else if (item.getStatus() == Action.ACTION_STATUS.Refuse.getValue()) {
                        Member member = item.getMemberId();
                        member = iRoomProxy.getMemberById(member.getObjectId());
                        if (member == null) {
                            return;
                        }

                        iRoomProxy.onInviteRefuse(member);
                    }
                }
            }

            @Override
            public void onUpdated(Action item) {

            }

            @Override
            public void onDeleted(String objectId) {

            }

            @Override
            public void onSubscribeError(int error) {
                if (error == AVLiveQueryHelp.ERROR_EXCEEDED_QUOTA) {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD_EXCEEDED_QUOTA);
                } else {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD);
                }
            }
        });
    }

    private void unregisterAnchorActionStatus() {
        mAVLiveQueryHelpAction.unregisterObserve();
    }

    /**
     * 作为观众，需要监听自己的Action变化。
     */
    private void registerMemberActionStatus() {
        Member member = iRoomProxy.getMine();
        if (member == null) {
            return;
        }
        AVObject memberAVObject = AVObject.createWithoutData(mIConfigSource.getMemberTableName(), member.getObjectId());

        AVQuery<AVObject> query = AVQuery.getQuery(mIConfigSource.getActionTableName());
        query.whereEqualTo(Config.ACTION_MEMBERID, memberAVObject);
        Log.i(AVLiveQueryHelp.TAG, String.format("%s registerObserve memberId= %s", mIConfigSource.getActionTableName(), member.getObjectId()));
        mAVLiveQueryHelpAction.registerObserve(query, new AVLiveQueryHelp.AttributeListener<Action>() {
            @Override
            public void onCreated(Action item) {
                if (item.getAction() == Action.ACTION.HandsUp
                        || item.getAction() == Action.ACTION.RequestLeft
                        || item.getAction() == Action.ACTION.RequestRight) {
                    if (item.getStatus() == Action.ACTION_STATUS.Agree.getValue()) {
                        iRoomProxy.onRequestAgreed(member, item.getAction());
                    } else if (item.getStatus() == Action.ACTION_STATUS.Refuse.getValue()) {
                        iRoomProxy.onRequestRefused(member);
                    }
                } else if (item.getAction() == Action.ACTION.Invite) {
                    if (item.getStatus() == Action.ACTION_STATUS.Ing.getValue()) {
                        iRoomProxy.onReceivedInvite(member);
                    }
                }
            }

            @Override
            public void onUpdated(Action item) {

            }

            @Override
            public void onDeleted(String objectId) {

            }

            @Override
            public void onSubscribeError(int error) {
                if (error == AVLiveQueryHelp.ERROR_EXCEEDED_QUOTA) {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD_EXCEEDED_QUOTA);
                } else {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD);
                }
            }
        });
    }

    private void unregisterMemberActionStatus() {
        mAVLiveQueryHelpAction.unregisterObserve();
    }

    /**
     * 监听房间内部成员信息变化
     */
    private void registerMemberChanged() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }
        AVObject roomAVObject = AVObject.createWithoutData(mIConfigSource.getRoomTableName(), room.getObjectId());

        AVQuery<AVObject> query = AVQuery.getQuery(mIConfigSource.getMemberTableName());
        query.whereEqualTo(Config.MEMBER_ROOMID, roomAVObject);
        Log.i(AVLiveQueryHelp.TAG, String.format("%s registerObserve roomId= %s", mIConfigSource.getMemberTableName(), room.getObjectId()));
        mAVLiveQueryHelpMember.registerObserve(query, new AVLiveQueryHelp.AttributeListener<Member>() {
            @Override
            public void onCreated(Member member) {
                if (iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                    return;
                }

                iRoomProxy.getMember(room.getObjectId(), member.getUserId().getObjectId())
                        .subscribe(new DataMaybeObserver<Member>(mContext) {
                            @Override
                            public void handleError(@NonNull BaseError e) {

                            }

                            @Override
                            public void handleSuccess(@Nullable Member member) {
                                if (member == null) {
                                    return;
                                }

                                if (iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                    return;
                                }

                                iRoomProxy.onMemberJoin(member);
                            }
                        });
            }

            @Override
            public void onUpdated(Member member) {
                if (!iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                    return;
                }

                Member memberOld = iRoomProxy.getMemberById(member.getObjectId());
                if (memberOld == null) {
                    return;
                }

                if (memberOld.getIsSpeaker() != member.getIsSpeaker()
                        || memberOld.getRole() != member.getRole()) {
                    iRoomProxy.onRoleChanged(false, member);
                }

                if (memberOld.getIsSelfMuted() != member.getIsSelfMuted()
                        || memberOld.getIsMuted() != member.getIsMuted()) {
                    iRoomProxy.onAudioStatusChanged(false, member);
                }
            }

            @Override
            public void onDeleted(String objectId) {
                if (!iRoomProxy.isMembersContainsKey(objectId)) {
                    return;
                }

                Member member = iRoomProxy.getMemberById(objectId);
                if (member == null) {
                    return;
                }

                iRoomProxy.onMemberLeave(member);
            }

            @Override
            public void onSubscribeError(int error) {
                if (error == AVLiveQueryHelp.ERROR_EXCEEDED_QUOTA) {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD_EXCEEDED_QUOTA);
                } else {
                    iRoomProxy.onRoomError(RoomManager.ERROR_REGISTER_LEANCLOUD);
                }
            }
        });
    }

    private void unregisterMemberChanged() {
        mAVLiveQueryHelpMember.unregisterObserve();
    }
}
