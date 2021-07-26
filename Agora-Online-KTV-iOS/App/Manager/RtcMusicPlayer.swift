//
//  RtcMusicPlayer.swift
//  app
//
//  Created by XC on 2021/7/21.
//

import AgoraRtcKit
import Core
import Foundation
import RxRelay
import RxSwift

enum RtcMusicStateType {
    case position
    case countdown
}

class RtcMusicState {
    let uid: UInt
    let streamId: Int
    let position: Int
    let duration: Int
    let time: String
    let state: AgoraMediaPlayerState?
    let type: RtcMusicStateType

    init(uid: UInt, streamId: Int, position: Int, duration: Int, state: AgoraMediaPlayerState?, type: RtcMusicStateType) {
        self.uid = uid
        self.streamId = streamId
        self.position = position
        self.duration = duration
        self.state = state
        self.type = type
        time = String(CLongLong(round(Date().timeIntervalSince1970 * 1000)))
    }
}

struct RtcMusicLrcMessage: Encodable, Decodable {
    let msgId: Int
    let peerUid: UInt
    var cmd: String = "setLrcTime"
    let lrcId: String
    let duration: Int
    let time: Int
    let ts: String
}

struct RtcMusicCountdownMessage: Encodable, Decodable {
    let msgId: Int
    let peerUid: UInt
    var cmd: String = "countdown"
    let time: Int
}

struct RtcTestDelayMessage: Encodable, Decodable {
    let msgId: Int
    let peerUid: UInt
    var cmd: String = "testDelay"
    let time: String
}

struct RtcCheckTestDelayMessage: Encodable, Decodable {
    let msgId: Int
    let peerUid: UInt
    var cmd: String = "replyTestDelay"
    let testDelayTime: String
    let time: String
    let position: Int
}

protocol IRtcMusicPlayer {
    var player: AgoraRtcMediaPlayerProtocol? { get set }
    var music: LocalMusic? { get set }
    var uid: UInt { get set }
    var streamId: Int { get set }
    var position: Int { get set }
    var duration: Int { get set }
    var isPlaying: Bool { get set }
    var state: AgoraMediaPlayerState? { get set }
    var isPause: Bool { get set }

    init(rtcServer: RtcServer)
    func initPlayer(onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void)
    func play(music: LocalMusic, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void)
    func getAudioTrackCount() -> Int
    func getPlayoutVolume() -> Int32
    func adjustPlayoutVolume(value: Int32)
    func originMusic(enable: Bool)
    func seek(position: Int)
    func pause()
    func resume()
    func stop()
    func destory()
    func sendRtcMusicStreamMessage(state: RtcMusicState)
    func sendCountdown(time: Int)

    func didChangedTo(position: Int)
    func didChangedTo(state: AgoraMediaPlayerState, error: AgoraMediaPlayerError)
    func receiveThenProcess(uid: UInt, cmd: String, data: NSDictionary) -> Bool
}

class AbstractRtcMusicPlayer: NSObject, IRtcMusicPlayer /* , AgoraRtcMediaPlayerAudioFrameDelegate */ {
    static var msgId: Int = 0
    weak var rtcServer: RtcServer!

    var player: AgoraRtcMediaPlayerProtocol?
    var streamId: Int = -1
    var uid: UInt = 0
    var music: LocalMusic?
    var position: Int = 0
    var duration: Int = 0
    var isPlaying: Bool = false
    var state: AgoraMediaPlayerState?
    var isPause: Bool = false
    var syncStreamId: Int = -1

    required init(rtcServer: RtcServer) {
        self.rtcServer = rtcServer
        super.init()
        player = self.rtcServer.rtcEngine!.createMediaPlayer(with: rtcServer)
        // player.setAudioFrameDelegate(self, mode: .readWrite)
    }

    func initPlayer(onSuccess: @escaping () -> Void, onFailed _: @escaping (String) -> Void) {
        onSuccess()
    }

    func play(music _: LocalMusic, onSuccess _: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        onFailed("Unrealized!")
    }

    func getAudioTrackCount() -> Int {
        if let player = player {
            let all = player.getStreamCount()
            Logger.log(self, message: "getStreamCount \(all)", level: .info)

            if all <= 0 {
                return 0
            }
            var count = 0
            for index in 0 ..< all {
                if player.getStreamBy(Int32(index))?.streamType == .audio {
                    count += 1
                }
            }
            return count
        } else {
            return 0
        }
    }

