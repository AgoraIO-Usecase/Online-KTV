# AgoraShareVideo_ijkplayer AgoraShareVideoAudience



*Read this in other languages: [English](README.en.md)*

这个开源示例项目演示了如何实现一起KTV的功能。

## 运行示例程序
首先在 [Agora.io 注册](https://dashboard.agora.io/cn/signup/) 注册账号，并创建自己的测试项目，获取到 AppID。将 AppID 填写进 MainViewController.m


```
[AgoraRtcEngineKit sharedEngineWithAppId:@"<#APP_ID#>" delegate:self] 

```

然后在 [Agora.io SDK](https://www.agora.io/cn/blog/download/) 下载 **视频通话 + 直播 SDK**，解压后将其中的 **libs/AgoraRtcEngineKit.framework** 分别复制到本项目的 “AgoraShareVideo_ijkplayer/AgoraShareVideo_ijkplayer” 和 “AgoraShareVideoAudience/AgoraShareVideoAudience” 文件夹下。

最后使用 XCode 打开 AgoraShareVideo_ijkplayer.xcodeproj 和 AgoraShareVideoAudience.xcodeproj 工程 ，连接 iPhone／iPad 测试设备，设置有效的开发者签名后即可运行。

## 实现KTV的方法
* 运行AgoraShareVideo_ijkplayer  点击开始播放
* 运行AgoraShareVideo_ijkplayer AgoraShareVideoAudience 分别连接1个手机 真机运行 加入到同一一个房间中 
* AgoraShareVideoAudience 加入相同的房间 观看KTV




## 运行环境
* XCode 8.0 +
* iOS 真机设备
* 不支持模拟器

## 联系我们

- 完整的 API 文档见 [文档中心](https://docs.agora.io/cn/)
- 如果在集成中遇到问题，你可以到 [开发者社区](https://dev.agora.io/cn/) 提问
- 如果有售前咨询问题，可以拨打 400 632 6626，或加入官方Q群 12742516 提问
- 如果需要售后技术支持，你可以在 [Agora Dashboard](https://dashboard.agora.io) 提交工单
- 如果发现了示例代码的bug，欢迎提交 [issue](https://github.com/AgoraIO/Agora-client-side-AV-capturing-for-streaming-iOS/issues)

## 代码许可

The MIT License (MIT).
