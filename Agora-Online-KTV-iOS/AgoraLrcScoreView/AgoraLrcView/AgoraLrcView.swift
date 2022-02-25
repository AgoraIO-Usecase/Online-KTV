//
//  AgoraLrcView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

class AgoraLrcView: UIView {
    /// 滚动歌词后设置播放器时间
    var seekToTime: ((TimeInterval) -> Void)?
    /// 当前播放的歌词
    var currentPlayerLrc: ((String, CGFloat) -> Void)?

    var lrcConfig: AgoraLrcConfigModel = .init() {
        didSet {
            updateUI()
        }
    }

    var miguSongModel: AgoraMiguSongLyric? {
        didSet {
            dataArray = miguSongModel?.sentences
        }
    }

    var lrcDatas: [AgoraLrcModel]? {
        didSet {
            dataArray = lrcDatas
        }
    }

    private var dataArray: [Any]? {
        didSet {
            tipsLabel.isHidden = dataArray != nil || !(dataArray?.isEmpty ?? true)
            tableView.reloadData()
        }
    }

    private var progress: CGFloat = 0 {
        didSet {
            let cell = tableView.cellForRow(at: IndexPath(row: scrollRow, section: 0)) as? AgoraMusicLrcCell
            cell?.setupMusicLrcProgress(with: progress)
        }
    }

