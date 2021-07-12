//
//  MiguLyric.swift
//  app
//
//  Created by XC on 2021/7/7.
//

import Core
import Foundation

class MiguLrcTone {
    let begin: TimeInterval
    let end: TimeInterval
    let pitch: Int
    let pronounce: String
    let lang: String
    fileprivate(set) var word: String

    init(begin: TimeInterval, end: TimeInterval, pitch: Int, pronounce: String, lang: String, word: String) {
        self.begin = begin
        self.end = end
        self.pitch = pitch
        self.pronounce = pronounce
        self.lang = lang
        self.word = word
    }
}

class MiguLrcSentence {
    fileprivate(set) var tones: [MiguLrcTone]

    init(tones: [MiguLrcTone]) {
        self.tones = tones
    }

    func startTime() -> TimeInterval {
        return tones.first?.begin ?? 0
    }

    func endTime() -> TimeInterval {
        return tones.last?.end ?? 0
    }

    func toSentence() -> String {
        return tones.map { $0.word }.joined()
    }

    fileprivate func processBlank() {
        tones.enumerated().forEach { item in
            let tone = item.element
            let index = item.offset
            if tone.lang == "2", tone.word != "" {
                let count = tones.count
                let lead = (index >= 1 && tones[index - 1].lang != "2" && tones[index - 1].word != "") ? " " : ""
                let trail = index == count - 1 ? "" : " "
                tone.word = "\(lead)\(tone.word)\(trail)"
            }
        }
    }
}

private class Song {
    var name: String = ""
    var singer: String = ""
    var type: String = ""
    var sentences: [MiguLrcSentence] = []
}

private enum ParserType {
    case general
    case name
    case singer
    case type
    case sentence
    case tone
    case word
    case overlap
}

private class MiguSongLyricXmlParser: NSObject, XMLParserDelegate {
    let parser: XMLParser!
    var song: Song?
    private var parserTypes: [ParserType] = []

    init?(lrcFile: String) {
        parser = XMLParser(contentsOf: URL(fileURLWithPath: lrcFile))
        super.init()
        if parser == nil {
            return nil
        } else {
            parser.delegate = self
            let success = parser.parse()
            Logger.log(self, message: "parse \(success) \(parser.parserError?.localizedDescription ?? "")", level: .info)
            if success, let song = song {
                song.sentences.forEach { sentence in
                    sentence.processBlank()
                }
            }
        }
    }

    func parserDidStartDocument(_: XMLParser) {
        Logger.log(self, message: "parserDidStartDocument", level: .info)
    }

    func parserDidEndDocument(_: XMLParser) {
        Logger.log(self, message: "parserDidEndDocument", level: .info)
    }

    func parser(_: XMLParser, didStartElement elementName: String, namespaceURI _: String?, qualifiedName _: String?, attributes attributeDict: [String: String] = [:]) {
        switch elementName {
        case "song":
            song = Song()
        case "general":
            push(.general)
        case "name":
            push(.name)
        case "singer":
            push(.singer)
        case "type":
            push(.type)
        case "sentence":
            push(.sentence)
            song?.sentences.append(MiguLrcSentence(tones: []))
        case "tone":
            push(.tone)
            if let sentence = song?.sentences.last {
                let begin: TimeInterval = attributeDict["begin"]!.doubleValue * 1000
                let end: TimeInterval = attributeDict["end"]!.doubleValue * 1000
                let pitch = Int(attributeDict["pitch"]!.floatValue)
                let pronounce = attributeDict["pronounce"]!
                let lang = attributeDict["lang"]!
                let tone = MiguLrcTone(begin: begin, end: end, pitch: pitch, pronounce: pronounce, lang: lang, word: "")
                sentence.tones.append(tone)
            }
        case "word":
            push(.word)
        case "overlap":
            push(.overlap)
            let begin: TimeInterval = attributeDict["begin"]!.doubleValue * 1000
            let end: TimeInterval = attributeDict["end"]!.doubleValue * 1000
            let pitch = 0
            let pronounce = ""
            let lang = attributeDict["lang"]!
            let tone = MiguLrcTone(begin: begin, end: end, pitch: pitch, pronounce: pronounce, lang: lang, word: "")
            song?.sentences.append(MiguLrcSentence(tones: [tone]))
        default:
            break
        }
    }

    func parser(_: XMLParser, foundCharacters string: String) {
        if let last = parserTypes.last {
            switch last {
            case .name:
                song?.name = string
            case .singer:
                song?.singer = string
            case .type:
                song?.type = string
            case .word, .overlap:
                if let tone = song?.sentences.last?.tones.last {
                    tone.word = string
                }
            default:
                break
            }
        }
    }

    func parser(_: XMLParser, didEndElement elementName: String, namespaceURI _: String?, qualifiedName _: String?) {
        switch elementName {
        case "general":
            pop(equal: .general)
        case "name":
            pop(equal: .name)
        case "singer":
            pop(equal: .singer)
        case "type":
            pop(equal: .type)
        case "sentence":
            pop(equal: .sentence)
        case "tone":
            pop(equal: .tone)
        case "word":
            pop(equal: .word)
        case "overlap":
            pop(equal: .overlap)
        default:
            break
        }
    }

    private func current(type: ParserType) -> Bool {
        return parserTypes.last == type
    }

    private func push(_ type: ParserType) {
        parserTypes.append(type)
    }

    private func pop() {
        parserTypes.removeLast()
    }

    private func pop(equal: ParserType) {
        if current(type: equal) {
            pop()
        }
    }
}

class MiguSongLyric {
    let name: String
    let singer: String
    // 歌曲的类型(1.快歌，2.慢歌)
    let type: String
    let sentences: [MiguLrcSentence]

    init?(lrcFile: String) {
        let parser = MiguSongLyricXmlParser(lrcFile: lrcFile)
        if let song = parser?.song {
            name = song.name
            singer = song.singer
            type = song.type
            sentences = song.sentences
        } else {
            return nil
        }
    }
}
