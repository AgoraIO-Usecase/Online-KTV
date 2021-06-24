//
//  SelectMVDialog.swift
//  LiveKtv
//
//  Created by XC on 2021/6/10.
//

import Core
import Foundation
import UIKit

private class MVCardView: UICollectionViewCell {
    weak var delegate: SelectMVDialog?
    var mv: String! {
        didSet {
            if let mv = mv {
                cover.image = UIImage(named: mv, in: Utils.bundle, with: nil)
            }
        }
    }

    override var isSelected: Bool {
        didSet {
            if isSelected != oldValue {
                if isSelected {
                    addSubview(selectView)
                    selectView.height(constant: 26)
                        .marginLeading(anchor: leadingAnchor)
                        .centerX(anchor: centerXAnchor)
                        .marginBottom(anchor: bottomAnchor)
                        .active()
                } else {
                    selectView.removeFromSuperview()
                }
            }
        }
    }

    var cover: UIImageView = {
        let view = RoundImageView()
        view.radius = 2
        view.color = nil
        view.borderWidth = 0
        return view
    }()

    var selectView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: Colors.Blue)
        let icon = UIImageView()
        icon.image = UIImage(named: "iconDone", in: Utils.bundle, with: nil)
        view.addSubview(icon)
        icon.width(constant: 26)
            .height(constant: 26)
            .centerX(anchor: view.centerXAnchor)
            .centerY(anchor: view.centerYAnchor)
            .active()
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor.clear
        addSubview(cover)
        cover.fill(view: self)
            .active()
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(onTapCard)))
    }

    @objc func onTapCard() {
        if let delegate = delegate {
            delegate.delegate.viewModel.changeRoomMV(mv: mv) { [unowned self] waiting in
                self.delegate?.delegate.show(processing: waiting)
            } onSuccess: { [unowned self] in
                self.delegate?.selectMV = mv
                self.delegate?.listView.reloadData()
            } onError: { [unowned self] message in
                self.delegate?.delegate.show(message: message, type: .error)
            }
        }
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        clipsToBounds = true
        rounded(radius: 4)
    }

    override func touchesBegan(_: Set<UITouch>, with _: UIEvent?) {
        highlight()
    }

    override func touchesEnded(_: Set<UITouch>, with _: UIEvent?) {
        unhighlight()
    }

    override func touchesCancelled(_: Set<UITouch>, with _: UIEvent?) {
        unhighlight()
    }
}

class SelectMVDialog: Dialog {
    weak var delegate: RoomController!
    private let mvList: [String] = [
        "mv1", "mv2", "mv3", "mv4", "mv5", "mv6", "mv7", "mv8", "mv9",
    ]
    var selectMV: String = "mv1"

    var titleView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.DialogTitle)
        view.text = "MV"
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
            .height(constant: 360, relation: .greaterOrEqual)
            .marginBottom(anchor: bottomAnchor, constant: 25)
            .active()

        let layout = WaterfallLayout()
        layout.delegate = self
        layout.sectionInset = UIEdgeInsets(top: 0, left: 15, bottom: 0, right: 15)
        layout.minimumLineSpacing = 20
        layout.minimumInteritemSpacing = 8

        listView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        listView.collectionViewLayout = layout
        listView.register(MVCardView.self, forCellWithReuseIdentifier: NSStringFromClass(MVCardView.self))
        listView.dataSource = self
    }

    override func render() {
        roundCorners([.topLeft, .topRight], radius: 10)
        shadow()
    }

    func show(delegate: RoomController) {
        self.delegate = delegate
        selectMV = LiveKtvRoom.getLocalMV(cover: delegate.viewModel.room.mv)
        show(controller: delegate)
    }
}

extension SelectMVDialog: WaterfallLayoutDelegate {
    public func collectionView(_: UICollectionView, layout: WaterfallLayout, sizeForItemAt _: IndexPath) -> CGSize {
        let width = (layout.collectionViewContentSize.width - 15 * 2 - 8 * 2) / 3
        return CGSize(width: width, height: width)
    }

    public func collectionViewLayout(for _: Int) -> WaterfallLayout.Layout {
        return .waterfall(column: 3, distributionMethod: .balanced)
    }
}

extension SelectMVDialog: UICollectionViewDataSource {
    public func numberOfSections(in _: UICollectionView) -> Int {
        return 1
    }

    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let card: MVCardView = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(MVCardView.self), for: indexPath) as! MVCardView
        card.delegate = self
        card.mv = mvList[indexPath.item]
        card.isSelected = card.mv == selectMV
        return card
    }

    public func collectionView(_: UICollectionView, numberOfItemsInSection _: Int) -> Int {
        return mvList.count
    }
}
