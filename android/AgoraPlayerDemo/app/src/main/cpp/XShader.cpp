//
// Created by zhanxiaochao on 2018/5/5.
//

#include "XShader.h"
#include "AgoraLog.h"
#include <GLES2/gl2.h>
//顶点着色器glsl
#define GET_STR(x) #x
static const char *vertexShader = GET_STR(
        attribute vec4 aPosition;//顶点坐标
        attribute vec2 aTexCoord; //材质顶点坐标
        varying vec2 vTexCoord;//输出材质坐标
        void main(){
            vTexCoord = vec2(aTexCoord.x,1.0-aTexCoord.y);
            gl_Position = aPosition;
        }
);
//片元着色器 软解码和部分x86解码
static const char *fragYUV420P = GET_STR(
    precision mediump float;//精度
    varying vec2 vTexCoord;//顶点着色器传递的坐标
    uniform sampler2D yTexture;
    uniform sampler2D uTexture;
    uniform sampler2D vTexture;
    void main(){
        vec3 yuv;
        vec3 rgb;
        yuv.r = texture2D(yTexture,vTexCoord).r;
        yuv.g = texture2D(uTexture,vTexCoord).r - 0.5;
        yuv.b = texture2D(vTexture,vTexCoord).r - 0.5;
        rgb = mat3(1.0,     1.0,    1.0,
                   0.0,-0.39465,2.03211,
                   1.13983,-0.58060,0.0)*yuv;
        //输出像素颜色
        gl_FragColor = vec4(rgb,1.0);
    }
);
//片元着色器,软解码和部分x86硬解码
static const char *fragNV12 = GET_STR(
        precision mediump float;    //精度
        varying vec2 vTexCoord;     //顶点着色器传递的坐标
        uniform sampler2D yTexture; //输入的材质（不透明灰度，单像素）
        uniform sampler2D uvTexture;
        void main(){
            vec3 yuv;
            vec3 rgb;
            yuv.r = texture2D(yTexture,vTexCoord).r;
            yuv.g = texture2D(uvTexture,vTexCoord).r - 0.5;
            yuv.b = texture2D(uvTexture,vTexCoord).a - 0.5;
            rgb = mat3(1.0,     1.0,    1.0,
                       0.0,-0.39465,2.03211,
                       1.13983,-0.58060,0.0)*yuv;
            //输出像素颜色
            gl_FragColor = vec4(rgb,1.0);
        }
);