    func getPlayoutVolume() -> Int32 {
        return player?.getPlayoutVolume() ?? 0
    }

    func adjustPlayoutVolume(value: Int32) {
        player?.adjustPlayoutVolume(value)
    }

    func originMusic(enable: Bool) {
        // monoChannel = enable
        if let player = player {
            player.setAudioDualMonoMode(enable ? .mixingDuraMonoL : .mixingDuraMonoMix)
        }
    }

    func seek(position: Int) {
        if let player = player, state == .paused || state == .playing {
            player.seek(toPosition: position)
        }
    }

    func pause() {
        if let player = player {
            player.pause()
        }
    }

    func resume() {
        if let player = player {
            player.resume()
        }
    }

    func stop() {
        if let player = player {
            player.stop()
        }
        music = nil
        AbstractRtcMusicPlayer.msgId = 0
    }

    func destory() {
        stop()
        uid = 0
        if let player = player, let rtc = rtcServer.rtcEngine {
            rtc.destroyMediaPlayer(player)
        }
    }

    fileprivate func sendRtcStreamMessage<T: Encodable>(msg: T, ordered: Bool = false) {
        guard let rtcEngine = rtcServer.rtcEngine else {
            return
        }
        if streamId == -1 {
            let config = AgoraDataStreamConfig()
            config.ordered = ordered
            config.syncWithAudio = ordered
            rtcEngine.createDataStream(&streamId, config: config)
            if streamId == -1 {
                Logger.log(self, message: "error streamId == -1", level: .error)
                return
            }
        }
        let jsonEncoder = JSONEncoder()
        do {
            let jsonData = try jsonEncoder.encode(msg)
            let code = rtcEngine.sendStreamMessage(streamId, data: jsonData)
            if code != 0 {
                Logger.log(self, message: "sendRtcStreamMessage error(\(code)", level: .error)
            }
        } catch {
            Logger.log(self, message: error.localizedDescription, level: .error)
        }
    }

    func sendRtcMusicStreamMessage(state: RtcMusicState) {
        guard let music = music else {
            return
        }
        RtcNormalMusicPlayer.msgId += 1
        let msg = RtcMusicLrcMessage(msgId: AbstractRtcMusicPlayer.msgId, peerUid: rtcServer.uid, lrcId: music.id, duration: state.duration, time: state.position, ts: state.time)
        sendRtcStreamMessage(msg: msg, ordered: true)
    }

    func sendCountdown(time: Int) {
        AbstractRtcMusicPlayer.msgId += 1
        let msg = RtcMusicCountdownMessage(msgId: AbstractRtcMusicPlayer.msgId, peerUid: rtcServer.uid, time: time)
        sendRtcStreamMessage(msg: msg)
    }

