//
//  LiveKtvMember.swift
//  LiveKtv
//
//  Created by XC on 2021/6/8.
//

import Core
import Foundation
import RxSwift

enum LiveKtvRoomRole: Int {
    case listener = 0
    case manager = 1
    case speaker = 2
    case unknown = 3
}

class LiveKtvMember: Codable, IAgoraModel {
    public var id: String
    public var isMuted: Bool
    public var isSelfMuted: Bool
    public var role: Int = LiveKtvRoomRole.listener.rawValue
    public var roomId: String
    public var streamId: UInt
    public var userId: String
    public var avatar: String

    public var isManager: Bool = false
    // public var isLocal: Bool = false

    public init(id: String, isMuted: Bool, isSelfMuted: Bool, role: Int, roomId: String, streamId: UInt, userId: String, avatar: String) {
        self.id = id
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.role = role
        self.roomId = roomId
        self.streamId = streamId
        self.userId = userId
        self.avatar = avatar
    }

    public func isSpeaker() -> Bool {
        return role == LiveKtvRoomRole.manager.rawValue || role == LiveKtvRoomRole.speaker.rawValue
    }

    public func toLiveKtvRoomRole() -> LiveKtvRoomRole {
        switch role {
        case LiveKtvRoomRole.listener.rawValue:
            return LiveKtvRoomRole.listener
        case LiveKtvRoomRole.manager.rawValue:
            return LiveKtvRoomRole.manager
        case LiveKtvRoomRole.speaker.rawValue:
            return LiveKtvRoomRole.speaker
        default:
            return LiveKtvRoomRole.unknown
        }
    }

    public func action(with action: LiveKtvActionType) -> LiveKtvAction {
        return LiveKtvAction(id: "", action: action, memberId: id, roomId: roomId)
    }

    func toDictionary() -> [String: Any?] {
        return [
            LiveKtvMember.MUTED: isMuted ? 1 : 0,
            LiveKtvMember.SELF_MUTED: isSelfMuted ? 1 : 0,
            LiveKtvMember.ROLE: role,
            LiveKtvMember.ROOM: roomId,
            LiveKtvMember.STREAM_ID: streamId,
            LiveKtvMember.USER: userId,
        ]
    }
}

extension LiveKtvMember {
    static let TABLE: String = "MEMBER_KTV"
    static let MUTED: String = "isMuted"
    static let SELF_MUTED: String = "isSelfMuted"
    static let ROLE: String = "role"
    static let ROOM: String = "roomId"
    static let STREAM_ID = "streamId"
    static let USER = "userId"
    static let AVATAR = "avatar"

    static func get(object: IAgoraObject, roomId: String) throws -> LiveKtvMember {
        let id = try object.getId()
        let isMuted: Bool = (try object.getValue(key: LiveKtvMember.MUTED, type: Int.self) as? Int ?? 0) == 1
        let isSelfMuted: Bool = (try object.getValue(key: LiveKtvMember.SELF_MUTED, type: Int.self) as? Int ?? 0) == 1
        let role: Int = try object.getValue(key: LiveKtvMember.ROLE, type: Int.self) as! Int
        let streamId: UInt = try object.getValue(key: LiveKtvMember.STREAM_ID, type: UInt.self) as! UInt
        let userId: String = try object.getValue(key: LiveKtvMember.USER, type: String.self) as! String
        let avatar: String = try object.getValue(key: LiveKtvMember.AVATAR, type: String.self) as! String
        let member = LiveKtvMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, roomId: roomId, streamId: streamId, userId: userId, avatar: avatar)
        return member
    }

    static func get(object: IAgoraObject, room: LiveKtvRoom) throws -> LiveKtvMember {
        let id = try object.getId()
        let isMuted: Bool = (try object.getValue(key: LiveKtvMember.MUTED, type: Int.self) as? Int ?? 0) == 1
        let isSelfMuted: Bool = (try object.getValue(key: LiveKtvMember.SELF_MUTED, type: Int.self) as? Int ?? 0) == 1
        let role: Int = try object.getValue(key: LiveKtvMember.ROLE, type: Int.self) as! Int
        let streamId: UInt = try object.getValue(key: LiveKtvMember.STREAM_ID, type: UInt.self) as! UInt
        let userId: String = try object.getValue(key: LiveKtvMember.USER, type: String.self) as! String
        let avatar: String = try object.getValue(key: LiveKtvMember.AVATAR, type: String.self) as! String
        let member = LiveKtvMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, roomId: room.id, streamId: streamId, userId: userId, avatar: avatar)
        member.isManager = member.userId == room.userId
        return member
    }

    func update(streamId: UInt) -> Observable<Result<Void>> {
        self.streamId = streamId
        return Observable.just(Result(success: true, data: nil))
    }
}
