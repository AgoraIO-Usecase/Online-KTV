//
//  AgoraCacheFileHandle.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

class AgoraCacheFileHandle: NSObject {
    /**
     *  创建临时文件
     */
    static func createTempFile() {
        let manager = FileManager.default
        let path = String.tempFilePath()
        if manager.fileExists(atPath: path) {
            try? manager.removeItem(atPath: path)
        }
        manager.createFile(atPath: path, contents: nil, attributes: nil)
    }

    /**
     *  往临时文件写入数据
     */
    static func writeTempFile(data: Data) {
        guard let handle = FileHandle(forWritingAtPath: String.tempFilePath()) else {
            return
        }
        handle.seekToEndOfFile()
        handle.write(data)
    }

    /**
     *  读取临时文件数据
     */
    static func readTempFileData(with offset: UInt64, length: Int) -> Data? {
        guard let handle = FileHandle(forReadingAtPath: String.tempFilePath()) else {
            return nil
        }

        handle.seek(toFileOffset: offset)
        return handle.readData(ofLength: length)
    }

    /**
     *  保存临时文件到缓存文件夹
     */
    static func cacheTempFile(with fileName: String) {
        let manager = FileManager.default
        let cacheFolderPath = String.cacheFolderPath()
        if !manager.fileExists(atPath: cacheFolderPath) {
            try? manager.createDirectory(atPath: cacheFolderPath, withIntermediateDirectories: true, attributes: nil)
        }
        let cacheFilePath = cacheFolderPath + "/\(fileName)"
        try? manager.copyItem(atPath: String.tempFilePath(), toPath: cacheFilePath)
    }

    static func moveFile(with locationPath: String, fileName: String) {
        let manager = FileManager.default
        let cacheFolderPath = String.cacheFolderPath()
        if !manager.fileExists(atPath: cacheFolderPath) {
            try? manager.createDirectory(atPath: cacheFolderPath, withIntermediateDirectories: true, attributes: nil)
        }
        let cacheFilePath = cacheFolderPath + "/\(fileName)"
        try? manager.copyItem(atPath: locationPath, toPath: cacheFilePath)
    }

    /**
     *  是否存在缓存文件 存在：返回文件路径 不存在：返回nil
     */
    static func cacheFileExists(with url: String) -> String? {
        let cacheFilePath = "\(String.cacheFolderPath())/\(url.fileName)"
        if FileManager.default.fileExists(atPath: cacheFilePath) {
            return cacheFilePath
        }
        return nil
    }

    /**
     *  清空缓存文件
     */
    static func clearCache() -> Bool? {
        let manager = FileManager.default

        if let _ = try? manager.removeItem(atPath: String.cacheFolderPath()) {
            return true
        }
        return false
    }
}
