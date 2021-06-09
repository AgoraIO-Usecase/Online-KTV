package io.agora.ktv.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.IDataRepositroy;
import com.agora.data.manager.RoomManager;
import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.agora.data.model.Room;
import com.agora.data.model.User;
import com.agora.data.provider.BaseDataProvider;
import com.agora.data.provider.IDataProvider;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

public class DataRepositroy implements IDataRepositroy {
    private static final String TAG = DataRepositroy.class.getSimpleName();

    private volatile static DataRepositroy instance;

    private Context mContext;

    private final IDataProvider mDataProvider;

    private DataRepositroy(Context context) {
        mContext = context.getApplicationContext();

        mDataProvider = new BaseDataProvider(context, RoomManager.Instance(mContext));
    }

    public static synchronized DataRepositroy Instance(Context context) {
        if (instance == null) {
            synchronized (DataRepositroy.class) {
                if (instance == null)
                    instance = new DataRepositroy(context);
            }
        }
        return instance;
    }

    @Override
    public Observable<User> login(@NonNull User user) {
        return mDataProvider.getStoreSource().login(user);
    }

    @Override
    public Observable<User> update(@NonNull User user) {
        return mDataProvider.getStoreSource().update(user);
    }

    @Override
    public Maybe<List<Room>> getRooms() {
        return mDataProvider.getStoreSource().getRooms();
    }

    @Override
    public Observable<Room> getRoomCountInfo(@NonNull Room room) {
        return mDataProvider.getStoreSource().getRoomCountInfo(room);
    }

    @Override
    public Maybe<Room> getRoomSpeakersInfo(@NonNull Room room) {
        return mDataProvider.getStoreSource().getRoomSpeakersInfo(room);
    }

    @Override
    public Observable<Room> creatRoom(@NonNull Room room) {
        return mDataProvider.getStoreSource().creatRoom(room);
    }

    @Override
    public Maybe<Room> getRoom(@NonNull Room room) {
        return mDataProvider.getStoreSource().getRoom(room);
    }

    @Override
    public Observable<List<Member>> getMembers(@NonNull Room room) {
        return mDataProvider.getStoreSource().getMembers(room);
    }

    @Override
    public Maybe<Member> getMember(@NonNull String roomId, @NonNull String userId) {
        return mDataProvider.getStoreSource().getMember(roomId, userId);
    }

    @Override
    public Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member) {
        return mDataProvider.getMessageSource().joinRoom(room, member);
    }

    @Override
    public Completable leaveRoom(@NonNull Room room, @NonNull Member member) {
        return mDataProvider.getMessageSource().leaveRoom(room, member);
    }

    @Override
    public Completable muteVoice(@NonNull Member member, int muted) {
        return mDataProvider.getMessageSource().muteVoice(member, muted);
    }

    @Override
    public Completable muteSelfVoice(@NonNull Member member, int muted) {
        return mDataProvider.getMessageSource().muteSelfVoice(member, muted);
    }

    @Override
    public Completable requestConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return mDataProvider.getMessageSource().requestConnect(member, action);
    }

    @Override
    public Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return mDataProvider.getMessageSource().agreeRequest(member, action);
    }

    @Override
    public Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action) {
        return mDataProvider.getMessageSource().refuseRequest(member, action);
    }

    @Override
    public Completable inviteConnect(@NonNull Member member, @NonNull Action.ACTION action) {
        return mDataProvider.getMessageSource().inviteConnect(member, action);
    }

    @Override
    public Completable agreeInvite(@NonNull Member member) {
        return mDataProvider.getMessageSource().agreeInvite(member);
    }

    @Override
    public Completable refuseInvite(@NonNull Member member) {
        return mDataProvider.getMessageSource().refuseInvite(member);
    }

    @Override
    public Completable seatOff(@NonNull Member member, @NonNull Member.Role role) {
        return mDataProvider.getMessageSource().seatOff(member, role);
    }

    @Override
    public Observable<List<RequestMember>> getRequestList() {
        return mDataProvider.getMessageSource().getRequestList();
    }

    @Override
    public int getHandUpListCount() {
        return mDataProvider.getMessageSource().getHandUpListCount();
    }
}
