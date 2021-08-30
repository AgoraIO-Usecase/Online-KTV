//
//  RoomViewModel.swift
//  LiveKtv
//
//  Created by XC on 2021/6/9.
//

import Core
import Foundation
import LrcView
import RxCocoa
import RxRelay
import RxSwift

class RoomViewModel {
    private let disposeBag = DisposeBag()
    private let manager = RoomManager.shared()
    let localMusicManager = LocalMusicManager()
    private var scheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "io")
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

//    private var lrcMusicCache = [String: LrcMusic]()
    private var lrcMusicCache = LrcMusic.getDefultList()

    func subcribeRoomEvent() {
        manager.subscribeRoom()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] result in
                if !result.success {
                    self.delegate?.onError(message: result.message)
                } else if result.data == nil {
                    if let message = result.message {
                        self.delegate?.onError(message: message)
                    }
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
                    if let member = result.data {
                        member.roomId = room.id
                        var isContain = false
                        for item in self.memberList {
                            if item.id == member.id {
                                item.role = member.role
                                isContain = true
                            }
                        }
                        if !isContain {
                            self.memberList.append(member)
                        }
                        self.delegate?.onMemberListChanged()
                    }
                } else {
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeMusicList()
            .observe(on: MainScheduler.instance)
            .concatMap { [unowned self] result -> Observable<Result<LocalMusic>> in
                result.onSuccess {
                    let list = result.data ?? []
                    self.musicList = list
                    self.delegate?.onPlayListChanged()
                    if let music = self.playingMusic {
                        if music.isOrderBy(member: self.member) {
                            return self.fetchMusic(music: music)
                                .do(onSubscribe: {
                                    self.delegate?.onFetchMusic(finish: false)
                                }, onDispose: {
                                    self.delegate?.onFetchMusic(finish: true)
                                })
                        }
                    }
                    return Observable.just(Result(success: true, data: nil))
                }
            }
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [unowned self] _ in
            })
            .disposed(by: disposeBag)

        manager.subscribeRtcMusicState()
            .subscribe(onNext: { [unowned self] result in
                if result.success {
                    if let state = result.data {
                        guard let music = lrcMusicCache[state.musicId] else {
                            return
                        }
                        let localMusic = LocalMusic(id: music.id!, name: music.name!, path: music.song, lrcPath: music.lrc, singer: music.singer!, poster: music.poster!)
                        if state.state == .playing {
                            self.musicList = [LiveKtvMusic(id: localMusic.id, userId: getUserId(streamId: state.uid), roomId: self.room.id, name: localMusic.name, musicId: localMusic.id, singer: localMusic.singer, poster: localMusic.poster)]
                        } else {
                            self.musicList = []
                        }
                        self.delegate?.onPlayListChanged()
                        self.delegate?.onMusic(state: state)
                    }
                } else {
                    self.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)
    }

    private func getUserId(streamId: UInt) -> String {
        for item in memberList {
            if item.streamId == streamId {
                return item.userId
            }
        }
        return "0"
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

    private func getLrcMusic(music: LiveKtvMusic) -> Observable<Result<LrcMusic>> {
        if let data = lrcMusicCache[music.musicId] {
            return Observable.just(Result(success: true, data: data))
        } else {
            return account.getMusic(id: music.musicId)
                .map { [weak self] result -> Result<LrcMusic> in
                    if result.success {
                        let data: LrcMusic = result.data!
                        data.id = music.musicId
                        data.name = music.name
                        data.singer = music.singer
                        data.poster = music.poster
//                        self?.lrcMusicCache[music.musicId] = data
                    }
                    return result
                }
        }
    }

    private func fetchMusic(music: LiveKtvMusic) -> Observable<Result<LocalMusic>> {
        return getLrcMusic(music: music)
            .concatMap { result -> Observable<Result<LocalMusic>> in
                result.onSuccess {
                    let lrcMusic = result.data!
                    return Single.create { single in
                        let task = DownloadManager.shared.getFile(url: lrcMusic.song) { downloadResult in
                            switch downloadResult {
                            case let .success(file: file):
                                single(.success(Result(success: true, data: LocalMusic(id: lrcMusic.id!, name: lrcMusic.name!, path: file, lrcPath: "", singer: lrcMusic.singer!, poster: lrcMusic.poster!))))
                            case let .failed(error: error):
                                single(.success(Result(success: false, message: error)))
                            }
                        }
                        return Disposables.create {
                            if task?.state == .running {
                                task?.cancel()
                            }
                        }
                    }.asObservable()
                }
            }
//            .delay(DispatchTimeInterval.seconds(1), scheduler: scheduler)
    }

    func fetchMusicLrc(music: LiveKtvMusic,
                       onWaiting: @escaping (Bool) -> Void,
                       onSuccess: @escaping (LocalMusic) -> Void,
                       onError: @escaping (String) -> Void)
    {
        getLrcMusic(music: music)
            .concatMap { result -> Observable<Result<LocalMusic>> in
                result.onSuccess {
                    let lrcMusic = result.data!
                    return Single.create { single in
                        let task = DownloadManager.shared.getFile(url: lrcMusic.lrc) { downloadResult in
                            switch downloadResult {
                            case let .success(file: file):
                                single(.success(Result(success: true, data: LocalMusic(id: lrcMusic.id!, name: lrcMusic.name!, path: "", lrcPath: file, singer: lrcMusic.singer!, poster: lrcMusic.poster!))))
                            case let .failed(error: error):
                                single(.success(Result(success: false, message: error)))
                            }
                        }
                        return Disposables.create {
                            if task?.state == .running {
                                task?.cancel()
                            }
                        }
                    }.asObservable()
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
                    onSuccess(result.data!)
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func isLocalMusicPlaying(music: LocalMusic) -> Bool {
        return music.id == manager.playingMusic?.id
    }

    func seekMusic(position: TimeInterval) {
        manager.seekMusic(position: position)
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
                    self.room.mv = LiveKtvRoom.getMV(local: mv)
                    self.delegate.onRoomUpdate()
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
               onWaiting _: @escaping (Bool) -> Void,
               onSuccess: @escaping () -> Void,
               onError _: @escaping (String) -> Void)
    {
        musicList = [LiveKtvMusic(id: music.id, userId: account.id, roomId: room.id, name: music.name, musicId: music.id, singer: music.singer, poster: music.poster)]
        delegate.onPlayListChanged()
        play(music: music) { [unowned self] waiting in
            self.delegate.show(processing: waiting)
        } onSuccess: {} onError: { [unowned self] message in
            self.delegate.onError(message: message)
        }
        onSuccess()
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

    func search(music: String,
                onWaiting: @escaping (Bool) -> Void,
                onSuccess: @escaping ([LocalMusic]) -> Void,
                onError: @escaping (String) -> Void)
    {
        account.getMusicList(key: music)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    let data = result.data ?? []
                    onSuccess(data.map { lrcMusic in
                        LocalMusic(id: lrcMusic.id!, name: lrcMusic.name!, path: "", lrcPath: "", singer: lrcMusic.singer!, poster: lrcMusic.poster!)
                    })
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }

    func getMusic(id: String,
                  onWaiting: @escaping (Bool) -> Void,
                  onSuccess: @escaping (LrcMusic) -> Void,
                  onError: @escaping (String) -> Void)
    {
        account.getMusic(id: id)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe { result in
                if result.success {
                    onSuccess(result.data!)
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            } onDisposed: {
                onWaiting(false)
            }
            .disposed(by: disposeBag)
    }
}
