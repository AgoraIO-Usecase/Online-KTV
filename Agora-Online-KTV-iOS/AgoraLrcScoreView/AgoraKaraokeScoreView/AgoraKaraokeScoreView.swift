//
//  AgoraKaraokeScoreView.swift
//  lineTTTT
//
//  Created by zhaoyongqiang on 2021/12/8.
//  Copyright © 2021 km. All rights reserved.
//

import UIKit

@objcMembers
class AgoraKaraokeScoreView: UIView {
    // MARK: 公开属性

    public weak var delegate: AgoraKaraokeScoreDelegate?

    var lrcSentence: [AgoraMiguLrcSentence]? {
        didSet {
            createScoreData(data: lrcSentence)
        }
    }

    /// 线的配置
    public var scoreConfig: AgoraScoreItemConfigModel = .init() {
        didSet {
            updateUI()
        }
    }

    // MARK: 私有

    private var dataArray: [AgoraScoreItemModel]? {
        didSet {
            collectionView.reloadData()
            totalScore = Double(dataArray?.filter { $0.isEmptyCell == false }.count ?? 0) * 2
        }
    }

    private lazy var flowLayout: UICollectionViewFlowLayout = {
        let flowLayout = UICollectionViewFlowLayout()
        flowLayout.scrollDirection = .horizontal
        flowLayout.estimatedItemSize = .zero
        flowLayout.minimumInteritemSpacing = 0
        flowLayout.minimumLineSpacing = 0
        flowLayout.footerReferenceSize = .zero
        flowLayout.headerReferenceSize = .zero
        return flowLayout
    }()

    private lazy var collectionView: UICollectionView = {
        let collectionView = UICollectionView(frame: .zero, collectionViewLayout: flowLayout)
        collectionView.showsVerticalScrollIndicator = false
        collectionView.showsHorizontalScrollIndicator = false
        collectionView.backgroundColor = .clear
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.register(AgoraKaraokeScoreCell.self, forCellWithReuseIdentifier: "AgoraKaraokeScoreCell")
        collectionView.isScrollEnabled = false
        return collectionView
    }()

    private lazy var separatorVerticalLine: UIView = {
        let view = UIView()
        view.backgroundColor = .systemPink
        return view
    }()

    private lazy var separatorTopLine: UIView = {
        let view = UIView()
        view.backgroundColor = .systemPink
        return view
    }()

    private lazy var separatorBottomLine: UIView = {
        let view = UIView()
        view.backgroundColor = .systemPink
        return view
    }()

    private lazy var cursorView: UIView = {
        let view = UIView()
        view.backgroundColor = .red
        view.layer.masksToBounds = true
        return view
    }()

    private lazy var emitterView = AgoraEmitterView()
    private lazy var triangleView = AgoraTriangleView()

    private var animationDuration: TimeInterval = 0.25
    private var status: AgoraKaraokeScoreStatus = .`init`
    private var verticalLineLeadingCons: NSLayoutConstraint?
    private var cursorTopCons: NSLayoutConstraint?
    private var currentTime: TimeInterval = 0
    private var isDrawingCell: Bool = false {
        didSet {
            updateDraw()
        }
    }

    private var totalScore: Double = 0
    private var currentScore: Double = 50
    private var isInsertEnd: Bool = false

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func reset() {
        currentScore = scoreConfig.defaultScore
        currentTime = 0
        isInsertEnd = false
    }

    func start(currentTime: TimeInterval, totalTime: TimeInterval) {
        self.currentTime = currentTime
        guard currentTime > 0 else { return }
        if isInsertEnd == false {
            guard let model = insertEndLrcData(lrcData: lrcSentence, totalTime: totalTime) else { return }
            dataArray?.append(model)
            isInsertEnd = true
            return
        }
        emitterView.setupEmitterPoint(point: cursorView.center)
        let contentWidth = collectionView.contentSize.width - frame.width
        let rate = currentTime / totalTime
        let pointX = contentWidth * rate
        collectionView.setContentOffset(CGPoint(x: pointX, y: 0),
                                        animated: false)
    }

