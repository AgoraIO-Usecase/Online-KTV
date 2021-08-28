//
//  SpeakerToolbar.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import UIKit

class SpeakerToolbar {
    weak var delegate: RoomController!
    weak var root: UIView!
    weak var micView: UIButton!
    weak var switchMVView: UIButton!
    weak var orderMusicView: UIButton!

    var musicListDialog: MusicListDialog?

    var member: LiveKtvMember? {
        didSet {
            if let member = member {
                root.isHidden = member.toLiveKtvRoomRole() == .listener
            } else {
                root.isHidden = true
            }
        }
    }

    func subcribeUIEvent() {
        onMuted(mute: delegate.viewModel.muted)
        micView.addTarget(self, action: #selector(onTapMicView), for: .touchUpInside)
        switchMVView.addTarget(self, action: #selector(onTapSwitchMV), for: .touchUpInside)
        orderMusicView.addTarget(self, action: #selector(onTapOrderMusicView), for: .touchUpInside)
    }

    @objc private func onTapMicView() {
        delegate.viewModel.selfMute(mute: !delegate.viewModel.member.isSelfMuted)
    }

    @objc private func onTapSwitchMV() {
        SelectMVDialog().show(delegate: delegate)
    }

    @objc private func onTapOrderMusicView() {
        if musicListDialog == nil {
            musicListDialog = MusicListDialog()
        }
        if let dialog = musicListDialog {
            dialog.show(delegate: delegate)
        }
    }

    func onMusicListChanged() {
        switchMVView.isHidden = delegate.viewModel.musicList.count == 0
        if let dialog = musicListDialog {
            dialog.reload()
        }
    }

    func onMuted(mute: Bool) {
        micView.setImage(UIImage(named: mute ? "iconMuted" : "iconVoice", in: Utils.bundle, with: nil), for: .normal)
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }
}
