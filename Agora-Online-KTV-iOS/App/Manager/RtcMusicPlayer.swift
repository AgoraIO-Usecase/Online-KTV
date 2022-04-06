//
//  RtcMusicPlayer.swift
//  app
//
//  Created by XC on 2021/7/21.
//

import AgoraRtcKit
import Core
import Foundation
import LrcView
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

struct PlayModeMessage: Encodable, Decodable {
    let uid: Int
    let mode: Int
    var cmd: String = "TrackMode"
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
    var player: AgoraRtcMediaPlayerProtocol? { get }
    var music: LocalMusic? { get set }
    var uid: UInt { get set }
    var streamId: Int { get }
    var orderedStreamId: Int { get }
    var position: Int { get set }
    var duration: Int { get set }
    var isPlaying: Bool { get set }
    var state: AgoraMediaPlayerState? { get set }
    var isPause: Bool { get set }

    init(rtcServer: RtcServer)
    func initPlayer(isMaster: Bool, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void)
    func play(music: LocalMusic, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void)
    func getAudioTrackCount() -> Int
    func getPlayoutVolume() -> Int32
    func adjustPlayoutVolume(value: Int32)
    func originMusic(enable: Bool)
    func seek(position: Int)
    func setPitch(pitch: Int)
    func pause()
    func resume()
    func stop()
    func destory()
    func sendRtcMusicStreamMessage(state: RtcMusicState)
    func sendMusicPlayMode(mode: Int)
    func sendCountdown(time: Int)

    func didChangedTo(position: Int)
    func didChangedTo(state: AgoraMediaPlayerState, error: AgoraMediaPlayerError)
    func receiveThenProcess(uid: UInt, cmd: String, data: NSDictionary) -> Bool
}

class AbstractRtcMusicPlayer: NSObject, IRtcMusicPlayer /* , AgoraRtcMediaPlayerAudioFrameDelegate */ {
    func sendMusicPlayMode(mode: Int) {
        Logger.log(self, message: "TrackMode", level: .info)
        AbstractRtcMusicPlayer.msgId += 1
        let msg = PlayModeMessage(uid: Int(rtcServer.uid), mode: mode)
        sendRtcStreamMessage(msg: msg)
    }

    static var msgId: Int = 0
    weak var rtcServer: RtcServer!

    var player: AgoraRtcMediaPlayerProtocol? {
        return rtcServer.getAgoraMusicPlayer()
    }

    var streamId: Int {
        return rtcServer.getDataStreamId()
    }

    var orderedStreamId: Int {
        return rtcServer.getOrderedDataStreamId()
    }

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
        // player = self.rtcServer.rtcEngine!.createMediaPlayer(with: rtcServer)
        // player.setAudioFrameDelegate(self, mode: .readWrite)
    }

    func initPlayer(isMaster _: Bool, onSuccess: @escaping () -> Void, onFailed _: @escaping (String) -> Void) {
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
        player?.adjustPublishSignalVolume(value)
        player?.adjustPlayoutVolume(value)
    }

    func originMusic(enable: Bool) {
        // monoChannel = enable
        if let player = player {
            player.setAudioDualMonoMode(enable ? .L : .R)
        }
    }

    func setPitch(pitch: Int) {
        // monoChannel = enable
        if let player = player {
            player.setAudioPitch(pitch)
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
//        if let player = player, let rtc = rtcServer.rtcEngine {
//            rtc.destroyMediaPlayer(player)
//        }
//        player = nil
    }

    fileprivate func sendRtcStreamMessage<T: Encodable>(msg: T, ordered: Bool = false) {
        guard let rtcEngine = rtcServer.rtcEngine else {
            return
        }
        let streamId = ordered ? orderedStreamId : self.streamId
        if streamId == -1 {
            Logger.log(self, message: "error streamId == -1", level: .error)
            return
        }

        let jsonEncoder = JSONEncoder()
        do {
            let jsonData = try jsonEncoder.encode(msg)
            let str = NSString(data: jsonData, encoding: String.Encoding.utf8.rawValue)
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
        let state = RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: self.state, type: .position)
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
//            rtc.setAudioProfile(AgoraAudioProfile(rawValue: 8)!)

            let option = AgoraRtcChannelMediaOptions()
            option.publishMediaPlayerId = AgoraRtcIntOptional.of(player.getMediaPlayerId())
            option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
            option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
            option.autoSubscribeAudio = AgoraRtcBoolOptional.of(true)
            option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(true)
            option.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(true)
            rtc.updateChannel(with: option)

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
            option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
            option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
            option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(true)
            rtc.updateChannel(with: option)
        }
    }
}

