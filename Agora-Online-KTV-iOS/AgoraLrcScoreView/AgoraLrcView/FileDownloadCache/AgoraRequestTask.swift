//
//  AgoraRequestTask.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

class AgoraRequestTask: NSObject {
    public weak var delegate: AgoraLrcDownloadDelegate?
    // 是否缓存
    var cache: Bool = true {
        didSet {
            task?.cancel()
            session?.invalidateAndCancel()
        }
    }

    // 是否取消请求
    var cancel: Bool = false {
        didSet {
            task?.cancel()
            session?.invalidateAndCancel()
        }
    }

    private var session: URLSession? // 会话对象
    private var task: URLSessionDownloadTask? // 任务
    private var requestURL: URL?
    private lazy var queue: OperationQueue = {
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 3
        return queue
    }()

    /**
     *  开始请求
     */
    func download(requestURL: URL?) {
        session = URLSession(configuration: .default,
                             delegate: nil,
                             delegateQueue: nil)
        self.requestURL = requestURL
        guard let url = requestURL else {
            return
        }
        queue.addOperation { [weak self] in
            self?.session = URLSession(configuration: URLSessionConfiguration.default, delegate: self, delegateQueue: OperationQueue.current)
            let request = URLRequest(url: url)
            self?.task = self?.session?.downloadTask(with: request)
            self?.task?.resume()
        }
    }
}

extension AgoraRequestTask: URLSessionDownloadDelegate {
    func urlSession(_: URLSession, downloadTask _: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        // 可以缓存则保存文件
        guard cache == true, let url = requestURL else { return }
        AgoraCacheFileHandle.moveFile(with: location.path,
                                      fileName: url.fileName)
        delegate?.downloadLrcFinished?(url: url.absoluteString)
    }

    func urlSession(_: URLSession, downloadTask _: URLSessionDownloadTask, didWriteData _: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        let progress = Double(totalBytesWritten) * 1.0 / Double(totalBytesExpectedToWrite)
        delegate?.downloadLrcProgress?(url: requestURL?.absoluteString ?? "",
                                       progress: progress)
    }

    // 请求完成会调用该方法，请求失败则error有值
    func urlSession(_: URLSession, task _: URLSessionTask, didCompleteWithError error: Error?) {
        if cancel {
            delegate?.downloadLrcCanceld?(url: requestURL?.absoluteString ?? "")
            return
        }
        if error != nil {
            delegate?.downloadLrcError?(url: requestURL?.absoluteString ?? "", error: error)
        }
    }
}
