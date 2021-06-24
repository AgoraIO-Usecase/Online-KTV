//
//  AppData.swift
//  Core
//
//  Created by XC on 2021/5/21.
//

import Foundation
import RxSwift

public enum AppDataManager {
    public static func getAccount() -> User? {
        return UserDefaults.standard.value(User.self, forKey: "Account")
    }

    public static func saveAccount(user: User) -> Observable<Result<User>> {
        return Single.create { single in
            UserDefaults.standard.set(encodable: user, forKey: "Account")
            UserDefaults.standard.synchronize()
            single(.success(Result(success: true, data: user)))
            return Disposables.create()
        }
        .asObservable()
        .subscribe(on: MainScheduler.instance)
    }

    public static func getSetting() -> LocalSetting? {
        return UserDefaults.standard.value(LocalSetting.self, forKey: "Setting")
    }

    public static func saveSetting(setting: LocalSetting) -> Observable<Result<LocalSetting>> {
        return Single.create { single in
            UserDefaults.standard.set(encodable: setting, forKey: "Setting")
            UserDefaults.standard.synchronize()
            single(.success(Result(success: true, data: setting)))
            return Disposables.create()
        }
        .asObservable()
        .subscribe(on: MainScheduler.instance)
    }
}
