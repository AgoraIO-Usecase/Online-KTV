# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Android Studio 4.0.0 或以上版本。
- Android 4.1 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用
#### 注册Agora
1. 前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId，然后替换工程**data**中 **strings_config.xml** 中 **app_id**，如果启用了token模式，需要替换 **token**。
2. 下载SDK，请参考 [说明](https://docs.agora.io/cn/Voice/start_call_audio_android?platform=Android#%E9%9B%86%E6%88%90-sdk)


#### 运行示例项目
1. 开启 Android 设备的开发者选项，通过 USB 连接线将 Android 设备接入电脑。
2. 在 Android Studio 中，点击 Sync Project with Gradle Files 按钮，同步项目。
3. 在 Android Studio 左下角侧边栏中，点击 Build Variants 选择对应的平台。
4. 点击 Run app 按钮。运行一段时间后，应用就安装到 Android 设备上了。
5. 打开应用，即可使用。
