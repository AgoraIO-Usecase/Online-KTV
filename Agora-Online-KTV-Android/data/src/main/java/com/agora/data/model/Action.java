package com.agora.data.model;

import java.io.Serializable;

public class Action implements Serializable {
    public static final String TABLE_NAME = "ACTION";

    private String objectId;
    private Member memberId;
    private Room roomId;
    private ACTION action;
    private int status;

    public Room getRoomId() {
        return roomId;
    }

    public void setRoomId(Room roomId) {
        this.roomId = roomId;
    }

    public Member getMemberId() {
        return memberId;
    }

    public void setMemberId(Member memberId) {
        this.memberId = memberId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public ACTION getAction() {
        return action;
    }

    public void setAction(ACTION action) {
        this.action = action;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Action action = (Action) o;

        return objectId.equals(action.objectId);
    }

    @Override
    public int hashCode() {
        return objectId.hashCode();
    }

    public enum ACTION implements Serializable {
        HandsUp(1), Invite(2), RequestLeft(3), RequestRight(4);

        private int value;

        ACTION(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ACTION parse(int value) {
            if (value == 1) {
                return HandsUp;
            } else if (value == 2) {
                return Invite;
            } else if (value == 3) {
                return RequestLeft;
            } else if (value == 4) {
                return RequestRight;
            }
            return HandsUp;
        }
    }

    public enum ACTION_STATUS {
        Ing(1), Agree(2), Refuse(3);

        private int value;

        ACTION_STATUS(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
