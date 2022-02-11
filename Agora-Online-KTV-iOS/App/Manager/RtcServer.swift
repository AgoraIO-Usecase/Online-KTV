//
//  RtcServer.swift
//  LiveKtv
//
//  Created by XC on 2021/6/8.
//

import AgoraRtcKit
import Core
import Foundation
import LrcView
import RxRelay
import RxSwift

enum RtcServerStateType {
    case join
    case error
    case members
}

class RtcServer: NSObject {
    var rtcEngine: AgoraRtcEngineKit?
    let voicePitchRelay: BehaviorRelay<[Double]> = BehaviorRelay(value: [])
    private var rtcMusicPlayer: IRtcMusicPlayer?
    private let statePublisher: PublishRelay<Result<RtcServerStateType>> = PublishRelay()
    private let rtcMusicStatePublisher: PublishRelay<Result<RtcMusicState>> = PublishRelay()
    private lazy var mediaOption: AgoraRtcChannelMediaOptions = {
        let option = AgoraRtcChannelMediaOptions()
        option.autoSubscribeAudio = AgoraRtcBoolOptional.of(true)
        option.autoSubscribeVideo = AgoraRtcBoolOptional.of(true)
        return option
    }()

    private(set) var uid: UInt = 0
    private(set) var channel: String?
    private(set) var members: [UInt] = []
    private(set) var speakers = [UInt: Bool]()
    private(set) var role: AgoraClientRole?
    private(set) var muted: Bool = false
    private(set) var isEnableBeauty: Bool = false
    private(set) var isEnableEarloop: Bool = false
    private(set) var recordingSignalVolume: Float = 100 / 400
    private(set) var musicVolume: Float = 100 / 400

    private var orderedDataStreamId: Int = -1
    private var dataStreamId: Int = -1
    private var rtcMediaPlayer: AgoraRtcMediaPlayerProtocol?

    var isJoinChannel: Bool {
        return channel != nil && channel?.isEmpty == false
    }

    var playingMusic: LocalMusic? {
        return rtcMusicPlayer?.music
    }

    override init() {
        super.init()
        let config = AgoraRtcEngineConfig()
        config.appId = BuildConfig.AppId
        config.audioScenario = .chorus
        config.channelProfile = .liveBroadcasting
        rtcEngine = AgoraRtcEngineKit.sharedEngine(with: config, delegate: self)
        if let engine = rtcEngine {
            engine.setChannelProfile(.liveBroadcasting)
        }
    }

    func setClientRole(_ role: AgoraClientRole, _: Bool) {
        Logger.log(message: "rtc setClientRole \(role.rawValue)", level: .info)
        if self.role == role {
            return
        }
        self.role = role
        guard let rtc = rtcEngine else {
            return
        }
        rtc.setClientRole(role)
    }

    func openVideoHandler(isOpen: Bool) {
        let option = mediaOption
        option.publishCustomVideoTrack = .of(isOpen)
        option.publishCameraTrack = .of(isOpen)
        if isOpen {
            rtcEngine?.enableVideo()
            rtcEngine?.startPreview()
        } else {
            rtcEngine?.disableVideo()
            rtcEngine?.stopPreview()
        }
        setClientRole(isOpen ? .broadcaster : .audience, true)
        rtcEngine?.updateChannel(with: option)
    }

    func createVideoCanvas(uid: UInt, isLocal: Bool = false, canvasView: UIView) {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.renderMode = .hidden
        canvas.view = canvasView
        if isLocal {
            rtcEngine?.setupLocalVideo(canvas)
        } else {
            rtcEngine?.setupRemoteVideo(canvas)
        }
    }

    func enable(earloop: Bool) {
        isEnableEarloop = earloop
        guard let rtc = rtcEngine else {
            return
        }
        rtc.enable(inEarMonitoring: earloop)
    }

