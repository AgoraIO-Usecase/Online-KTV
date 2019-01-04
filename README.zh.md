# Agora Online KTV（一起 KTV）

*其他语言：[English](README.md)*

## 场景描述
- 创建 KTV 房间，用户可以互相音视频沟通；
- 用户可以在 KTV 房间播放 MV 文件；
- 用户可以随着 MV 拿麦唱歌；
- 拿麦者可在演唱时自行调节 MV 伴奏与人声音量；

## 实现方案
下图为一起 KTV 场景的声网实现架构图：

![KTV 架构图](Image/ktv_together.png)

在这个实现架构图中：

- 演唱者（拿麦者）播放本地或者在线的 MV 文件；
- 演唱者的歌声与 MV 的音频伴奏通过 Agora Video SDK 在本地进行混音，并和 MV 的视频一起，传输至 Agora SD-RTN 传输网络；
- 听众通过 Agora SD-RTN 网络获取到演唱者歌声和 MV 伴奏的音频混音以及 MV 的视频画面。

## 运行示例程序
在 [Agora.io 用户注册页](https://dashboard.agora.io/cn/signup/) 注册账号，并创建自己的项目获取到 App ID。

下载 Agora [视频通话／视频直播 SDK](https://docs.agora.io/cn/Interactive%20Broadcast/downloads)。

#### Android
1. 将有效的 App ID 填写进本项目的 `Android/Agora-Online-KTV/app/src/main/res/values/strings_config.xml` 中。

	```
	<string name="agora_app_id"><#YOUR APP ID#></string>
	```

2. 解压下载到的 SDK 包，将其中的 `libs` 文件夹下的 `*.jar` 复制到本项目的 `Android/Agora-Online-KTV/app/libs` 下，其中的 `libs` 文件夹下的 `arm64-v8a/x86/armeabi-v7a` 复制到本项目的 `Android/Agora-Online-KTV/app/src/main/jniLibs` 下。

3. 使用 Android Studio 打开该项目，连接 Android 测试设备，编译并运行。

		运行环境:
	​	* Android SDK API Level >= 16
	​	* Android Studio 3.1 +
	​	* 支持语音和视频功能的真机
	​	* App 要求 Android 4.1 或以上设备

#### iOS
1. 将有效的 App ID 填写进 AgoraVideoViewController.m 中。

	```
	self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:<#APP_ID#> delegate:self];
	```
2. 解压下载到的 SDK 包，将其中的 `AgoraRtcEngineKit.framework` 复制到本项目的 `iOS/Agora-Online-KTV/Agora-Online-KTV` 目录下。

3. 使用 Xcode 打开 `iOS/Agora-Online-KTV/Agora-Online-KTV.xcodeproj`，连接 iOS 测试设备，设置有效的开发者签名后即可运行。

		运行环境：
	​	* Xcode 10.0 +
	​	* iOS 8.0 +

## 功能列表
一起 KTV 主要由两大部分功能组成。
#### 一、加入声网频道实现音视频通话

1. 创建 Agora 视频引擎对象
2. 启用视频模式 [enableVideo](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/enableVideo)
3. 设置视频分辨率 [setVideoProfile](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setVideoProfile:swapWidthAndHeight:)
4. 设置频道为直播模式 [setChannelProfile](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setChannelProfile:)
5. 设置用户角色 [setClientRole](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setClientRole:)，拿麦者：BROADCASTER，观众：AUDIENCE
6. 设置本地视频视图 [setupLocalVideo](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setupLocalVideo:)
7. 设置远端视频视图 [setupRemoteVideo](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setupRemoteVideo:)
8. 加入频道 [joinChannel](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/joinChannelByToken:channelId:info:uid:joinSuccess:)
9. 离开频道 [leaveChannel](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/leaveChannel:)
10. 停止视频预览 [stopPreview](https://docs.agora.io/cn/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/stopPreview)

#### 二、MV 文件播放和控制
本示例代码中抽象了一个 KTVKit 类，专门负责对 MV 的控制，以及将视频帧推送给 Agora SDK。

1. 创建 KTVKit，并传入 Agora SDK `create`。
2. 打开并播放文件 `openAndPlayVideoFile`
3. 暂停播放/继续播放 `pause`/`resume`
4. 停止 `stopPlayVideoFile`
5. 切音轨/切伴奏以及原音 `switchAudioTrack`
6. 重置音频缓冲区，切换拿麦者/观众时候调用 `resetAudioBuffer`
7. 调整人声大小，百分比参数 `adjustVoiceVolume`
8. 调整伴奏大小，百分比参数 `adjustAccompanyVolume`
9. 获取当前 MV 播放位置，以百分比返回 `getCurrentPosition`
10. 获取 MV 总长度，以毫秒为单位返回 `getDuration`

#### 进阶
1. 启用耳返功能，详见[耳返](https://docs.agora.io/cn/Interactive%20Broadcast/in-ear_android?platform=Android) 。

## 常见问题
1. 对 MP4 的文件大小有没有要求？文件大小对费用有没有影响？

   答：费用根据所用时长和发送分辨率等计算，跟文件大小无关，具体可以咨询商务。

2. 为集成 KTV 功能，从语音 SDK 改为视频通话 SDK，原来语音的接口兼容吗？

   答：是的，视频通话 SDK 完全兼容语音 SDK 的接口。

3. 是否支持 MKV 格式的文件？

   答：本示例程序中暂不支持，可以自行添加 ijkplayer 的 MKV 的扩展。

4. 使用自己的视频文件播放的时候，有时出现接收方的音频对不上。

   答：可能是因为视频文件实际音频采样率和接口中设置的采样率不一致导致的。

5. 怎么查看一个视频文件的采样率？

   答：可以通过 VLC 工具，或者 FFmpeg 相关命令。本样例视频的采样率是 48000 Hz。

6. 支持哪些采样率？

   答：常见的 8000、16000、32000、44100、48000 Hz 采样率都能支持。 

7. 怎么修改视频文件的采样率？

   答：可以使用 FFmpeg 的相关命令，比如把双音轨视频 MKV 格式转化为 44100 Hz 音频采样率的 MP4 格式：
   ​	ffmpeg -i ~/video.mkv -map 0:v -vcodec mpeg4 -map 0:a -acodec copy -ar 44100 -strict -2 output.mp4

8. 怎么控制采样点数？

   答：`setRecordingAudioFrameParameters` 接口传入的参数会影响最终采样的点数。采样点数 = 采样率 x 采样回调触发间隔时间 x 声道数。

9. 可以使用非 ijkplayer 的其他播放器吗？

   答：可以，只要按照同样的逻辑去获取音视频文件中的数据推给 Agora SDK 就行了。

10. 观众端只有声音没有视频是什么问题？

  答：检查下发送端有没有正确地将视频帧推送给 Agora SDK。

## 联系我们

- 完整的 API 文档见 [文档中心](https://docs.agora.io/cn/)。
- 如果在集成中遇到问题，你可以到 [开发者社区](https://dev.agora.io/cn/) 提问。
- 如果有售前咨询问题，可以拨打 400 632 6626，或加入官方 QQ 群 12742516 提问。
- 如果需要售后技术支持，你可以在 [Agora Dashboard](https://dashboard.agora.io) 提交工单。
- 如果发现了示例代码的 bug，欢迎提交 [issue](https://github.com/AgoraIO/Agora-Online-KTV/issues)。

## 代码许可

The MIT License (MIT).
