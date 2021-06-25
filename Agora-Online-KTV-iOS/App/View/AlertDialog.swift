//
//  AlertDialog.swift
//  LiveKtv
//
//  Created by XC on 2021/6/9.
//

import Core
import Foundation
import UIKit

class AlertDialog: UIView {
    var cancelAction: (() -> Void)?
    var okAction: (() -> Void)?

    private var titleView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 17, weight: .medium)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Black)
        view.textAlignment = .center
        return view
    }()

    private var messageView: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 17)
        view.numberOfLines = 0
        view.textColor = UIColor.black.withAlphaComponent(0.5)
        view.textAlignment = .center
        return view
    }()

    private var hLineView: UIView = {
        let line = UIView()
        line.backgroundColor = UIColor(hex: "#EEE")
        return line
    }()

    private var vLineView: UIView = {
        let line = UIView()
        line.backgroundColor = UIColor(hex: "#EEE")
        return line
    }()

    private var cancelButton: UIButton = {
        let view = RoundButton()
        view.borderColor = nil
        view.setTitle("Cancel".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Blue), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 17)
        return view
    }()

    private var okButton: UIButton = {
        let view = RoundButton()
        view.setTitle("Ok".localized, for: .normal)
        view.borderColor = nil
        view.setTitleColor(UIColor(hex: Colors.Blue), for: .normal)
        view.backgroundColor = .clear
        view.titleLabel?.font = UIFont.systemFont(ofSize: 17, weight: .medium)
        return view
    }()

    init(title: String, message: String) {
        super.init(frame: CGRect())
        titleView.text = title
        messageView.text = message

        addSubview(titleView)
        addSubview(messageView)
        addSubview(hLineView)
        addSubview(vLineView)
        addSubview(cancelButton)
        addSubview(okButton)

        titleView.marginTop(anchor: topAnchor, constant: 28)
            .marginLeading(anchor: leadingAnchor, constant: 28)
            .centerX(anchor: centerXAnchor)
            .active()

        messageView.width(constant: 240, relation: .lessOrEqual)
            .marginTop(anchor: titleView.bottomAnchor, constant: 16)
            .marginLeading(anchor: leadingAnchor, constant: 30)
            .centerX(anchor: centerXAnchor)
            .active()

        hLineView.height(constant: 1)
            .marginLeading(anchor: leadingAnchor)
            .centerX(anchor: centerXAnchor)
            .marginTop(anchor: messageView.bottomAnchor, constant: 23)
            .active()

        vLineView.width(constant: 1)
            .height(constant: 44)
            .marginTop(anchor: hLineView.bottomAnchor)
            .centerX(anchor: centerXAnchor)
            .marginBottom(anchor: bottomAnchor)
            .active()

        cancelButton
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: vLineView.leadingAnchor)
            .centerY(anchor: vLineView.centerYAnchor)
            .active()

        okButton
            .marginLeading(anchor: vLineView.trailingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .centerY(anchor: vLineView.centerYAnchor)
            .active()

        backgroundColor = UIColor.white
        okButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(ok(_:))))
        cancelButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(cancel(_:))))
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        rounded(radius: 12)
        shadow()
    }

    @objc func cancel(_: UITapGestureRecognizer? = nil) {
        cancelAction?()
    }

    @objc func ok(_: UITapGestureRecognizer? = nil) {
        okAction?()
    }
}
