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

    private var lrcMusicCache = [String: LrcMusic]()

    func subcribeRoomEvent() {
        manager.subscribeRoom()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [weak self] result in
                guard let weakself = self else { return }
                if !result.success {
                    weakself.delegate?.onError(message: result.message)
                } else if result.data == nil {
                    if let message = result.message {
                        weakself.delegate?.onError(message: message)
                    }
                    weakself.delegate?.onRoomClosed()
                } else {
                    weakself.delegate?.onRoomUpdate()
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeMembers()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [weak self] result in
                guard let weakself = self else { return }
                weakself.delegate?.onMuted(mute: weakself.muted)
                if result.success {
                    if let list = result.data {
                        weakself.memberList = list
                        weakself.delegate?.onMemberListChanged()
                    }
                } else {
                    weakself.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeMusicList()
            .observe(on: MainScheduler.instance)
            .subscribe(onNext: { [weak self] result in
                guard let weakself = self else { return }
                if result.success {
                    let list = result.data ?? []
                    weakself.musicList = list
                    weakself.delegate?.onPlayListChanged()
                } else {
                    weakself.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)

        manager.subscribeRtcMusicState()
            .subscribe(onNext: { [weak self] result in
                guard let weakself = self else { return }
                if result.success {
                    if let state = result.data {
                        weakself.delegate?.onMusic(state: state)
                    }
                } else {
                    weakself.delegate?.onError(message: result.message)
                }
            })
            .disposed(by: disposeBag)
    }

    func play(music: LocalMusic,
              option: LocalMusicOption? = nil,
              onWaiting: @escaping (Bool) -> Void,
              onSuccess: @escaping () -> Void,
              onError: @escaping (String) -> Void)
    {
        manager.handsUp()
            .flatMap { [weak self] result -> Observable<Result<Void>> in
                guard let weakself = self else { return Observable.empty() }
                return result.onSuccess {
                    weakself.manager.play(music: music, option: option)
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

    func updateLocalMusic(option: LocalMusicOption?) {
        manager.updateLocalMusic(option: option)
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
                        self?.lrcMusicCache[music.musicId] = data
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

    func fetchMusic(music: LiveKtvMusic,
                    onWaiting: @escaping (Bool) -> Void,
                    onSuccess: @escaping (LocalMusic) -> Void,
                    onError: @escaping (String) -> Void)
    {
        getLrcMusic(music: music)
            .concatMap { result -> Observable<Result<LocalMusic>> in
                result.onSuccess {
                    let lrcMusic = result.data!
                    return Single.create { single in
                        let task = DownloadManager.shared.getFile(url: lrcMusic.song) { downloadResult in
                            switch downloadResult {
                            case let .success(file: file):
                                single(.success(Result(success: true, data: LocalMusic(id: lrcMusic.id!, name: lrcMusic.name!, path: file, lrcPath: "", singer: music.singer, poster: music.poster))))
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

    func countdown(time: Int) {
        manager.countdown(time: time)
    }

    func pauseMusic() {
        manager.pauseMusic()
    }

    func resumeMusic() {
        manager.resumeMusic()
    }

    func stopMusic() {
        manager.stopMusic()
    }

    func toNormal(music: LiveKtvMusic,
                  onWaiting: @escaping (Bool) -> Void,
                  onSuccess: @escaping () -> Void,
                  onError: @escaping (String) -> Void)
    {
        music.asNormal()
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

    func applyAsFollower(music: LiveKtvMusic,
                         onWaiting: @escaping (Bool) -> Void,
                         onSuccess: @escaping () -> Void,
                         onError: @escaping (String) -> Void)
    {
        music.applyAsFollower(member: member)
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

    func acceptAsFollower(music: LiveKtvMusic,
                          onWaiting: @escaping (Bool) -> Void,
                          onSuccess: @escaping () -> Void,
                          onError: @escaping (String) -> Void)
    {
        member.acceptAsFollower(music: music)
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

    func setPlayMusicReady(music: LiveKtvMusic,
                           onWaiting: @escaping (Bool) -> Void,
                           onSuccess: @escaping () -> Void,
                           onError: @escaping (String) -> Void)
    {
        manager.initChorusMusicPlayer()
            .concatMap { [weak self] result -> Observable<Result<Void>> in
                guard let weakself = self else { return Observable.empty() }
                return result.onSuccess {
                    weakself.member.setPlayMusicReady(music: music, uid: result.data!)
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
               orderChorusMusic: Bool = false,
               onWaiting: @escaping (Bool) -> Void,
               onSuccess: @escaping () -> Void,
               onError: @escaping (String) -> Void)
    {
        member.orderMusic(id: music.id, name: music.name, chorus: orderChorusMusic, singer: music.singer, poster: music.poster)
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
            .subscribe(onNext: { [weak self] result in
                guard let weakself = self else { return }
                if !result.success {
                    weakself.delegate?.onMuted(mute: weakself.muted)
                    weakself.delegate?.onError(message: result.message)
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

//    func _musicDataSource(music: String) -> Observable<Result<[LocalMusic]>> {
//        return Observable.just(Result(success: true, data: music.isEmpty ? localMusicManager.localMusicList : [])).delay(DispatchTimeInterval.seconds(5), scheduler: scheduler)
//    }
}
