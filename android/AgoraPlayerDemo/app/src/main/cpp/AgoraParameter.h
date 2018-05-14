//
// Created by 湛孝超 on 2018/5/4.
//

#ifndef AGORAPLAYERDEMO_AGORAPARAMETER_H
#define AGORAPLAYERDEMO_AGORAPARAMETER_H

struct AVCodecParameters;
class AgoraParameter
{
public:
    AVCodecParameters *para = 0;
    int channels = 2;
    int sample_rate = 44100;
};


#endif //AGORAPLAYERDEMO_AGORAPARAMETER_H
