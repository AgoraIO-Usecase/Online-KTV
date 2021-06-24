//
//  ProcessingView.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/6.
//

import Foundation
import UIKit

class ProcessingView: UIView {
    private var indeicator: UIActivityIndicatorView = {
        let view = UIActivityIndicatorView()
        if #available(iOS 13.0, *) {
            view.style = .large
        }
        view.color = UIColor(hex: "#FFF")
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor(hex: "#000").withAlphaComponent(0.65)
        rounded(radius: 10)
        addSubview(indeicator)
        indeicator
            .width(constant: 40)
            .height(constant: 40)
            .centerX(anchor: centerXAnchor)
            .centerY(anchor: centerYAnchor)
            .active()
        indeicator.startAnimating()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    static func create() -> ProcessingView {
        return ProcessingView(frame: CGRect(x: 0, y: 0, width: 128, height: 128))
    }
}
