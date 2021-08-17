//
//  LiveKtvMusic.swift
//  LiveKtv
//
//  Created by XC on 2021/6/10.
//

import Core
import Foundation
import RxSwift

class LiveKtvMusic: Codable, IAgoraModel {
    public static let NORMAL = 0
    public static let CHORUS = 1
    public static let READY = 1

    public var id: String
    public var roomId: String
    public var name: String
    public var musicId: String
    public var poster: String?
    public var singer: String?

    public var type: Int

    public var userId: String
    public var userStatus: Int?
    public var userbgId: UInt?

    public var user1Id: String?
    public var user1Status: Int?
    public var user1bgId: UInt?

    public var applyUser1Id: String?

    init(id: String, userId: String, roomId: String, name: String, musicId: String, type: Int = LiveKtvMusic.NORMAL,poster:String?,singer:String?) {
        self.id = id
        self.userId = userId
        self.roomId = roomId
        self.name = name
        self.musicId = musicId
        self.type = type
        self.poster = poster
        self.singer = singer
    }

    func isOrderBy(member: LiveKtvMember) -> Bool {
        return userId == member.userId
    }

    func isChorus() -> Bool {
        return type == LiveKtvMusic.CHORUS
    }

    func isChorusReady() -> Bool {
        return isChorus() &&
            userStatus == LiveKtvMusic.READY &&
            userbgId != nil &&
            user1Id != nil &&
            user1Status == LiveKtvMusic.READY &&
            user1bgId != nil
    }

    func toDictionary() -> [String: Any?] {
        return [
            LiveKtvMusic.NAME: name,
            LiveKtvMusic.MUSIC_ID: musicId,
            LiveKtvMusic.ROOM: roomId,
            LiveKtvMusic.POSTER: poster,
            LiveKtvMusic.SINGER: singer,
            LiveKtvMusic.TYPE: type,
            LiveKtvMusic.USER: userId,
            LiveKtvMusic.USER_STATUS: userStatus,
            LiveKtvMusic.USER_BG_ID: userbgId,
            LiveKtvMusic.USER1: user1Id,
            LiveKtvMusic.USER1_STATUS: user1Status,
            LiveKtvMusic.USER1_BG_ID: user1bgId,
            LiveKtvMusic.APPLY_USER1_ID: applyUser1Id,
        ]
    }
}

extension LiveKtvMusic {
    static let TABLE = "MUSIC_KTV"
    static let ROOM = "roomId"
    static let NAME = "name"
    static let MUSIC_ID = "musicId"
    static let POSTER = "poster"
    static let SINGER = "singer"

    static let TYPE = "type"
    static let USER = "userId"
    static let USER_STATUS = "userStatus"
    static let USER_BG_ID = "userbgId"
    static let USER1 = "user1Id"
    static let USER1_STATUS = "user1Status"
    static let USER1_BG_ID = "user1bgId"
    static let APPLY_USER1_ID = "applyUser1Id"

    private static var manager: SyncManager {
        SyncManager.shared
    }

    static func get(object: IAgoraObject, room: LiveKtvRoom) throws -> LiveKtvMusic {
        let id = try object.getId()
        let name: String = try object.getValue(key: LiveKtvMusic.NAME, type: String.self) as! String
        let music: String = try object.getValue(key: LiveKtvMusic.MUSIC_ID, type: String.self) as! String
        let roomId = room.id
        let poster: String? = try object.getValue(key: LiveKtvMusic.POSTER, type: String.self) as? String
        let singer: String? = try object.getValue(key: LiveKtvMusic.SINGER, type: String.self) as? String

        let type: Int = try object.getValue(key: LiveKtvMusic.TYPE, type: Int.self) as? Int ?? LiveKtvMusic.NORMAL
        let userId: String = try object.getValue(key: LiveKtvMusic.USER, type: String.self) as! String
        let userStatus: Int? = try object.getValue(key: LiveKtvMusic.USER_STATUS, type: Int.self) as? Int
        let userbgId: UInt? = try object.getValue(key: LiveKtvMusic.USER_BG_ID, type: UInt.self) as? UInt
        let user1Id: String? = try object.getValue(key: LiveKtvMusic.USER1, type: String.self) as? String
        let user1Status: Int? = try object.getValue(key: LiveKtvMusic.USER1_STATUS, type: Int.self) as? Int
        let user1bgId: UInt? = try object.getValue(key: LiveKtvMusic.USER1_BG_ID, type: UInt.self) as? UInt
        let applyUser1Id: String? = try object.getValue(key: LiveKtvMusic.APPLY_USER1_ID, type: String.self) as? String

        let liveMusic = LiveKtvMusic(id: id, userId: userId, roomId: roomId, name: name, musicId: music, type: type,poster:poster,singer: singer)
        liveMusic.userStatus = userStatus
        liveMusic.userbgId = userbgId
        liveMusic.user1Id = user1Id
        liveMusic.user1Status = user1Status
        liveMusic.user1bgId = user1bgId
        liveMusic.applyUser1Id = applyUser1Id

        return liveMusic
    }

//    static func get(object: IAgoraObject, member: LiveKtvMember) throws -> LiveKtvMusic {
//        let id = try object.getId()
//        let name: String = try object.getValue(key: LiveKtvMusic.NAME, type: String.self) as! String
//        let music: String = try object.getValue(key: LiveKtvMusic.MUSIC_ID, type: String.self) as! String
//        let roomId = member.roomId
//        let memberId: String = member.id // try object.getValue(key: LiveKtvMusic.MEMBER, type: String.self) as! String
//        return LiveKtvMusic(id: id, memberId: memberId, roomId: roomId, name: name, musicId: music)
//    }

    func order() -> Observable<Result<LiveKtvMusic>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .add(data: self.toDictionary(), delegate: AgoraObjectDelegate(success: { object in
                    do {
                        self.id = try object.getId()
                        single(.success(Result(success: true, data: self)))
                    } catch {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    }
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func asNormal() -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMusic.TYPE: LiveKtvMusic.NORMAL], delegate: AgoraObjectDelegate(success: { _ in
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func applyAsFollower(member: LiveKtvMember) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .document(id: self.id)
                .update(data: [LiveKtvMusic.APPLY_USER1_ID: member.userId], delegate: AgoraObjectDelegate(success: { _ in
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    func delete() -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: self.roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .document(id: self.id)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))
            return Disposables.create()
        }.asObservable()
    }

    static func delete(roomId: String) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))

            return Disposables.create()
        }.asObservable()
    }

    static func delete(roomId: String, userId: String) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvMusic.manager
                .getRoom(id: roomId)
                .collection(className: LiveKtvMusic.TABLE)
                .document()
                .whereEqual(key: LiveKtvMusic.USER, value: userId)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))

            return Disposables.create()
        }.asObservable()
    }
}
