//
//  RoomController.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import AgoraRtcKit
import Core
import Foundation
import LrcView
import UIKit

extension UIColor {
    func lighter(by percentage: CGFloat = 30.0) -> UIColor? {
        return adjust(by: abs(percentage))
    }

    func darker(by percentage: CGFloat = 30.0) -> UIColor? {
        return adjust(by: -1 * abs(percentage))
    }

    func adjust(by percentage: CGFloat = 30.0) -> UIColor? {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        if getRed(&r, green: &g, blue: &b, alpha: &a) {
            return UIColor(red: min(r + percentage / 100, 1.0),
                           green: min(g + percentage / 100, 1.0),
                           blue: min(b + percentage / 100, 1.0),
                           alpha: a)
        } else {
            return nil
        }
    }
}

protocol RoomControlDelegate: AnyObject {
    func onTap(view: SeatView)
    func onMemberListChanged()
    func onMuted(mute: Bool)
    func onRoomUpdate()
    func onRoomClosed()
    func onPlayListChanged()
    func onFetchMusic(finish: Bool)
    func onMusic(state: RtcMusicState)
    func show(processing: Bool)
    func onError(message: String?)
    func onSelectVoiceEffect(effect: AgoraAudioEffectPreset)
}

class RoomController: BaseViewContoller, DialogDelegate {
    @IBOutlet var closeRoomView: UIButton!
    @IBOutlet var roomCoverView: UIImageView!
    @IBOutlet var roomNameView: UILabel!
    @IBOutlet var player: UIView! {
        didSet {
            mvPlayer.delegate = self
            mvPlayer.player = player
        }
    }

    @IBOutlet var mvView: UIImageView! {
        didSet {
            mvPlayer.mv = mvView
        }
    }

    @IBOutlet var iconView: UIImageView! {
        didSet {
            mvPlayer.icon = iconView
            iconView.isHidden = true
        }
    }

    @IBOutlet var musicNameView: UILabel! {
        didSet {
            mvPlayer.name = musicNameView
            musicNameView.isHidden = true
        }
    }

    @IBOutlet var originSettingView: UISwitch! {
        didSet {
            mvPlayer.originSettingView = originSettingView
        }
    }

    @IBOutlet var settingsView: UIButton! {
        didSet {
            mvPlayer.settingsView = settingsView
        }
    }

    @IBOutlet var playerControlView: UIButton! {
        didSet {
            mvPlayer.playerControlView = playerControlView
        }
    }

    @IBOutlet var switchMusicView: UIButton! {
        didSet {
            mvPlayer.switchMusicView = switchMusicView
            switchMusicView.layer.cornerRadius = 17
            switchMusicView.layer.masksToBounds = true
            switchMusicView.layer.borderColor = UIColor.white.cgColor
            switchMusicView.layer.borderWidth = 1
            switchMusicView.setTitle("Cut song".localized, for: .normal)
        }
    }

    @IBOutlet var seat1Root: UIView! {
        didSet {
            seatViews[0].delegate = self
            seatViews[0].root = seat1Root
        }
    }

    @IBOutlet var seat1CanvasView: UIView! {
        didSet {
            seat1CanvasView.layer.cornerRadius = 30.5
            seat1CanvasView.layer.masksToBounds = true
            seatViews[0].canvas = seat1CanvasView
        }
    }

    @IBOutlet var seat1Icon: UIImageView! {
        didSet {
            seatViews[0].icon = seat1Icon
        }
    }

    @IBOutlet var seat1Label: UILabel! {
        didSet {
            seatViews[0].label = seat1Label
        }
    }

    @IBOutlet var seat2Root: UIView! {
        didSet {
            seatViews[1].delegate = self
            seatViews[1].root = seat2Root
        }
    }

    @IBOutlet var seat2CanvasView: UIView! {
        didSet {
            seat2CanvasView.layer.cornerRadius = 30.5
            seat2CanvasView.layer.masksToBounds = true
            seatViews[1].canvas = seat2CanvasView
        }
    }