    public func setVoicePitch(_ voicePitch: [Double]) {
        calcuSongScore(pitch: voicePitch.last ?? 0)
    }

    private var preModel: AgoraScoreItemModel?
    private func calcuSongScore(pitch: Double) {
        let time = currentTime * 1000 - 30
        guard let model = dataArray?.first(where: { time >= $0.startTime * 1000 && $0.endTime * 1000 >= time }), model.isEmptyCell == false
        else {
            isDrawingCell = false
            cursorAnimation(y: scoreConfig.scoreViewHeight - scoreConfig.cursorHeight * 0.5, isDraw: false)
            triangleView.updateAlpha(at: 0)
            return
        }

        let y = pitchToY(min: model.pitchMin, max: model.pitchMax, pitch)

        // 计算线的中心位置
        let lineCenterY = (model.topKM + scoreConfig.lineHeight) - scoreConfig.lineHeight * 0.5
        var score = 100 - abs(y - lineCenterY)
        score = score > 100 ? 100 : score < 0 ? 0 : score

        if preModel?.startTime == model.startTime,
           preModel?.endTime == model.endTime,
           score >= 85
        {
            cursorAnimation(y: y, isDraw: true)
            triangleView.updateAlpha(at: pitch <= 0 ? 0 : score / 100)
            return
        }

        if score >= 95, pitch > 0 {
            cursorAnimation(y: y, isDraw: true)
            triangleView.updateAlpha(at: pitch <= 0 ? 0 : score / 100)
            currentScore += 2
            preModel = model

        } else if score >= 85, pitch > 0 {
            cursorAnimation(y: y, isDraw: true)
            triangleView.updateAlpha(at: pitch <= 0 ? 0 : score / 100)
            currentScore += 1
            preModel = model

        } else {
            cursorAnimation(y: y, isDraw: false)
            triangleView.updateAlpha(at: 0)
        }
        delegate?.agoraKaraokeScore?(score: currentScore > totalScore ? totalScore : currentScore,
                                     totalScore: totalScore)
    }

    private func cursorAnimation(y: CGFloat, isDraw: Bool) {
        cursorTopCons?.constant = y - scoreConfig.cursorHeight * 0.5
        cursorTopCons?.isActive = true
        if isDraw {
            isDrawingCell = true
        }
        UIView.animate(withDuration: animationDuration, delay: 0, options: [.overrideInheritedCurve]) {
            self.layoutIfNeeded()
        } completion: { _ in
            self.isDrawingCell = isDraw
        }
    }

    private func updateDraw() {
        status = isDrawingCell ? .drawing : .new_layer
        if isDrawingCell {
            emitterView.startEmittering()
        } else {
            emitterView.stopEmittering()
        }
    }

    private func pitchToY(min: CGFloat, max: CGFloat, _ value: CGFloat) -> CGFloat {
        let viewH = scoreConfig.scoreViewHeight - scoreConfig.lineHeight
        let y = viewH - (viewH / (max - min) * (value - min))
        return y.isNaN ? 0 : y
    }

    private func calcuToWidth(time: TimeInterval) -> CGFloat {
        let w = scoreConfig.lineWidht * time
        return w.isNaN ? 0 : w
    }

    private func createScoreData(data: [AgoraMiguLrcSentence]?) {
        guard let lrcData = data else { return }
        var dataArray = [AgoraScoreItemModel]()
        let tones = lrcData.flatMap { $0.tones }
        if let startData = insertStartLrcData(lrcData: tones) {
            dataArray.append(startData)
        }
        var preEndTime: Double = 0
        for i in 0 ..< tones.count {
            let tone = tones[i]
            var model = AgoraScoreItemModel()
            let startTime = tone.begin / 1000
            let endTime = tone.end / 1000
            if preEndTime > 0, preEndTime != startTime {
                var model = insertMiddelLrcData(startTime: startTime,
                                                endTime: preEndTime)
                model.leftKM = dataArray.map { $0.widthKM }.reduce(0, +)
                dataArray.append(model)
            }
            model.startTime = startTime
            model.endTime = endTime
            model.pitch = Double(tone.pitch)
            model.widthKM = calcuToWidth(time: endTime - startTime)
            model.leftKM = dataArray.map { $0.widthKM }.reduce(0, +)
            model.pitchMin = CGFloat(tones.sorted(by: { $0.pitch < $1.pitch }).first?.pitch ?? 0) - 50
            model.pitchMax = CGFloat(tones.sorted(by: { $0.pitch > $1.pitch }).first?.pitch ?? 0) + 50
            model.topKM = pitchToY(min: model.pitchMin, max: model.pitchMax, CGFloat(tone.pitch))

            preEndTime = endTime
            dataArray.append(model)
        }
        self.dataArray = dataArray
    }

