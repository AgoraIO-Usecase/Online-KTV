//
//  Utils.swift
//  LiveKtv
//
//  Created by XC on 2021/6/4.
//

import Core
import Foundation
import RxCocoa
import RxSwift
import UIKit

extension Utils {
    static let bundle = Bundle(identifier: "io.agora.LiveKtv")!

    static let namesData: [String: [String]] = [
        "cn": [
            "和你一起看月亮",
            "治愈",
            "一锤定音",
            "有酒吗",
            "早安序曲",
            "风情万种的歌房",
        ],
        "en": [
            "Watch the moon with you.",
            "cure",
            "Colossal Smash",
            "have any wine?",
            "Good morning prelude",
            "singing room ",
        ],
        "default": [
            "和你一起看月亮",
            "治愈",
            "一锤定音",
            "有酒吗",
            "早安序曲",
            "风情万种的歌房",
        ],
    ]

    static func randomRoomName() -> String {
        let language = getCurrentLanguage()
        let names = namesData[language] ?? namesData["default"]!
        let index = Int(arc4random_uniform(UInt32(names.count)))
        return names[index]
    }

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

public extension String {
    var localized: String { NSLocalizedString(self, comment: "") }
}

extension LiveKtvRoom {
    static func randomCover() -> String {
        let value = Int.random(in: 1 ... 9)
        return String(value)
    }

    static func getLocalCover(cover: String?) -> String {
        switch cover {
        case "1":
            return "cover0"
        case "2":
            return "cover1"
        case "3":
            return "cover2"
        case "4":
            return "cover3"
        case "5":
            return "cover4"
        case "6":
            return "cover5"
        case "7":
            return "cover6"
        case "8":
            return "cover7"
        case "9":
            return "cover8"
        default:
            return "cover0"
        }
    }

    static func randomMV() -> String {
        let value = Int.random(in: 1 ... 9)
        return String(value)
    }

    static func getLocalMV(cover: String?) -> String {
        switch cover {
        case "1":
            return "mv1"
        case "2":
            return "mv2"
        case "3":
            return "mv3"
        case "4":
            return "mv4"
        case "5":
            return "mv5"
        case "6":
            return "mv6"
        case "7":
            return "mv7"
        case "8":
            return "mv8"
        case "9":
            return "mv9"
        default:
            return "mv1"
        }
    }

    static func getMV(local: String?) -> String {
        switch local {
        case "mv1":
            return "1"
        case "mv2":
            return "2"
        case "mv3":
            return "3"
        case "mv4":
            return "4"
        case "mv5":
            return "5"
        case "mv6":
            return "6"
        case "mv7":
            return "7"
        case "mv8":
            return "8"
        case "mv9":
            return "9"
        default:
            return "1"
        }
    }
}
