//
//  Dialog.swift
//  InteractivePodcast
//
//  Created by XC on 2021/3/13.
//

import Foundation
import RxSwift
import UIKit

public enum DialogStyle: Int {
    case center = 233
    case bottom
    case top
    case topNoMask
    case bottomNoMask

    static func valueOf(style: Int) -> DialogStyle {
        if style == DialogStyle.bottom.rawValue {
            return .bottom
        } else if style == DialogStyle.top.rawValue {
            return .top
        } else if style == DialogStyle.bottomNoMask.rawValue {
            return .bottomNoMask
        } else if style == DialogStyle.topNoMask.rawValue {
            return .topNoMask
        } else {
            return .center
        }
    }
}

public protocol DialogDelegate: AnyObject {
    func show(dialog: UIView, style: DialogStyle, padding: CGFloat, relation: UIView.Relation, onDismiss: (() -> Void)?) -> Single<Bool>
    func dismiss(dialog: UIView) -> Single<Bool>
    func show(message: String?, type: NotificationType, duration: CGFloat)
}

open class Dialog: UIView {
    public let disposeBag = DisposeBag()

    override public init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    @available(*, unavailable)
    public required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override open func layoutSubviews() {
        super.layoutSubviews()
        render()
    }

    open func setup() {}
    open func render() {}

    public func show(
        controller: DialogDelegate,
        style: DialogStyle = .bottom,
        padding: CGFloat = 0,
        onDismiss: (() -> Void)? = nil
    ) {
        controller.show(dialog: self, style: style, padding: padding, relation: UIView.Relation.equal, onDismiss: onDismiss)
            .subscribe()
            .disposed(by: disposeBag)
    }

    public func dismiss(controller: BaseViewContoller) {
        controller.dismiss(dialog: self)
            .subscribe()
            .disposed(by: disposeBag)
    }
}
