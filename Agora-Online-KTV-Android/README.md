# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Android Studio 4.0.0 或以上版本。
- Android 4.1 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 使用
#### 注册Agora
前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId，然后替换工程**data**中 **strings_config.xml** 中 **app_id**，如果启用了token模式，需要替换 **token**。

#### 数据源
- 本项目目前提供了1种数据接入：**leancloud**。
- 如果需要自己实现数据源，请参考项目 **data** 代码中实现，主要继承接口 **IDataRepositroy**和 **ISyncManager** 实现具体方法。

##### 注册Leanclould
1. 前往 [Leancloud官网](https://www.leancloud.cn/) 注册项目，生产 appId、appKey、server_url。
- 替换工程 **data** 中  **strings_config.xml** 中 **leancloud_app_id**、**leancloud_app_key**、**leancloud_server_url**。
2. 启用LiveQuery，前往 [Leancloud控制台](https://www.leancloud.cn/)，打开**数据存储**-**服务设置**-**启用 LiveQuery**。

#### 运行示例项目
1. 开启 Android 设备的开发者选项，通过 USB 连接线将 Android 设备接入电脑。
2. 在 Android Studio 中，点击 Sync Project with Gradle Files 按钮，同步项目。
3. 在 Android Studio 左下角侧边栏中，点击 Build Variants 选择对应的平台。
4. 点击 Run app 按钮。运行一段时间后，应用就安装到 Android 设备上了。
5. 打开应用，即可使用。

# 开源相关

| 名称 | 作用 |
| ---- | ---- |
| com.github.AgoraIO-Community:LrcView-Android | 歌词下载、解析、拖动改变进度，音调曲线展示、滚动 |