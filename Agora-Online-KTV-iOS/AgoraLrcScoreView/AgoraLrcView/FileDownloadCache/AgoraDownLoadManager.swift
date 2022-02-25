//
//  AgoraDownLoadManager.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit
import Zip

class AgoraDownLoadManager {
    static let manager = AgoraDownLoadManager()
    typealias Completion = (Any?) -> Void
    typealias Sunccess = (String?) -> Void
    private lazy var request = AgoraRequestTask()
    private var urlString: String = ""
    private var retryCount: Int = 0
    private var completion: [String: Completion] = [:]
    private var success: [String: Sunccess] = [:]

    public weak var delegate: AgoraLrcDownloadDelegate?

    func downloadLrcFile(urlString: String, completion: @escaping Completion) {
        delegate?.beginDownloadLrc?(url: urlString)
        self.urlString = urlString
        let fileName = urlString.fileName.components(separatedBy: ".").first ?? ""
        let xmlPath = AgoraCacheFileHandle.cacheFileExists(with: fileName + ".xml")
        let lrcPath = AgoraCacheFileHandle.cacheFileExists(with: urlString)
        if xmlPath != nil {
            parseXml(path: xmlPath ?? "", completion: completion)
        } else if lrcPath == nil {
            if !urlString.hasPrefix("http") {
                parseLrc(path: urlString, completion: completion)
                return
            }
            guard let url = URL(string: urlString) else { return }
            delegate?.beginDownloadLrc?(url: urlString)
            request.delegate = self
            request.download(requestURL: url)
            self.completion[urlString] = completion
        } else {
            if urlString.hasSuffix("zip") {
                unzip(path: lrcPath ?? "", completion: completion)
            } else {
                parseLrc(path: lrcPath ?? "", completion: completion)
            }
        }
    }

    func downloadMP3(urlString: String, success: @escaping Sunccess) {
        self.urlString = urlString
        let cachePath = AgoraCacheFileHandle.cacheFileExists(with: urlString)
        if cachePath == nil {
            guard let url = URL(string: urlString) else { return }
            request.delegate = self
            request.download(requestURL: url)
            self.success[urlString] = success
        } else {
            success(cachePath)
        }
    }

    private func unzip(path: String, completion: @escaping Completion) {
        delegate?.beginParseLrc?()
        let zipFile = URL(fileURLWithPath: path)
        let unZipPath = String.cacheFolderPath()
        do {
            try Zip.unzipFile(zipFile, destination: URL(fileURLWithPath: unZipPath), overwrite: true, password: nil, fileOutputHandler: { url in
                self.parseXml(path: url.path, completion: completion)
                try? FileManager.default.removeItem(atPath: path)
            })
        } catch {
            debugPrint("unzip error == \(error.localizedDescription)")
            guard retryCount < 3 else {
                retryCount = 0
                delegate?.downloadLrcError?(url: urlString,
                                            error: error)
                return
            }
            try? FileManager.default.removeItem(atPath: path)
            downloadLrcFile(urlString: urlString, completion: completion)
            retryCount += 1
        }
    }

    private func parseXml(path: String, completion: @escaping (AgoraMiguSongLyric?) -> Void) {
        let lyric = AgoraMiguSongLyric(lrcFile: path)
        DispatchQueue.main.async {
            completion(lyric)
            self.delegate?.parseLrcFinished?()
        }
    }

    private func parseLrc(path: String, completion: @escaping ([AgoraLrcModel]?) -> Void) {
        let lyric = AgoraLrcParse()
        let string = try? String(contentsOfFile: path)
        lyric.analyzerLrc(lrcConnect: string ?? "")
        DispatchQueue.main.async {
            completion(lyric.lrcArray)
            self.delegate?.parseLrcFinished?()
        }
    }
}

extension AgoraDownLoadManager: AgoraLrcDownloadDelegate {
    func beginDownloadLrc(url: String) {
        delegate?.beginDownloadLrc?(url: url)
    }

    func downloadLrcFinished(url: String) {
        let cacheFilePath = "\(String.cacheFolderPath())/\(url.fileName)"
        if url.fileName.hasSuffix(".mp3") {
            DispatchQueue.main.async {
                self.success[url]?(cacheFilePath)
            }
        } else if url.fileName.hasSuffix(".xml") {
            guard let completion = completion[url] else { return }
            parseXml(path: cacheFilePath, completion: completion)
        } else {
            guard let completion = completion[url] else { return }
            if url.hasSuffix(".zip") {
                unzip(path: cacheFilePath, completion: completion)
            } else {
                parseLrc(path: cacheFilePath, completion: completion)
            }
        }
    }

    func downloadLrcProgress(url: String, progress: Double) {
        delegate?.downloadLrcProgress?(url: url, progress: progress)
    }

    func downloadLrcError(url: String, error: Error?) {
        delegate?.downloadLrcError?(url: url, error: error)
    }

    func downloadLrcCanceld(url: String) {
        delegate?.downloadLrcCanceld?(url: url)
    }

    func beginParseLrc() {
        delegate?.beginParseLrc?()
    }

    func parseLrcFinished() {
        delegate?.parseLrcFinished?()
    }
}
