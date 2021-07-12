//
//  MusicPlayerManager.swift
//  LiveKtv
//
//  Created by XC on 2021/6/11.
//

import Core
import Foundation

struct LocalMusic {
    let id: String
    let name: String
    let path: String
    let lrcPath: String
}

class LocalMusicManager {
//    let localMusicList = [
//        LocalMusic(
//            id: "music0",
//            name: "青花瓷",
//            path: Bundle.main.path(forResource: "qinghuaci", ofType: "m4a")!,
//            lrcPath: Bundle.main.path(forResource: "qinghuaci", ofType: "lrc")!
//        ),
//        LocalMusic(
//            id: "music1",
//            name: "Send It",
//            path: Bundle.main.path(forResource: "send_it", ofType: "m4a")!,
//            lrcPath: Bundle.main.path(forResource: Utils.getCurrentLanguage() == "cn" ? "send_it_cn" : "send_it_en", ofType: "lrc")!
//        ),
//    ]

//    static func parseLyric(music: LocalMusic) -> [LyricModel] {
//        return LyricParser.parseLyric(filePath: music.lrcPath)
//    }

    static func parseLyric(music: LocalMusic) -> [LrcSentence] {
        Logger.log(self, message: "parseLyric \(music.lrcPath)", level: .info)
        if music.lrcPath.hasSuffix(".lrc") {
            let lyrics = LyricParser.parseLyric(filePath: music.lrcPath)
            let count = lyrics.count
            return lyrics.enumerated().map { item in
                let mode = item.element
                let index = item.offset
                let next = (index + 1) < count ? lyrics[index + 1] : nil
                return NormalLrc(model: mode, endMsTime: next?.msTime ?? 0)
            }
        } else if music.lrcPath.hasSuffix(".xml") {
            let lyric = MiguSongLyric(lrcFile: music.lrcPath)
            if let lyric = lyric {
                return lyric.sentences.map { miguLrcSentence in
                    MiguLrc(model: miguLrcSentence)
                }
            }
        }
        return []
    }
}
