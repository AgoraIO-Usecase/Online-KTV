//
//  UserManagerProxy.swift
//  Core
//
//  Created by XC on 2021/6/1.
//
import Core
import Foundation
import RxSwift

class LocalUserManager: IUserManager {
    func create(user _: User) -> Observable<Result<String>> {
        Observable.just(Result(success: false, message: "empty result!"))
    }

    func getUser(by userId: String) -> Observable<Result<User>> {
        Observable.just(Result(success: true, data: User(id: userId, name: "", avatar: userId)))
    }

    func update(user _: User, name _: String) -> Observable<Result<Void>> {
        Observable.just(Result(success: false, message: "empty result!"))
    }

    func getMusicList(key _: String?) -> Observable<Result<[LrcMusic]>> {
        Observable.just(Result(success: false, message: "empty result!"))
    }

    func getMusic(id _: String) -> Observable<Result<LrcMusic>> {
        Observable.just(Result(success: false, message: "empty result!"))
    }

    func randomUser() -> Observable<Result<User>> {
        let id = Utils.randomAvatar()
        let user = User(id: id, name: Utils.randomString(length: 8), avatar: id)
        return Observable.just(Result(success: true, data: user))
    }
}
