package com.agora.data;

import java.util.List;

import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMessage;

/**
 * @author chenhengfei(Aslanchen)
 */
public class SimpleRtmChannelListener implements RtmChannelListener {
    @Override
    public void onMemberCountUpdated(int i) {

    }

    @Override
    public void onAttributesUpdated(List<RtmChannelAttribute> list) {

    }

    @Override
    public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember) {

    }

    @Override
    public void onImageMessageReceived(RtmImageMessage rtmImageMessage, RtmChannelMember rtmChannelMember) {

    }

    @Override
    public void onFileMessageReceived(RtmFileMessage rtmFileMessage, RtmChannelMember rtmChannelMember) {

    }

    @Override
    public void onMemberJoined(RtmChannelMember rtmChannelMember) {

    }

    @Override
    public void onMemberLeft(RtmChannelMember rtmChannelMember) {

    }
}
