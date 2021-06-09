package com.agora.data.provider;

import io.agora.rtc.RtcEngine;

/**
 * 每个场景下房间配置各有差异，所以进行独立配置。
 *
 * @author chenhengfei(Aslanchen)
 * @date {date}
 */
public interface IRoomConfigProvider {
    void setup(RtcEngine mRtcEngine);

    boolean isNeedVideo();

    boolean isNeedAudio();
}
