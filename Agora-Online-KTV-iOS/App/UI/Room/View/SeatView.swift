//
//  SeatView.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import RxSwift
import UIKit

class SeatView {
    weak var delegate: RoomControlDelegate!
    weak var root: UIView!

    weak var icon: UIImageView!

    weak var label: UILabel!

    weak var canvas: UIView!

    private var getUserDisposable: Disposable?

    var music: LiveKtvMusic? {
        didSet {
            onMusicChange()
        }
    }

    var member: LiveKtvMember? {
        didSet {
            if let member = member {
                if member.userId == oldValue?.userId {
                    return
                }
                getUserDisposable?.dispose()
                getUserDisposable = User.getUser(by: member.userId)
                    .observe(on: MainScheduler.instance)
                    .subscribe { [weak self] result in
                        guard let self = self else {
                            return
                        }
                        if result.success {
                            let user = result.data!
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
            } else {
                icon.image = UIImage(named: "seat", in: Utils.bundle, with: nil)
            }
            onMusicChange()
        }
    }

    private let index: Int

    init(index: Int) {
        self.index = index
    }

    private func onMusicChange() {
        if let music = music, let member = member, music.isOrderBy(member: member), member.isSpeaker() {
            if music.type == LiveKtvMusic.CHORUS {
                if music.isChorusReady() {
                    label.text = "合唱中"
                    root.shadowAnimation(color: Colors.Music)
                } else {
                    label.text = "准备中"
                    root.stopShadowAnimation()
                }
            } else {
                label.text = "演唱中"
                root.shadowAnimation(color: Colors.Music)
            }
            return
        } else if let member = member {
            if let music = music, music.type == LiveKtvMusic.CHORUS, music.user1Id == member.userId, member.isSpeaker() {
                if music.isChorusReady() {
                    label.text = "合唱中"
                    root.shadowAnimation(color: Colors.Music)
                } else {
                    label.text = "准备中"
                    root.stopShadowAnimation()
                }
                return
            }
            if member.isManager {
                label.text = "房主"
                root.stopShadowAnimation()
                return
            }
        }
        label.text = String(index + 1)
        root.stopShadowAnimation()
    }

    func subcribeUIEvent() {
        root.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(onTapIcon)))
    }

    private func setIconImage(named: String) {
        icon.image = UIImage(named: named)
    }

    @objc func onTapIcon() {
        Logger.log(message: "onTapIcon \(index + 1)", level: .info)
        delegate.onTap(view: self)
    }

    deinit {
        // Logger.log(self, message: "deinit", level: .info)
        getUserDisposable?.dispose()
        getUserDisposable = nil
    }
}