    private func insertStartLrcData(lrcData: [AgoraMiguLrcTone]) -> AgoraScoreItemModel? {
        guard let firstTone = lrcData.first(where: { $0.pitch > 0 }) else { return nil }
        let endTime = firstTone.begin / 1000
        var model = AgoraScoreItemModel()
        model.widthKM = calcuToWidth(time: endTime)
        model.isEmptyCell = true
        model.startTime = 0
        model.endTime = endTime
        return model
    }

    private func insertMiddelLrcData(startTime: Double,
                                     endTime: Double) -> AgoraScoreItemModel
    {
        // 中间间隔部分
        var model = AgoraScoreItemModel()
        let time = startTime - endTime
        model.startTime = startTime
        model.endTime = endTime
        model.widthKM = calcuToWidth(time: time)
        model.isEmptyCell = true
        return model
    }

    private func insertEndLrcData(lrcData: [AgoraMiguLrcSentence]?, totalTime: TimeInterval) -> AgoraScoreItemModel? {
        guard let lrcData = lrcData else { return nil }
        let tones = lrcData.flatMap { $0.tones }
        guard let firstTone = tones.last(where: { $0.pitch > 0 }) else { return nil }
        let endTime = totalTime - (firstTone.end / 1000)
        var model = AgoraScoreItemModel()
        model.widthKM = calcuToWidth(time: endTime)
        model.isEmptyCell = true
        model.startTime = firstTone.end / 1000
        model.endTime = model.startTime + endTime
        model.leftKM = (dataArray?.last?.leftKM ?? 0) + (dataArray?.last?.widthKM ?? 0)
        return model
    }

    private func setupUI() {
        addSubview(collectionView)
        addSubview(separatorVerticalLine)
        addSubview(separatorTopLine)
        addSubview(separatorBottomLine)
        emitterView.insertSubview(triangleView, at: 0)
        addSubview(cursorView)
        addSubview(emitterView)

        collectionView.translatesAutoresizingMaskIntoConstraints = false
        separatorVerticalLine.translatesAutoresizingMaskIntoConstraints = false
        separatorTopLine.translatesAutoresizingMaskIntoConstraints = false
        separatorBottomLine.translatesAutoresizingMaskIntoConstraints = false
        cursorView.translatesAutoresizingMaskIntoConstraints = false
        triangleView.translatesAutoresizingMaskIntoConstraints = false

        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true

        verticalLineLeadingCons = separatorVerticalLine.leadingAnchor.constraint(equalTo: leadingAnchor, constant: scoreConfig.innerMargin)
        verticalLineLeadingCons?.isActive = true
        separatorVerticalLine.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        separatorVerticalLine.topAnchor.constraint(equalTo: topAnchor).isActive = true
        separatorVerticalLine.widthAnchor.constraint(equalToConstant: 1).isActive = true

        separatorTopLine.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        separatorTopLine.topAnchor.constraint(equalTo: topAnchor).isActive = true
        separatorTopLine.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        separatorTopLine.heightAnchor.constraint(equalToConstant: 1).isActive = true
        separatorBottomLine.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        separatorBottomLine.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        separatorBottomLine.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        separatorBottomLine.heightAnchor.constraint(equalToConstant: 1).isActive = true
        cursorView.centerXAnchor.constraint(equalTo: separatorVerticalLine.centerXAnchor).isActive = true
        cursorTopCons = cursorView.topAnchor.constraint(equalTo: separatorVerticalLine.topAnchor, constant: scoreConfig.scoreViewHeight - scoreConfig.cursorHeight)
        cursorView.widthAnchor.constraint(equalToConstant: scoreConfig.cursorWidth).isActive = true
        cursorView.heightAnchor.constraint(equalToConstant: scoreConfig.cursorHeight).isActive = true
        cursorTopCons?.isActive = true

        triangleView.trailingAnchor.constraint(equalTo: cursorView.leadingAnchor, constant: 1).isActive = true
        triangleView.centerYAnchor.constraint(equalTo: cursorView.centerYAnchor).isActive = true
        triangleView.widthAnchor.constraint(equalToConstant: 45).isActive = true
        triangleView.heightAnchor.constraint(equalToConstant: 6).isActive = true

        updateUI()
    }

