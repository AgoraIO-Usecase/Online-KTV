# Agora Online KTV

*Other language: [中文](README.zh.md)*

## Introduction

Users can create karaoke television (KTV) rooms to perform the following:

- Make voice or video calls with other users.
- Play music videos (MVs).
- Sing along with the MV.
- Adjust the volume of their own voice and the MV music.

## Architecture

Here is the architecture of the Agora Online KTV:

![KTV 架构图](Image/ktv_together.en.png)

- The host (singer) plays local or online MV files.
- The voice of the host and the music of the MV are locally mixed by the Agora Native SDK for Video and transmitted to the Agora SD-RTN along with the MV video.
- The audience receives the mixed audio and the MV video through the Agora SD-RTN.

## Run the Sample App

1. Create a developer account at [agora.io](https://sso.agora.io/en/signup) and create a project in Dashboard to get the App ID.
![](Image/appid.en.jpg)

2. Download the [Agora Native SDK for Voice/Video](https://docs.agora.io/en/Interactive%20Broadcast/downloads).
![](Image/sdk.en.png)

#### Android

Development environment:

- Android SDK API Level 16+.
- Android Studio 3.1+.
- Devices with audio and video support.
- Android 4.1+.

1. Add the App ID in the `Android/Agora-Online-KTV/app/src/main/res/values/strings_config.xml` file of your project.

   ```
   <string name="agora_app_id"><#YOUR APP ID#></string>
   ```

2. Unpack the SDK and do the following:

   - Copy the `*.jar`  file under the `libs` folder and save it to the `Android/Agora-Online-KTV/app/libs` folder of your project.
   - Copy the `arm64-v8a/x86/armeabi-v7a` file under the `libs` folder and save it to the `Android/Agora-Online-KTV/app/src/main/jniLibs` folder of your project.

3. Open your project in Android Studio and connect to an Android test device. Compile and run the sample app. 


#### iOS

Development environment:

- Xcode 10.0+.
- iOS 8.0+.

1. Add the App ID in the `AgoraVideoViewController.m` file.

   ```
   self.rtcEngine = [AgoraRtcEngineKit sharedEngineWithAppId:<#APP_ID#> delegate:self];
   ```

2. Unpack the SDK and copy the `AgoraRtcEngineKit.framework` file to the `iOS/Agora-Online-KTV/Agora-Online-KTV` folder of your project.

3. Open `iOS/Agora-Online-KTV/Agora-Online-KTV.xcodeproj` in Xcode. Connect to an iOS test device, fill in a valid developer signature and run the sample app.

## API methods

The API methods related to the Agora Online KTV feature can be divided into two categories.

### Join a Channel and Make Voice/Video Calls

- Creates an Agora RTC engine.
- [`enableVideo`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/enableVideo): Enables the video mode.
- [`setVideoProfile`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setVideoProfile:swapWidthAndHeight:): Sets the video profile.
- [`setChannelProfile`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setChannelProfile:): Sets the channel profile.
- [`setClientRole`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setClientRole:):Sets the role of the user, such as a host or an audience (default).
- [`setupLocalVideo`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setupLocalVideo:): Sets the local video view.
- [`setupRemoteVideo`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/setupRemoteVideo:): Sets the remote video view.
- [`joinChannel`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/joinChannelByToken:channelId:info:uid:joinSuccess:): Allows a user to join a channel.
- [`leaveChannel`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/leaveChannel:): Allows a user to leave the channel.
- [`stopPreview`](https://docs.agora.io/en/Interactive%20Broadcast/API%20Reference/oc/Classes/AgoraRtcEngineKit.html#//api/name/stopPreview): Stops the local video preview and the video.

### Play and Control MV Files

The following sample code abstracts a KTVKit class that controls the MV and pushes the video frames to the Agora SDK.

- `create`: Creates KTVKit and passes it to the Agora SDK.
- `openAndPlayVideoFile`: Opens and plays an MV file.
- `pause` or `resume`: Pauses or resumes playback.
- `stopPlayVideoFile`: Stops playback.
- `switchAudioTrack`: Switches between the instrumental and vocal version of a song.
- `resetAudioBuffer`: Resets the audio buffer. The SDK calls this method when the user role switches.
- `adjustVoiceVolume`: Adjusts the voice volume (%).
- `adjustAccompanyVolume`: Adjusts the music volume (%).
- `getCurrentPosition`: Gets the MV playback position (%).
- `getDuration`: Gets the MV playback duration (ms).

#### Advanced functions

1. To enable the in-ear monitoring function, see [In-ear Monitoring](https://docs.agora.io/en/Interactive%20Broadcast/in-ear_android?platform=Android).

## FAQ

1. Q: When playing an MP4 file, does the file size affect the billing?

   A: The fees are calculated based on the duration and the transmission resolution of the MP4 file, not on the file size. For more information, contact [sales-us@agora.io](mailto:sales-us@agora.io).

2. Q: If I use the voice SDK, can I just develop it to the video SDK when integrating the Online KTV feature?

   A: Yes, the APIs of the voice SDK are compatible with those of the video SDK.

3. Q: Does Agora support MKV files?

   A: The sample app does not support MKV files. You can compile the ijkplayer to support the MKV format.

4. Q: When a host plays an MV, why are the audio and video out of sync at the audience?

   A: This may be because the actual audio sample rate of the video file is inconsistent with the audio sample rate set in the API method.

5. How can I check the audio sample rate of a video file?

   A: You can use the VLC tool or FFmpeg commands. The audio sample rate of the video file used in the sample app is 48000 Hz.

6. What audio sample rates are supported?

   A: 8000, 16000, 32000, 44100, and 48000 Hz are supported.

7. Q: How can I change the audio sample rate of a video file?

   A: You can use FFmpeg commands, such as converting a dual-track MKV video file to an MP4 file with an audio sample rate of 44100 Hz: 

   ​	ffmpeg -i ~/video.mkv -map 0:v -vcodec mpeg4 -map 0:a -acodec copy -ar 44100 -strict -2 output.mp4

8. Q: What affects the number of samples?

   A: The parameter `samplesPerCall` passed in the `setRecordingAudioFrameParameters` method affect the number of samples.

9. Q: Can I use other players besides ijkplayer?

   A: Yes, you can use other players with the same logic to get the data of the audio and video files and push it to the SDK.

10. Q: Why does the audience only hear the voice and not see the video?

  A: Ensure that the video frames are sent to the Agora SDK.

## Contact Us

- API documentation is available at the [Document Center](https://docs.agora.io/en/).
- For any issue with integration, connect with global developers in the [Developer Community](https://dev.agora.io/en/).
- For any question about purchasing our service, contact [sales-us@agora.io](mailto:sales-us@agora.io).
- For technical support, submit a ticket at [Dashboard](https://dashboard.agora.io).
- For any bug in our sample code, submit an issue at [GitHub](https://github.com/AgoraIO/Agora-Online-KTV/issues).

## License

The MIT License (MIT).
