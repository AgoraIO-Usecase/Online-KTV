package com.agora.data.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.agora.data.BaseError;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.observer.DataMaybeObserver;
import com.agora.data.provider.model.ActionTemp;
import com.agora.data.provider.model.MemberTemp;
import com.agora.data.provider.model.RoomTemp;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

class MessageSource extends BaseMessageSource {

    private final Logger.Builder mLogger = XLog.tag(MessageSource.class.getSimpleName());

    private Gson mGson = new Gson();
    private Context mContext;

    private FirebaseFirestore db;

    /**
     * 申请举手用户列表
     */
    private final Map<String, RequestMember> requestMembers = new ConcurrentHashMap<>();

    public MessageSource(@NonNull Context context, @NonNull IRoomProxy iRoomProxy, @NonNull IConfigSource mIConfigSource) {
        super(iRoomProxy, mIConfigSource);
        this.mContext = context;

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        return Observable.create((ObservableOnSubscribe<Member>) emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
            DocumentReference drUser = db.collection(BaseDataProvider.TAG_TABLE_USER).document(member.getUserId().getObjectId());

            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo(BaseDataProvider.MEMBER_ROOMID, drRoom)
                    .whereEqualTo(BaseDataProvider.MEMBER_USERID, drUser)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                HashMap<String, Object> map = new HashMap<>();
                                map.put(BaseDataProvider.MEMBER_ROOMID, drRoom);
                                map.put(BaseDataProvider.MEMBER_USERID, drUser);
                                map.put(BaseDataProvider.MEMBER_STREAMID, member.getStreamId());
                                map.put(BaseDataProvider.MEMBER_ISSPEAKER, member.getIsSpeaker());
                                map.put(BaseDataProvider.MEMBER_ISMUTED, member.getIsMuted());
                                map.put(BaseDataProvider.MEMBER_ISSELFMUTED, member.getIsSelfMuted());
                                map.put(BaseDataProvider.MEMBER_CREATEDAT, Timestamp.now());

                                db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                                        .add(map)
                                        .addOnSuccessListener(documentReference -> {
                                            String objectId = documentReference.getId();
                                            member.setObjectId(objectId);
                                            emitter.onNext(member);
                                            emitter.onComplete();
                                        })
                                        .addOnFailureListener(new OnFailureListener() {

                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                emitter.onError(e);
                                                emitter.onComplete();
                                            }
                                        });
                                return;
                            }

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MemberTemp memberTemp = document.toObject(MemberTemp.class);
                                Member member2 = memberTemp.createMember(document.getId());