    func joinChannel(member: LiveKtvMember, channel: String, setting: LocalSetting) -> Observable<Result<Void>> {
        guard let rtc = rtcEngine else {
            return Observable.just(Result(success: false, message: "rtcEngine is nil!"))
        }
        role = nil
        members.removeAll()
        isEnableEarloop = false
        if member.isSpeaker() {
            setClientRole(.broadcaster, setting.audienceLatency)
        } else {
            setClientRole(.audience, setting.audienceLatency)
        }
        rtc.enableAudio()
        rtc.disableVideo()
        rtc.enableAudioVolumeIndication(200, smooth: 3, reportvad: true)
        muteLocalMicrophone(mute: member.isSelfMuted)
        rtc.enable(inEarMonitoring: isEnableEarloop)
        setRecordingSignalVolume(value: recordingSignalVolume)
        return Single.create { single in
            let code = rtc.joinChannel(byToken: BuildConfig.Token, channelId: channel, uid: member.streamId, mediaOptions: self.mediaOption)
            // rtc.joinChannel(byToken: BuildConfig.Token, channelId: channel, info: nil, uid: 0)
            single(.success(code))
            return Disposables.create()
        }.asObservable().subscribe(on: MainScheduler.instance)
            .concatMap { (code: Int32) -> Observable<Result<Void>> in
                if code != 0 {
                    return Observable.just(Result(success: false, message: RtcServer.toErrorString(type: .join, code: code)))
                } else {
                    return self.statePublisher.filter { state -> Bool in
                        state.data == RtcServerStateType.join || state.data == RtcServerStateType.error
                    }.take(1).map { state -> Result<Void> in
                        Result(success: state.success, message: state.message)
                    }
                }
            }
    }

    func leaveChannel() -> Observable<Result<Void>> {
        return Single.create { [weak self] single in
            if let weakself = self {
                if weakself.isJoinChannel {
                    if let rtc = weakself.rtcEngine {
                        weakself.dataStreamId = -1
                        weakself.orderedDataStreamId = -1
                        weakself.channel = nil
                        weakself.uid = 0
                        weakself.members.removeAll()
                        weakself.statePublisher.accept(Result(success: true, data: RtcServerStateType.members))
                        Logger.log(message: "rtc leaveChannel", level: .info)
                        let code = rtc.leaveChannel { _ in
                            single(.success(Result(success: true)))
                        }
                        if code != 0 {
                            single(.success(Result(success: false, message: "leaveChannel error code:\(code) !")))
                        }
                    } else {
                        single(.success(Result(success: false, message: "rtcEngine is nil!")))
                    }
                } else {
                    single(.success(Result(success: true)))
                }
            }

            return Disposables.create()
        }.asObservable()
    }

    func releaseMusicPlayer() -> Observable<Result<Void>> {
        if let player = rtcMusicPlayer {
            return Single.create { single in
                player.destory()
                self.rtcMusicPlayer = nil
                if let rtc = self.rtcEngine, let mediaPlayer = self.rtcMediaPlayer {
                    rtc.destroyMediaPlayer(mediaPlayer)
                    self.rtcMediaPlayer = nil
                }
                single(.success(Result(success: true)))
                return Disposables.create()
            }.asObservable()
        } else {
            return Observable.just(Result(success: true))
        }
    }

    func onSpeakersChanged() -> Observable<[UInt: Bool]> {
        return statePublisher
            .filter { state -> Bool in
                state.data == RtcServerStateType.members
            }
            .startWith(Result(success: true, data: RtcServerStateType.members))
            .map { [weak self] _ in
                var speakers = [UInt: Bool]()
                guard let weakself = self else { return speakers }
                weakself.members.forEach { member in
                    speakers[member] = weakself.speakers[member] ?? true
                }
                return speakers
            }
    }

    func muteLocalMicrophone(mute: Bool) {
        muted = mute
        rtcEngine?.muteRecordingSignal(mute)
//        let option = AgoraRtcChannelMediaOptions()
//        option.publishAudioTrack = AgoraRtcBoolOptional.of(!mute)
//        rtcEngine?.updateChannel(with: option)
    }

    func onRtcMusicStateChanged() -> Observable<Result<RtcMusicState>> {
        return rtcMusicStatePublisher.asObservable().observe(on: MainScheduler.instance)
    }