    private func updateUI() {
        triangleView.config = scoreConfig
        emitterView.config = scoreConfig
        emitterView.isHidden = scoreConfig.isHiddenEmitterView
        cursorView.layer.cornerRadius = scoreConfig.cursorHeight * 0.5
        cursorView.backgroundColor = scoreConfig.cursorColor
        separatorTopLine.backgroundColor = scoreConfig.separatorLineColor
        separatorBottomLine.backgroundColor = scoreConfig.separatorLineColor
        separatorVerticalLine.backgroundColor = scoreConfig.separatorLineColor
        separatorTopLine.isHidden = scoreConfig.isHiddenSeparatorLine
        separatorBottomLine.isHidden = scoreConfig.isHiddenSeparatorLine
        separatorVerticalLine.isHidden = scoreConfig.isHiddenVerticalSeparatorLine
        verticalLineLeadingCons?.constant = scoreConfig.innerMargin
        verticalLineLeadingCons?.isActive = true
        cursorTopCons?.constant = scoreConfig.scoreViewHeight - scoreConfig.cursorHeight
        cursorTopCons?.isActive = true
        currentScore = scoreConfig.defaultScore
    }
}

extension AgoraKaraokeScoreView: UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout {
    func collectionView(_: UICollectionView, numberOfItemsInSection _: Int) -> Int {
        dataArray?.count ?? 0
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "AgoraKaraokeScoreCell", for: indexPath) as! AgoraKaraokeScoreCell
        let model = dataArray?[indexPath.item]
        cell.setScore(with: model, config: scoreConfig)
        return cell
    }

    func collectionView(_: UICollectionView, layout _: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        CGSize(width: dataArray?[indexPath.item].widthKM ?? 0,
               height: scoreConfig.scoreViewHeight)
    }

    func collectionView(_: UICollectionView, layout _: UICollectionViewLayout, minimumLineSpacingForSectionAt _: Int) -> CGFloat {
        0
    }

    func collectionView(_: UICollectionView, layout _: UICollectionViewLayout, minimumInteritemSpacingForSectionAt _: Int) -> CGFloat {
        0
    }

    func collectionView(_: UICollectionView, layout _: UICollectionViewLayout, insetForSectionAt _: Int) -> UIEdgeInsets {
        UIEdgeInsets(top: 0, left: scoreConfig.innerMargin, bottom: 0, right: frame.width - scoreConfig.innerMargin)
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        guard let dataArray = dataArray else { return }
        let moveX = scrollView.contentOffset.x
        for i in 0 ..< dataArray.count {
            var model = dataArray[i]
            let indexPath = IndexPath(item: i, section: 0)
            let cell = collectionView.cellForItem(at: indexPath) as? AgoraKaraokeScoreCell
            if model.leftKM < moveX, moveX < model.leftKM + model.widthKM {
                model.offsetXKM = moveX
                model.status = status
            } else if model.leftKM + model.widthKM <= moveX {
                model.status = .end
            } else if moveX <= model.leftKM {
                model.status = .`init`
            }
            cell?.setScore(with: model, config: scoreConfig)
        }
    }
}
