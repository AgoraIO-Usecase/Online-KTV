//
//  RoomViewModel.swift
//  LiveKtv
//
//  Created by XC on 2021/6/9.
//

import Core
import Foundation
import RxCocoa
import RxRelay
import RxSwift

class RoomViewModel {
    private let disposeBag = DisposeBag()
    private let manager = RoomManager.shared()
    let localMusicManager = LocalMusicManager()

    weak var delegate: RoomControlDelegate!

    var room: LiveKtvRoom {
        return manager.room!
    }

    var isSpeaker: Bool {
        return member.isSpeaker()
    }

    var isManager: Bool {
        return member.isManager
    }

    var role: LiveKtvRoomRole {
        return member.toLiveKtvRoomRole()
    }

    var account: User {
        return manager.account!
    }

    var member: LiveKtvMember {
        return manager.member!
    }

    var muted: Bool {
        return manager.isMicrophoneClose()
    }

    var isEnableEarloop: Bool {
        return manager.isEnableEarloop()
    }

    var recordingSignalVolume: Float {
        return manager.getRecordingSignalVolume()
    }

    var playoutVolume: Float {
        return manager.getPlayoutVolume()
    }

    var isSupportSwitchOriginMusic: Bool {
        return manager.isSupportSwitchOriginMusic()
    }

    var playingMusic: LiveKtvMusic? {
        return musicList.first
    }

    private(set) var memberList: [LiveKtvMember] = []
    private(set) var musicList: [LiveKtvMusic] = []

    func subcribeRoomEvent() {
        manager.subscribeRoom()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if !result.success {
                    self.delegate?.onError(message: result.message)
                } else if result.data == nil {
                    self.delegate?.onRoomClosed()
                } else {
                    self.delegate?.onRoomUpdate()
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeMembers()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                self.delegate?.onMuted(mute: muted)
                if result.success {
                    if let list = result.data {
                        self.memberList = list
                        self.delegate?.onMemberListChanged()
                    }
                } else {
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeMusicList()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if result.success {
                    if let list = result.data {
                        self.musicList = list
                        self.delegate?.onPlayListChanged()
                        if let music = self.playingMusic {
                            if !music.isOrderBy(member: self.member) {
                                return
                            }
                            if let localMusic = self.getLocalMusic(music: music),
                               !self.isLocalMusicPlaying(music: localMusic)
                            {
                                self.play(music: localMusic) { [unowned self] waiting in
                                    self.delegate.show(processing: waiting)
                                } onSuccess: {} onError: { [unowned self] message in
                                    self.delegate.onError(message: message)
                                }
                            }
                        }
                    }
                } else {
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeRtcMusicState()
            .subscribe(onNext: { [unowned self] result in
                if result.success {
                    if let state = result.data {
                        self.delegate?.onMusic(state: state)
                    }
                } else {
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)
    }

    private func play(music: LocalMusic,
                      onWaiting: @escaping (Bool) -> Void,
                      onSuccess: @escaping () -> Void,
                      onError: @escaping (String) -> Void)
    {
        manager.handsUp()
            .flatMap { result in
                result.onSuccess {
                    self.manager.play(music: music)
                }
            }
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func getLocalMusic(music: LiveKtvMusic) -> LocalMusic? {
        return localMusicManager.localMusicList.first { local in
            local.id == music.musicId
        }
    }

    func isLocalMusicPlaying(music: LocalMusic) -> Bool {
        return music.id == manager.playingMusic?.id
    }

    func pauseMusic() {
        manager.pauseMusic()
    }

    func resumeMusic() {
        manager.resumeMusic()
    }

    func end(music: LiveKtvMusic,
             onWaiting: @escaping (Bool) -> Void,
             onSuccess: @escaping () -> Void,
             onError: @escaping (String) -> Void)
    {
        manager.stop(music: music)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func changeRoomMV(mv: String,
                      onWaiting: @escaping (Bool) -> Void,
                      onSuccess: @escaping () -> Void,
                      onError: @escaping (String) -> Void)
    {
        manager.changeRoomMV(mv: mv)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func order(music: LocalMusic,
               onWaiting: @escaping (Bool) -> Void,
               onSuccess: @escaping () -> Void,
               onError: @escaping (String) -> Void)
    {
        member.orderMusic(id: music.id, name: music.name)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func kickSpeaker(member: LiveKtvMember,
                     onWaiting: @escaping (Bool) -> Void,
                     onSuccess: @escaping () -> Void,
                     onError: @escaping (String) -> Void)
    {
        manager.kickSpeaker(member: member)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func handsUp(onWaiting: @escaping (Bool) -> Void,
                 onSuccess: @escaping () -> Void,
                 onError: @escaping (String) -> Void)
    {
        manager.handsUp()
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func selfMute(mute: Bool) {
        delegate?.onMuted(mute: mute)
        manager.closeMicrophone(close: mute)
            .subscribe(onNext: { [unowned self] result in
                if !result.success {
                    self.delegate?.onMuted(mute: self.muted)
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)
    }

    func enable(earloop: Bool) {
        manager.enable(earloop: earloop)
    }

    func setRecordingSignalVolume(volume: Float) {
        manager.setRecordingSignalVolume(value: volume)
    }

    func setPlayoutVolume(volume: Float) {
        manager.setPlayoutVolume(value: volume)
    }

    func originMusic(enable: Bool) {
        manager.originMusic(enable: enable)
    }

    func leaveRoom(onWaiting: @escaping (Bool) -> Void,
                   onSuccess: @escaping () -> Void,
                   onError: @escaping (String) -> Void)
    {
        manager.leave()
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }
}
