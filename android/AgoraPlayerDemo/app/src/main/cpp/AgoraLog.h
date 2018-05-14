//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_AGORALOG_H
#define AGORAPLAYERDEMO_AGORALOG_H


class AgoraLog {

};
#ifdef ANDROID
#include <android/log.h>
#define XLOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"XPlay",__VA_ARGS__)
#define XLOGI(...) __android_log_print(ANDROID_LOG_INFO,"XPlay",__VA_ARGS__)
#define XLOGE(...) __android_log_print(ANDROID_LOG_ERROR,"XPlay",__VA_ARGS__)
#else
#define XLOGD(...) printf("XPlay",__VA_ARGS__)
#define XLOGI(...) printf("XPlay",__VA_ARGS__)
#define XLOGE(...) printf("XPlay",__VA_ARGS__)

#endif


#endif //AGORAPLAYERDEMO_AGORALOG_H