    func initChorusMusicPlayer(isMaster: Bool) -> Observable<Result<UInt>> {
        return Single.create { single in
            if self.rtcMusicPlayer is RtcNormalMusicPlayer {
                self.rtcMusicPlayer?.destory()
                self.rtcMusicPlayer = nil
            }
            if self.rtcMusicPlayer == nil {
                self.rtcMusicPlayer = RtcChorusMusicPlayer(rtcServer: self)
            }
            if let player = self.rtcMusicPlayer {
                player.initPlayer(isMaster: isMaster, onSuccess: {
                    single(.success(Result(success: true, data: player.uid)))
                }, onFailed: { error in
                    single(.success(Result(success: false, message: error)))
                })
            } else {
                single(.success(Result(success: false, message: "RtcChorusMusicPlayer init error!")))
            }
            return Disposables.create()
        }.asObservable()
    }

    func updateLocalMusic(option: LocalMusicOption?) {
        if rtcMusicPlayer is RtcChorusMusicPlayer {
            (rtcMusicPlayer as? RtcChorusMusicPlayer)?.option = option
        }
    }

    func play(music: LocalMusic, option: LocalMusicOption?) -> Observable<Result<Void>> {
        if let option = option {
            if rtcMusicPlayer is RtcNormalMusicPlayer {
                rtcMusicPlayer?.destory()
                rtcMusicPlayer = nil
            }
            if rtcMusicPlayer == nil {
                rtcMusicPlayer = RtcChorusMusicPlayer(rtcServer: self)
            }
            (rtcMusicPlayer as? RtcChorusMusicPlayer)?.option = option
        } else {
            if rtcMusicPlayer is RtcChorusMusicPlayer {
                rtcMusicPlayer?.destory()
                rtcMusicPlayer = nil
            }
            if rtcMusicPlayer == nil {
                rtcMusicPlayer = RtcNormalMusicPlayer(rtcServer: self)
            }
        }

        if let player = rtcMusicPlayer {
            return Single.create { single in
                player.play(music: music) {
                    single(.success(Result(success: true)))
                } onFailed: { message in
                    single(.success(Result(success: false, message: "play \(music.name) failed! (\(message))")))
                }
                return Disposables.create()
            }.asObservable()
        } else {
            return Observable.just(Result(success: false, message: "rtcMusicPlayer is nil"))
        }
    }

    func seekMusic(position: TimeInterval) {
        if let player = rtcMusicPlayer {
            player.seek(position: Int(position))
        }
    }

    func countdown(time: Int) {
        if rtcMusicPlayer == nil {
            rtcMusicPlayer = RtcChorusMusicPlayer(rtcServer: self)
        }
        if let player = rtcMusicPlayer {
            player.sendCountdown(time: time)
            let state = RtcMusicState(uid: uid, streamId: player.streamId, position: time, duration: time, state: .idle, type: .countdown)
            rtcMusicStatePublisher.accept(Result(success: true, data: state))
        }
    }

    func pauseMusic() {
        if let player = rtcMusicPlayer {
            player.pause()
        }
    }

    func resumeMusic() {
        if let player = rtcMusicPlayer {
            player.resume()
        }
    }

    func stopMusic() {
        enable(earloop: false)
        if let player = rtcMusicPlayer {
            player.stop()
        }
    }

    func getRecordingSignalVolume() -> Float {
        return recordingSignalVolume
    }

    func setRecordingSignalVolume(value: Float) {
        recordingSignalVolume = value
        rtcEngine?.adjustRecordingSignalVolume(Int(recordingSignalVolume * 400))
    }

    func getPlayoutVolume() -> Float {
        return musicVolume
    }

    func setPlayoutVolume(value: Float) {
        musicVolume = value
        rtcMusicPlayer?.adjustPlayoutVolume(value: Int32(musicVolume * 400))
    }

    func isSupportSwitchOriginMusic() -> Bool {
        return true
//        return (rtcMusicPlayer?.getAudioTrackCount() ?? 0) > 1
    }

    func setPitch(pitch: Int) {
        if let player = rtcMusicPlayer {
            player.setPitch(pitch: pitch)
        }
//        Logger.log(self, message: "setPitch \(pitch), result is \(String(describing: ret))", level: .info)
    }

