# KTV 后台微服务

> 由于本示例的歌单系统需要集成声网歌单版权中心，因此提供了这个基于Leancloud云引擎的微服务，提供了同步版权中心歌单，实时获取歌曲，歌词防盗链URL的功能

## 前提条件

1. 安装Python3.x
2. 安装leanCloud[命令行工具](https://leancloud.cn/docs/leanengine_faq.html#hash-864044521)

## 初始化歌单

1. 参考声网文档，获取agora_app_id， customer_key， customer_secret并且更新leanCloudHelp.py的对应变量。
2. 前往 [Leancloud官网](https://www.leancloud.cn/) 项目，获取生产 appId、appKey，并且更新leanCloudHelp.py的对应变量。
3. 执行``python leanCloudHelper.py`` 自动导入版权中心歌单至leanCloud云数据库

## 部署云函数

1. 打开命令行工具，进入Backend/testproject目录下
2. 执行如下命令
``lean switch --region REGION --group GROUP_NAME APP_ID``
   切换至您对应的leancloud项目下
3. 执行如下命令
``lean deploy --prod 1``
   部署云函数