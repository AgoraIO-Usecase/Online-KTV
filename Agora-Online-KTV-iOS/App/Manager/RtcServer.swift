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

class RtcMusicState {
    let uid: UInt
    let streamId: Int
    let position: Int
    let duration: Int
    let musicId: String
    let state: AgoraAudioMixingStateCode?

    init(uid: UInt, streamId: Int, position: Int, duration: Int, musicId: String, state: AgoraAudioMixingStateCode?) {
        self.uid = uid
        self.streamId = streamId
        self.position = position
        self.duration = duration
        self.musicId = musicId
        self.state = state
    }
}

private struct RtcMusicLrcMessage: Encodable, Decodable {
    let msgId: Int
    let peerUid: UInt
    let cmd: String
    let lrcId: String
    let duration: Int
    let time: Int
    let state: Int
}

private struct RtcMemberMessage: Encodable, Decodable {
    let cmd: String
    let userId: String
    let role: Int
}

private class RtcMusicPlayer: NSObject {
    static var msgId: Int = 0
    private weak var rtcServer: RtcServer!
    private var timer: Timer?
    private var timerPausedDate: Date?

    var streamId: Int = -1
    var uid: UInt = 0
    var music: LocalMusic?
    var memberId: String?
    var position: Int = 0
    var duration: Int = 0
    var isPlaying: Bool = false
    var state: AgoraAudioMixingStateCode?
    var isPause: Bool = false

    init(rtcServer: RtcServer) {
        self.rtcServer = rtcServer
        super.init()
    }

    func play(music: LocalMusic) -> Bool {
        if let rtc = rtcServer.rtcEngine {
            self.music = music
            if state == .playing {
                rtc.stopAudioMixing()
            }
            if let timer = timer {
                timer.invalidate()
            }
            let success = rtc.startAudioMixing(music.path, loopback: false, replace: false, cycle: 1) == 0
            if success {
                originMusic(enable: false)
                timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] _ in
                    if let self = self, let server = self.rtcServer, let rtc = server.rtcEngine {
                        if self.state == .playing {
                            server.agoraRtcMediaPlayer(rtc, didChangedToPosition: Int(rtc.getAudioMixingCurrentPosition()))
                        }
                    }
                })
            }
            return success
        } else {
            return false
        }
    }

    func getAudioTrackCount() -> Int {
        return Int(rtcServer.rtcEngine?.getAudioTrackCount() ?? 0)
    }

    func originMusic(enable: Bool) {
        rtcServer.rtcEngine?.setAudioMixingDualMonoMode(enable ? .duraMonoL : .duraMonoMix)
    }

    func seek(position: Int) {
        if let rtc = rtcServer.rtcEngine {
            rtc.setAudioMixingPosition(position)
        }
    }

    func pause() {
        if let rtc = rtcServer.rtcEngine {
            rtc.pauseAudioMixing()
        }
        if let timer = timer {
            timerPausedDate = timer.fireDate
            timer.fireDate = Date.distantFuture
        }
    }

    func resume() {
        if let rtc = rtcServer.rtcEngine {
            rtc.resumeAudioMixing()
        }
        if let timer = timer, let timerPausedDate = timerPausedDate {
            timer.fireDate = timerPausedDate
        }
        timerPausedDate = nil
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        timerPausedDate = nil
        if let rtc = rtcServer.rtcEngine {
            rtc.stopAudioMixing()
        }
        music = nil
    }

    func release() {
        stop()
        uid = 0
    }

    func sendRtcMusicState(state: RtcMusicState) {
        guard let rtcEngine = rtcServer.rtcEngine else {
            return
        }
        if streamId == -1 {
            let config = AgoraDataStreamConfig()
            config.ordered = true
            config.syncWithAudio = true
            rtcEngine.createDataStream(&streamId, config: config)
            if streamId == -1 {
                Logger.log(self, message: "error streamId == -1", level: .error)
                return
            }
        }
        RtcMusicPlayer.msgId += 1
        let msg = RtcMusicLrcMessage(msgId: RtcMusicPlayer.msgId, peerUid: uid, cmd: "setLrcTime", lrcId: state.musicId, duration: state.duration, time: state.position, state: state.state == .playing ? 1 : 0)
        let jsonEncoder = JSONEncoder()
        do {
            let jsonData = try jsonEncoder.encode(msg)
            let code = rtcEngine.sendStreamMessage(streamId, data: jsonData)
            if code != 0 {
                Logger.log(self, message: "sendRtcMusicState error(\(code)", level: .error)
            } else {
                Logger.log(self, message: "send RtcMusicLrcMessage successful! \(String(describing: state.state))", level: .info)
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .error)
        }
    }
}