class RtcChorusMusicPlayer: AbstractRtcMusicPlayer {
    private(set) var connection: AgoraRtcConnection?
    private let playerUid = UInt.random(in: 1001 ... 2000)
    var option: LocalMusicOption!
    private var timer: Timer?

    override func initPlayer(isMaster: Bool, onSuccess: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        if let player = player, let channelId = rtcServer.channel, let rtc = rtcServer.rtcEngine {
            if player.getPlayerState() == .playing {
                player.stop()
            }
            originMusic(enable: false)
            rtc.setParameters("{\"rtc.audio_fec\":[3,2]}")
            rtc.setParameters("{\"rtc.audio_resend\":false}")
            rtc.setParameters("{\"rtc.audio.opensl.mode\":0}")

            if connection == nil, isMaster {
                let option = AgoraRtcChannelMediaOptions()
                option.clientRoleType = AgoraRtcIntOptional.of(Int32(AgoraClientRole.broadcaster.rawValue))
                option.publishCameraTrack = AgoraRtcBoolOptional.of(false)
                option.publishCustomAudioTrack = AgoraRtcBoolOptional.of(false)
                option.enableAudioRecordingOrPlayout = AgoraRtcBoolOptional.of(false)
                option.autoSubscribeAudio = AgoraRtcBoolOptional.of(false)
                option.publishAudioTrack = AgoraRtcBoolOptional.of(false)
                let id = player.getMediaPlayerId()
                option.publishMediaPlayerId = AgoraRtcIntOptional.of(id)
                option.publishMediaPlayerAudioTrack = AgoraRtcBoolOptional.of(true)
                connection = AgoraRtcConnection()
                connection?.localUid = playerUid
                connection?.channelId = channelId
                let code = rtc.joinChannelEx(byToken: BuildConfig.Token, connection: connection!, delegate: nil, mediaOptions: option) { _, uid, _ in
                    Logger.log(self, message: "joinChannelEx success! channelId:\(channelId) uid:\(uid)", level: .info)
                    self.uid = uid
                    onSuccess()
                }
                if code != 0 {
                    onFailed("joinChannelEx error! \(code)")
                }
            } else {
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
            if (isMaster() && connection == nil) || option == nil {
                onFailed("未初始化！")
            } else {
                rtc.muteRemoteAudioStream(option.masterMusicUid, mute: true)
                if isFollower() {
                    player.adjustPlayoutVolume(70)

                    if let timer = timer {
                        timer.invalidate()
                    }
                    sentTestDelayMessage()
                    timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: true, block: { [weak self] _ in
                        if let self = self {
                            self.sentTestDelayMessage()
                        }
                    })
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
            } else if isMaster(), let _ = rtcServer.channel, let _ = connection {
                rtc.leaveChannelEx(connection!, leaveChannelBlock: nil)
                connection = nil
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
        let state = RtcMusicState(uid: rtcServer.uid, streamId: streamId, position: position, duration: duration, state: self.state, type: .position)
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
    private var waitting = false
    private let delayPlayTime = 1000
    private let MAX_DELAY_MS = 40

    private func initDelay() {
        delayWithBrod = 0
        lastSeekTime = 0
        lastExpectLocalPosition = 0
        seekTime = 0
        delay = 0
        needSeek = false
        waitting = false
    }

    private func isMaster() -> Bool {
        return option != nil && option.masterUid == rtcServer.uid
    }

    private func isFollower() -> Bool {
        return option != nil && option.followerUid == rtcServer.uid
    }

    /**
     *     m_                                                                     f_
     *  [master]        <-         testDelay:(f_ts)                  <-        [follower]
     *                  ->    replyTestDelay:(f_ts,m_ts,m_postion)   ->
     *
     *                  ->        setLrcTime:(m_position,m_ts)       ->
     */
    override func receiveThenProcess(uid: UInt, cmd: String, data: NSDictionary) -> Bool {
        if music == nil {
            return false
        }

        if let player = player {
            Logger.log(self, message: "receiveThenProcess uid:\(uid) cmd:\(cmd)", level: .info)
            if isFollower(), option.masterUid == uid {
                // follower receive message from master
                switch cmd {
                case "TrackMode":
                    if let mode = data["mode"] as? Int {
                        originMusic(enable: mode == 1)
                    }
                case "replyTestDelay":
                    if let testDelayTime = data["testDelayTime"] as? String,
                       let position = data["position"] as? Int,
                       let start = CLongLong(testDelayTime)
                    {
//                        let position = CLongLong(iPosition)
                        let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))

                        delay = (now - start) / 2
                        delayWithBrod = Int64(position) + delay
                        if needSeek, player.getPlayerState() == .playing {
//                            let expLocalTs = brodTs - position - delayWithBrod
                            let localPosition = CLongLong(player.getPosition())
                            let diff = localPosition - delayWithBrod
                            if abs(diff) > MAX_DELAY_MS {
//                                let expSeek = now - expLocalTs + seekTime
//                                lastExpectLocalPosition = expSeek
//                                lastSeekTime = now
                                player.seek(toPosition: Int(delayWithBrod))
                                // Logger.log(self, message: "toPosition:\(delayWithBrod) from:\(localPosition) remote:\(position)", level: .info)
                            }
                        }
                    }
                case "setLrcTime":
                    if let position = data["time"] as? Int {
                        let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))
                        if position < 0 {
                            if player.getPlayerState() == .playing {
                                player.pause()
                            }
                        } else if position == 0 {
                            if player.getPlayerState() == .openCompleted {
                                if !waitting {
                                    waitting = true
                                    MachWrapper.wait(Int32(delayPlayTime - Int(delay)))
                                    player.play()
                                    waitting = false
                                }
                            } else if player.getPlayerState() == .paused {
                                player.resume()
                            }
                        } else {
//                            let expLocalTs = brodTs - CLongLong(position) - delayWithBrod
                            if player.getPlayerState() == .playing {
                                let localPosition = CLongLong(player.getPosition())
                                if lastSeekTime != 0 {
                                    seekTime = lastExpectLocalPosition + now - lastSeekTime - localPosition
                                    lastSeekTime = 0
                                    lastExpectLocalPosition = 0
                                }
                                if abs(Int(CLongLong(position) + delay - localPosition)) > MAX_DELAY_MS {
                                    needSeek = true
                                }
                            } else if player.getPlayerState() == .openCompleted {
                                if !waitting {
                                    waitting = true
                                    player.seek(toPosition: position)
                                    player.play()
                                    waitting = false
                                }
                            } else if player.getPlayerState() == .paused {
                                player.resume()
                            }
                        }
                    }
                    return false
                default:
                    break
                }
            } else if isMaster(), option.followerUid == uid {
                // master receive message from folower
                switch cmd {
                case "testDelay":
                    if let testDelayTime = data["time"] as? String {
                        let now = CLongLong(round(Date().timeIntervalSince1970 * 1000))
                        sentCheckTestDelayMessage(testDelayTime: testDelayTime, time: String(now), position: player.getPosition())
                    }
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
            if let _ = rtcServer.channel, let _ = connection {
                rtc.leaveChannelEx(connection!, leaveChannelBlock: nil)
                connection = nil
            }
        }
    }
}