    @IBOutlet var seat2Icon: UIImageView! {
        didSet {
            seatViews[1].icon = seat2Icon
        }
    }

    @IBOutlet var seat2Label: UILabel! {
        didSet {
            seatViews[1].label = seat2Label
        }
    }

    @IBOutlet var seat3Root: UIView! {
        didSet {
            seatViews[2].delegate = self
            seatViews[2].root = seat3Root
        }
    }

    @IBOutlet var seat3CanvasView: UIView! {
        didSet {
            seat3CanvasView.layer.cornerRadius = 30.5
            seat3CanvasView.layer.masksToBounds = true
            seatViews[2].canvas = seat3CanvasView
        }
    }

    @IBOutlet var seat3Icon: UIImageView! {
        didSet {
            seatViews[2].icon = seat3Icon
        }
    }

    @IBOutlet var seat3Label: UILabel! {
        didSet {
            seatViews[2].label = seat3Label
        }
    }

    @IBOutlet var seat4Root: UIView! {
        didSet {
            seatViews[3].delegate = self
            seatViews[3].root = seat4Root
        }
    }

    @IBOutlet var seat4CanvasView: UIView! {
        didSet {
            seat4CanvasView.layer.cornerRadius = 30.5
            seat4CanvasView.layer.masksToBounds = true
            seatViews[3].canvas = seat4CanvasView
        }
    }

    @IBOutlet var seat4Icon: UIImageView! {
        didSet {
            seatViews[3].icon = seat4Icon
        }
    }

    @IBOutlet var seat4Label: UILabel! {
        didSet {
            seatViews[3].label = seat4Label
        }
    }

    @IBOutlet var seat5Root: UIView! {
        didSet {
            seatViews[4].delegate = self
            seatViews[4].root = seat5Root
        }
    }

    @IBOutlet var seat5CanvasView: UIView! {
        didSet {
            seat5CanvasView.layer.cornerRadius = 30.5
            seat5CanvasView.layer.masksToBounds = true
            seatViews[4].canvas = seat5CanvasView
        }
    }

    @IBOutlet var seat5Icon: UIImageView! {
        didSet {
            seatViews[4].delegate = self
            seatViews[4].icon = seat5Icon
        }
    }

    @IBOutlet var seat5Label: UILabel! {
        didSet {
            seatViews[4].label = seat5Label
        }
    }

    @IBOutlet var seat6Root: UIView! {
        didSet {
            seatViews[5].delegate = self
            seatViews[5].root = seat6Root
        }
    }

    @IBOutlet var seat6CanvasView: UIView! {
        didSet {
            seat6CanvasView.layer.cornerRadius = 30.5
            seat6CanvasView.layer.masksToBounds = true
            seatViews[5].canvas = seat6CanvasView
        }
    }

    @IBOutlet var seat6Icon: UIImageView! {
        didSet {
            seatViews[5].icon = seat6Icon
        }
    }

    @IBOutlet var seat6Label: UILabel! {
        didSet {
            seatViews[5].label = seat6Label
        }
    }

    @IBOutlet var seat7Root: UIView! {
        didSet {
            seatViews[6].delegate = self
            seatViews[6].root = seat7Root
        }
    }

    @IBOutlet var seat7CanvasView: UIView! {
        didSet {
            seat7CanvasView.layer.cornerRadius = 30.5
            seat7CanvasView.layer.masksToBounds = true
            seatViews[6].canvas = seat7CanvasView
        }
    }

    @IBOutlet var seat7Icon: UIImageView! {
        didSet {
            seatViews[6].icon = seat7Icon
        }
    }

    @IBOutlet var seat7Label: UILabel! {
        didSet {
            seatViews[6].label = seat7Label
        }
    }

    @IBOutlet var seat8Root: UIView! {
        didSet {
            seatViews[7].delegate = self
            seatViews[7].root = seat8Root
        }
    }

    @IBOutlet var seat8CanvasView: UIView! {
        didSet {
            seat8CanvasView.layer.cornerRadius = 30.5
            seat8CanvasView.layer.masksToBounds = true
            seatViews[7].canvas = seat8CanvasView
        }
    }

