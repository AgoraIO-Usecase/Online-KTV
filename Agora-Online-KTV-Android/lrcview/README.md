# 歌词LrcView
主要负责歌词的显示，支持上下拖动调整进度。

## 引入工程
```
implementation project(':lrcview')
```
## xml中使用
```
<io.agora.lrcview.LrcView
 android:id="@+id/lrcView"
 android:layout_width="match_parent"
 android:layout_height="match_parent"
 android:paddingStart="10dp"
 android:paddingTop="20dp"
 android:paddingEnd="10dp"
 android:paddingBottom="20dp"
 app:lrcCurrentTextColor="@color/ktv_lrc_highligh"
 app:lrcDividerHeight="20dp"
 app:lrcLabel=" "
 app:lrcNormalTextColor="@color/ktv_lrc_nomal"
 app:lrcNormalTextSize="16sp"
 app:lrcTextGravity="center"
 app:lrcTextSize="26sp" />
```

|xml参数|说明|代码|
|----|----|----|
|lrcCurrentTextColor|歌词高亮下颜色|setCurrentColor|
|lrcNormalTextColor|歌词正常下颜色|setNormalColor|
|lrcTextSize|文字高亮下大小|setCurrentTextSize|
|lrcNormalTextSize|文字正常下大小|setNormalTextSize|
|lrcLabel|没有歌词下默认文字|setLabel|
|lrcTextGravity|歌词对齐方式|无|
|lrcDividerHeight|歌词上下之间间距|无|

## 主要API
|API|说明|
|----|----|
|setActionListener|绑定事件回调|
|setTotalDuration|设置音乐总长度，单位毫秒|
|loadLrc|加载本地歌词文件|
|setEnableDrag|设置是否允许上下滑动|
|updateTime|更新进度，单位毫秒|
|hasLrc|是否有歌词文件|
|reset|重置内部状态，清空已经加载的歌词|

## 使用流程
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