//片元着色器,软解码和部分x86硬解码
static const char *fragNV21 = GET_STR(
        precision mediump float;    //精度
        varying vec2 vTexCoord;     //顶点着色器传递的坐标
        uniform sampler2D yTexture; //输入的材质（不透明灰度，单像素）
        uniform sampler2D uvTexture;
        void main(){
            vec3 yuv;
            vec3 rgb;
            yuv.r = texture2D(yTexture,vTexCoord).r;
            yuv.g = texture2D(uvTexture,vTexCoord).a - 0.5;
            yuv.b = texture2D(uvTexture,vTexCoord).r - 0.5;
            rgb = mat3(1.0,     1.0,    1.0,
                       0.0,-0.39465,2.03211,
                       1.13983,-0.58060,0.0)*yuv;
            //输出像素颜色
            gl_FragColor = vec4(rgb,1.0);
        }
);
static GLuint InitShader(const char * code,GLint type){
    //创建Shader
    GLuint sh = glCreateShader(type);
    if(sh == 0){
        XLOGE("glCreateShader failed!");
        return  0;
    }
    //加载shader
    glShaderSource(sh,
                    1,
                   &code,
                    0
    );

    glCompileShader(sh);

    //获取编译情况
    GLint  status;
    glGetShaderiv(sh,GL_COMPILE_STATUS,&status);
    if(status == 0)
    {
        XLOGE("glGetShaderiv failed!");
        return  0;
    }
    XLOGE("glGetShaderiv success!");
    return sh;
}
void XShader::Close() {
    mux.lock();
    if(program){
        glDeleteProgram(program);
    }
    if (fsh){
        glDeleteShader(fsh);
    }
    if (vsh){
        glDeleteShader(vsh);
    }
    for (int i = 0; i < sizeof(texts)/ sizeof(unsigned int); ++i) {
        if(texts[i]){
            glDeleteTextures(1,&texts[i]);
        }
        texts[i] = 0;
    }
    mux.unlock();
}
bool XShader::Init(XShaderType type) {
    Close();
    mux.lock();
    //顶点和片元shader初始化
    vsh = InitShader(vertexShader,GL_VERTEX_SHADER);
    if(vsh == 0){
        mux.unlock();
        XLOGE("initShader GL_VERTEX_SHADER failded");
        return false;
    }
    XLOGE("InitShader GL_VERTEX_SHADER SUCCESS! %d ",type);
    //片元着色器初始化
    switch(type)
    {
        case XSHADER_YUV420P:
            fsh = InitShader(fragYUV420P,GL_FRAGMENT_SHADER);
            break;
        case XSHADER_NV12:
            fsh = InitShader(fragNV12,GL_FRAGMENT_SHADER);
            break;
        case XSHADER_NV21:
            fsh = InitShader(fragNV21,GL_FRAGMENT_SHADER);
            break;
        default:
            mux.unlock();
            XLOGE("XSHADER format is error");
            break;
    }
    if(fsh == 0){
        mux.unlock();
        XLOGE("InitShader GL_FRAGEMENT FAILED!");
        return false;
    }
    XLOGE("InitShader GL_FRAGMENt SUCCESS!");
    //////////////////////////
    //创建渲染程序
    program = glCreateProgram();
    if(program == 0)
    {
        mux.unlock();
        XLOGE("glCreateProgram failed!");
        return false;
    }
    //渲染程序加入着色器代码
    glAttachShader(program,vsh);
    glAttachShader(program,fsh);

    //链接程序
    glLinkProgram(program);
    GLint status = 0;
    //判断连接程序的状态
    glGetProgramiv(program,GL_LINK_STATUS,&status);
    if(status != GL_TRUE){
        mux.unlock();
        XLOGE("glLinkProgram failed!");
        return false;
    }
    glUseProgram(program);
    XLOGE("glinkProgram successs!");
    //////////////////////////////////


    //加入三维顶点数 两个三角形组成正方形
    static float vers[] ={
            1.0f,-1.0f,0.0f,
            -1.0f,-1.0f,0.0f,
            1.0f,1.0f,0.0f,
            -1.0f,1.0f,0.0f,
    };
    GLuint apos = (GLuint)glGetAttribLocation(program,"aPosition");
    glEnableVertexAttribArray(apos);
    //传递顶点
    glVertexAttribPointer(apos,3,GL_FLOAT,GL_FALSE,12,vers);

    //加入材质坐标数据
    static float txts[]={
            1.0f,0.0f , //右下
            0.0f,0.0f,
            1.0f,1.0f,
            0.0,1.0
    };
    GLuint  atex = (GLuint)glGetAttribLocation(program,"aTexCoord");
    glEnableVertexAttribArray(atex);
    glVertexAttribPointer(atex,3,GL_FLOAT,GL_FALSE,8,txts);


    //材质纹理初始化
    //设置纹理
    glUniform1i(glGetUniformLocation(program,"yTexture"),0);//渲染纹理第一层
    switch (type){
        case XSHADER_YUV420P:
            glUniform1i(glGetUniformLocation(program,"uTexture"),1);//对于纹理第二层
            glUniform1i(glGetUniformLocation(program,"vTexture"),2);//对于纹理第三层
            break;
        case XSHADER_NV12:
        case XSHADER_NV21:
            glUniform1i(glGetUniformLocation(program,"uvTexture"),1);//纹理第二层
            break;
    }
    XLOGE("Init Shader Success!");
    mux.unlock();
    return true;
}
void XShader::Draw() {
    mux.lock();
    if(!program){
        mux.unlock();
        return;;
    }
    glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    mux.unlock();
}
void XShader::GetTexture(unsigned int index, int width, int height, unsigned char *buf, bool isa) {
    mux.lock();
    unsigned int format = GL_LUMINANCE;
    if(isa)
    {
        format = GL_LUMINANCE_ALPHA;
    }
    if (texts[index] == 0) {
        //材质初始化
        glGenTextures(1, &texts[index]);
        //设置纹理属性
        glBindTexture(GL_TEXTURE_2D, texts[index]);
        //缩小 放大过滤器
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        //设置纹理的格式和大小
        glTexImage2D(GL_TEXTURE_2D,
                     0,
                     format,
                     width,// 拉伸到全屏
                     height,//
                     0,//边框
                     format,//数据的像素格式
                     GL_UNSIGNED_BYTE,//像素的数据类型
                     NULL   //纹理的数据
        );
    }
    //激活到第一层纹理 绑定到第一层纹理
    glActiveTexture(GL_TEXTURE0 + index);
    glBindTexture(GL_TEXTURE_2D,texts[index]);
    //替换纹理内容
    glTexSubImage2D(GL_TEXTURE_2D,0,0,0,width,height,format,GL_UNSIGNED_BYTE,buf);
    mux.unlock();

}
