//
//  ManageSpeakerDialog.swift
//  LiveKtv
//
//  Created by XC on 2021/6/10.
//

import Core
import Foundation
import RxSwift
import UIKit

class ManageSpeakerDialog: Dialog {
    weak var delegate: RoomController!
    private var getUserDisposable: Disposable?
    var model: LiveKtvMember! {
        didSet {
            getUserDisposable?.dispose()
            getUserDisposable = User.getUser(by: model.userId, avatar: model.avatar)
                .observe(on: MainScheduler.instance)
                .subscribe { [weak self] result in
                    guard let self = self else {
                        return
                    }
                    if result.success {
                        let user = result.data!
                        self.name.text = user.name
                        self.setIconImage(named: user.getLocalAvatar())
                    } else {
                        self.setIconImage(named: "default")
                    }
                } onError: { [weak self] _ in
                    guard let self = self else {
                        return
                    }
                    self.setIconImage(named: "default")
                }
        }
    }

    var avatar: UIImageView = {
        let view = RoundImageView()
        return view
    }()

    var name: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 12)
        view.numberOfLines = 1
        view.textColor = UIColor(hex: Colors.Text).withAlphaComponent(0.5)
        return view
    }()

    var kickButton: UIButton = {
        let view = RoundButton()
        view.borderWidth = 0
        view.setTitle("Become audience".localized, for: .normal)
        view.setTitleColor(UIColor(hex: Colors.Text), for: .normal)
        view.backgroundColor = UIColor(hex: Colors.Blue)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 17)
        return view
    }()

    override func setup() {
        backgroundColor = UIColor(hex: Colors.Primary)

        addSubview(avatar)
        addSubview(name)
        addSubview(kickButton)

        kickButton.addTarget(self, action: #selector(onTapKickButton), for: .touchUpInside)

        avatar.width(constant: 60)
            .height(constant: 60)
            .marginTop(anchor: topAnchor, constant: 24)
            .centerX(anchor: centerXAnchor)
            .active()

        name.marginTop(anchor: avatar.bottomAnchor, constant: 6)
            .marginLeading(anchor: leadingAnchor, constant: 20, relation: .greaterOrEqual)
            .centerX(anchor: centerXAnchor)
            .active()

        kickButton.width(constant: 160)
            .height(constant: 44)
            .marginTop(anchor: name.bottomAnchor, constant: 25)
            .marginBottom(anchor: safeAreaLayoutGuide.bottomAnchor, constant: 20)
            .centerX(anchor: centerXAnchor)
            .active()
    }

    @objc func onTapKickButton() {
        delegate.viewModel.kickSpeaker(member: model) { [weak self] waiting in
            if let self = self {
                self.delegate.show(processing: waiting)
            }
        } onSuccess: { [weak self] in
            if let self = self {
                self.delegate.dismiss(dialog: self, completion: nil)
            }
        } onError: { [weak self] message in
            if let self = self {
                self.delegate.show(message: message, type: .error)
            }
        }
    }

    override func render() {
        roundCorners([.topLeft, .topRight], radius: 10)
        shadow()
    }

    private func setIconImage(named: String) {
        avatar.image = UIImage(named: named, in: Bundle(identifier: "io.agora.InteractivePodcast"), with: nil)
    }

    func show(with member: LiveKtvMember, delegate: RoomController) {
        self.delegate = delegate
        model = member
        show(controller: delegate)
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
        getUserDisposable?.dispose()
        getUserDisposable = nil
    }
}