    func setVoiceEffect(effect: AgoraAudioEffectPreset) {
        let ret = rtcEngine?.setAudioEffectPreset(effect)
        Logger.log(self, message: "setAudioEffectPreset \(effect.rawValue), result is \(String(describing: ret))", level: .info)
    }

    func originMusic(enable: Bool) {
        if isSupportSwitchOriginMusic() {
            if let player = rtcMusicPlayer {
                player.originMusic(enable: enable)
                let mode = enable ? 1 : 0
                player.sendMusicPlayMode(mode: mode)
            } else {
                Logger.log(self, message: "rtcMusicPlayer is nil", level: .error)
            }
        } else {
            Logger.log(self, message: "not support switch origin", level: .error)
        }
    }

    func sendMusic(state: RtcMusicState) {
        rtcMusicStatePublisher.accept(Result(success: true, data: state))
    }

    func getDataStreamId() -> Int {
        if let rtc = rtcEngine {
            if dataStreamId == -1 {
                let config = AgoraDataStreamConfig()
                config.ordered = false
                config.syncWithAudio = false
                rtc.createDataStream(&dataStreamId, config: config)
                if dataStreamId == -1 {
                    Logger.log(self, message: "error dataStreamId == -1", level: .error)
                }
            }
        } else {
            dataStreamId = -1
        }
        return dataStreamId
    }

    func getOrderedDataStreamId() -> Int {
        if let rtc = rtcEngine {
            if orderedDataStreamId == -1 {
                let config = AgoraDataStreamConfig()
                config.ordered = true
                config.syncWithAudio = true
                rtc.createDataStream(&orderedDataStreamId, config: config)
                if orderedDataStreamId == -1 {
                    Logger.log(self, message: "error orderedDataStreamId == -1", level: .error)
                }
            }
        } else {
            orderedDataStreamId = -1
        }
        return orderedDataStreamId
    }

    func getAgoraMusicPlayer() -> AgoraRtcMediaPlayerProtocol? {
        if let rtc = rtcEngine {
            if rtcMediaPlayer == nil {
                rtcMediaPlayer = rtc.createMediaPlayer(with: self)
            }
        }
        return rtcMediaPlayer
    }
}

extension RtcServer: AgoraRtcMediaPlayerDelegate {
    func agoraRtcMediaPlayer(_: AgoraRtcMediaPlayerProtocol, didChangedToPosition position: Int) {
        // Logger.log(self, message: "didChangedToPosition \(position)", level: .info)
        rtcMusicPlayer?.didChangedTo(position: position)
    }

    func agoraRtcMediaPlayer(_: AgoraRtcMediaPlayerProtocol, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        Logger.log(self, message: "didChangedTo \(state.rawValue)", level: .info)
        rtcMusicPlayer?.didChangedTo(state: state, error: error)
    }
}

extension RtcServer: AgoraRtcEngineDelegate {
    func rtcEngine(_: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        Logger.log(message: "didOccurError \(AgoraRtcEngineKit.getErrorDescription(errorCode.rawValue))", level: .info)
        statePublisher.accept(Result(success: false, data: RtcServerStateType.error, message: AgoraRtcEngineKit.getErrorDescription(errorCode.rawValue)))
    }

    func rtcEngine(_: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed _: Int) {
        Logger.log(message: "rtc didJoinChannel:\(channel) uid:\(uid)", level: .info)
        self.uid = uid
        self.channel = channel
        members.append(uid)
        speakers[uid] = role == .audience
        statePublisher.accept(Result(success: true, data: RtcServerStateType.join))
    }

    func rtcEngine(_: AgoraRtcEngineKit, didLeaveChannelWith stats: AgoraChannelStats) {
        Logger.log(message: "rtc didLeaveChannelWith:\(stats)", level: .info)
    }

    func rtcEngine(_: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed _: Int) {
        Logger.log(message: "rtc didJoinedOfUid uid:\(uid)", level: .info)
        members.append(uid)
        speakers[uid] = true
        statePublisher.accept(Result(success: true, data: RtcServerStateType.members))
    }

