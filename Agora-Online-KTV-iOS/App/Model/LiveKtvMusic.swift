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

    public var id: String
    public var roomId: String
    public var name: String
    public var musicId: String
    public var singer: String
    public var poster: String

    public var type: Int

    public var userId: String
    public var userStatus: Int?

    public var user1Id: String?
    public var user1Status: Int?

    public var applyUser1Id: String?

    init(id: String, userId: String, roomId: String, name: String, musicId: String, type: Int = LiveKtvMusic.NORMAL, singer: String, poster: String) {
        self.id = id
        self.userId = userId
        self.roomId = roomId
        self.name = name
        self.musicId = musicId
        self.type = type
        self.singer = singer
        self.poster = poster
    }

    func isOrderBy(member: LiveKtvMember) -> Bool {
        return userId == member.userId
    }

    func toDictionary() -> [String: Any?] {
        return [
            LiveKtvMusic.NAME: name,
            LiveKtvMusic.MUSIC_ID: musicId,
            LiveKtvMusic.ROOM: roomId,
            LiveKtvMusic.USER: userId,
            LiveKtvMusic.SINGER: singer,
            LiveKtvMusic.POSTER: poster,
        ]
    }
}

extension LiveKtvMusic {
    static let TABLE: String = "MUSIC_KTV"
    static let ROOM: String = "roomId"
    static let USER = "userId"
    static let NAME = "name"
    static let MUSIC_ID = "musicId"
    static let SINGER = "singer"
    static let POSTER = "poster"

    static func get(object: IAgoraObject, room: LiveKtvRoom) throws -> LiveKtvMusic {
        let id = try object.getId()
        let name: String = try object.getValue(key: LiveKtvMusic.NAME, type: String.self) as! String
        let music: String = try object.getValue(key: LiveKtvMusic.MUSIC_ID, type: String.self) as! String
        let singer: String = try object.getValue(key: LiveKtvMusic.SINGER, type: String.self) as! String
        let poster: String = try object.getValue(key: LiveKtvMusic.POSTER, type: String.self) as! String
        let roomId = room.id
        let userId: String = try object.getValue(key: LiveKtvMusic.USER, type: String.self) as! String
        return LiveKtvMusic(id: id, userId: userId, roomId: roomId, name: name, musicId: music, singer: singer, poster: poster)
    }
}
