//
//  AgoraURLExtention.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

extension URL {
    /**
     *  自定义scheme
     */
    func customSchemeURL() -> URL? {
        var components = URLComponents(url: self, resolvingAgainstBaseURL: false)
        components?.scheme = "streaming"
        return components?.url
    }

    /**
     *  还原scheme
     */
    func originalSchemeURL() -> URL? {
        var components = URLComponents(url: self, resolvingAgainstBaseURL: false)
        components?.scheme = "http"
        return components?.url
    }
}

extension URL {
    /**
     *  获取网址中的文件名
     */
    var fileName: String {
        path.components(separatedBy: "/").last ?? ""
    }
}
