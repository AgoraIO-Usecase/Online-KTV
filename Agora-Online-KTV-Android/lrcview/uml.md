``` sequence
title: 加载歌词文件以及播放
participant 用户
participant lrcView

用户->>lrcView:setActionListener(listener)
用户->>lrcView:setTotalDuration(d)
用户->>lrcView:loadLrc(file)

lrcView->>用户:listener.onLoadLrcCompleted()

用户->>lrcView:updateTime(d)

用户->>lrcView:reset()
```
