//
//  AgoraLrcScoreConfigModel.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/17.
//

import UIKit

@objcMembers
public class AgoraLrcScoreConfigModel: NSObject {
    /// 评分组件配置
    public var scoreConfig: AgoraScoreItemConfigModel?
    /// 歌词组件配置
    public var lrcConfig: AgoraLrcConfigModel?
    /// 是否隐藏评分组件
    public var isHiddenScoreView: Bool = false
    /// 背景图
    public var backgroundImageView: UIImageView?
    /// 评分组件和歌词组件之间的间距 默认: 0
    public var spacing: CGFloat = 0
}
