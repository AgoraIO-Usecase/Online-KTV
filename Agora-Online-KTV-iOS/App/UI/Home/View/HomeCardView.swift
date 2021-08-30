//
//  HomeCardView.swift
//  LiveKtv
//
//  Created by XC on 2021/6/8.
//

import Core
import RxCocoa
import RxSwift
import UIKit

protocol HomeCardDelegate: AnyObject {
    func onTapCard(with room: LiveKtvRoom)
}

final class HomeCardView: UICollectionViewCell {
    fileprivate static let padding: CGFloat = 10

    // fileprivate let onRoomChanged: PublishRelay<Room> = PublishRelay()
    weak var delegate: HomeCardDelegate?
    let disposeBag = DisposeBag()

    fileprivate static let font = UIFont.systemFont(ofSize: 12)
    private let textStyle: [NSAttributedString.Key: Any] = {
        let style = NSMutableParagraphStyle()
        let shadow = NSShadow()
        shadow.shadowColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5)
        shadow.shadowBlurRadius = 2
        shadow.shadowOffset = CGSize(width: 0, height: 1)
        let attributes = [
            NSAttributedString.Key.font: HomeCardView.font,
            NSAttributedString.Key.paragraphStyle: style,
            NSAttributedString.Key.shadow: shadow,
        ]
        return attributes
    }()

    private var getRoomDisposable: Disposable?

    var room: LiveKtvRoom! {
        didSet {
            title.attributedText = NSAttributedString(string: room.channelName, attributes: textStyle)
            avatar.countView.attributedText = NSAttributedString(string: String(room.count), attributes: textStyle)
            cover.image = UIImage(named: LiveKtvRoom.getLocalCover(cover: room.cover), in: Utils.bundle, with: nil)
            getRoomDisposable?.dispose()
            getRoomDisposable = LiveKtvRoom.queryMemberCount(by: room.id)
                .observe(on: MainScheduler.instance)
                .subscribe { [weak self] countResult in
                    guard let self = self else {
                        return
                    }
                    if countResult.success {
                        self.avatar.countView.text = String(countResult.data!)
                    } else {
                        self.avatar.countView.text = "--"
                    }
                } onError: { [weak self] _ in
                    guard let self = self else {
                        return
                    }
                    self.avatar.countView.text = "--"
                }
        }
    }

    var cover: UIImageView = {
        let view = RoundImageView()
        view.radius = 8
        view.color = nil
        view.borderWidth = 0
        return view
    }()

    var avatar: AvatarView = {
        let view = AvatarView()
        return view
    }()

    var title: UILabel = {
        let view = UILabel()
        view.textColor = UIColor(hex: Colors.Text)
        view.numberOfLines = 1
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor.clear

        addSubview(cover)
//        addSubview(avatar)
        addSubview(title)

        cover.fill(view: self)
            .active()

        title.marginLeading(anchor: leadingAnchor, constant: HomeCardView.padding)
            .marginBottom(anchor: bottomAnchor, constant: HomeCardView.padding)
            .active()

        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(onTapCard)))
    }

    @objc func onTapCard() {
        delegate?.onTapCard(with: room)
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        rounded(radius: 8)
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

    class AvatarView: UIView {
        var avatar: UIImageView = {
            let view = UIImageView()
            view.image = UIImage(named: "iconMine", in: Utils.bundle, with: nil)
            return view
        }()

        var countView: UILabel = {
            let view = UILabel()
            view.numberOfLines = 1
            view.textColor = UIColor(hex: Colors.Text)
            return view
        }()

        override init(frame: CGRect) {
            super.init(frame: frame)
            backgroundColor = .clear
            addSubview(countView)
            addSubview(avatar)
            countView.marginTrailing(anchor: trailingAnchor)
                .centerY(anchor: centerYAnchor)
                .active()
            avatar
                .width(constant: 18)
                .height(constant: 18)
                .marginLeading(anchor: leadingAnchor)
                .marginTrailing(anchor: countView.leadingAnchor, constant: 4)
                .marginTop(anchor: topAnchor)
                .centerY(anchor: centerYAnchor)
                .active()
        }

        @available(*, unavailable)
        required init?(coder _: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
    }

    deinit {
        getRoomDisposable?.dispose()
        Logger.log(self, message: "deinit", level: .info)
    }

    static func sizeForItem(room _: LiveKtvRoom, width: CGFloat) -> CGSize {
        return CGSize(width: width, height: width)
    }
}
