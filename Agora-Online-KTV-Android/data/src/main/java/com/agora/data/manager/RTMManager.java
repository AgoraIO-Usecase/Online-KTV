package com.agora.data.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.agora.data.BaseError;
import com.agora.data.R;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public final class RTMManager {
    private Logger.Builder mLogger = XLog.tag(RTMManager.class.getSimpleName());

    private Context mContext;
    private volatile static RTMManager instance;

    private RtmClient mRtmClient;

    private RTMManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static RTMManager Instance(Context context) {
        if (instance == null) {
            synchronized (RTMManager.class) {
                if (instance == null)
                    instance = new RTMManager(context);
            }
        }
        return instance;
    }

    public void init() {
        String appid = mContext.getString(R.string.app_id);
        if (TextUtils.isEmpty(appid)) {
            throw new NullPointerException("please check \"strings_config.xml\"");
        }

        try {
            mRtmClient = RtmClient.createInstance(mContext, appid,
                    new RtmClientListener() {

                        @Override
                        public void onConnectionStateChanged(int state, int reason) {
                            mLogger.d("onConnectionStateChanged() called with: state = [" + state + "], reason = [" + reason + "]");
                        }

                        @Override
                        public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                            mLogger.d("onMessageReceived() called with: rtmMessage = [" + rtmMessage.getText() + "], peerId = [" + peerId + "]");
                        }

                        @Override
                        public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String peerId) {
                            mLogger.d("onImageMessageReceivedFromPeer() called with: rtmImageMessage = [" + rtmImageMessage + "], s = [" + peerId + "]");
                        }

                        @Override
                        public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String peerId) {
                            mLogger.d("onFileMessageReceivedFromPeer() called with: rtmFileMessage = [" + rtmFileMessage + "], s = [" + peerId + "]");
                        }

                        @Override
                        public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
                            mLogger.d("onMediaUploadingProgress() called with: rtmMediaOperationProgress = [" + rtmMediaOperationProgress + "], l = [" + l + "]");
                        }

                        @Override
                        public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
                            mLogger.d("onMediaDownloadingProgress() called with: rtmMediaOperationProgress = [" + rtmMediaOperationProgress + "], l = [" + l + "]");
                        }

                        @Override
                        public void onTokenExpired() {
                            mLogger.d("onTokenExpired() called");
                        }

                        @Override
                        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {
                            mLogger.d("onPeersOnlineStatusChanged() called with: map = [" + map + "]");
                        }
                    });
        } catch (Exception e) {
            mLogger.e("init error,", e);
        }
    }

    public Completable login(String userId) {
        mLogger.d("login() called with: userId = [%s]", userId);
        return Completable.create(emitter -> {
            if (mRtmClient == null) {
                emitter.onError(new NullPointerException("mRtmClient is null, please call init first"));
                return;
            }

            mRtmClient.login(null, userId, new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void responseInfo) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    mLogger.e("login onFailure: %s", errorInfo);
                    emitter.onError(new BaseError(errorInfo.getErrorCode(), errorInfo.getErrorDescription()));
                }
            });
        }).subscribeOn(Schedulers.io());
    }

    public Completable logout() {
        return Completable.create(emitter -> {
            mLogger.d("logout() called");
            if (mRtmClient == null) {
                emitter.onComplete();
            }

            mRtmClient.logout(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    mLogger.e("logout onFailure: %s", errorInfo);
                }
            });
            emitter.onComplete();
        }).subscribeOn(Schedulers.io());
    }

    private final List<RtmChannelListener> channelListeners = new ArrayList<>();
    private final RtmChannelListener mRtmChannelListener = new RtmChannelListener() {
        @Override
        public void onMemberCountUpdated(int i) {
            mLogger.d("onMemberCountUpdated() called with: i = [" + i + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onMemberCountUpdated(i);
            }
        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {
            mLogger.d("onAttributesUpdated() called with: list = [" + list + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onAttributesUpdated(list);
            }
        }

        @Override
        public void onMessageReceived(RtmMessage message, RtmChannelMember fromMember) {
            mLogger.d("onMessageReceived() called with: message = [" + message.getText() + "], fromMember = [" + fromMember.getUserId() + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onMessageReceived(message, fromMember);
            }
        }

        @Override
        public void onImageMessageReceived(RtmImageMessage rtmImageMessage, RtmChannelMember rtmChannelMember) {
            mLogger.d("onImageMessageReceived() called with: rtmImageMessage = [" + rtmImageMessage + "], rtmChannelMember = [" + rtmChannelMember + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onImageMessageReceived(rtmImageMessage, rtmChannelMember);
            }
        }

        @Override
        public void onFileMessageReceived(RtmFileMessage rtmFileMessage, RtmChannelMember rtmChannelMember) {
            mLogger.d("onFileMessageReceived() called with: rtmFileMessage = [" + rtmFileMessage + "], rtmChannelMember = [" + rtmChannelMember + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onFileMessageReceived(rtmFileMessage, rtmChannelMember);
            }
        }

        @Override
        public void onMemberJoined(RtmChannelMember member) {
            mLogger.d("onMemberJoined() called with: member = [" + member + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onMemberJoined(member);
            }
        }

        @Override
        public void onMemberLeft(RtmChannelMember member) {
            mLogger.d("onMemberLeft() called with: member = [" + member + "]");
            for (RtmChannelListener channelListener : channelListeners) {
                channelListener.onMemberLeft(member);
            }
        }
    };

    public void addChannelListeners(@NonNull RtmChannelListener channelListener) {
        this.channelListeners.add(channelListener);
    }

    public void removeChannelListeners(@NonNull RtmChannelListener channelListener) {
        this.channelListeners.remove(channelListener);
    }

    private volatile RtmChannel mRtmChannel;

    public Completable joinChannel(String channelId) {
        mLogger.d("joinChannel() called with: channelId = [" + channelId + "]");
        return Completable.create(emitter -> {
            if (mRtmClient == null) {
                emitter.onError(new NullPointerException("mRtmClient is null, please call init first"));
                return;
            }

            mRtmChannel = mRtmClient.createChannel(channelId, mRtmChannelListener);
            mRtmChannel.join(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    mLogger.e("joinChannel onFailure: %s", errorInfo);
                    emitter.onError(new BaseError(errorInfo.getErrorCode(), errorInfo.getErrorDescription()));
                }
            });
        }).subscribeOn(Schedulers.io());
    }

    public Completable leaveChannel() {
        mLogger.d("leaveChannel() called");
        return Completable.create(emitter -> {
            if (mRtmChannel == null) {
                mLogger.e("mRtmChannel is null");
                emitter.onComplete();
                return;
            }

            mRtmChannel.leave(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    mLogger.e("leaveChannel onFailure: %s", errorInfo);
                    emitter.onError(new BaseError(errorInfo.getErrorCode(), errorInfo.getErrorDescription()));
                }
            });
        }).subscribeOn(Schedulers.io());
    }

    public Completable sendChannelMessage(String msg) {
        return Completable.create(emitter -> {
            if (mRtmChannel == null) {
                emitter.onError(new NullPointerException("mRtmChannel is null, please call create first"));
                return;
            }

            RtmMessage message = mRtmClient.createMessage();
            message.setText(msg);

            mRtmChannel.sendMessage(message, new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    emitter.onComplete();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    mLogger.e("sendChannelMessage onFailure: %s", errorInfo);
                    emitter.onError(new BaseError(errorInfo.getErrorCode(), errorInfo.getErrorDescription()));
                }
            });
        }).subscribeOn(Schedulers.io());
    }
}