    func rtcEngine(_: AgoraRtcEngineKit, didAudioMuted muted: Bool, byUid uid: UInt) {
        Logger.log(message: "rtc didAudioMuted uid:\(uid) muted:\(muted)", level: .info)
        speakers[uid] = muted
        statePublisher.accept(Result(success: true, data: RtcServerStateType.members))
    }

    func rtcEngine(_: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason _: AgoraUserOfflineReason) {
        Logger.log(message: "rtc didOfflineOfUid uid:\(uid)", level: .info)
        if let index = members.firstIndex(of: uid) {
            members.remove(at: index)
        }
        speakers[uid] = false
        statePublisher.accept(Result(success: true, data: RtcServerStateType.members))
    }

    func rtcEngine(_: AgoraRtcEngineKit, receiveStreamMessageFromUid uid: UInt, streamId: Int, data: Data) {
        Logger.log(self, message: "receiveStreamMessageFromUid \(uid) \(streamId)", level: .info)
//        if rtcMusicPlayer?.state == .playing || rtcMusicPlayer?.uid == uid {
//            return
//        }
        do {
            guard let content: NSDictionary = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as? NSDictionary,
                  let cmd: String = content["cmd"] as? String else { return }

            var state: RtcMusicState?

            switch cmd {
            case "musicStopped":
                Logger.log(self, message: "musicStopped", level: .info)
            case "setLrcTime":
                let duration = content["duration"] as? Int ?? 0
                let position = content["time"] as? Int ?? -1
                state = RtcMusicState(uid: uid, streamId: getOrderedDataStreamId(), position: position, duration: duration, state: .playing, type: .position)
            case "countdown":
                if let position = content["time"] as? Int {
                    state = RtcMusicState(uid: uid, streamId: getDataStreamId(), position: position, duration: position, state: .idle, type: .countdown)
                }
            default:
                state = nil
            }

            if let player = rtcMusicPlayer {
                if !player.receiveThenProcess(uid: uid, cmd: cmd, data: content) {
                    if let state = state {
                        sendMusic(state: state)
                    }
                }
            } else {
                if let state = state {
                    sendMusic(state: state)
                }
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .error)
        }
    }

    func rtcEngine(_: AgoraRtcEngineKit, reportAudioVolumeIndicationOfSpeakers speakers: [AgoraRtcAudioVolumeInfo], totalVolume _: Int) {
        voicePitchRelay.accept(speakers.map { $0.voicePitch })
    }
}

enum RtcServerError: Int {
    case join = 0
    case register = 1
    case leave = 2
}

extension RtcServer: ErrorDescription {
    static func toErrorString(type: RtcServerError, code: Int32) -> String {
        switch type {
        case RtcServerError.join:
            switch code {
            case -2:
                return "Invalid Argument".localized
            case -3:
                return "SDK Not Ready".localized
            case -5:
                return "SDK Refused".localized
            case -7:
                return "SDK Not Initialized".localized
            default:
                return "Unknown Error".localized
            }
        case RtcServerError.register:
            return "Unknown Error".localized
        case RtcServerError.leave:
            switch code {
            case -2:
                return "Invalid Argument".localized
            case -7:
                return "SDK Not Initialized".localized
            default:
                return "Unknown Error".localized
            }
        }
    }
}

public extension AgoraAudioEffectPreset {
    func description() -> String {
        switch self {
        case .off: return "原声"
        case .roomAcousticsKTV: return "KTV"
        case .roomAcousVocalConcer: return "演唱会"
        case .roomAcousStudio: return "录音棚"
        case .roomAcousPhonograph: return "留声机"
        case .roomAcousSpatial: return "空旷"
        case .roomAcousEthereal: return "空灵"
        case .styleTransformationPopular: return "流行"
        case .styleTransformationRnb: return "R&B"
        default:
            return "原声"
        }
    }

    static func fmDefault(with agoraKit: AgoraRtcEngineKit) {
        changeVoice(with: agoraKit, type: .off)
    }

    static func changeVoice(with agoraKit: AgoraRtcEngineKit, type: AgoraAudioEffectPreset) {
        agoraKit.setAudioEffectPreset(type)
    }

    func character(with agoraKit: AgoraRtcEngineKit) {
        AgoraAudioEffectPreset.changeVoice(with: agoraKit, type: self)
    }
}
