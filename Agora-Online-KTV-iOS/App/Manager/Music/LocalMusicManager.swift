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
    let localMusicList = [
        LocalMusic(
            id: "music0",
            name: "青花瓷",
            path: Bundle.main.path(forResource: "qinghuaci", ofType: "m4a")!,
            lrcPath: Bundle.main.path(forResource: "qinghuaci", ofType: "lrc")!
        ),
        LocalMusic(
            id: "music1",
            name: "Send It",
            path: Bundle.main.path(forResource: "send_it", ofType: "m4a")!,
            lrcPath: Bundle.main.path(forResource: Utils.getCurrentLanguage() == "cn" ? "send_it_cn" : "send_it_en", ofType: "lrc")!
        ),
//        LocalMusic(
//            id: "music2",
//            name: "七里香",
//            path: Bundle.main.path(forResource: "七里香", ofType: "mp3")!,
//            lrcPath: Bundle.main.path(forResource: "七里香", ofType: "lrc")!
//        ),
//        LocalMusic(
//            id: "music3",
//            name: "十年",
//            path: Bundle.main.path(forResource: "十年", ofType: "mp3")!,
//            lrcPath: Bundle.main.path(forResource: "十年", ofType: "lrc")!
//        ),
//        LocalMusic(
//            id: "music4",
//            name: "后来",
//            path: Bundle.main.path(forResource: "后来", ofType: "mp3")!,
//            lrcPath: Bundle.main.path(forResource: "后来", ofType: "lrc")!
//        ),
//        LocalMusic(
//            id: "music5",
//            name: "我怀念的",
//            path: Bundle.main.path(forResource: "我怀念的", ofType: "mp3")!,
//            lrcPath: Bundle.main.path(forResource: "我怀念的", ofType: "lrc")!
//        ),
//        LocalMusic(
//            id: "music6",
//            name: "突然好想你",
//            path: Bundle.main.path(forResource: "突然好想你", ofType: "mp3")!,
//            lrcPath: Bundle.main.path(forResource: "突然好想你", ofType: "lrc")!
//        ),
    ]

    static func parseLyric(music: LocalMusic) -> [LyricModel] {
        return LyricParser.parseLyric(filePath: music.lrcPath)
    }
}
