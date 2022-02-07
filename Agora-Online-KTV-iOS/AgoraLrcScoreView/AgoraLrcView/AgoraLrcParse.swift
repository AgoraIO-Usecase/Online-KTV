//
//  AgoraLrcParse.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2022/1/10.
//

import UIKit

struct AgoraLrcModel {
    var lrc: String?
    var time: TimeInterval = 0
}

class AgoraLrcParse: NSObject {
    var lrcArray: [AgoraLrcModel] = []
    
    //根据换行符\n分割字符串，获得包含每一句歌词的数组
    func analyzerLrc(lrcConnect: String) {
        let lrcConnectArray = lrcConnect.components(separatedBy: "\n")

        let pattern = "\\[[0-9][0-9]:[0-9][0-9].[0-9]{1,}\\]"
        guard let regular = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
            return
        }
        for line in lrcConnectArray {
            let matchesArray = regular.matches(in: line, options: .reportProgress, range: NSRange(location: 0, length: line.count))
            let lrc = line.components(separatedBy: "]").last
            
            //如果时间点对应的歌词为空就不加入歌词数组
//            if lrc?.count == 0 || lrc == "\r" || lrc == "\n" {
//                continue
//            }
            
            for match in matchesArray {
                var timeStr = NSString(string: line).substring(with: match.range)
                // 去掉开头和结尾的[],得到时间00:00.00
                timeStr = timeStr.textSubstring(startIndex: 1, length: timeStr.count-2)
                
                let df = DateFormatter()
                df.dateFormat = "mm:ss.SS"
                let date1 = df.date(from: timeStr)
                let date2 = df.date(from: "00:00.00")
                var interval1 = date1!.timeIntervalSince1970
                let interval2 = date2!.timeIntervalSince1970
                
                interval1 -= interval2
                if (interval1 < 0) {
                    interval1 *= -1
                }
                var eachLrc = AgoraLrcModel()
                eachLrc.lrc = lrc
                eachLrc.time = interval1
                lrcArray.append(eachLrc)
            }
        }
    }
}