    func didChangedTo(position: Int) {
        guard let player = player, let rtcServer = rtcServer else {
            return
        }
        self.position = position
        duration = player.getDuration()
        let state = RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: state, type: .position)
        sendRtcMusicStreamMessage(state: state)
        rtcServer.sendMusic(state: state)
    }

    func didChangedTo(state: AgoraMediaPlayerState, error _: AgoraMediaPlayerError) {
        guard let player = player, let rtcServer = rtcServer else {
            return
        }
        var sync = false
        self.state = state
        switch state {
        case .openCompleted:
            isPlaying = true
            position = 0
            duration = player.getDuration()
            player.play()
            sync = true
        case .paused:
            isPause = true
            sync = true
        case .stopped, .playBackCompleted, .playBackAllLoopsCompleted:
            isPlaying = false
            sync = true
        case .playing:
            isPlaying = true
            sync = true
        default:
            Logger.log(self, message: "status: \(state)", level: .info)
        }
        if sync {
            let state = RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: state, type: .position)
            rtcServer.sendMusic(state: state)
        }
    }

    func receiveThenProcess(uid _: UInt, cmd _: String, data _: NSDictionary) -> Bool {
        return music != nil
    }

    //    func agoraMediaPlayer(_: AgoraRtcMediaPlayerProtocol, audioFrame: AgoraAudioPcmFrame) -> AgoraAudioPcmFrame {
    //        if monoChannel, audioFrame.channelNumbers == 2 {
    //            let cpBytes = audioFrame.bytesPerSample.rawValue
    //            for index in 0 ..< audioFrame.samplesPerChannel {
    //                let leftStart = index * 2 * cpBytes
    //                let rightStart = leftStart + cpBytes
    //                let tempBuf = audioFrame.pcmBuffer.subdata(in: leftStart ..< leftStart + cpBytes)
    //                audioFrame.pcmBuffer.replaceSubrange(rightStart ..< rightStart + cpBytes, with: tempBuf)
    //            }
    //        } else if audioFrame.channelNumbers == 2 {
    ////            let cpBytes = audioFrame.bytesPerSample.rawValue
    ////            for index in 0 ..< audioFrame.samplesPerChannel {
    ////                let leftStart = index * 2 * cpBytes
    ////                let rightStart = leftStart + cpBytes
    ////                var leftBuf = audioFrame.pcmBuffer.subdata(in: leftStart ..< leftStart + cpBytes)
    ////                let rightBuf = audioFrame.pcmBuffer.subdata(in: rightStart ..< rightStart + cpBytes)
    ////                for bufIndex in 0 ..< cpBytes / 2 {
    ////                    var left = UInt16(leftBuf[bufIndex * 2] << 8) + UInt16(leftBuf[bufIndex * 2 + 1])
    ////                    let right = UInt16(rightBuf[bufIndex * 2] << 8) + UInt16(rightBuf[bufIndex * 2 + 1])
    ////                    if left > UInt16.max - right {
    ////                        left = UInt16.max
    ////                    } else {
    ////                        left = left + right
    ////                    }
    ////                    leftBuf[bufIndex * 2] = UInt8(left >> 8)
    ////                    leftBuf[bufIndex * 2 + 1] = UInt8(left & 0x00FF)
    ////                }
    ////                audioFrame.pcmBuffer.replaceSubrange(leftStart ..< leftStart + cpBytes, with: leftBuf)
    ////                audioFrame.pcmBuffer.replaceSubrange(rightStart ..< rightStart + cpBytes, with: leftBuf)
    ////            }
    //            let cpBytes = audioFrame.bytesPerSample.rawValue
    //            for index in 0 ..< audioFrame.samplesPerChannel {
    //                let leftStart = index * 2 * cpBytes
    //                let rightStart = leftStart + cpBytes
    //                let tempBuf = audioFrame.pcmBuffer.subdata(in: rightStart ..< rightStart + cpBytes)
    //                audioFrame.pcmBuffer.replaceSubrange(leftStart ..< leftStart + cpBytes, with: tempBuf)
    //            }
    //        }
    //        return audioFrame
    //    }
}

class RtcNormalMusicPlayer: AbstractRtcMusicPlayer {
    override func play(music: LocalMusic, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        if let player = player, let rtc = rtcServer.rtcEngine {
            self.music = music
            uid = rtcServer.uid
            if player.getPlayerState() == .playing {
                player.stop()
            }
            originMusic(enable: false)
            // rtc.setAudioProfile(.musicHighQualityStereo, scenario: .highDefinition)
            rtc.setAudioProfile(.default)

            let option = AgoraRtcChannelMediaOptions()
            option.publishMediaPlayerId = AgoraRtcIntOptional.of(player.getMediaPlayerId())
            option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
            option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
            option.autoSubscribeVideo = AgoraRtcBoolOptional.of(false)
            option.autoSubscribeAudio = AgoraRtcBoolOptional.of(true)
            option.publishMediaPlayerVideoTrack = AgoraRtcBoolOptional.of(false)
            option.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
            option.publishAudioTrack = AgoraRtcBoolOptional.of(true)
            option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(true)
            option.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(true)
            rtc.updateChannel(with: option)
            Logger.log(self, message: "open \(music.path)", level: .info)
            if player.open(music.path, startPos: 0) == 0 {
                onSuccess()
            } else {
                onFailed("打开文件失败！")
            }
        } else {
            onFailed("未初始化！")
        }
    }

    override func destory() {
        super.destory()
        if let rtc = rtcServer.rtcEngine {
            let option = AgoraRtcChannelMediaOptions()
            option.publishMediaPlayerId = nil
            option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
            option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
            option.autoSubscribeVideo = AgoraRtcBoolOptional.of(false)
            option.autoSubscribeAudio = AgoraRtcBoolOptional.of(true)
            option.publishMediaPlayerVideoTrack = AgoraRtcBoolOptional.of(false)
            option.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
            option.publishAudioTrack = AgoraRtcBoolOptional.of(true)
            option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(true)
            option.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(false)
            rtc.updateChannel(with: option)
        }
    }
}

