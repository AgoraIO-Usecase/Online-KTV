//
//  VoiceEffectDialog.swift
//  app
//
//  Created by xianing on 2021/11/26.
//

import AgoraRtcKit
import Core
import Foundation
import RxSwift
import UIKit

class VoiceEffectDialog: Dialog {
    weak var delegate: RoomController!
    private var getUserDisposable: Disposable?
    var selectedEffect: AgoraAudioEffectPreset = .off

    var titleView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.DialogTitle)
        view.text = "Sound effect".localized
        return view
    }()

    var listView: UICollectionView = {
        let view = UICollectionView(frame: CGRect.zero, collectionViewLayout: UICollectionViewFlowLayout())
        view.backgroundColor = .clear
        return view
    }()

    override func setup() {
        backgroundColor = UIColor(hex: Colors.Primary)

        addSubview(titleView)
        addSubview(listView)

        titleView.marginTop(anchor: topAnchor, constant: 17)
            .marginLeading(anchor: leadingAnchor, constant: 15, relation: .greaterOrEqual)
            .centerX(anchor: centerXAnchor)
            .active()

        listView.marginTop(anchor: titleView.bottomAnchor, constant: 17)
            .marginLeading(anchor: leadingAnchor)
            .centerX(anchor: centerXAnchor)
            .height(constant: 210, relation: .greaterOrEqual)
            .marginBottom(anchor: bottomAnchor, constant: 25)
            .active()

        let layout = WaterfallLayout()
        layout.delegate = self
        layout.sectionInset = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        layout.minimumLineSpacing = 20
        layout.minimumInteritemSpacing = 8

        listView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        listView.collectionViewLayout = layout
        listView.register(VoiceChangerCell.self, forCellWithReuseIdentifier: NSStringFromClass(VoiceChangerCell.self))
        listView.dataSource = self
    }

    override func render() {
        roundCorners([.topLeft, .topRight], radius: 10)
        shadow()
    }

    func show(delegate: RoomController) {
        self.delegate = delegate
        show(controller: delegate)
    }

    deinit {
        Logger.log(self, message: "deinit effect dialog", level: .info)
        getUserDisposable?.dispose()
        getUserDisposable = nil
    }
}

enum EffectType {
    static func rolesList() -> [AgoraAudioEffectPreset] {
        return [.off,
                .roomAcousticsKTV,
                .roomAcousVocalConcer,
                .roomAcousStudio,
                .roomAcousPhonograph,
                .roomAcousSpatial,
                .roomAcousEthereal,
                .styleTransformationPopular,
                .styleTransformationRnb]
    }
}

extension VoiceEffectDialog: UICollectionViewDelegate {}

extension VoiceEffectDialog: WaterfallLayoutDelegate {
    public func collectionView(_: UICollectionView, layout: WaterfallLayout, sizeForItemAt _: IndexPath) -> CGSize {
        let width = (layout.collectionViewContentSize.width - 15 * 2 - 8 * 2) / 3
        return CGSize(width: width, height: 60)
    }

    public func collectionViewLayout(for _: Int) -> WaterfallLayout.Layout {
        return .waterfall(column: 3, distributionMethod: .balanced)
    }
}

extension VoiceEffectDialog: UICollectionViewDataSource {
    public func numberOfSections(in _: UICollectionView) -> Int {
        return 1
    }

    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let card: VoiceChangerCell = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(VoiceChangerCell.self), for: indexPath) as? VoiceChangerCell else {
            return UICollectionViewCell()
        }
        card.delegate = delegate
        card.collectionDelegate = self
        card.effect = EffectType.rolesList()[indexPath.item]
        card.effectButton.setTitle(EffectType.rolesList()[indexPath.item].description(), for: .normal)
        card.effectButton.isEnabled = (card.effect == selectedEffect) ? false : true
        return card
    }

    public func collectionView(_: UICollectionView, numberOfItemsInSection _: Int) -> Int {
        return EffectType.rolesList().count
    }
}

class VoiceChangerCell: UICollectionViewCell {
    weak var delegate: RoomController!
    weak var collectionDelegate: VoiceEffectDialog!
    var effect: AgoraAudioEffectPreset = .off

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func setupUI() {
        addSubview(effectButton)

        effectButton.addTarget(self, action: #selector(onTapEffectButton), for: .touchUpInside)

        effectButton.marginTop(anchor: topAnchor, constant: 10)
            .width(constant: 100)
            .marginLeading(anchor: leadingAnchor, constant: 10, relation: .greaterOrEqual)
            .centerX(anchor: centerXAnchor)
            .marginBottom(anchor: bottomAnchor, constant: 10)
            .active()
    }

    @objc func onTapEffectButton() {
        delegate.onSelectVoiceEffect(effect: effect)
        collectionDelegate.selectedEffect = effect
        collectionDelegate.listView.reloadData()
        collectionDelegate.dismiss(controller: delegate)
    }

    var effectButton: UIButton = {
        let view = RoundButton()
        view.borderWidth = 1
        view.borderColor = "#CCCCCC"
        view.setTitleColor(UIColor(hex: "#CCCCCC"), for: .normal)
        view.setTitleColor(UIColor(hex: "#09BDF4"), for: .disabled)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        view.backgroundColor = .clear
        return view
    }()
}
