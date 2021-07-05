#include <jni.h>
#include <android/log.h>
#include <cstring>
#include "include/IMediaPlayer.h"
#include <string.h>
#include "io_agora_MusicPlayProcessing.h"

#include <map>

using namespace std;

class AgoraAudioFrameObserver : public agora::rtc::IAudioFrameObserver {

public:
    AgoraAudioFrameObserver() {

    }

    ~AgoraAudioFrameObserver() {
    }

    void writebackAudioFrame(AudioFrame &audioFrame, void *byteBuffer) {
        if (byteBuffer == nullptr) {
            return;
        }

        int len = audioFrame.samples * audioFrame.bytesPerSample;
        memcpy(audioFrame.buffer, byteBuffer, (size_t) len);
    }

public:
    bool onRecordAudioFrame(AudioFrame &audioFrame) override {
        return true;
    }

    bool onPlaybackAudioFrame(AudioFrame &audioFrame) override {
        return true;
    }

    bool onMixedAudioFrame(AudioFrame &audioFrame) override {
        return true;
    }

    bool onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame &audioFrame) override {
        return true;
    }
};


static AgoraAudioFrameObserver s_audioFrameObserver;

static agora::rtc::IMediaPlayer *mediaPlayer = nullptr;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_io_agora_MusicPlayProcessing_start(JNIEnv *, jclass, jint mediaPlayerId) {
    mediaPlayer = (agora::rtc::IMediaPlayer *) mediaPlayerId;
    mediaPlayer->registerAudioFrameObserver(&s_audioFrameObserver,
                                            agora::rtc::RAW_AUDIO_FRAME_OP_MODE_TYPE::WRITE);
}

JNIEXPORT void JNICALL Java_io_agora_MusicPlayProcessing_stop(JNIEnv *, jclass) {
    if (!mediaPlayer) return;

    mediaPlayer->unregisterAudioFrameObserver(&s_audioFrameObserver);
    mediaPlayer = nullptr;
}

JNIEXPORT void JNICALL
Java_io_agora_MusicPlayProcessing_change(JNIEnv *, jclass, jint channelIndex) {

}

#ifdef __cplusplus
}
#endif
