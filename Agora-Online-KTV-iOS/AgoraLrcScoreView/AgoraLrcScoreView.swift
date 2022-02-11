//
//  AgoraLrcScoreView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/16.
//

import UIKit

@objc
public
protocol AgoraLrcViewDelegate {
    /// 当前播放器的时间 单位: 秒
    func getPlayerCurrentTime() -> TimeInterval
    /// 获取歌曲总时长
    func getTotalTime() -> TimeInterval

    /// 设置播放器时间
    @objc
    optional func seekToTime(time: TimeInterval)
    /// 当前正在播放的歌词和进度
    @objc
    optional func currentPlayerLrc(lrc: String, progress: CGFloat)
}

@objc
public
protocol AgoraLrcDownloadDelegate {
    /// 开始下载
    @objc
    optional func beginDownloadLrc(url: String)
    /// 下载完成
    @objc
    optional func downloadLrcFinished(url: String)
    /// 下载进度
    @objc
    optional func downloadLrcProgress(url: String, progress: Double)
    /// 下载失败
    @objc
    optional func downloadLrcError(url: String, error: Error?)
    /// 下载取消
    @objc
    optional func downloadLrcCanceld(url: String)
    /// 开始解析歌词
    @objc
    optional func beginParseLrc()
    /// 解析歌词结束
    @objc
    optional func parseLrcFinished()
}

@objc
public
protocol AgoraKaraokeScoreDelegate {
    /// 分数实时回调
    @objc optional func agoraKaraokeScore(score: Double, totalScore: Double)
}

public class AgoraLrcScoreView: UIView {
    /// 配置
    public var config: AgoraLrcScoreConfigModel = .init() {
        didSet {
            scoreView!.scoreConfig = config.scoreConfig
            lrcView!.lrcConfig = config.lrcConfig
            scoreViewHCons?.constant = scoreView?.scoreConfig.scoreViewHeight ?? 0
            scoreViewHCons?.isActive = true
            statckView.spacing = config.spacing
            setupBackgroundImage()
        }
    }

    /// 事件回调
    public weak var delegate: AgoraLrcViewDelegate?
    /// 下载歌词事件回调
    public weak var downloadDelegate: AgoraLrcDownloadDelegate? {
        didSet {
            AgoraDownLoadManager.manager.delegate = downloadDelegate
        }
    }

    /// 实时评分回调
    public weak var scoreDelegate: AgoraKaraokeScoreDelegate? {
        didSet {
            scoreView?.delegate = scoreDelegate
        }
    }

    /// 清除缓存文件
    public static func cleanCache() {
        try? FileManager.default.removeItem(atPath: String.cacheFolderPath())
    }

    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .vertical
        stackView.distribution = .fill
        stackView.spacing = 0
        return stackView
    }()

    private var _scoreView: AgoraKaraokeScoreView?
    private var scoreView: AgoraKaraokeScoreView? {
        get {
            guard _scoreView == nil else { return _scoreView }
            _scoreView = AgoraKaraokeScoreView()
            _scoreView?.delegate = scoreDelegate
            return _scoreView
        }
        set {
            _scoreView = newValue
        }
    }

    private var _lrcView: AgoraLrcView?
    private var lrcView: AgoraLrcView? {
        get {
            guard _lrcView == nil else { return _lrcView }
            _lrcView = AgoraLrcView()
            _lrcView?.seekToTime = { [weak self] time in
                self?.delegate?.seekToTime?(time: time)
            }
            _lrcView?.currentPlayerLrc = { [weak self] lrc, progress in
                self?.delegate?.currentPlayerLrc?(lrc: lrc,
                                                  progress: progress)
            }
            return _lrcView
        }
        set {
            _lrcView = newValue
        }
    }

    private lazy var timer = GCDTimer()
    private var scoreViewHCons: NSLayoutConstraint?

    public init(delegate: AgoraLrcViewDelegate) {
        super.init(frame: .zero)
        setupUI()
        self.delegate = delegate
    }

    override private init(frame _: CGRect) {
        super.init(frame: .zero)
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: 赋值方法

    /// 歌词的URL
    public func setLrcUrl(url: String) {
        AgoraDownLoadManager.manager.downloadLrcFile(urlString: url) { lryic in
            if lryic is AgoraMiguSongLyric {
                self.lrcView?.miguSongModel = lryic as? AgoraMiguSongLyric
            } else {
                self.lrcView?.lrcDatas = lryic as? [AgoraLrcModel]
            }
            self.scoreView?.isHidden = lryic is [AgoraLrcModel]
            if let senences = lryic as? AgoraMiguSongLyric {
                self.scoreView?.lrcSentence = senences.sentences
            }
            self.downloadDelegate?.downloadLrcFinished?(url: url)
        }
    }

    /// 实时声音数据
    public func setVoicePitch(_ voicePitch: [Double]) {
        scoreView?.setVoicePitch(voicePitch)
    }

    /// 开始滚动
    public func start() {
        timer.scheduledMillisecondsTimer(withName: "lrc",
                                         countDown: .infinity,
                                         milliseconds: 17,
                                         queue: .main, action: { [weak self] _, _ in
                                             guard let self = self else { return }
                                             self.timerHandler()
                                         })
        guard statckView.arrangedSubviews.isEmpty else { return }
        updateUI()
        config.scoreConfig = config.scoreConfig
        config.lrcConfig = config.lrcConfig
    }

    /// 停止
    public func stop() {
        timer.destoryAllTimer()
    }

    public func reset() {
        stop()
        scoreView?.reset()
        lrcView?.reset()
        lrcView?.removeFromSuperview()
        lrcView = nil
        scoreView?.removeFromSuperview()
        scoreView = nil
    }

    @objc
    private func timerHandler() {
        let currentTime = delegate?.getPlayerCurrentTime() ?? 0
        lrcView?.start(currentTime: currentTime)
        let totalTime = delegate?.getTotalTime() ?? 0
        scoreView?.start(currentTime: currentTime,
                         totalTime: totalTime)
    }

    private func setupBackgroundImage() {
        guard let bgImageView = config.backgroundImageView else { return }
        insertSubview(bgImageView, at: 0)
        bgImageView.translatesAutoresizingMaskIntoConstraints = false
        bgImageView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        bgImageView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        bgImageView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        bgImageView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }

    private func setupUI() {
        statckView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(statckView)

        statckView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        statckView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        statckView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        updateUI()
    }

    private func updateUI() {
        scoreView?.translatesAutoresizingMaskIntoConstraints = false
        statckView.addArrangedSubview(scoreView!)
        statckView.addArrangedSubview(lrcView!)
        scoreViewHCons = scoreView?.heightAnchor.constraint(equalToConstant: config.scoreConfig.scoreViewHeight)
        scoreViewHCons?.isActive = true
    }
}
