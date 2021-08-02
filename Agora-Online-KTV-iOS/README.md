# 前提条件
开始前，请确保你的开发环境满足如下条件：
- Xcode 12.0 或以上版本。
- Cocoapods。你可以参考 [Getting Started with CocoaPods](https://guides.cocoapods.org/using/getting-started.html#getting-started) 安装。
- iOS 11.0 或以上版本的设备。部分模拟机可能无法支持本项目的全部功能，所以推荐使用真机。

# 操作步骤
#### 注册Agora
前往 [Agora官网](https://console.agora.io/) 注册项目，生产appId，然后替换 **Config.swift** 中 **AppId**。

##### 注册Leancloud
1. 前往 [Leancloud官网](https://www.leancloud.cn/) 注册项目，生产 appId、appKey、server_url。
- 替换 **Config.swift** 中 **LeanCloudAppId**、**LeanCloudAppKey**、**LeanCloudServerUrl**。
- 替换 [LeanCloudHelp.py](../LeanCloudHelp.py) 中 **appid** 和 **appkey**。
2. 安装 [Python](https://www.python.org/)，如果已经安装请忽略。
3. Python安装之后，控制台执行以下命令。
```
pip install leancloud
或者
pip3 install leancloud
```
4. Terminal 中执行文件 [LeanCloudHelp.py](../LeanCloudHelp.py)。
```
python ./LeanCloudHelp.py
或者
python3 ./LeanCloudHelp.py
```

## 联系我们

- 如果你遇到了困难，可以先参阅 [常见问题](https://docs.agora.io/cn/faq)
- 如果你想了解更多官方示例，可以参考 [官方SDK示例](https://github.com/AgoraIO)
- 如果你想了解声网SDK在复杂场景下的应用，可以参考 [官方场景案例](https://github.com/AgoraIO-usecase)
- 如果你想了解声网的一些社区开发者维护的项目，可以查看 [社区](https://github.com/AgoraIO-Community)
- 完整的 API 文档见 [文档中心](https://docs.agora.io/cn/)
- 若遇到问题需要开发者帮助，你可以到 [开发者社区](https://rtcdeveloper.com/) 提问
- 如果需要售后技术支持，你可以在 [Agora Dashboard](https://dashboard.agora.io) 提交工单
- 如果发现了示例代码的 bug，欢迎提交 [issue](https://github.com/AgoraIO-Usecase/InteractivePodcast/issues)

## 代码许可

The MIT License (MIT)
