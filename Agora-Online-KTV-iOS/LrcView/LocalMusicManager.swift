//
//  MusicPlayerManager.swift
//  LiveKtv
//
//  Created by XC on 2021/6/11.
//

import Foundation
import Zip

@objc public class LocalMusic: NSObject {
    @objc public var id: String = ""
    @objc public var name: String = ""
    @objc public var path: String = ""
    @objc public var lrcPath: String = ""
    @objc public var singer: String = ""
    @objc public var poster: String = ""
    @objc public init(id: String, name: String, path: String, lrcPath: String, singer: String, poster: String) {
        self.id = id
        self.name = name
        self.path = path
        self.lrcPath = lrcPath
        self.singer = singer
        self.poster = poster
    }
}

@objc public class LocalMusicOption: NSObject {
    let masterUid: UInt = 0
    let masterMusicUid: UInt = 0
    let followerUid: UInt = 0
    let followerMusicUid: UInt = 0
}

@objc public class LocalMusicManager: NSObject {
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
   // public init() {}

    @objc public static func parseLyric(music: LocalMusic) -> [LrcSentence] {
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
            checkAndReWriteXML(url: URL(fileURLWithPath: music.lrcPath))
            let lyric = MiguSongLyric(lrcFile: music.lrcPath)
            if let lyric = lyric {
                return lyric.sentences.map { miguLrcSentence in
                    MiguLrc(model: miguLrcSentence)
                }
            }
        } else if music.lrcPath.hasSuffix(".zip") {
            do {
                let zipFile = URL(fileURLWithPath: music.lrcPath)
                let documentsURL = try FileManager.default.url(for: .documentDirectory,
                                                               in: .userDomainMask,
                                                               appropriateFor: nil,
                                                               create: false)
                var unzipFiles: [URL] = []
                try Zip.unzipFile(zipFile, destination: documentsURL, overwrite: true, password: nil, fileOutputHandler: { url in
                    Logger.log(self, message: url.path, level: .info)
                    unzipFiles.append(url)
                })
                if let unZipfile = unzipFiles.first {
                    checkAndReWriteXML(url: unZipfile)
                    let lyric = MiguSongLyric(lrcFile: unZipfile.path)
                    if let lyric = lyric {
                        return lyric.sentences.map { miguLrcSentence in
                            MiguLrc(model: miguLrcSentence)
                        }
                    }
                }
            } catch {
                Logger.log(self, message: error.localizedDescription, level: .info)
            }
        }
        return []
    }

    private static func checkAndReWriteXML(url: URL) {
        do {
            let enc = CFStringConvertEncodingToNSStringEncoding(CFStringEncoding(CFStringEncodings.GB_18030_2000.rawValue))
            var lyricStr = try String(contentsOfFile: url.path, encoding: String.Encoding(rawValue: enc))
            if lyricStr.starts(with: "<?xml version=\"1.0\" encoding=\"GB2312\" ?>") {
                Logger.log(self, message: "need rewrite xml", level: .info)
                lyricStr = lyricStr.replacingOccurrences(of: "<?xml version=\"1.0\" encoding=\"GB2312\" ?>", with: "<?xml version=\"1.0\" encoding=\"GB2312\"?>")
                let data = lyricStr.data(using: String.Encoding(rawValue: enc))
                try data?.write(to: URL(fileURLWithPath: url.path))
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .info)
        }
    }
}
