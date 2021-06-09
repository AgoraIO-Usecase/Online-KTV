package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.agora.data.model.Action;
import com.agora.data.model.Member;
import com.agora.data.model.RequestMember;
import com.agora.data.model.Room;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface IMessageSource {

    Observable<Member> joinRoom(@NonNull Room room, @NonNull Member member);

    Completable leaveRoom(@NonNull Room room, @NonNull Member member);

    Completable muteVoice(@NonNull Member member, int muted);

    Completable muteSelfVoice(@NonNull Member member, int muted);

    Completable requestConnect(@NonNull Member member, @NonNull Action.ACTION action);

    Completable agreeRequest(@NonNull Member member, @NonNull Action.ACTION action);

    Completable refuseRequest(@NonNull Member member, @NonNull Action.ACTION action);

    Completable inviteConnect(@NonNull Member member, @NonNull Action.ACTION action);

    Completable agreeInvite(@NonNull Member member);

    Completable refuseInvite(@NonNull Member member);

    Completable seatOff(@NonNull Member member, @NonNull Member.Role role);

    Observable<List<RequestMember>> getRequestList();

    int getHandUpListCount();
}
