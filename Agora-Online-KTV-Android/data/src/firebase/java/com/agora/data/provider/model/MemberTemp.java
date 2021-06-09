package com.agora.data.provider.model;

import com.agora.data.model.Member;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class MemberTemp {
    private DocumentReference roomId;
    private DocumentReference userId;
    private Long streamId;
    private int isSpeaker = 0;
    private int role = 0;
    private int isMuted = 0;
    private int isSelfMuted = 0;
    private Timestamp createdAt;

    public MemberTemp() {
    }

    public Member createMember(String objectId) {
        Member member = new Member();
        member.setObjectId(objectId);
        member.setStreamId(streamId);
        member.setIsSpeaker(isSpeaker);
        member.setIsMuted(isMuted);
        member.setIsSelfMuted(isSelfMuted);
        member.setRole(Member.Role.parse(role));

        return member;
    }

    public DocumentReference getRoomId() {
        return roomId;
    }

    public void setRoomId(DocumentReference roomId) {
        this.roomId = roomId;
    }

    public DocumentReference getUserId() {
        return userId;
    }

    public void setUserId(DocumentReference userId) {
        this.userId = userId;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public int getIsSpeaker() {
        return isSpeaker;
    }

    public void setIsSpeaker(int isSpeaker) {
        this.isSpeaker = isSpeaker;
    }

    public int getIsMuted() {
        return isMuted;
    }

    public void setIsMuted(int isMuted) {
        this.isMuted = isMuted;
    }

    public int getIsSelfMuted() {
        return isSelfMuted;
    }

    public void setIsSelfMuted(int isSelfMuted) {
        this.isSelfMuted = isSelfMuted;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