class RtcServer: NSObject {
    var rtcEngine: AgoraRtcEngineKit?
    private var rtcMusicPlayer: RtcMusicPlayer?
    private let statePublisher: PublishRelay<Result<RtcServerStateType>> = PublishRelay()
    private let rtcMusicStatePublisher: PublishRelay<Result<RtcMusicState>> = PublishRelay()
    private let membersPublisher: PublishRelay<Result<LiveKtvMember>> = PublishRelay()

    private(set) var uid: UInt = 0
    private(set) var channel: String?
    private(set) var members: [UInt] = []
    private(set) var speakers = [UInt: Bool]()
    private(set) var role: AgoraClientRole?
    private(set) var muted: Bool = false
    private(set) var isEnableBeauty: Bool = false
    private(set) var isEnableEarloop: Bool = false
    private(set) var recordingSignalVolume: Float = 100 / 400

    var isJoinChannel: Bool {
        return channel != nil && channel?.isEmpty == false
    }

    var playingMusic: LocalMusic? {
        return rtcMusicPlayer?.music
    }

    var memberStreamId: Int = -1

    override init() {
        super.init()
        let config = AgoraRtcEngineConfig()
        config.appId = BuildConfig.AppId
//        config.areaCode = AgoraAreaCode.CN.rawValue
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
        muteLocalMicrophone(mute: member.isSelfMuted)
        rtc.enable(inEarMonitoring: isEnableEarloop)
        setRecordingSignalVolume(value: recordingSignalVolume)
        return Single.create { single in
            let code = rtc.joinChannel(byToken: BuildConfig.Token, channelId: channel, info: nil, uid: 0)
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
        return Single.create { [unowned self] single in
            if isJoinChannel {
                if let rtc = self.rtcEngine {
                    self.channel = nil
                    self.uid = 0
                    self.memberStreamId = -1
                    self.members.removeAll()
                    self.statePublisher.accept(Result(success: true, data: RtcServerStateType.members))
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

            return Disposables.create()
        }.asObservable()
    }

    func releaseMusicPlayer() -> Observable<Result<Void>> {
        if let player = rtcMusicPlayer {
            return Single.create { single in
                player.release()
                self.rtcMusicPlayer = nil
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
            .map { [unowned self] _ in
                var speakers = [UInt: Bool]()
                self.members.forEach { member in
                    speakers[member] = self.speakers[member] ?? true
                }
                return speakers
            }
    }

    func muteLocalMicrophone(mute: Bool) {
        muted = mute
        if muted {
            recordingSignalVolume = getRecordingSignalVolume()
            rtcEngine?.adjustRecordingSignalVolume(0)
        } else {
            rtcEngine?.adjustRecordingSignalVolume(Int(recordingSignalVolume * 400))
        }
    }

    func onRtcMusicStateChanged() -> Observable<Result<RtcMusicState>> {
        return rtcMusicStatePublisher.asObservable().observe(on: MainScheduler.instance)
    }

    func onMemberListChanged() -> Observable<Result<LiveKtvMember>> {
        return membersPublisher.asObservable().observe(on: MainScheduler.instance)
    }

    func play(music: LocalMusic) -> Observable<Result<Void>> {
        if rtcMusicPlayer == nil {
            rtcMusicPlayer = RtcMusicPlayer(rtcServer: self)
        }
        if let player = rtcMusicPlayer {
            return Single.create { single in
                player.uid = self.uid
                let success = player.play(music: music)
                if !success {
                    single(.success(Result(success: false, message: "play \(music.name) failed!")))
                } else {
                    single(.success(Result(success: true)))
                }
                return Disposables.create()
            }.asObservable()
        } else {
            return Observable.just(Result(success: false, message: "rtcMusicPlayer is nil"))
        }
    }

    func seekMusic(position: TimeInterval) {
        if let play = rtcMusicPlayer {
            play.seek(position: Int(position))
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
            let state = RtcMusicState(uid: uid, streamId: 0, position: player.position, duration: player.duration, musicId: player.music?.id ?? "0", state: .stopped)
            player.stop()
            player.sendRtcMusicState(state: state)
        }
    }

    func getRecordingSignalVolume() -> Float {
        return recordingSignalVolume
    }

    func setRecordingSignalVolume(value: Float) {
        if muted {
            return
        }
        recordingSignalVolume = value
        rtcEngine?.adjustRecordingSignalVolume(Int(recordingSignalVolume * 400))
    }

    func getPlayoutVolume() -> Float {
        return Float(rtcEngine?.getAudioMixingPlayoutVolume() ?? 0) / 400
    }

    func setPlayoutVolume(value: Float) {
        rtcEngine?.adjustAudioMixingPlayoutVolume(Int(value * 400))
    }

    func isSupportSwitchOriginMusic() -> Bool {
        return true
    }

    func originMusic(enable: Bool) {
        if isSupportSwitchOriginMusic() {
            if let player = rtcMusicPlayer {
                player.originMusic(enable: enable)
            } else {
                Logger.log(self, message: "rtcMusicPlayer is nil", level: .error)
            }
        } else {
            Logger.log(self, message: "not support switch origin", level: .error)
        }
    }

    func sendMemberState(member: LiveKtvMember) {
        guard let rtcEngine = rtcEngine else {
            return
        }
        if memberStreamId == -1 {
            let config = AgoraDataStreamConfig()
            config.ordered = true
            config.syncWithAudio = true
            rtcEngine.createDataStream(&memberStreamId, config: config)
            if memberStreamId == -1 {
                Logger.log(self, message: "error streamId == -1", level: .error)
                return
            }
        }
        let msg = RtcMemberMessage(cmd: "syncMember", userId: member.id, role: member.role)
        let jsonEncoder = JSONEncoder()
        do {
            let jsonData = try jsonEncoder.encode(msg)
//            NSString(data: jsonData, encoding: String.Encoding.utf8.rawValue)
            let code = rtcEngine.sendStreamMessage(memberStreamId, data: jsonData)
            if code != 0 {
                Logger.log(self, message: "sendMemberState error(\(code)", level: .error)
            } else {
                membersPublisher.accept(Result(success: true, data: member))
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .error)
        }
    }
}

extension RtcServer: AgoraRtcEngineDelegate {
    func rtcEngine(_: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        Logger.log(message: "didOccurError \(AgoraRtcEngineKit.getErrorDescription(errorCode.rawValue) ?? "")", level: .info)
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

    func agoraRtcMediaPlayer(_ engine: AgoraRtcEngineKit, didChangedToPosition position: Int) {
        Logger.log(self, message: "didChangedToPosition \(position)", level: .info)
        if let player = rtcMusicPlayer {
            player.position = position
            player.duration = Int(engine.getAudioMixingDuration())

            let state = RtcMusicState(uid: uid, streamId: 0, position: player.position, duration: player.duration, musicId: player.music!.id, state: player.state)
            player.sendRtcMusicState(state: state)
            rtcMusicStatePublisher.accept(Result(success: true, data: state))
        }
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, localAudioMixingStateDidChanged state: AgoraAudioMixingStateCode, errorCode: AgoraAudioMixingErrorCode) {
        Logger.log(self, message: "localAudioMixingStateDidChanged \(state.rawValue)", level: .info)
        if let player = rtcMusicPlayer {
            var sync = false
            if player.state == state {
                return
            }
            player.state = state
            switch state {
            case .playing:
                if !player.isPlaying {
                    player.isPlaying = true
                    player.position = 0
                    player.duration = Int(engine.getAudioMixingDuration())
                }
                sync = true
            case .paused:
                player.isPause = true
                sync = true
            case .stopped:
                player.isPlaying = false
                sync = true
            case .failed:
                Logger.log(self, message: "status: \(state), code: \(errorCode)", level: .info)
            default: break
            }
            if sync {
                let state = RtcMusicState(uid: player.uid, streamId: 0, position: player.position, duration: player.duration, musicId: player.music?.id ?? "", state: player.state)
                rtcMusicStatePublisher.accept(Result(success: true, data: state))
            }
        }
    }

    func rtcEngine(_: AgoraRtcEngineKit, receiveStreamMessageFromUid uid: UInt, streamId: Int, data: Data) {
        Logger.log(self, message: "receiveStreamMessageFromUid \(uid) \(streamId)", level: .info)
        if rtcMusicPlayer?.state == .playing || rtcMusicPlayer?.uid == uid {
            return
        }
        do {
            let content: NSDictionary = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as! NSDictionary
            let cmd: String? = content["cmd"] as? String
            guard let cmd = cmd else {
                return
            }
            switch cmd {
            case "musicStopped":
                Logger.log(self, message: "musicStopped", level: .info)
            case "setLrcTime":
                let duration = content["duration"] as! Int
                let position = content["time"] as! Int
                let musicId = content["lrcId"] as! String
                let status = content["state"] as! Int
                let state = RtcMusicState(uid: uid, streamId: streamId, position: position, duration: duration, musicId: musicId, state: status == 1 ? .playing : .stopped)
                rtcMusicStatePublisher.accept(Result(success: true, data: state))
            case "syncMember":
                let member = LiveKtvMember(id: content["userId"] as! String, isMuted: false, isSelfMuted: false, role: content["role"] as! Int, roomId: "0", streamId: 0, userId: content["userId"] as! String)
                membersPublisher.accept(Result(success: true, data: member))
            default: break
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .error)
        }
    }

    func rtcEngine(_: AgoraRtcEngineKit, didOccurStreamMessageErrorFromUid uid: UInt, streamId _: Int, error: Int, missed _: Int, cached _: Int) {
        Logger.log(self, message: "didOccurStreamMessageErrorFromUid \(uid) \(error)", level: .info)
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
