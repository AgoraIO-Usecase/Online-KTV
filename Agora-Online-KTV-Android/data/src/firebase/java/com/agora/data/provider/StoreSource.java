package com.agora.data.provider;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.model.Member;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.provider.model.MemberTemp;
import com.agora.data.provider.model.RoomTemp;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

class StoreSource extends BaseStoreSource {

    private FirebaseFirestore db;

    public StoreSource(@NonNull IConfigSource mIConfigSource) {
        super(mIConfigSource);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return Observable.create((ObservableOnSubscribe<User>) emitter -> {
            String objectId = user.getObjectId();

            if (TextUtils.isEmpty(objectId)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(BaseDataProvider.USER_NAME, user.getName());
                map.put(BaseDataProvider.USER_AVATAR, user.getAvatar());
                map.put(BaseDataProvider.USER_CREATEDAT, Timestamp.now());

                db.collection(BaseDataProvider.TAG_TABLE_USER)
                        .add(map)
                        .addOnSuccessListener(documentReference -> {
                            String objectIdNew = documentReference.getId();
                            user.setObjectId(objectIdNew);
                            emitter.onNext(user);
                        })
                        .addOnFailureListener(e -> emitter.onError(e));
            } else {
                HashMap<String, Object> map = new HashMap<>();
                map.put(BaseDataProvider.USER_NAME, user.getName());
                map.put(BaseDataProvider.USER_AVATAR, user.getAvatar());

                db.collection(BaseDataProvider.TAG_TABLE_USER)
                        .document(objectId)
                        .set(map)
                        .addOnSuccessListener(aVoid -> emitter.onNext(user))
                        .addOnFailureListener(new OnFailureListener() {

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                emitter.onError(e);
                            }
                        });
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return Observable.create((ObservableOnSubscribe<User>) emitter -> {
            String objectId = user.getObjectId();
            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.USER_NAME, user.getName());
            map.put(BaseDataProvider.USER_AVATAR, user.getAvatar());

            db.collection(BaseDataProvider.TAG_TABLE_USER)
                    .document(objectId)
                    .set(map)
                    .addOnSuccessListener(aVoid -> emitter.onNext(user))
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<List<Room>> getRooms() {
        return Maybe.create((MaybeOnSubscribe<List<Room>>) emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_ROOM)
                    .get()
                    .continueWithTask(new Continuation<QuerySnapshot, Task<List<Task<?>>>>() {
                        @Override
                        public Task<List<Task<?>>> then(@NonNull Task<QuerySnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            if (task.getResult() == null || task.getResult().isEmpty()) {
                                return Tasks.forResult(null);
                            }

                            List<Task<Room>> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                RoomTemp roomTemp = document.toObject(RoomTemp.class);
                                Room roomNew = roomTemp.createRoom(document.getId());
                                Task<Room> taskUser = roomTemp.getAnchorId().get().continueWith(new Continuation<DocumentSnapshot, Room>() {
                                    @Override
                                    public Room then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            throw task.getException();
                                        }

                                        DocumentSnapshot document = task.getResult();
                                        if (document == null || !document.exists()) {
                                            return null;
                                        }

                                        User user = document.toObject(User.class);
                                        user.setObjectId(document.getId());
                                        roomNew.setAnchorId(user);
                                        return roomNew;
                                    }
                                });
                                tasks.add(taskUser);
                            }
                            return Tasks.whenAllComplete(tasks);
                        }
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult() == null) {
                                emitter.onComplete();
                                return;
                            }

                            List<Room> rooms = new ArrayList<>();
                            for (Task<?> document : task.getResult()) {
                                rooms.add((Room) document.getResult());
                            }

                            emitter.onSuccess(rooms);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<Room> getRoomCountInfo(@NonNull Room room) {
        return Observable.create((ObservableOnSubscribe<Room>) emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            int members = task.getResult().size();
                            room.setMembers(members);
                            emitter.onNext(room);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<Room> getRoomSpeakersInfo(@NonNull Room room) {
        return Maybe.create((MaybeOnSubscribe<Room>) emitter -> {
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo(BaseDataProvider.MEMBER_ISSPEAKER, 1)
                    .limit(3)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            List<Member> members = new ArrayList<>();
                            List<Task<Void>> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MemberTemp memberTemp = document.toObject(MemberTemp.class);
                                Member member = memberTemp.createMember(document.getId());
                                members.add(member);

                                tasks.add(memberTemp.getUserId().get().continueWith(new Continuation<DocumentSnapshot, Void>() {
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
                                        member.setUserId(user);
                                        return null;
                                    }
                                }));
                            }
                            room.setSpeakers(members);

                            Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    emitter.onSuccess(room);
                                }
                            });
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<Room> creatRoom(@NonNull Room room) {
        return Observable.create((ObservableOnSubscribe<Room>) emitter -> {
            DocumentReference drUser = db.collection(BaseDataProvider.TAG_TABLE_USER).document(room.getAnchorId().getObjectId());

            HashMap<String, Object> map = new HashMap<>();
            map.put(BaseDataProvider.MEMBER_CHANNELNAME, room.getChannelName());
            map.put(BaseDataProvider.MEMBER_ANCHORID, drUser);
            map.put(BaseDataProvider.MEMBER_CREATEDAT, Timestamp.now());

            db.collection(BaseDataProvider.TAG_TABLE_ROOM)
                    .add(map)
                    .addOnSuccessListener(documentReference -> {
                        String objectId = documentReference.getId();
                        room.setObjectId(objectId);
                        emitter.onNext(room);
                    })
                    .addOnFailureListener(new OnFailureListener() {

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<Room> getRoom(@NonNull Room room) {
        return Maybe.create((MaybeOnSubscribe<Room>) emitter ->
                db.collection(BaseDataProvider.TAG_TABLE_ROOM)
                        .document(room.getObjectId())
                        .get()
                        .continueWithTask(new Continuation<DocumentSnapshot, Task<Room>>() {
                            @Override
                            public Task<Room> then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }

                                DocumentSnapshot document = task.getResult();
                                if (document == null || !document.exists()) {
                                    return Tasks.forResult(null);
                                }

                                RoomTemp roomTemp = document.toObject(RoomTemp.class);
                                Room roomNew = roomTemp.createRoom(document.getId());
                                Task<Room> taskUser = roomTemp.getAnchorId().get().continueWith(new Continuation<DocumentSnapshot, Room>() {
                                    @Override
                                    public Room then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                                        if (!task.isSuccessful()) {
                                            throw task.getException();
                                        }

                                        DocumentSnapshot document = task.getResult();
                                        if (document == null || !document.exists()) {
                                            return roomNew;
                                        }

                                        User user = document.toObject(User.class);
                                        user.setObjectId(document.getId());
                                        roomNew.setAnchorId(user);
                                        return roomNew;
                                    }
                                });
                                return taskUser;
                            }
                        })
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (task.getResult() == null) {
                                    emitter.onComplete();
                                    return;
                                }

                                emitter.onSuccess(task.getResult());
                            } else {
                                emitter.onError(task.getException());
                            }
                        })).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<Member>> getMembers(@NonNull Room room) {
        return Observable.create((ObservableOnSubscribe<List<Member>>) emitter -> {
            List<Member> members = new ArrayList<>();
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .get()
                    .continueWithTask(new Continuation<QuerySnapshot, Task<Void>>() {
                        @Override
                        public Task<Void> then(@NonNull Task<QuerySnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            if (task.getResult() == null || task.getResult().isEmpty()) {
                                return null;
                            }

                            List<Task<Void>> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MemberTemp memberTemp = document.toObject(MemberTemp.class);
                                Member member = memberTemp.createMember(document.getId());
                                members.add(member);

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
                                                member.setUserId(user);
                                                return null;
                                            }
                                        });
                                tasks.add(taskRoom);
                                tasks.add(taskUser);
                            }
                            return Tasks.whenAll(tasks);
                        }
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            emitter.onNext(members);
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId) {
        return Maybe.create((MaybeOnSubscribe<Member>) emitter -> {
            DocumentReference drRoom = db.collection(BaseDataProvider.TAG_TABLE_ROOM).document(roomId);
            DocumentReference drUser = db.collection(BaseDataProvider.TAG_TABLE_USER).document(userId);

            List<Member> members = new ArrayList<>();
            db.collection(BaseDataProvider.TAG_TABLE_MEMBER)
                    .whereEqualTo(BaseDataProvider.MEMBER_ROOMID, drRoom)
                    .whereEqualTo(BaseDataProvider.MEMBER_USERID, drUser)
                    .get()
                    .continueWithTask(new Continuation<QuerySnapshot, Task<Void>>() {
                        @Override
                        public Task<Void> then(@NonNull Task<QuerySnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                return Tasks.forResult(null);
                            }

                            if (task.getResult() == null || task.getResult().isEmpty()) {
                                return Tasks.forResult(null);
                            }

                            List<Task<Void>> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MemberTemp memberTemp = document.toObject(MemberTemp.class);
                                Member member = memberTemp.createMember(document.getId());
                                members.add(member);

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
                                                member.setUserId(user);
                                                return null;
                                            }
                                        });
                                tasks.add(taskRoom);
                                tasks.add(taskUser);
                                break;
                            }
                            return Tasks.whenAll(tasks);
                        }
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (members.isEmpty()) {
                                emitter.onComplete();
                                return;
                            }

                            for (Member member : members) {
                                emitter.onSuccess(member);
                                break;
                            }
                        } else {
                            emitter.onError(task.getException());
                        }
                    });
        }).subscribeOn(Schedulers.io());
    }
}