    @IBOutlet var seat8Icon: UIImageView! {
        didSet {
            seatViews[7].icon = seat8Icon
        }
    }

    @IBOutlet var seat8Label: UILabel! {
        didSet {
            seatViews[7].label = seat8Label
        }
    }

    @IBOutlet var speakerControllViewRoot: UIView! {
        didSet {
            speakerToolbar.root = speakerControllViewRoot
            speakerToolbar.delegate = self
        }
    }

    @IBOutlet var micView: UIButton! {
        didSet {
            speakerToolbar.micView = micView
        }
    }

    @IBOutlet var moreOptionsView: UIButton! {
        didSet {
            speakerToolbar.moreOptionsView = moreOptionsView
        }
    }

    @IBOutlet var switchMVView: UIButton! {
        didSet {
            speakerToolbar.switchMVView = switchMVView
        }
    }

    @IBOutlet var orderMusicView: UIButton! {
        didSet {
            speakerToolbar.orderMusicView = orderMusicView
            orderMusicView.layer.cornerRadius = 20
            orderMusicView.layer.masksToBounds = true
            orderMusicView.backgroundColor = UIColor(hex: "#1b6def", alpha: 1.0)
            orderMusicView.setTitle("Order a song".localized, for: .normal)
        }
    }

    @IBOutlet var orderChorusMusicView: UIButton! {
        didSet {
            speakerToolbar.orderChorusMusicView = orderChorusMusicView
            orderChorusMusicView.layer.cornerRadius = 20
            orderChorusMusicView.layer.masksToBounds = true
            orderChorusMusicView.backgroundColor = UIColor(hex: "#1b6def", alpha: 1.0)
            orderChorusMusicView.setTitle("Chorus".localized, for: .normal)
        }
    }

    @IBOutlet var listenerViewRoot: UILabel! {
        didSet {
            listenerToolbar.root = listenerViewRoot
            listenerToolbar.delegate = self
        }
    }

    let viewModel = RoomViewModel()
    private let mvPlayer = MVPlayer()

    private let seatViews: [SeatView] = [0, 1, 2, 3, 4, 5, 6, 7].map { index in
        SeatView(index: index)
    }

    private let speakerToolbar = SpeakerToolbar()
    private let listenerToolbar = ListenerToolbar()

    override func viewDidLoad() {
        super.viewDidLoad()
        viewModel.delegate = self
        // initUI()
        renderToolbar()
        subcribeUIEvent()
        subcribeRoomEvent()
    }

