//
//  CenterNotification.swift
//  LiveKtv
//
//  Created by XC on 2021/6/9.
//

import Core
import Foundation
import UIKit

class CenterNotification: UIView {
    private var icon: UIImageView = {
        let view = UIImageView()
        view.width(constant: 16)
            .height(constant: 16)
            .active()
        return view
    }()

    private var message: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.textColor = UIColor(hex: Colors.Text)
        view.numberOfLines = 0
        return view
    }()

    init(type: NotificationType, info: String) {
        super.init(frame: CGRect())
        backgroundColor = UIColor.black
        rounded(color: nil, borderWidth: 0, radius: 8)

        message.text = info
        switch type {
        case .error, .warning:
            icon.image = UIImage(named: "iconWarning", in: Utils.bundle, with: nil)
        default:
            icon.image = UIImage(named: "iconDone", in: Utils.bundle, with: nil)
        }

        switch type {
        case .error, .warning, .done:
            addSubview(icon)
            addSubview(message)
            icon.marginLeading(anchor: leadingAnchor, constant: 15)
                .centerY(anchor: centerYAnchor)
                .active()
            message.marginLeading(anchor: icon.trailingAnchor, constant: 5)
                .marginTrailing(anchor: trailingAnchor, constant: 15)
                .marginTop(anchor: topAnchor, constant: 12)
                .centerY(anchor: centerYAnchor)
                .active()
        default:
            addSubview(message)
            message.fill(view: self, leading: 15, top: 15, trailing: 15, bottom: 15)
                .active()
        }
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override public func layoutSubviews() {
        super.layoutSubviews()
        shadow()
    }

    static func create(message: String, type: NotificationType) -> CenterNotification {
        return CenterNotification(type: type, info: message)
    }
}

extension BaseViewContoller {
    func show(message: String?, type: NotificationType, duration: CGFloat = 1.5) {
        guard let message = message else {
            return
        }
        DispatchQueue.main.async { [weak self] in
            if let self = self {
                let view = CenterNotification.create(message: message, type: type)
                view.alpha = 0
                let root = self.addViewTop(view)
                view.marginLeading(anchor: root.leadingAnchor, constant: 16, relation: .greaterOrEqual)
                    .centerX(anchor: root.centerXAnchor)
                    .centerY(anchor: root.centerYAnchor)
                    .active()
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    view.alpha = 1
                }, completion: { _ in
                    UIView.animate(withDuration: 0.3, delay: TimeInterval(duration), options: .curveEaseInOut, animations: {
                        view.alpha = 0
                    }, completion: { _ in
                        view.removeFromSuperview()
                    })
                })
            }
        }
    }
}
