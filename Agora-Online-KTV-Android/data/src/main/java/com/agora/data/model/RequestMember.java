package com.agora.data.model;

public class RequestMember {
    private Member member;
    private Action.ACTION action;

    public RequestMember(Member member, Action.ACTION action) {
        this.member = member;
        this.action = action;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Action.ACTION getAction() {
        return action;
    }

    public void setAction(Action.ACTION action) {
        this.action = action;
    }
}