    /// 当前歌词所在的位置
    private var preRow: Int = -1
    private var scrollRow: Int = -1 {
        didSet {
            if scrollRow == oldValue { return }
            if preRow > -1 {
                UIView.performWithoutAnimation {
                    tableView.reloadRows(at: [IndexPath(row: preRow, section: 0)], with: .fade)
                }
            }
            let indexPath = IndexPath(row: scrollRow, section: 0)
            tableView.scrollToRow(at: indexPath, at: .middle, animated: true)
            let cell = tableView.cellForRow(at: indexPath) as? AgoraMusicLrcCell
            cell?.setupCurrentLrcScale()
            preRow = scrollRow
        }
    }

    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .fill
        stackView.axis = .vertical
        stackView.distribution = .fill
        stackView.spacing = 0
        return stackView
    }()

    private lazy var loadView: AgoraLoadingView = {
        let view = AgoraLoadingView()
        view.delegate = self
        return view
    }()

    private lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.showsVerticalScrollIndicator = false
        tableView.dataSource = self
        tableView.delegate = self
        tableView.separatorStyle = .none
        tableView.backgroundColor = .clear
        tableView.scrollsToTop = false
        tableView.setContentCompressionResistancePriority(.defaultHigh, for: .vertical)
        tableView.register(AgoraMusicLrcCell.self, forCellReuseIdentifier: "AgoaraLrcViewCell")
        return tableView
    }()

    private lazy var gradientLayer: CAGradientLayer = {
        let gradientLayer = CAGradientLayer()
        gradientLayer.colors = [UIColor(white: 0, alpha: 0.05).cgColor,
                                UIColor(white: 0, alpha: 0.8).cgColor]
        gradientLayer.locations = [0.7, 1.0]
        gradientLayer.startPoint = CGPoint(x: 0, y: 0)
        gradientLayer.endPoint = CGPoint(x: 0, y: 1)
        return gradientLayer
    }()

    /** 提示 */
    private lazy var tipsLabel: UILabel = {
        let view = UILabel()
        view.textColor = .blue
        view.text = "纯音乐，无歌词"
        view.font = .systemFont(ofSize: 17)
        view.isHidden = true
        return view
    }()

    private lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .darkGray
        view.isHidden = true
        return view
    }()

    private var isDragging: Bool = false {
        didSet {
            lineView.isHidden = lrcConfig.isHiddenSeparator || !isDragging
        }
    }

    private var currentTime: TimeInterval = 0

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let margin = tableView.frame.height * 0.5
        tableView.contentInset = UIEdgeInsets(top: 0,
                                              left: 0,
                                              bottom: margin,
                                              right: 0)

        gradientLayer.frame = CGRect(x: 0,
                                     y: 0,
                                     width: bounds.width,
                                     height: lrcConfig.bottomMaskHeight > 0 ? lrcConfig.bottomMaskHeight : bounds.height)
        tableView.superview?.layer.addSublayer(gradientLayer)
    }

    private func setupUI() {
        backgroundColor = .clear
        statckView.translatesAutoresizingMaskIntoConstraints = false
        loadView.translatesAutoresizingMaskIntoConstraints = false
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tipsLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(statckView)
        statckView.addArrangedSubview(loadView)
        statckView.addArrangedSubview(tableView)
        tableView.addSubview(tipsLabel)
        addSubview(lineView)

        statckView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        statckView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        statckView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        statckView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true

        tipsLabel.centerXAnchor.constraint(equalTo: tableView.centerXAnchor).isActive = true
        tipsLabel.centerYAnchor.constraint(equalTo: tableView.centerYAnchor).isActive = true

        lineView.leadingAnchor.constraint(equalTo: tableView.leadingAnchor).isActive = true
        lineView.trailingAnchor.constraint(equalTo: tableView.trailingAnchor).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
        lineView.centerYAnchor.constraint(equalTo: tableView.centerYAnchor).isActive = true

        updateUI()
    }

    private var preTime: TimeInterval = 0
    func start(currentTime: TimeInterval) {
        if tableView.backgroundColor != .clear {
            tableView.backgroundColor = .clear
            tableView.separatorStyle = .none
        }
        guard !(dataArray?.isEmpty ?? false) else { return }
        let time: TimeInterval = lrcDatas == nil ? 1000AgoraLrcScoreView/AgoraLrcView/FileDownloadCache/AgoraDownLoadManager.swift
        : 1
        if self.currentTime == 0 {
            loadView.beginAnimation()
        }
        var beginTime = (dataArray?.first as? AgoraMiguLrcSentence)?.startTime() ?? 0
        if beginTime <= 0 {
            beginTime = (dataArray?.first as? AgoraLrcModel)?.time ?? 0
        }
        if currentTime > beginTime {
            loadView.hiddenLoadView()
        }
        self.currentTime = currentTime * time
        preTime = currentTime
        updatePerSecond()
    }

    func reset() {
        currentTime = 0
        miguSongModel = nil
        lrcDatas = nil
    }

    private func updateUI() {
        tipsLabel.text = lrcConfig.tipsString
        tipsLabel.textColor = lrcConfig.tipsColor
        tipsLabel.font = lrcConfig.tipsFont
        lineView.backgroundColor = lrcConfig.separatorLineColor
        loadView.lrcConfig = lrcConfig
        loadView.isHidden = lrcConfig.isHiddenWatitingView
        tableView.isScrollEnabled = lrcConfig.isDrag
        gradientLayer.locations = lrcConfig.bottomMaskLocations
        gradientLayer.colors = lrcConfig.bottomMaskColors
        gradientLayer.isHidden = lrcConfig.isHiddenBottomMask
    }

    // MARK: - 更新歌词的时间

    private func updatePerSecond() {
        if lrcDatas != nil {
            if let lrc = getLrc() {
                scrollRow = lrc.index ?? 0
                progress = lrc.progress ?? 0
                currentPlayerLrc?(lrc.lrcText ?? "", progress)
            }
            return
        }
        if let lrc = getXmlLrc() {
            scrollRow = lrc.index ?? 0
            progress = lrc.progress ?? 0
            currentPlayerLrc?(lrc.lrcText ?? "", progress)
        }
    }

    // MARK: - 获取播放歌曲的信息

    // 获取xml类型的歌词信息
    private func getXmlLrc() -> (index: Int?,
                                 lrcText: String?,
                                 progress: CGFloat?)?
    {
        guard let lrcArray = miguSongModel?.sentences,
              !lrcArray.isEmpty else { return nil }
        var i = 0
        var progress: CGFloat = 0.0
        // 歌词滚动显示
        for (index, lrc) in lrcArray.enumerated() {
            let currentLrc = lrc
            var nextLrc: AgoraMiguLrcSentence?
            // 获取下一句歌词
            var nextStartTime: TimeInterval = 0
            if index == lrcArray.count - 1 {
                nextLrc = lrcArray[index]
                nextStartTime = nextLrc?.endTime() ?? 0
            } else {
                nextLrc = lrcArray[index + 1]
                nextStartTime = nextLrc?.startTime() ?? 0
            }
            if currentTime >= currentLrc.startTime(),
               currentLrc.startTime() > 0,
               currentTime < nextStartTime
            {
                i = index
                progress = currentLrc.getProgress(with: currentTime)
                return (i, currentLrc.toSentence(), progress)
            }
        }
        return nil
    }

    // 获取lrc格式的歌词信息
    func getLrc() -> (index: Int?, lrcText: String?, progress: CGFloat?)? {
        guard let lrcArray = lrcDatas,
              !lrcArray.isEmpty else { return nil }
        var i = 0
        var progress: CGFloat = 0.0
        for (index, lrc) in lrcArray.enumerated() {
            let currrentLrc = lrc
            var nextLrc: AgoraLrcModel?
            // 获取下一句歌词
            if index == lrcArray.count - 1 {
                nextLrc = lrcArray[index]
            } else {
                nextLrc = lrcArray[index + 1]
            }

            if currentTime >= currrentLrc.time, currentTime < (nextLrc?.time ?? 0) {
                i = index
                progress = CGFloat((currentTime - currrentLrc.time) / ((nextLrc?.time ?? 0) - currrentLrc.time))
                return (i, currrentLrc.lrc, progress)
            }
        }
        return nil
    }
}

