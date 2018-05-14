//
// Created by zhanxiaochao on 2018/5/5.
//

#ifndef AGORAPLAYERDEMO_XTEXTURE_H
#define AGORAPLAYERDEMO_XTEXTURE_H

enum XTextureType
{
    XTEXTURE_YUV420P = 0,
    XTEXTURE_NV12 = 25,
    XTEXTURE_NV21 = 26

};
class XTexture {
public:
    static XTexture *Create();
    virtual bool  Init(void *win ,XTextureType type = XTEXTURE_YUV420P) = 0;
    virtual void  Draw(unsigned char *data[],int width,int height) = 0;
    virtual void Drop() = 0;
    virtual ~XTexture(){};
protected:
    XTexture(){};
};


#endif //AGORAPLAYERDEMO_XTEXTURE_H