class RtcChorusMusicPlayer: AbstractRtcMusicPlayer {
    private(set) var connectionId: UInt32 = 0
    var option: LocalMusicOption! {
        didSet {
            if let rtc = rtcServer.rtcEngine {
                if isFollower() {
                    if let oldMasterMusicUid = oldValue?.masterMusicUid {
                        rtc.muteRemoteAudioStream(oldMasterMusicUid, mute: false)
                    }
                    rtc.muteRemoteAudioStream(option.masterMusicUid, mute: true)
                }
            }
        }
    }

    private var timer: Timer?

    override func initPlayer(onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        if let player = player, let channelId = rtcServer.channel, let rtc = rtcServer.rtcEngine {
            if player.getPlayerState() == .playing {
                player.stop()
            }
            originMusic(enable: false)
            rtc.setParameters("{\"rtc.audio_fec\":[3,2]}")
            rtc.setParameters("{\"rtc.audio_resend\":false}")
            rtc.setParameters("{\"rtc.audio.max_neteq_packets\":2}")
            rtc.setParameters("{\"rtc.audio.max_target_delay\":20}")
            rtc.setAudioProfile(.default, scenario: .chorus)

            if connectionId == 0 {
                let option = AgoraRtcChannelMediaOptions()
                option.publishMediaPlayerId = AgoraRtcIntOptional.of(player.getMediaPlayerId())
                option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
                option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
                option.autoSubscribeVideo = AgoraRtcBoolOptional.of(false)
                option.autoSubscribeAudio = AgoraRtcBoolOptional.of(false)
                option.publishMediaPlayerVideoTrack = AgoraRtcBoolOptional.of(false)
                option.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
                option.publishAudioTrack = AgoraRtcBoolOptional.of(false)
                option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(false)
                option.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(false)
                // var connectionId: UInt = 0
                let code = rtc.joinChannelEx(byToken: BuildConfig.Token, channelId: channelId, uid: 0, connectionId: &connectionId, delegate: nil, mediaOptions: option) { _, uid, _ in
                    // self.connectionId = connectionId
                    Logger.log(self, message: "joinChannelEx success! channelId:\(channelId) uid:\(uid)", level: .info)
                    self.uid = uid
                    rtc.muteRemoteAudioStream(uid, mute: true)
                    onSuccess()
                }
                if code != 0 {
                    onFailed("joinChannelEx error!")
                }
            } else {
                rtc.muteRemoteAudioStream(uid, mute: true)
                onSuccess()
            }
        } else {
            onFailed("未初始化！")
        }
    }

