//
//  AgoraScoreItemModel.swift
//  lineTTTT
//
//  Created by zhaoyongqiang on 2021/12/8.
//  Copyright © 2021 km. All rights reserved.
//

import UIKit

@objc
enum AgoraKaraokeScoreStatus: Int {
    /// 初始状态所有cell没有layer 或 竖线右方cell还没画layer，移除layer防重用
    case `init` = 0
    /// 当前cell在画layer
    case drawing = 1
    /// 当前cell产生新的layer
    case new_layer = 2
    /// 竖线左方cell已经画过了layer
    case end = 3
}

class AgoraScoreItemModel: NSObject {
    /// position
    var topKM: CGFloat = 0
    /// startTime
    var leftKM: CGFloat = 0
    /// endTime-startTime
    var widthKM: CGFloat = 0
    /// 实时绘制的offset
    var offsetXKM: CGFloat = 0
    /// 当前状态
    var status: AgoraKaraokeScoreStatus = .`init`

    var startTime: TimeInterval = 0
    var endTime: TimeInterval = 0
    var isEmptyCell: Bool = false
    var pitch: Double = 0
    var pitchMin: CGFloat = 0
    var pitchMax: CGFloat = 0
}

@objcMembers
public class AgoraScoreItemConfigModel: NSObject {
    /// 评分视图高度 默认:100
    public var scoreViewHeight: CGFloat = 100
    /// 圆的起始位置: 默认: 100
    public var innerMargin: CGFloat = 100
    /// 线的高度 默认:10
    public var lineHeight: CGFloat = 10
    /// 线的宽度 默认: 120
    public var lineWidht: CGFloat = 120
    /// 默认线的背景色
    public var normalColor: UIColor = .gray
    /// 匹配后线的背景色
    public var highlightColor: UIColor = .orange
    /// 分割线的颜色
    public var separatorLineColor: UIColor = .systemPink
    /// 是否隐藏垂直分割线
    public var isHiddenVerticalSeparatorLine: Bool = false
    /// 是否隐藏上下分割线
    public var isHiddenSeparatorLine: Bool = false
    /// 游标背景色
    public var cursorColor: UIColor = .systemPink
    /// 游标的宽
    public var cursorWidth: CGFloat = 20
    /// 游标的高
    public var cursorHeight: CGFloat = 20
    /// 是否隐藏粒子动画效果
    public var isHiddenEmitterView: Bool = false
    /// 使用图片创建粒子动画
    public var emitterImages: [UIImage]?
    /// emitterImages为空默认使用颜色创建粒子动画
    public var emitterColors: [UIColor] = [.red]
    /// 尾部动画图片
    public var tailAnimateImage: UIImage?
    /// 尾部动画颜色 图片为空时使用颜色
    public var tailAnimateColor: UIColor? = .yellow
    /// 评分默认分数: 50
    public var defaultScore: Double = 50
}
