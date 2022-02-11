//
//  AgoraLrcModel.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

enum AgoraMiguLang: String {
    case zh = "1"
    case en = "2"
    case unknown = "-1"

    static func toLang(_ lang: String?) -> AgoraMiguLang {
        if let lang = lang {
            switch lang {
            case "1":
                return .zh
            case "2":
                return .en
            default:
                return .unknown
            }
        } else {
            return .unknown
        }
    }
}

class AgoraMiguLrcTone {
    let begin: TimeInterval
    let end: TimeInterval
    let pitch: Int
    let pronounce: String
    var lang: AgoraMiguLang
    var word: String

    init(begin: TimeInterval,
         end: TimeInterval,
         pitch: Int, pronounce:
         String, lang: AgoraMiguLang, word: String)
    {
        self.begin = begin
        self.end = end
        self.pitch = pitch
        self.pronounce = pronounce
        self.lang = lang
        self.word = word
    }
}

class AgoraMiguLrcSentence {
    var tones: [AgoraMiguLrcTone]

    init(tones: [AgoraMiguLrcTone]) {
        self.tones = tones
    }

    func startTime() -> TimeInterval {
        tones.first?.begin ?? 0
    }

    func endTime() -> TimeInterval {
        tones.last?.end ?? 0
    }

    func toSentence() -> String {
        tones.map { $0.word }.joined()
    }

    func toPitch() -> [Int] {
        tones.map { $0.pitch }
    }

    func processBlank() {
        tones.enumerated().forEach { item in
            let tone = item.element
            let index = item.offset
            if tone.lang == .en, tone.word != "" {
                let count = tones.count
                let lead = (index >= 1 && tones[index - 1].lang != .en && tones[index - 1].word != "") ? " " : ""
                let trail = index == count - 1 ? "" : " "
                tone.word = "\(lead)\(tone.word)\(trail)"
            }
        }
    }

    private var totalProgress: CGFloat = 1.0
    func getProgress(with time: TimeInterval) -> CGFloat {
        for (index, tone) in tones.enumerated() {
            if time >= tone.begin, time <= tone.end {
                let progress = (time - tone.begin) / (tone.end - tone.begin)
                let total = (CGFloat(index) + progress) / CGFloat(tones.count)
                totalProgress = total
                return total
            }
        }
        return totalProgress
    }
}