                                Task<Void> taskRoom = memberTemp.getRoomId()
                                        .get()
                                        .continueWith(new Continuation<DocumentSnapshot, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                                if (!task.isSuccessful()) {
                                                    throw task.getException();
                                                }

                                                DocumentSnapshot document = task.getResult();
                                                if (document == null || !document.exists()) {
                                                    return null;
                                                }

                                                RoomTemp roomTemp = document.toObject(RoomTemp.class);
                                                Room room = roomTemp.createRoom(document.getId());
                                                member.setRoomId(room);
                                                return null;
                                            }
                                        });
                                Task<Void> taskUser = memberTemp.getUserId()
                                        .get()
                                        .continueWith(new Continuation<DocumentSnapshot, Void>() {
                                            @Override
                                            public Void then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                                if (!task.isSuccessful()) {
                                                    throw task.getException();
                                                }

                                                DocumentSnapshot document = task.getResult();
                                                if (document == null || !document.exists()) {
                                                    return null;
                                                }

                                                User user = document.toObject(User.class);
                                                user.setObjectId(document.getId());
                                                member2.setUserId(user);
                                                return null;
                                            }
                                        });


                                List<Task<Void>> tasks = new ArrayList<>();
                                tasks.add(taskRoom);
                                tasks.add(taskUser);
                                Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            emitter.onNext(member2);
                                        } else {
                                            emitter.onError(task.getException());
                                        }
                                        emitter.onComplete();
                                    }
                                });
                                break;
                            }
                        } else {
                            emitter.onError(task.getException());
                            emitter.onComplete();
                        }
                    });
        }).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                registerMemberChanged();

                if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
                    registerAnchorActionStatus();
                } else {
                    registerMemberActionStatus();
                }
            }
        });
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        unregisterAnchorActionStatus();
        unregisterMemberActionStatus();
        unregisterMemberChanged();

        if (ObjectsCompat.equals(room.getAnchorId(), member.getUserId())) {
            return Completable.create(emitter -> {
                DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(room.getObjectId());

                db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                        .whereEqualTo(BaseDataProvider.ACTION_ROOMID, drRoom)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            } else {
                            }
                        });

                db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                        .whereEqualTo(BaseDataProvider.MEMBER_ROOMID, drRoom)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    emitter.onComplete();
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            }
                        });

                drRoom.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                emitter.onComplete();
                            } else {
                                emitter.onError(task.getException());
                            }
                        });

            });
        } else {
            return Completable.create(emitter -> {
                DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
                DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

                db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                        .whereEqualTo(BaseDataProvider.ACTION_ROOMID, drRoom)
                        .whereEqualTo(BaseDataProvider.ACTION_MEMBERID, drMember)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    return;
                                }

                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference dr = db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                                            .document(document.getId());
                                    batch.delete(dr);
                                }

                                batch.commit();
                            } else {
                            }
                        });

                drMember.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                emitter.onComplete();
                            } else {
                                emitter.onError(task.getException());
                            }
                        });

            });
        }
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return Completable.create(emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(BaseDataProvider.MEMBER_ISMUTED, muted)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return Completable.create(emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(BaseDataProvider.MEMBER_ISSELFMUTED, muted)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable requestConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return Completable.create(emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
            DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
            map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
            map.put(BaseDataProvider.ACTION_ACTION, action.getValue());
            map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());
            map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());

            db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(emitter::onError);
        });
    }

    @Override
    public Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return Completable.create(emitter -> {
            db.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                    DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
                    DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

                    HashMap<String, Object> map = new HashMap<>();
                    map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
                    map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
                    map.put(BaseDataProvider.ACTION_ACTION, action.getValue());
                    map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());
                    map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());
                    db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                            .add(map);

                    HashMap<String, Object> map2 = new HashMap<>();
                    map2.put(BaseDataProvider.MEMBER_ISSPEAKER, 1);
                    map2.put(BaseDataProvider.MEMBER_ROLE, member.getRole().getValue());
                    transaction.update(drMember, map2);
                    return null;
                }
            }).addOnSuccessListener(documentReference -> {
                emitter.onComplete();
            }).addOnFailureListener(e -> emitter.onError(e));
        }).subscribeOn(Schedulers.io()).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                requestMembers.remove(member.getObjectId());
            }
        });
    }

    @Override
    public Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return Completable.create(emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
            DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
            map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
            map.put(BaseDataProvider.ACTION_ACTION, action.getValue());
            map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());
            map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());

            db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        }).subscribeOn(Schedulers.io()).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                requestMembers.remove(member.getObjectId());
            }
        });
    }

    @Override
    public Completable inviteConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return Completable.create(emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
            DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
            map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
            map.put(BaseDataProvider.ACTION_ACTION, action.getValue());
            map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Ing.getValue());
            map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());

            db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return Completable.create(emitter -> {
            db.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                    DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
                    DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

                    HashMap<String, Object> map = new HashMap<>();
                    map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
                    map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
                    map.put(BaseDataProvider.ACTION_ACTION, Action.ACTION.Invite.getValue());
                    map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Agree.getValue());
                    map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());
                    db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                            .add(map);

                    HashMap<String, Object> map2 = new HashMap<>();
                    map2.put(BaseDataProvider.MEMBER_ISSPEAKER, 1);
                    map2.put(BaseDataProvider.MEMBER_ROLE, member.getRole().getValue());
                    transaction.update(drMember, map2);
                    return null;
                }
            }).addOnSuccessListener(documentReference -> {
                emitter.onComplete();
            }).addOnFailureListener(e -> emitter.onError(e));
        }).subscribeOn(Schedulers.io()).doOnComplete(new io.reactivex.functions.Action() {
            @Override
            public void run() throws Exception {
                requestMembers.remove(member.getObjectId());
            }
        });
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return Completable.create(emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(member.getRoomId().getObjectId());
            DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());

            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.ACTION_MEMBERID, drMember);
            map.put(BaseDataProvider.ACTION_ROOMID, drRoom);
            map.put(BaseDataProvider.ACTION_ACTION, Action.ACTION.Invite.getValue());
            map.put(BaseDataProvider.ACTION_STATUS, Action.ACTION_STATUS.Refuse.getValue());
            map.put(BaseDataProvider.ACTION_CREATEDAT, Timestamp.now());

            db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectIdNew = documentReference.getId();
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> emitter.onError(e));
        });
    }

    @Override
    public Completable seatOff(@NonNull Member member, @NonNull Member.Role role) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(BaseDataProvider.MEMBER_ISSPEAKER, 0);
        map.put(BaseDataProvider.MEMBER_ROLE, role.getValue());

        return Completable.create(emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .document(member.getObjectId())
                    .update(map)
                    .addOnCompleteListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(e -> emitter.onError(e));
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

    ListenerRegistration lrAction;

    /**
     * 作为房主，需要监听房间中Action变化。
     */
    private void registerAnchorActionStatus() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }

        final DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
        lrAction = db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                .whereEqualTo(BaseDataProvider.ACTION_ROOMID, drRoom)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ActionTemp actionTemp = dc.getDocument().toObject(ActionTemp.class);
                            mLogger.d("AnchorActionChanged onEvent type= [%s] action= [%s]", dc.getType().toString(), actionTemp.toString());

                            Action item = actionTemp.create(dc.getDocument().getId());
                            item.setRoomId(iRoomProxy.getRoom());
                            actionTemp.getMemberId().get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (!task.isSuccessful()) {
                                                return;
                                            }

                                            DocumentSnapshot ds = task.getResult();
                                            if (!ds.exists()) {
                                                return;
                                            }

                                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                                if (item.getAction() == Action.ACTION.HandsUp
                                                        || item.getAction() == Action.ACTION.RequestLeft
                                                        || item.getAction() == Action.ACTION.RequestRight) {
                                                    if (item.getStatus() == Action.ACTION_STATUS.Ing.getValue()) {
                                                        Member member = iRoomProxy.getMemberById(ds.getId());
                                                        if (member == null) {
                                                            return;
                                                        }

                                                        if (requestMembers.containsKey(member.getObjectId())) {
                                                            return;
                                                        }

                                                        RequestMember requestMember = new RequestMember(member, item.getAction());
                                                        requestMembers.put(member.getObjectId(), requestMember);
                                                        iRoomProxy.onReceivedRequest(member, item.getAction());
                                                    }
                                                } else if (item.getAction() == Action.ACTION.Invite) {
                                                    if (item.getStatus() == Action.ACTION_STATUS.Agree.getValue()) {
                                                        Member member = iRoomProxy.getMemberById(ds.getId());
                                                        if (member == null) {
                                                            return;
                                                        }

                                                        iRoomProxy.onInviteAgree(member);
                                                    } else if (item.getStatus() == Action.ACTION_STATUS.Refuse.getValue()) {
                                                        Member member = iRoomProxy.getMemberById(ds.getId());
                                                        if (member == null) {
                                                            return;
                                                        }

                                                        iRoomProxy.onInviteRefuse(member);
                                                    }
                                                }
                                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void unregisterAnchorActionStatus() {
        if (lrAction != null) {
            lrAction.remove();
        }
    }

    ListenerRegistration lrAction2;

    /**
     * 作为观众，需要监听自己的Action变化。
     */
    private void registerMemberActionStatus() {
        Member member = iRoomProxy.getMine();
        if (member == null) {
            return;
        }

        final DocumentReference drMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER).document(member.getObjectId());
        lrAction2 = db.collection(BaseDataProvider.TAG_TABLE_ACTION)
                .whereEqualTo(BaseDataProvider.ACTION_MEMBERID, drMember)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ActionTemp actionTemp = dc.getDocument().toObject(ActionTemp.class);
                            mLogger.d("MemberActionChanged onEvent type= [%s] action= [%s]", dc.getType().toString(), actionTemp.toString());

                            Action item = actionTemp.create(dc.getDocument().getId());
                            item.setRoomId(iRoomProxy.getRoom());
                            item.setMemberId(iRoomProxy.getMine());
                            if (dc.getType() == DocumentChange.Type.ADDED) {
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
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            }
                        }
                    }
                });
    }

    private void unregisterMemberActionStatus() {
        if (lrAction2 != null) {
            lrAction2.remove();
        }
    }

    ListenerRegistration lrMember;

    /**
     * 监听房间内部成员信息变化
     */
    private void registerMemberChanged() {
        Room room = iRoomProxy.getRoom();
        if (room == null) {
            return;
        }

        final DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(room.getObjectId());
        lrMember = db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                .whereEqualTo(BaseDataProvider.MEMBER_ROOMID, drRoom)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            MemberTemp temp = dc.getDocument().toObject(MemberTemp.class);
                            mLogger.d("MemberChanged onEvent type= [%s] member= [%s]", dc.getType().toString(), temp.toString());

                            Member member = temp.createMember(dc.getDocument().getId());
                            member.setRoomId(iRoomProxy.getRoom());
                            temp.getUserId().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        return;
                                    }

                                    DocumentSnapshot ds = task.getResult();
                                    if (!ds.exists()) {
                                        return;
                                    }

                                    User user = ds.toObject(User.class);
                                    user.setObjectId(ds.getId());
                                    member.setUserId(user);

                                    if (dc.getType() == DocumentChange.Type.ADDED) {
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
                                    } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
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
                                    } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                        if (!iRoomProxy.isMembersContainsKey(member.getObjectId())) {
                                            return;
                                        }

                                        Member member2 = iRoomProxy.getMemberById(member.getObjectId());
                                        if (member2 == null) {
                                            return;
                                        }

                                        iRoomProxy.onMemberLeave(member2);
                                    }
                                }
                            });
                        }
                    }
                });
    }

    private void unregisterMemberChanged() {
        if (lrMember != null) {
            lrMember.remove();
        }
    }
}
