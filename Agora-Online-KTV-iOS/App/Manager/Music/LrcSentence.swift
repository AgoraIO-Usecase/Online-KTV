//
//  File.swift
//  app
//
//  Created by XC on 2021/7/7.
//

import Core
import Foundation

protocol LrcSentence {
    func startMsTime() -> TimeInterval
    func endMsTime() -> TimeInterval
    func getProgress(consume: TimeInterval, totalMsTime: TimeInterval) -> Double
    func getSentence() -> String
    func timeString() -> String
}

class NormalLrc: LrcSentence {
    private let model: LyricModel
    private var _endMsTime: TimeInterval

    init(model: LyricModel, endMsTime: TimeInterval = 0) {
        self.model = model
        _endMsTime = endMsTime
    }

    func startMsTime() -> TimeInterval {
        return model.msTime
    }

    func endMsTime() -> TimeInterval {
        return _endMsTime
    }

    func setEndMsTime(_ endMsTime: TimeInterval) {
        _endMsTime = endMsTime
    }

    func getProgress(consume: TimeInterval, totalMsTime: TimeInterval) -> Double {
        if endMsTime() <= 0 || endMsTime() <= startMsTime() {
            _endMsTime = totalMsTime
        }
        return consume / (endMsTime() - startMsTime())
    }

    func getSentence() -> String {
        return model.content
    }

    func timeString() -> String {
        return model.timeString
    }
}

class MiguLrc: LrcSentence {
    private let model: MiguLrcSentence

    init(model: MiguLrcSentence) {
        self.model = model
    }

    func startMsTime() -> TimeInterval {
        return model.startTime()
    }

    func endMsTime() -> TimeInterval {
        return model.endTime()
    }

    func getProgress(consume: TimeInterval, totalMsTime _: TimeInterval) -> Double {
        let total = endMsTime() - startMsTime()
        return total > 0 ? consume / total : 0
    }

    func getSentence() -> String {
        return model.toSentence()
    }

    func timeString() -> String {
        return Utils.format(time: startMsTime() / 1000)
    }
}
