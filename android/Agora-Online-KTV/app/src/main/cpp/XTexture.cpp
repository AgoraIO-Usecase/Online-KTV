//
// Created by zhanxiaochao on 2018/5/5.
//

#include "XTexture.h"
#include "XShader.h"
#include "XEGL.h"
#include "AgoraLog.h"

class CXTexture:public XTexture
{
public:
    XShader sh;
    XTextureType type;
    std::mutex mux;
    virtual void Drop(){
        mux.lock();
        XEGL::Get()->Close();
        sh.Close();
        mux.unlock();
        delete this;
    }
    virtual bool Init(void *win ,XTextureType type)
    {
        mux.lock();
        XEGL::Get()->Close();
        sh.Close();
        this->type = type;
        if(!win)
        {
            mux.unlock();
            XLOGE("win is NULL");
            return false;
        }
        if(!XEGL::Get()->Init(win)){
            mux.unlock();
            return false;
        }
        sh.Init((XShaderType)type);
        mux.unlock();
        return true;

    }
    virtual void Draw(unsigned char*data[],int width,int height){

        mux.lock();
        sh.GetTexture(0,width,height,data[0]);
        if(type == XTEXTURE_YUV420P)
        {
            sh.GetTexture(1,width/2,height/2,data[1]);//u
            sh.GetTexture(2,width/2,height/2,data[2]);//v
        } else{
            sh.GetTexture(1,width/2,height/2,data[1], true);//uv
        }
        sh.Draw();
        XEGL::Get()->Draw();
        mux.unlock();
    }

};
XTexture *XTexture::Create() {
    return  new CXTexture();
}