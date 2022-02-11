//
//  MoreDialog.swift
//  app
//
//  Created by xianing on 2021/11/26.
//

import Core
import Foundation
import RxSwift
import UIKit

class MoreDialog: Dialog {
    weak var delegate: RoomController!
    private var getUserDisposable: Disposable?

    var cameraMuted: Bool = false
    var openVoiceDialog: Bool = false

    var labelVoiceEffect: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Text).withAlphaComponent(0.8)
        view.text = "音效"
        return view
    }()

    var labelCamera: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 14)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Text).withAlphaComponent(0.8)
        view.text = "摄像头"
        return view
    }()

    var btnVoiceEffect: UIButton = {
        let view = UIButton()
        view.setImage(UIImage(named: "iconVoiceEffect", in: Utils.bundle, with: nil), for: .normal)
        return view
    }()

    var btnCamera: UIButton = {
        let view = UIButton()
        view.setImage(UIImage(named: "iconCameraOn", in: Utils.bundle, with: nil), for: .normal)
        return view
    }()

    override func setup() {
        backgroundColor = UIColor(hex: Colors.Primary)
        addSubview(labelCamera)
        addSubview(labelVoiceEffect)
        addSubview(btnCamera)
        addSubview(btnVoiceEffect)

        btnVoiceEffect.marginTop(anchor: topAnchor, constant: 26)
            .marginLeading(anchor: leadingAnchor, constant: 100)
            .active()
        labelVoiceEffect.marginTop(anchor: btnVoiceEffect.bottomAnchor, constant: 10)
            .centerX(anchor: btnVoiceEffect.centerXAnchor)
            .active()

        btnCamera.marginTop(anchor: topAnchor, constant: 26)
            .marginTrailing(anchor: trailingAnchor, constant: 100)
            .active()
        labelCamera.marginTop(anchor: btnCamera.bottomAnchor, constant: 10)
            .marginBottom(anchor: bottomAnchor, constant: 30)
            .centerX(anchor: btnCamera.centerXAnchor)
            .active()

        btnCamera.addTarget(self, action: #selector(onTapCamera(sender:)), for: .touchUpInside)
        btnVoiceEffect.addTarget(self, action: #selector(onTapVoiceEffect(sender:)), for: .touchUpInside)
    }

    @objc func onTapCamera(sender _: UIButton) {
        cameraMuted = !cameraMuted
        btnCamera.setImage(UIImage(named: cameraMuted ? "iconCameraOff" : "iconCameraOn", in: Utils.bundle, with: nil), for: .normal)
        delegate.viewModel.openVideoHandler(isOpen: cameraMuted)
    }

    @objc func onTapVoiceEffect(sender _: UIButton) {
        openVoiceDialog = true
        dismiss(controller: delegate)
    }

    override func render() {
        roundCorners([.topLeft, .topRight], radius: 10)
        shadow()
    }

    func show(delegate: RoomController) {
        self.delegate = delegate
        show(controller: delegate) {
            if self.openVoiceDialog {
                sleep(1)
                VoiceEffectDialog().show(delegate: delegate)
                self.openVoiceDialog = false
            }
        }
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
        getUserDisposable?.dispose()
        getUserDisposable = nil
    }
}