    private func initUI() {
        Logger.log(self, message: "initUI", level: .info)
        roomNameView.text = viewModel.room.channelName
        roomCoverView.image = UIImage(named: LiveKtvRoom.getLocalCover(cover: viewModel.room.cover), in: Utils.bundle, with: nil)
        mvPlayer.mv.image = UIImage(named: LiveKtvRoom.getLocalMV(cover: viewModel.room.mv), in: Utils.bundle, with: nil)
        performSelector(inBackground: #selector(getImageColor), with: nil)
    }

    @objc private func getImageColor() {
        if let image = UIImage(named: LiveKtvRoom.getLocalMV(cover: viewModel.room.mv), in: Utils.bundle, with: nil) {
            let color = ColorThief.getColor(from: image)?.makeUIColor()
            if let color = color {
                Logger.log(self, message: "set MusicLyricView.hightColor \(color)", level: .info)
                MusicLyricView.hightColor = (color.lighter(by: 30))!
            }
        }
    }

    private func renderToolbar() {
        Logger.log(self, message: "renderToolbar", level: .info)
        mvPlayer.member = viewModel.member
        speakerToolbar.member = viewModel.member
        listenerToolbar.member = viewModel.member
    }

    private func subcribeUIEvent() {
        Logger.log(self, message: "subcribeUIEvent", level: .info)
        closeRoomView.addTarget(self, action: #selector(onTapCloseButton), for: .touchUpInside)
        seatViews.forEach { view in
            view.subcribeUIEvent()
        }
        listenerToolbar.delegate = self
        speakerToolbar.delegate = self
        speakerToolbar.subcribeUIEvent()
        mvPlayer.subcribeUIEvent()
    }

    private func subcribeRoomEvent() {
        Logger.log(self, message: "subcribeRoomEvent", level: .info)
        viewModel.subcribeRoomEvent()
    }

    @objc func onTapCloseButton() {
        if viewModel.member.isManager {
            let alert = AlertDialog(title: "Close room".localized, message: "Leaving the room ends the session and removes everyone".localized)
            alert.cancelAction = { [weak self] in
                guard let weakself = self else { return }
                weakself.dismiss(dialog: alert, completion: nil)
            }
            alert.okAction = { [weak self] in
                guard let weakself = self else { return }
                weakself.dismiss(dialog: alert, completion: nil)
                weakself.leaveRoom()
            }
            show(dialog: alert, style: .center, padding: 27, relation: .greaterOrEqual, completion: nil)
        } else {
            leaveRoom()
        }
    }

    private func leaveRoom() {
        viewModel.leaveRoom { [weak self] waiting in
            guard let weakself = self else { return }
            weakself.show(processing: waiting)
        } onSuccess: { [weak self] in
            guard let weakself = self else { return }
            weakself.dismiss(completion: nil)
        } onError: { [weak self] message in
            guard let weakself = self else { return }
            weakself.show(message: message, type: .error)
        }
    }

    override func viewDidAppear(_: Bool) {
        navigationController?.interactivePopGestureRecognizer?.delegate = self
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }

    public static func instance() -> RoomController? {
        let storyBoard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        return storyBoard.instantiateViewController(withIdentifier: "RoomController") as? RoomController
    }
}

extension RoomController: RoomControlDelegate {
    func onFetchMusic(finish: Bool) {
        show(loading: !finish, message: "Preparing Music".localized)
    }

    func onRoomUpdate() {
        initUI()
    }

    func onRoomClosed() {
        leaveRoom()
    }

    func onPlayListChanged() {
        mvPlayer.music = viewModel.playingMusic
        seatViews.forEach { view in
            view.music = viewModel.playingMusic
        }
        speakerToolbar.onMusicListChanged()
    }

    func onMusic(state: RtcMusicState) {
        mvPlayer.onMusic(state: state)
    }

    func onMemberListChanged() {
        let list = viewModel.memberList.filter { member in
            member.isSpeaker()
        }
        let count = min(list.count, 8)
        for index in 0 ..< count {
            let uid = seatViews[index].member?.streamId ?? 0
            let member = list[index]
            seatViews[index].member = member
            if member.isVideoMuted {
                seatViews[index].canvas.subviews.forEach { $0.removeFromSuperview() }
            } else {
                viewModel.setCanvasView(uid: uid, isLocal: viewModel.account.id == member.userId, canvasView: seatViews[index].canvas)
            }
        }
        renderToolbar()
    }

    func onMuted(mute: Bool) {
        speakerToolbar.onMuted(mute: mute)
    }

    func onTap(view: SeatView) {
        if viewModel.isManager {
            if let member = view.member {
                if member.id != viewModel.member.id {
                    ManageSpeakerDialog().show(with: member, delegate: self)
                }
            }
        } else if !viewModel.isSpeaker, view.member == nil {
            viewModel.handsUp { [weak self] waiting in
                guard let weakself = self else { return }
                weakself.show(processing: waiting)
            } onSuccess: {} onError: { [weak self] message in
                guard let weakself = self else { return }
                weakself.show(message: message, type: .error)
            }
        }
    }

    func onError(message: String?) {
        show(message: message, type: .error)
    }

    func onSelectVoiceEffect(effect: AgoraAudioEffectPreset) {
        viewModel.setVoiceEffect(effect: effect)
    }
}

extension RoomController: UIGestureRecognizerDelegate {
    func gestureRecognizerShouldBegin(_: UIGestureRecognizer) -> Bool {
        false
    }
}