extension AgoraLrcView: AgoraLoadViewDelegate {
    func getCurrentTime() -> TimeInterval {
        guard let model = dataArray?.first else { return 0 }
        if let xmlModel = model as? AgoraMiguLrcSentence {
            return xmlModel.startTime() / 1000 - currentTime / 1000
        } else if let lrcModel = model as? AgoraLrcModel {
            return lrcModel.time - currentTime
        }
        return 0
    }
}

extension AgoraLrcView: UITableViewDataSource, UITableViewDelegate {
    func tableView(_: UITableView, numberOfRowsInSection _: Int) -> Int {
        dataArray?.count ?? 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "AgoaraLrcViewCell", for: indexPath) as! AgoraMusicLrcCell
        cell.lrcConfig = lrcConfig
        let lrcModel = dataArray?[indexPath.row]
        if lrcModel is AgoraMiguLrcSentence {
            cell.setupMusicXmlLrc(with: lrcModel as? AgoraMiguLrcSentence, progress: 0)
        } else {
            cell.setupMusicLrc(with: lrcModel as? AgoraLrcModel, progress: 0)
        }
        if indexPath.row == 0, preRow < 0 {
            cell.setupCurrentLrcScale()
        }
        return cell
    }

    func scrollViewWillBeginDragging(_: UIScrollView) {
        isDragging = true
    }

    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate _: Bool) {
        dragCellHandler(scrollView: scrollView)
    }

    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        dragCellHandler(scrollView: scrollView)
    }

    private func dragCellHandler(scrollView: UIScrollView) {
        isDragging = false
        let point = CGPoint(x: 0, y: scrollView.contentOffset.y + scrollView.bounds.height * 0.5)
        guard let indexPath = tableView.indexPathForRow(at: point) else { return }
        guard let model = dataArray?[indexPath.row] else { return }
        if let xmlModel = model as? AgoraMiguLrcSentence {
            seekToTime?(xmlModel.startTime() / 1000)
        } else if let lrcModel = model as? AgoraLrcModel {
            seekToTime?(lrcModel.time)
        }
        loadView.hiddenLoadView()
    }
}
