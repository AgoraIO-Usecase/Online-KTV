//
//  AgoraLrcScoreView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/16.
//

import UIKit

@objc(AgoraLrcViewDelegate)
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
    /// 歌词pitch回调
    @objc
    optional func agoraWordPitch(pitch: Int, totalCount: Int)
}

@objc(AgoraLrcDownloadDelegate)
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

@objcMembers
public class AgoraLrcScoreView: UIView {
    /// 配置
    private var _config: AgoraLrcScoreConfigModel = .init() {
        didSet {
            scoreView?.isHidden = _config.isHiddenScoreView
            scoreView?.scoreConfig = _config.scoreConfig
            lrcView?.lrcConfig = _config.lrcConfig
            statckView.spacing = _config.spacing
            isHiddenWatitingView = _config.lrcConfig?.isHiddenWatitingView ?? false
            setupBackgroundImage()
            updateUI()
        }
    }

    public var config: AgoraLrcScoreConfigModel? {
        set {
            _config = newValue ?? AgoraLrcScoreConfigModel()
        }
        get {
            return _config
        }
    }

    public var updateScoreConfig: AgoraScoreItemConfigModel? {
        didSet {
            scoreView?.scoreConfig = updateScoreConfig
        }
    }

    public var updateLrcConfig: AgoraLrcConfigModel? {
        didSet {
            lrcView?.lrcConfig = updateLrcConfig
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

    /// 是否开始
    public private(set) var isStart: Bool = false

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
            _lrcView?.currentWordPitchClosure = { [weak self] pitch, totalCount in
                guard pitch > 0 else { return }
                self?.delegate?.agoraWordPitch?(pitch: pitch, totalCount: totalCount)
            }
            return _lrcView
        }
        set {
            _lrcView = newValue
        }
    }

    // 记录是否隐藏等待小圆点
    private var isHiddenWatitingView: Bool = false
    private lazy var timer = GCDTimer()
    private var scoreViewHCons: NSLayoutConstraint?
    private var currentTime: TimeInterval = 0
    private var totalTime: TimeInterval = 0

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
        AgoraDownLoadManager.manager.downloadLrcFile(urlString: url, completion: { lryic in
            self.scoreView?.isHidden = self._config.isHiddenScoreView || lryic is [AgoraLrcModel]
            self.config?.lrcConfig?.isHiddenWatitingView = self.isHiddenWatitingView
            self.lrcView?.lrcConfig = self.config?.lrcConfig
            if lryic is AgoraMiguSongLyric {
                self.lrcView?.miguSongModel = lryic as? AgoraMiguSongLyric
            } else {
                self.lrcView?.lrcDatas = lryic as? [AgoraLrcModel]
            }
            if let senences = lryic as? AgoraMiguSongLyric, self.scoreView?.isHidden == false {
                self.scoreView?.lrcSentence = senences.sentences
            }
            self.downloadDelegate?.downloadLrcFinished?(url: url)
        }, failure: {
            self.lrcView?.lrcDatas = []
            self.config?.lrcConfig?.isHiddenWatitingView = true
        })
    }

    /// 实时声音数据
    public func setVoicePitch(_ voicePitch: [Double]) {
        scoreView?.setVoicePitch(voicePitch)
    }

    /// 滚到顶部
    public func scrollToTop(animation: Bool = false) {
        lrcView?.scrollToTop(animation: animation)
        scoreView?.scrollToTop(animation: animation)
    }

    /// 根据时间滚到指定位置
    public func scrollToTime(timestamp: TimeInterval) {
        lrcView?.scrollToTime(timestamp: timestamp * 1000)
        scoreView?.start(currentTime: timestamp * 1000, totalTime: totalTime)
    }

    private var preTime: TimeInterval = 0
    private var isStop: Bool = false
    /// 开始滚动
    public func start() {
        isStart = true
        timer.scheduledMillisecondsTimer(withName: "lrc", countDown: 1000 * 60 * 30, milliseconds: 10, queue: .main) { [weak self] _, duration in
            guard let self = self else { return }
            if duration.truncatingRemainder(dividingBy: 1000) == 0 {
                let currentTime = self.roundToPlaces(value: self.delegate?.getPlayerCurrentTime() ?? 0, places: 10)
                self.isStop = currentTime == self.preTime
                self.totalTime = self.roundToPlaces(value: self.delegate?.getTotalTime() ?? 0, places: 10)
                self.currentTime = currentTime
                self.preTime = currentTime
            }
            guard self.isStop == false else { return }
            self.startMillisecondsHandler()
        }
    }

    /// 停止
    public func stop() {
        isStart = false
        timer.destoryAllTimer()
    }

    public func reset() {
        resetTime()
        stop()
        scoreView?.reset()
        lrcView?.reset()
    }

    public func resetTime() {
        preTime = 0
        currentTime = 0
    }

    private func startMillisecondsHandler() {
        currentTime += 0.010
        timerHandler(time: currentTime)
    }

    private func timerHandler(time: TimeInterval) {
        lrcView?.start(currentTime: time)
        scoreView?.start(currentTime: time,
                         totalTime: totalTime)
    }

    private func roundToPlaces(value: Double, places: Int) -> Double {
        let divisor = pow(10.0, Double(places))
        return round(value * divisor) / divisor
    }

    private func setupBackgroundImage() {
        guard let bgImageView = _config.backgroundImageView else { return }
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
    }

    private func updateUI() {
        guard statckView.arrangedSubviews.isEmpty,
              let scoreView = scoreView,
              let lrcView = lrcView,
              let lrcConfig = _config.lrcConfig else { return }
        scoreView.scoreConfig = _config.scoreConfig
        lrcConfig.isHiddenWatitingView = isHiddenWatitingView
        lrcView.lrcConfig = lrcConfig
        _scoreView?.delegate = scoreDelegate
        scoreView.translatesAutoresizingMaskIntoConstraints = false
        let height = _config.scoreConfig?.scoreViewHeight ?? 100
        scoreView.isHidden = config?.isHiddenScoreView ?? false
        scoreView.heightAnchor.constraint(equalToConstant: height).isActive = true
        statckView.addArrangedSubview(scoreView)
        statckView.addArrangedSubview(lrcView)
    }
}
