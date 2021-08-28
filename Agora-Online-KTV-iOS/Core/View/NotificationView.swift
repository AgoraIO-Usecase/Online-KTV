//
//  NotificationView.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/6.
//

import Foundation
import UIKit

public class NotificationView: UIView {
    public var icon: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "redcaution", in: Bundle(identifier: "io.agora.Core"), with: nil)
        view.width(constant: 22)
            .height(constant: 22)
            .active()
        return view
    }()

    public var message: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 13)
        view.textColor = UIColor(hex: Colors.White)
        view.numberOfLines = 0
        return view
    }()

    var type: NotificationType = .info

    override public func layoutSubviews() {
        super.layoutSubviews()
        shadow()
        switch type {
        case .info, .warning, .done:
            backgroundColor = UIColor(hex: "#1E3763")
            rounded(color: "#2F67CA", borderWidth: 1, radius: 8)
            addSubview(message)
            message.fill(view: self, leading: 16, top: 12, trailing: 16, bottom: 12)
                .height(constant: 13, relation: .greaterOrEqual)
                .active()
        case .error:
            backgroundColor = UIColor(hex: "#322222")
            rounded(color: "#663D3C", borderWidth: 1, radius: 8)
            addSubview(icon)
            addSubview(message)
            icon.marginLeading(anchor: leadingAnchor, constant: 16)
                .centerY(anchor: centerYAnchor)
                .active()
            message.marginLeading(anchor: icon.trailingAnchor, constant: 5)
                .marginTrailing(anchor: trailingAnchor, constant: 16)
                .marginTop(anchor: topAnchor, constant: 12)
                .centerY(anchor: centerYAnchor)
                .active()
        }
    }

    static func create(message: String, type: NotificationType) -> NotificationView {
        let view = NotificationView()
        view.type = type
        view.message.text = message
        return view
    }
}
