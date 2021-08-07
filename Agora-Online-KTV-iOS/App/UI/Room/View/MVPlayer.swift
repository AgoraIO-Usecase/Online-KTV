//
//  MVPlayer.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import LrcView
import UIKit

private class ChorusMasterView: UIView {
    weak var delegate: MVPlayer?
    var music: LiveKtvMusic? {
        didSet {
            titleView.text = music?.name
            time = 20
        }
    }

    var isEnabled: Bool {
        get {
            return button.isEnabled
        }
        set {
            button.isEnabled = newValue
        }
    }

    // sec
    var time: TimeInterval? {
        didSet {
            let time = time ?? 0
            tipsView.text = "等待加入合唱 \(Utils.format(time: time))"
        }
    }

    private let titleView: UILabel = {
        let view = UILabel()
        view.textColor = UIColor.white
        view.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        view.textAlignment = .center
        return view
    }()

    private let tipsView: UILabel = {
        let view = UILabel()
        view.textColor = UIColor.white.withAlphaComponent(0.8)
        view.font = UIFont.systemFont(ofSize: 16)
        view.textAlignment = .center
        return view
    }()

    private let button: RoundButton = {
        let view = RoundButton()
        view.backgroundColor = UIColor.clear
        view.borderColor = Colors.Text
        view.borderWidth = 1
        view.setTitle("不等了，独唱", for: .normal)
        view.setTitleColor(UIColor.white, for: .normal)
        view.titleLabel?.font = UIFont.systemFont(ofSize: 15)
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(titleView)
        addSubview(tipsView)
        addSubview(button)

        titleView.marginTop(anchor: topAnchor)
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .active()
        tipsView.marginTop(anchor: titleView.bottomAnchor, constant: 12)
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .active()
        button.marginTop(anchor: tipsView.bottomAnchor, constant: 17)
            .width(constant: 138)
            .height(constant: 34)
            .centerX(anchor: centerXAnchor)
            .marginBottom(anchor: bottomAnchor)
            .active()
        button.addTarget(self, action: #selector(onTapButton), for: .touchUpInside)
    }

    @objc private func onTapButton() {
        delegate?.onTapChorusMasterButton()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private class ChorusFollowerView: UIView {
    weak var delegate: MVPlayer?
    var music: LiveKtvMusic? {
        didSet {
            titleView.text = music?.name
            time = 20
        }
    }

    var isEnabled: Bool {
        get {
            return button.isEnabled
        }
        set {
            button.isEnabled = newValue
        }
    }

    // sec
    var time: TimeInterval? {
        didSet {
            let time = time ?? 0
            tipsView.text = "抢麦倒计时 \(Utils.format(time: time))"
        }
    }

    private let titleView: UILabel = {
        let view = UILabel()
        view.textColor = UIColor.white
        view.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        view.textAlignment = .center
        return view
    }()

    private let tipsView: UILabel = {
        let view = UILabel()
        view.textColor = UIColor.white.withAlphaComponent(0.8)
        view.font = UIFont.systemFont(ofSize: 16)
        view.textAlignment = .center
        return view
    }()

    private let button: RoundButton = {
        let view = RoundButton()
        view.backgroundColor = UIColor(hex: Colors.Blue)
        view.setTitle("加入合唱", for: .normal)
        view.setTitleColor(UIColor.white, for: .normal)
        view.borderWidth = 0
        view.titleLabel?.font = UIFont.systemFont(ofSize: 15)
        return view
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(titleView)
        addSubview(tipsView)
        addSubview(button)

        titleView.marginTop(anchor: topAnchor)
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .active()
        tipsView.marginTop(anchor: titleView.bottomAnchor, constant: 12)
            .marginLeading(anchor: leadingAnchor)
            .marginTrailing(anchor: trailingAnchor)
            .active()
        button.marginTop(anchor: tipsView.bottomAnchor, constant: 16)
            .width(constant: 136)
            .height(constant: 37)
            .centerX(anchor: centerXAnchor)
            .marginBottom(anchor: bottomAnchor)
            .active()
        button.addTarget(self, action: #selector(onTapButton), for: .touchUpInside)
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc private func onTapButton() {
        delegate?.onTapChorusFollowerButton()
    }
}

class MVPlayer: NSObject {
    private static let COUNTDOWN_SECOND = 20
    enum Status {
        case stop
        case play
        case pause

        case waitChorusApply
        case processChorusApply
        case downloadChorusMusic
        case syncChorusMusicReady
        case startChorus
        case resyncChorus
        case redownloadChorusMusic
        case resyncChorusMusicReady
    }

    weak var delegate: RoomController!
    weak var player: UIView! {
        didSet {
            player.addSubview(musicLyricView)
            musicLyricView.fill(view: player, leading: 0, top: 32, trailing: 0, bottom: 32)
                .active()
        }
    }

    weak var mv: UIImageView!
    weak var originSettingView: UISwitch! {
        didSet {
            originSettingView.isOn = true
            originSettingView.superview?.addSubview(hookSwitchView)
            hookSwitchView.fill(view: originSettingView)
                .active()
        }
    }

    weak var settingsView: UIButton!
    weak var playerControlView: UIButton!
    weak var switchMusicView: UIButton!
    weak var icon: UIImageView!
    weak var name: UILabel!

    let musicLyricView = MusicLyricView()

    private var lastRole: Int?
    var member: LiveKtvMember? {
        didSet {
            Logger.log(self, message: "didSet member", level: .info)
            onChanged()
            if let music = music, music.isChorus(), member?.role != lastRole {
                onRoleChange()
            }
            lastRole = member?.role
        }
    }

    var status: MVPlayer.Status! {
        didSet {
            onChanged()
        }
    }

    private lazy var hookSwitchView = UIView()

    private lazy var stopView: UIView = {
        let view = UIView()
        let icon = UIImageView()
        icon.image = UIImage(named: "empty1", in: Utils.bundle, with: nil)
        let tips1 = UILabel()
        tips1.font = UIFont.systemFont(ofSize: 14, weight: .medium)
        tips1.textColor = UIColor(hex: Colors.Text)
        tips1.numberOfLines = 0
        tips1.textAlignment = .center
        tips1.text = "当前无人演唱"
        let tips2 = UILabel()
        tips2.font = UIFont.systemFont(ofSize: 14, weight: .medium)
        tips2.textColor = UIColor(hex: Colors.Text)
        tips2.numberOfLines = 0
        tips2.textAlignment = .center
        tips2.text = "点击“点歌”一展歌喉"

        view.addSubview(icon)
        view.addSubview(tips1)
        view.addSubview(tips2)

        icon.width(constant: 117)
            .height(constant: 117)
            .marginTop(anchor: view.topAnchor, constant: 50)
            .centerX(anchor: view.centerXAnchor)
            .active()
        tips1.marginTop(anchor: icon.bottomAnchor, constant: 16)
            .marginLeading(anchor: view.leadingAnchor, constant: 15)
            .centerX(anchor: view.centerXAnchor)
            .active()
        tips2.marginTop(anchor: tips1.bottomAnchor, constant: 8)
            .marginLeading(anchor: view.leadingAnchor, constant: 15)
            .centerX(anchor: view.centerXAnchor)
            .active()

        return view
    }()

    private var timer: Timer?
    private var countdown: Int = 0
    private lazy var chorusMasterView: ChorusMasterView = {
        let view = ChorusMasterView()
        view.delegate = self
        return view
    }()

    private lazy var chorusFollowerView: ChorusFollowerView = {
        let view = ChorusFollowerView()
        view.delegate = self
        return view
    }()

    private var _localMusic: LocalMusic?
    private var _isMyOrdedMusic: Bool {
        if let music = music, let member = member {
            return music.isOrderBy(member: member)
        } else {
            return false
        }
    }

    var music: LiveKtvMusic? {
        didSet {
            if music?.id == oldValue?.id {
                if music?.type != oldValue?.type {
                    onMusicTypeChange(old: oldValue)
                } else {
                    if let music = music, let member = member, music.isChorus() {
                        if !music.isChorusReady() {
                            if _isMyOrdedMusic {
                                if status == .waitChorusApply, music.user1Id == nil,
                                   oldValue?.applyUser1Id == nil, music.applyUser1Id != nil
                                {
                                    status = .processChorusApply
                                } else if music.user1Id != nil, oldValue?.user1Id == nil {
                                    // wait sync users status (master)
                                    timer?.invalidate()
                                    timer = nil
                                    status = .downloadChorusMusic
                                }
                            } else if music.user1Id != nil, oldValue?.user1Id == nil {
                                if music.user1Id != member.userId {
                                    // listener
                                    listenerOnPlayMusicChange()
                                } else {
                                    // wait sync users status (follower)
                                    status = .downloadChorusMusic
                                }
                            }
                        } else if oldValue?.isChorusReady() != true {
                            if _isMyOrdedMusic || music.user1Id == member.userId {
                                status = .startChorus
                            }
                        } else if (music.userbgId != oldValue?.userbgId) || (music.user1bgId != oldValue?.user1bgId) {
                            if _isMyOrdedMusic || music.user1Id == member.userId {
                                if status == .resyncChorusMusicReady {
                                    status = .startChorus
                                } else {
                                    status = .resyncChorus
                                }
                            }
                        }
                    }
                }
                return
            }
            if let old = oldValue, old.isChorus() {
                if old.user1Id == member?.userId {
                    delegate.viewModel.stopMusic()
                }
            }
            onPlayMusicChange()
        }
    }

    private func onChanged() {
        Logger.log(self, message: "onChanged status:\(String(describing: status))", level: .info)
        switch status {
        case .stop, .none:
            settingsView.superview?.isHidden = true
            if stopView.superview == nil {
                player.superview!.addSubview(stopView)
                stopView.fill(view: player.superview!)
                    .active()
            }
            stopView.isHidden = false
            mv.isHidden = true
            musicLyricView.isHidden = true
            chorusMasterView.isHidden = true
            chorusFollowerView.isHidden = true
        case .play, .pause:
            stopView.isHidden = true
            mv.isHidden = false
            musicLyricView.isHidden = false
            updateMusicLyricViewLayout()
            if settingsView.superview?.isHidden == false {
                playerControlView.setImage(UIImage(named: status == .play ? "iconPause" : "iconPlay", in: Utils.bundle, with: nil), for: .normal)
            }

        case .waitChorusApply:
            settingsView.superview?.isHidden = true
            stopView.isHidden = true
            mv.isHidden = false
            musicLyricView.isHidden = true
            chorusMasterView.isHidden = false
            chorusFollowerView.isHidden = false
        case .processChorusApply:
            guard let music = music else {
                return
            }
            delegate.viewModel.acceptAsFollower(music: music) { [weak self] waiting in
                self?.show(processing: waiting)
            } onSuccess: {} onError: { [weak self] message in
                self?.onError(message: message)
            }
        case .downloadChorusMusic:
            guard let music = music else {
                return
            }
            delegate.viewModel.fetchMusic(music: music) { [weak self] waiting in
                self?.delegate.onFetchMusic(finish: !waiting)
            } onSuccess: { [weak self] localMusic in
                self?._localMusic = localMusic
                self?.status = .syncChorusMusicReady
            } onError: { [weak self] message in
                self?.onError(message: message)
            }
        case .syncChorusMusicReady:
            guard let music = music else {
                return
            }
            delegate.viewModel.setPlayMusicReady(music: music) { [weak self] waiting in
                self?.show(processing: waiting)
            } onSuccess: { [weak self] in
                self?.listenerOnPlayMusicChange()
                // self?.show(processing: true)
            } onError: { [weak self] message in
                self?.onError(message: message)
            }
        case .startChorus:
            let option = getLocalMusicOption()
            if let option = option, let localMusic = _localMusic {
                delegate.viewModel.play(music: localMusic, option: option) { [weak self] waiting in
                    self?.show(processing: waiting)
                } onSuccess: { [weak self] in
                    self?.status = .play
                } onError: { [weak self] message in
                    self?.onError(message: message)
                }
            } else {
                show(processing: false)
            }
        case .resyncChorus:
            let option = getLocalMusicOption()
            if let option = option {
                delegate.viewModel.updateLocalMusic(option: option)
            }
        case .redownloadChorusMusic:
            guard let music = music else {
                return
            }
            delegate.viewModel.fetchMusic(music: music) { [weak self] waiting in
                self?.delegate.onFetchMusic(finish: !waiting)
            } onSuccess: { [weak self] localMusic in
                self?._localMusic = localMusic
                self?.status = .resyncChorusMusicReady
            } onError: { [weak self] message in
                self?.onError(message: message)
            }
        case .resyncChorusMusicReady:
            guard let music = music else {
                return
            }
            delegate.viewModel.setPlayMusicReady(music: music) { [weak self] waiting in
                self?.show(processing: waiting)
            } onSuccess: {} onError: { [weak self] message in
                self?.onError(message: message)
            }
        }
    }

    private func getLocalMusicOption() -> LocalMusicOption? {
        if let masterId = music?.userId, let followerId = music?.user1Id {
            let masterUid = delegate.viewModel.memberList.first { member in
                member.userId == masterId
            }?.streamId
            let followerUid = delegate.viewModel.memberList.first { member in
                member.userId == followerId
            }?.streamId
            if let masterUid = masterUid, let masterMusicUid = music?.userbgId,
               let followerUid = followerUid, let folowerMusicUid = music?.user1bgId
            {
                return LocalMusicOption(masterUid: masterUid, masterMusicUid: masterMusicUid, followerUid: followerUid, followerMusicUid: folowerMusicUid)
            }
        }
        return nil
    }

    private func show(processing: Bool) {
        delegate.show(processing: processing)
    }

    private func onError(message: String) {
        delegate.onError(message: message)
    }

    private func updateMusicLyricViewLayout() {
        settingsView.superview?.isHidden = !_isMyOrdedMusic
        if let root = player.superview {
            if !_isMyOrdedMusic {
                if musicLyricView.superview != root {
                    musicLyricView.removeFromSuperview()
                    root.addSubview(musicLyricView)
                    let padding = (root.bounds.height - musicLyricView.bounds.height) / 2
                    musicLyricView.fill(view: root, leading: 0, top: padding, trailing: 0, bottom: padding)
                        .active()
                }
            } else {
                if musicLyricView.superview != player {
                    musicLyricView.removeFromSuperview()
                    player.addSubview(musicLyricView)
                    musicLyricView.fill(view: player, leading: 0, top: 32, trailing: 0, bottom: 32)
                        .active()
                }
            }
        }
    }

    private func onRoleChange() {
        onPlayMusicChange()
        if let music = music, music.user1Id == member?.userId, lastRole == LiveKtvRoomRole.speaker.rawValue {
            delegate.viewModel.stopMusic()
        }
    }

    private func onPlayMusicChange() {
        if timer != nil {
            timer?.invalidate()
            timer = nil
        }

        if let member = member, let music = music {
            switch music.type {
            case LiveKtvMusic.NORMAL:
                listenerOnPlayMusicChange()
                if _isMyOrdedMusic {
                    delegate.viewModel.fetchMusic(music: music) { [weak self] waiting in
                        self?.delegate.onFetchMusic(finish: !waiting)
                    } onSuccess: { [weak self] localMusic in
                        guard let self = self else { return }
                        if localMusic.id == self.delegate.viewModel.playingMusic?.musicId,
                           !self.delegate.viewModel.isLocalMusicPlaying(music: localMusic)
                        {
                            self.delegate.viewModel.play(music: localMusic) { [unowned self] waiting in
                                self.delegate.show(processing: waiting)
                            } onSuccess: {} onError: { [unowned self] message in
                                self.delegate.onError(message: message)
                            }
                        }
                    } onError: { [weak self] message in
                        self?.delegate.show(message: message, type: .error)
                    }
                }
            case LiveKtvMusic.CHORUS:
                if member.isSpeaker(), !music.isChorusReady() {
                    status = .waitChorusApply
                    if _isMyOrdedMusic {
                        chorusMasterView.music = music
                        chorusMasterView.isEnabled = true
                        if chorusFollowerView.superview != nil {
                            chorusFollowerView.removeFromSuperview()
                        }
                        if chorusMasterView.superview == nil {
                            if let root = player.superview {
                                root.addSubview(chorusMasterView)
                                chorusMasterView.marginLeading(anchor: root.leadingAnchor)
                                    .marginTrailing(anchor: root.trailingAnchor)
                                    .centerY(anchor: root.centerYAnchor)
                                    .active()
                            }
                        }
                        countdown = MVPlayer.COUNTDOWN_SECOND
                        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] _ in
                            if let self = self {
                                self.countdown -= 1
                                if self.countdown <= 0 {
                                    self.onTapChorusMasterButton()
                                } else {
                                    self.delegate.viewModel.countdown(time: self.countdown)
                                }
                            }
                        })
                    } else {
                        chorusFollowerView.music = music
                        chorusFollowerView.isEnabled = true
                        if chorusMasterView.superview != nil {
                            chorusMasterView.removeFromSuperview()
                        }
                        if chorusFollowerView.superview == nil {
                            if let root = player.superview {
                                root.addSubview(chorusFollowerView)
                                chorusFollowerView.marginLeading(anchor: root.leadingAnchor)
                                    .marginTrailing(anchor: root.trailingAnchor)
                                    .centerY(anchor: root.centerYAnchor)
                                    .active()
                            }
                        }
                    }
                } else if member.isSpeaker(), music.isChorusReady(), _isMyOrdedMusic || music.user1Id == member.userId {
                    listenerOnPlayMusicChange()
                    status = .redownloadChorusMusic
                } else {
                    listenerOnPlayMusicChange()
                }
            default:
                break
            }

        } else {
            status = .none
        }

        if let musicName = music?.name {
            name.text = musicName
            icon.isHidden = false
            name.isHidden = false
        } else {
            icon.isHidden = true
            name.isHidden = true
        }
    }

    private func onMusicTypeChange(old: LiveKtvMusic?) {
        if old?.isChorus() == true {
            onPlayMusicChange()
        }
    }

    func subcribeUIEvent() {
        settingsView.addTarget(self, action: #selector(onTapSettingsView), for: .touchUpInside)
        playerControlView.addTarget(self, action: #selector(onTapPlayerControlView), for: .touchUpInside)
        switchMusicView.addTarget(self, action: #selector(onSwitchMusic), for: .touchUpInside)
        hookSwitchView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(onSwitchOrigin)))
        musicLyricView.delegate = self
    }

    func onMusic(state: RtcMusicState) {
        switch status {
        case .waitChorusApply:
            if state.type != .countdown {
                return
            }
        case .downloadChorusMusic, .syncChorusMusicReady, .startChorus, .redownloadChorusMusic, .resyncChorusMusicReady:
            return
        default:
            break
        }

        switch state.type {
        case .position:
            musicLyricView.scrollLyric(currentTime: TimeInterval(state.position), totalTime: TimeInterval(state.duration))
            if let playerState = state.state {
                switch playerState {
                case .playing:
                    if status != .play {
                        status = .play
                    }
                    musicLyricView.paused = false
                case .paused:
                    if status != .pause {
                        status = .pause
                    }
                    musicLyricView.paused = true
                case .playBackCompleted, .playBackAllLoopsCompleted:
                    if status != .stop {
                        status = .stop
                        originSettingView.setOn(true, animated: true)
                    }
                    if let music = music {
                        delegate.viewModel.end(music: music) { [unowned self] waiting in
                            delegate.show(processing: waiting)
                        } onSuccess: { [unowned self] in
                            self.music = nil
                        } onError: { [unowned self] message in
                            delegate.show(message: message, type: .error)
                        }
                    }
                default: break
                }
            }
        case .countdown:
            if music?.isChorus() == true {
                if _isMyOrdedMusic {
                    chorusMasterView.time = TimeInterval(state.position)
                } else {
                    chorusFollowerView.time = TimeInterval(state.position)
                }
            }
        }
    }

    @objc func onTapSettingsView() {
        PlayerSettingDialog().show(delegate: delegate)
    }

    @objc func onTapPlayerControlView() {
        if status == .play {
            delegate.viewModel.pauseMusic()
        } else if status == .pause {
            delegate.viewModel.resumeMusic()
        }
    }

    @objc func onSwitchMusic() {
        let alert = AlertDialog(title: "切歌".localized, message: "终止当前歌曲的演唱？".localized)
        alert.cancelAction = { [unowned self] in
            self.delegate.dismiss(dialog: alert, completion: nil)
        }
        alert.okAction = { [unowned self] in
            self.delegate.dismiss(dialog: alert, completion: nil)
            if let music = music {
                delegate.viewModel.end(music: music) { [unowned self] waiting in
                    delegate.show(processing: waiting)
                } onSuccess: { [unowned self] in
                    self.music = nil
                } onError: { [unowned self] message in
                    delegate.show(message: message, type: .error)
                }
            }
        }
        delegate.show(dialog: alert, style: .center, padding: 27, relation: .greaterOrEqual, completion: nil)
    }

    @objc func onSwitchOrigin() {
        let enable = originSettingView.isOn
        if enable {
            if delegate.viewModel.isSupportSwitchOriginMusic {
                originSettingView.setOn(false, animated: true)
                delegate.viewModel.originMusic(enable: true)
            } else {
                delegate.show(message: "该歌曲无法进行人声切换", type: .error)
            }
        } else {
            if delegate.viewModel.isSupportSwitchOriginMusic {
                originSettingView.setOn(true, animated: true)
                delegate.viewModel.originMusic(enable: false)
            }
        }
    }

    func onTapChorusMasterButton() {
        if let music = music {
            chorusMasterView.isEnabled = false
            delegate.viewModel.toNormal(music: music) { [weak self] waiting in
                self?.delegate.show(processing: waiting)
            } onSuccess: {} onError: { [weak self] message in
                self?.chorusMasterView.isEnabled = true
                self?.delegate.show(message: message, type: .error)
            }
        }
    }

    func onTapChorusFollowerButton() {
        if let music = music {
            chorusFollowerView.isEnabled = false
            delegate.viewModel.applyAsFollower(music: music) { [weak self] waiting in
                self?.delegate.show(processing: waiting)
            } onSuccess: {} onError: { [weak self] message in
                self?.chorusFollowerView.isEnabled = true
                self?.delegate.show(message: message, type: .error)
            }
        }
    }

    private func listenerOnPlayMusicChange() {
        guard let music = music else {
            return
        }
        if chorusMasterView.superview != nil {
            chorusMasterView.removeFromSuperview()
        }
        if chorusFollowerView.superview != nil {
            chorusFollowerView.removeFromSuperview()
        }
        originSettingView.setOn(true, animated: true)
        status = .play
        updateMusicLyricViewLayout()
        delegate.viewModel.fetchMusicLrc(music: music) { [weak self] waiting in
            if waiting {
                self?.musicLyricView.lyrics = nil
            }
        } onSuccess: { [weak self] localMusic in
            if localMusic.id == self?.music?.musicId {
                self?.musicLyricView.lyrics = LocalMusicManager.parseLyric(music: localMusic)
            }
        } onError: { [weak self] message in
            self?.delegate.show(message: message, type: .error)
        }
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }
}

extension MVPlayer: MusicLyricViewDelegate {
    func userEndSeeking(time: TimeInterval) {
        if _isMyOrdedMusic {
            Logger.log(self, message: "userEndSeeking \(time)", level: .info)
            delegate.viewModel.seekMusic(position: time)
        }
    }
}