    override func play(music: LocalMusic, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        if let player = player, let rtc = rtcServer.rtcEngine {
            self.music = music
            if player.getPlayerState() == .playing {
                player.stop()
            }
            originMusic(enable: false)
            initDelay()
            if connectionId == 0 || option == nil {
                onFailed("未初始化！")
            } else {
                if isFollower() {
                    let mediaOption = AgoraRtcChannelMediaOptions()
                    mediaOption.publishMediaPlayerId = AgoraRtcIntOptional.of(player.getMediaPlayerId())
                    mediaOption.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
                    mediaOption.publishCameraTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.autoSubscribeVideo = AgoraRtcBoolOptional.of(false)
                    mediaOption.autoSubscribeAudio = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishMediaPlayerVideoTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishAudioTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(false)
                    rtc.updateChannelEx(with: mediaOption, connectionId: connectionId)

                    if let timer = timer {
                        timer.invalidate()
                    }
                    sentTestDelayMessage()
                    timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: true, block: { [weak self] _ in
                        if let self = self {
                            self.sentTestDelayMessage()
                        }
                    })
                } else if isMaster() {
                    let mediaOption = AgoraRtcChannelMediaOptions()
                    mediaOption.publishMediaPlayerId = AgoraRtcIntOptional.of(player.getMediaPlayerId())
                    mediaOption.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
                    mediaOption.publishCameraTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.autoSubscribeVideo = AgoraRtcBoolOptional.of(false)
                    mediaOption.autoSubscribeAudio = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishMediaPlayerVideoTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishAudioTrack = AgoraRtcBoolOptional.of(false)
                    mediaOption.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(false)
                    mediaOption.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(true)
                    rtc.updateChannelEx(with: mediaOption, connectionId: connectionId)
                }
                if player.open(music.path, startPos: 0) == 0 {
                    onSuccess()
                } else {
                    onFailed("打开文件失败！")
                }
            }
        } else {
            onFailed("未初始化！")
        }
    }

    override func stop() {
        timer?.invalidate()
        timer = nil
        super.stop()
        if let rtc = rtcServer.rtcEngine {
            if isFollower() {
                rtc.muteRemoteAudioStream(option.masterMusicUid, mute: false)
            }
        }
    }

    override func didChangedTo(state: AgoraMediaPlayerState, error _: AgoraMediaPlayerError) {
        guard let player = player, let rtcServer = rtcServer else {
            return
        }
        var sync = false
        self.state = state
        switch state {
        case .openCompleted:
            isPlaying = true
            position = 0
            duration = player.getDuration()
            if option.masterUid == rtcServer.uid {
                sendRtcMusicStreamMessage(state: RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: 0, duration: duration, state: state, type: .position))
                MachWrapper.wait(Int32(delayPlayTime))
                player.play()
            } else {
                sentTestDelayMessage()
            }
            sync = true
        case .paused:
            isPause = true
            sync = true
        case .stopped, .playBackCompleted, .playBackAllLoopsCompleted:
            isPlaying = false
            sync = true
        case .playing:
            isPlaying = true
            sync = true
        default:
            Logger.log(self, message: "status: \(state)", level: .info)
        }
        if sync {
            rtcServer.sendMusic(state: RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: state, type: .position))
            if isMaster(), isPause || !isPlaying {
                sendRtcMusicStreamMessage(state: RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: -1, duration: duration, state: state, type: .position))
            }
        }
    }

    override func didChangedTo(position: Int) {
        guard let player = player, let rtcServer = rtcServer else {
            return
        }
        self.position = position
        duration = player.getDuration()
        let state = RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: state, type: .position)
        if isMaster() {
            sendRtcMusicStreamMessage(state: state)
        }
        rtcServer.sendMusic(state: state)
    }

    private func sentTestDelayMessage() {
        Logger.log(self, message: "sentTestDelayMessage", level: .info)
        AbstractRtcMusicPlayer.msgId += 1
        let time = CLongLong(round(Date().timeIntervalSince1970 * 1000))
        let msg = RtcTestDelayMessage(msgId: AbstractRtcMusicPlayer.msgId, peerUid: rtcServer.uid, time: String(time))
        sendRtcStreamMessage(msg: msg)
    }

    private func sentCheckTestDelayMessage(testDelayTime: String, time: String, position: Int) {
        Logger.log(self, message: "sentCheckTestDelayMessage", level: .info)
        AbstractRtcMusicPlayer.msgId += 1
        let msg = RtcCheckTestDelayMessage(msgId: AbstractRtcMusicPlayer.msgId, peerUid: rtcServer.uid, testDelayTime: testDelayTime, time: time, position: position)
        sendRtcStreamMessage(msg: msg)
    }

    private var delayWithBrod: CLongLong = 0
    private var lastSeekTime: CLongLong = 0
    private var lastExpectLocalPosition: CLongLong = 0
    private var seekTime: CLongLong = 0
    private var delay: CLongLong = 0
    private var needSeek = false

    private let delayPlayTime = 1000
    private let MAX_DELAY_MS = 40

    private func initDelay() {
        delayWithBrod = 0
        lastSeekTime = 0
        lastExpectLocalPosition = 0
        seekTime = 0
        delay = 0
        needSeek = false
    }

    /**
     *     m_                                                                     f_
     *  [master]        <-         testDelay:(f_ts)                  <-        [follower]
     *                  ->    replyTestDelay:(f_ts,m_ts,m_postion)   ->
     *
     *                            start = f_ts
     *                            bordTs = m_ts
     *                            postion = m_postion
     *                            Delay = (now - start)/2
     *                            DelayWithBrod = bordTs - now + Delay
     *                            expLocalTs = now - delay - position
     *                            diff_postion = position - f_postion
     *                            diff = abs(delay + diff_postion)
     *                              -> if > 40
     *                                   expSeek = Delay + position + SeekTime
     *                                   LastExpectLocalPosition = expSeek
     *                                   LastSeekTime = now
     *
     *
     *                  ->        setLrcTime:(m_position,m_ts)            ->
     *
     *                            bordTs = m_ts
     *                            postion = m_postion
     *                            -> if position = 0
     *                                 delay(500ms - Delay) -> start play
     *                            -> if position > 0
     *                                 expLocalTs = m_ts - m_postion - DelayWithBrod
     *                                 -> if LastSeekTime != 0
     *                                      SeekTime = LastExpectLocalPosition + (now - LastSeekTime) - f_position
     *                                      LastSeekTime = 0
     *                                      LastExpectLocalPosition = 0
     *                                 -> if abs(now - f_postion - expLocalTs) > 40
     *                                      expSeek = now - expLocalTs + SeekTime
     *                                      NeedSeek = true
     *
     *
     *
     *
     */

    private func isMaster() -> Bool {
        return option != nil && option.masterUid == rtcServer.uid
    }

    private func isFollower() -> Bool {
        return option != nil && option.followerUid == rtcServer.uid
    }

    override func receiveThenProcess(uid: UInt, cmd: String, data: NSDictionary) -> Bool {
        if music == nil {
            return false
        }

        if let player = self.player {
            Logger.log(self, message: "receiveThenProcess uid:\(uid) cmd:\(cmd)", level: .info)
            if isFollower(), option.masterUid == uid {
                // follower receive message from master
                switch cmd {
                case "replyTestDelay":
                    let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))
                    let testDelayTime = data["testDelayTime"] as! String
                    let time = data["time"] as! String
                    let start = CLongLong(testDelayTime)!
                    let brodTs = CLongLong(time)!
                    let position = CLongLong(data["position"] as! Int)

                    delay = (now - start) / 2
                    delayWithBrod = brodTs - now + delay
                    if needSeek, player.getPlayerState() == .playing {
                        let expLocalTs = brodTs - position - delayWithBrod
                        let localPosition = CLongLong(player.getPosition())
                        let diff = now - localPosition - expLocalTs
                        if abs(diff) > MAX_DELAY_MS {
                            let expSeek = now - expLocalTs + seekTime
                            lastExpectLocalPosition = expSeek
                            lastSeekTime = now
                            player.seek(toPosition: Int(expSeek))
                            Logger.log(self, message: "checkTestDelay:\(delay) seekTime:\(seekTime) diff:\(diff)", level: .info)
                        }
                    }
                case "setLrcTime":
                    let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))
                    let position = data["time"] as! Int
                    let ts = data["ts"] as! String
                    let brodTs = CLongLong(ts)!
                    if position < 0 {
                        if player.getPlayerState() == .playing {
                            player.pause()
                        }
                    } else if position == 0 {
                        if player.getPlayerState() == .openCompleted {
                            MachWrapper.wait(Int32(delayPlayTime - Int(delay)))
                            player.play()
                        } else if player.getPlayerState() == .paused {
                            player.resume()
                        }
                    } else {
                        let expLocalTs = brodTs - CLongLong(position) - delayWithBrod
                        if player.getPlayerState() == .playing {
                            let localPosition = CLongLong(player.getPosition())
                            if lastSeekTime != 0 {
                                seekTime = lastExpectLocalPosition + now - lastSeekTime - localPosition
                                lastSeekTime = 0
                                lastExpectLocalPosition = 0
                            }
                            if abs(Int(now - localPosition - expLocalTs)) > MAX_DELAY_MS {
                                needSeek = true
                            }
                        } else if player.getPlayerState() == .openCompleted {
                            player.seek(toPosition: Int(now - expLocalTs))
                            player.play()
                        } else if player.getPlayerState() == .paused {
                            player.resume()
                        }
                    }
                // sentTestDelayMessage()
                default:
                    break
                }
            } else if isMaster(), option.followerUid == uid {
                // master receive message from folower
                switch cmd {
                case "testDelay":
                    let testDelayTime = data["time"] as! String
                    let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))
                    sentCheckTestDelayMessage(testDelayTime: testDelayTime, time: String(now), position: player.getPosition())
                default:
                    break
                }
            }
        }
        return true
    }

    override func destory() {
        super.destory()
        if let rtc = rtcServer.rtcEngine {
            if let channelId = rtcServer.channel, connectionId != 0 {
                rtc.leaveChannelEx(channelId, connectionId: connectionId, leaveChannelBlock: nil)
                connectionId = 0
            }
        }
    }
}
