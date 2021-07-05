#ifndef AGORA_MEDIA_ENGINE_H
#define AGORA_MEDIA_ENGINE_H
#if defined _WIN32 || defined __CYGWIN__
typedef __int64 int64_t;
typedef unsigned __int64 uint64_t;
#else

#include <stdint.h>

#endif

namespace agora {
    namespace rtc {
        enum RAW_AUDIO_FRAME_OP_MODE_TYPE {
            READ = 0,
            WRITE = 1,
        };

        class IAudioFrameObserver {
        public:
            enum AUDIO_FRAME_TYPE {
                FRAME_TYPE_PCM16 = 0,  //PCM 16bit little endian
            };
            struct AudioFrame {
                AUDIO_FRAME_TYPE type;
                int samples;  //number of samples in this frame
                int bytesPerSample;  //number of bytes per sample: 2 for PCM16
                int channels;  //number of channels (data are interleaved if stereo)
                int samplesPerSec;  //sampling rate
                void *buffer;  //data buffer
                int64_t renderTimeMs;
                int avsync_type;
            };
        public:
            virtual bool onRecordAudioFrame(AudioFrame &audioFrame) = 0;

            virtual bool onPlaybackAudioFrame(AudioFrame &audioFrame) = 0;

            virtual bool onMixedAudioFrame(AudioFrame &audioFrame) = 0;

            virtual bool
            onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame &audioFrame) = 0;
        };

        class IMediaPlayer {
        public:
            virtual int registerAudioFrameObserver(IAudioFrameObserver *observer,
                                                   RAW_AUDIO_FRAME_OP_MODE_TYPE mode) = 0;

            virtual int unregisterAudioFrameObserver(IAudioFrameObserver *observer) = 0;
        };
    } //media
} //agora

#endif //AGORA_MEDIA_ENGINE_H
