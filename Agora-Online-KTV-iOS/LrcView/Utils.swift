//
//  Utils.swift
//  lrcview
//
//  Created by xianing on 2021/8/7.
//

import Foundation

public enum Utils {
    static func format(time: TimeInterval) -> String {
        if time >= 3600 {
            let hour = time / 3600
            let min = time.truncatingRemainder(dividingBy: 3600) / 60
            let sec = time.truncatingRemainder(dividingBy: 3600).truncatingRemainder(dividingBy: 60)
            return String(format: "%02d:%02d:%02d", Int(hour), Int(min), Int(sec))
        } else {
            let min = time / 60
            let sec = time.truncatingRemainder(dividingBy: 60)
            return String(format: "%02d:%02d", Int(min), Int(sec))
        }
    }
}
