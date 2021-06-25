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

    public var isManager: Bool = false
    // public var isLocal: Bool = false

    public init(id: String, isMuted: Bool, isSelfMuted: Bool, role: Int, roomId: String, streamId: UInt, userId: String) {
        self.id = id
        self.isMuted = isMuted
        self.isSelfMuted = isSelfMuted
        self.role = role
        self.roomId = roomId
        self.streamId = streamId
        self.userId = userId
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

    private static var manager: SyncManager {
        SyncManager.shared
    }

    static func get(object: IAgoraObject, roomId: String) throws -> LiveKtvMember {
        let id = try object.getId()
        let isMuted: Bool = (try object.getValue(key: LiveKtvMember.MUTED, type: Int.self) as? Int ?? 0) == 1
        let isSelfMuted: Bool = (try object.getValue(key: LiveKtvMember.SELF_MUTED, type: Int.self) as? Int ?? 0) == 1
        let role: Int = try object.getValue(key: LiveKtvMember.ROLE, type: Int.self) as! Int
        let streamId: UInt = try object.getValue(key: LiveKtvMember.STREAM_ID, type: UInt.self) as! UInt
        let userId: String = try object.getValue(key: LiveKtvMember.USER, type: String.self) as! String
        let member = LiveKtvMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, roomId: roomId, streamId: streamId, userId: userId)
        // member.isManager = member.userId == room.userId
        return member
    }

    static func get(object: IAgoraObject, room: LiveKtvRoom) throws -> LiveKtvMember {
        let id = try object.getId()
        let isMuted: Bool = (try object.getValue(key: LiveKtvMember.MUTED, type: Int.self) as? Int ?? 0) == 1
        let isSelfMuted: Bool = (try object.getValue(key: LiveKtvMember.SELF_MUTED, type: Int.self) as? Int ?? 0) == 1
        let role: Int = try object.getValue(key: LiveKtvMember.ROLE, type: Int.self) as! Int
        let streamId: UInt = try object.getValue(key: LiveKtvMember.STREAM_ID, type: UInt.self) as! UInt
        let userId: String = try object.getValue(key: LiveKtvMember.USER, type: String.self) as! String
        let member = LiveKtvMember(id: id, isMuted: isMuted, isSelfMuted: isSelfMuted, role: role, roomId: room.id, streamId: streamId, userId: userId)
        member.isManager = member.userId == room.userId
        return member
    }

    static func delete(roomId: String) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: roomId)
                .collection(className: LiveKtvMember.TABLE)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func delete() -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .document(id: self.id)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func join(room: LiveKtvRoom) -> Observable<Result<Void>> {
        return Single.create { single in
            self.streamId = 0
            self.roomId = room.id
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .get(delegate: AgoraObjectListDelegate(success: { result in
                    do {
                        let members = try result.map { object -> LiveKtvMember in
                            try LiveKtvMember.get(object: object, roomId: self.roomId)
                        }
                        let member = members.first { member in
                            member.userId == self.userId
                        }
                        if let member = member {
                            self.id = member.id
                            self.isMuted = member.isMuted
                            self.isSelfMuted = member.isSelfMuted
                            self.role = member.role
                            single(.success(Result(success: true)))
                        } else {
                            LiveKtvMember.manager
                                .getRoom(id: self.roomId)
                                .collection(className: LiveKtvMember.TABLE)
                                .add(data: self.toDictionary(), delegate: AgoraObjectDelegate(success: { object in
                                    do {
                                        self.id = try object.getId()
                                        single(.success(Result(success: true)))
                                    } catch {
                                        single(.success(Result(success: false, message: error.localizedDescription)))
                                    }
                                }, failed: { _, message in
                                    single(.success(Result(success: false, message: message)))
                                }))
                        }
                    } catch {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    }
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func update(streamId: UInt) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMember.STREAM_ID: streamId], delegate: AgoraObjectDelegate(success: { _ in
                    self.streamId = streamId
                    single(.success(Result<Void>(success: true)))
                }, failed: { _, message in
                    single(.success(Result<Void>(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func leave() -> Observable<Result<Void>> {
        if isManager {
            return Observable.zip(
                LiveKtvRoom.delete(roomId: roomId),
                LiveKtvMember.delete(roomId: roomId),
                LiveKtvMusic.delete(roomId: roomId)
            ).map { args in
                let (result0, result1, result2) = args
                if result0.success, result1.success, result2.success {
                    return result0
                } else {
                    return result0.success ? result1.success ? result2 : result1 : result0
                }
            }
        } else {
            return Observable.zip(
                LiveKtvMusic.delete(roomId: roomId, userId: userId),
                delete()
            ).map { args in
                let (result0, result1) = args
                if result0.success, result1.success {
                    return result0
                } else {
                    return result0.success ? result1 : result0
                }
            }
        }
    }

    func selfMute(mute: Bool) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMember.SELF_MUTED: mute ? 1 : 0], delegate: AgoraObjectDelegate(success: { _ in
                    // self.isSelfMuted = true
                    single(.success(Result<Void>(success: true)))
                }, failed: { _, message in
                    single(.success(Result<Void>(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func asSpeaker() -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMember.ROLE: LiveKtvRoomRole.speaker.rawValue, LiveKtvMember.SELF_MUTED: 0, LiveKtvMember.MUTED: 0], delegate: AgoraObjectDelegate(success: { _ in
                    single(.success(Result<Void>(success: true)))
                }, failed: { _, message in
                    single(.success(Result<Void>(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func asListener() -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMember.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMember.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMember.ROLE: LiveKtvRoomRole.listener.rawValue, LiveKtvMember.SELF_MUTED: 0, LiveKtvMember.MUTED: 0], delegate: AgoraObjectDelegate(success: { _ in
                    single(.success(Result<Void>(success: true)))
                }, failed: { _, message in
                    single(.success(Result<Void>(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func orderMusic(id: String, name: String) -> Observable<Result<Void>> {
        return LiveKtvMusic(id: "", userId: userId, roomId: roomId, name: name, musicId: id).order().map { $0.transform() }
    }
}
