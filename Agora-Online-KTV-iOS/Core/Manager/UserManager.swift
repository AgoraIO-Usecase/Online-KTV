//
//  UserManager.swift
//  Core
//
//  Created by XC on 2021/6/1.
//

import Foundation
import RxSwift

public extension User {
    static let TABLE: String = "USER"
    static let NAME: String = "name"
    static let AVATAR: String = "avatar"

    func getLocalAvatar() -> String {
        switch avatar {
        case "0":
            return "default"
        case "1":
            return "portrait02"
        case "2":
            return "portrait03"
        case "3":
            return "portrait04"
        case "4":
            return "portrait05"
        case "5":
            return "portrait06"
        case "6":
            return "portrait07"
        case "7":
            return "portrait08"
        case "8":
            return "portrait09"
        case "9":
            return "portrait10"
        case "10":
            return "portrait11"
        case "11":
            return "portrait12"
        case "12":
            return "portrait13"
        case "13":
            return "portrait14"
        default:
            return "default"
        }
    }
}

public protocol IUserManager {
    func randomUser() -> Observable<Result<User>>
    func create(user: User) -> Observable<Result<String>>
    func getUser(by objectId: String, avatar: String) -> Observable<Result<User>>
    func update(user: User, name: String) -> Observable<Result<Void>>
    func getMusicList(key: String?) -> Observable<Result<[LrcMusic]>>
    func getMusic(id: String) -> Observable<Result<LrcMusic>>
}

public extension User {
    private static var manager: IUserManager {
        InjectionService.shared.resolve(IUserManager.self)
    }

    static func create(user: User) -> Observable<Result<String>> {
        return User.manager.create(user: user)
    }

    static func getUser(by objectId: String, avatar: String) -> Observable<Result<User>> {
        return User.manager.getUser(by: objectId, avatar: avatar)
    }

    static func randomUser() -> Observable<Result<User>> {
        return User.manager.randomUser()
    }

    func update(name: String) -> Observable<Result<Void>> {
        return User.manager.update(user: self, name: name)
    }

    func getMusicList(key: String? = nil) -> Observable<Result<[LrcMusic]>> {
        return User.manager.getMusicList(key: key)
    }

    func getMusic(id: String) -> Observable<Result<LrcMusic>> {
        return User.manager.getMusic(id: id)
    }
}
