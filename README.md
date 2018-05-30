# AgoraShareVideoBroadcasting AgoraShareVideoAudience

*其他语言版本： [简体中文](README.md)*

The client show the method of how to KTV funtion base on Agora SDK. 
This open source include 2 project folders:
     Broadcast side AgoraShareVideoBroadcasting: project folder is AgoraShareVideo_ijkplayer
     Audience side AgoraShareVideoAudience: project folder is AgoraShareVideoAudience

## Running the App
First, create a developer account at [Agora.io](https://dashboard.agora.io/signin/), and obtain an App ID. Update "ViewController.m" with your App ID.  (2 projects need add appid follows the same step as below)

```
[AgoraRtcEngineKit sharedEngineWithAppId:@"<#APP_ID#>" delegate:self] 
```


Next, download the **Agora Video SDK** from [Agora.io SDK](https://www.agora.io/en/blog/download/). Unzip the downloaded SDK package and copy the **libs/AgoraRtcEngineKit.framework** to the "AgoraShareVideo_ijkplayer/AgoraShareVideo_ijkplayer" folder in project, copy the **libs/AgoraRtcEngineKit.framework** to the "AgoraShareVideoAudience/AgoraShareVideoAudience" folder in project . 

Finally, Open AgoraShareVideo_ijkplayer,  AgoraShareVideoAudience.xcodeproj  connect your iPhone／iPad device, setup your development signing and run.

## Operating steps
* Run AgoraShareVideo_ijkplayer.xcodeproj create a room.
* Run AgoraShareVideoAudience join the same room.
* two people joining the same room.
## Developer Environment Requirements
* XCode 8.0 +
* Real devices (iPhone or iPad)
* iOS simulator is NOT supported

## Connect Us

- You can find full API document at [Document Center](https://docs.agora.io/en/)
- You can file bugs about this demo at [issue](https://github.com/AgoraIO/Agora-client-side-AV-capturing-for-streaming-iOS/issues)

## License

The MIT License (MIT).
