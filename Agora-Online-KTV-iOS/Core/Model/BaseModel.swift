//
//  Model.swift
//  Core
//
//  Created by XC on 2021/4/19.
//

import Foundation
import RxSwift

public struct Result<T> {
    public var success: Bool
    public var data: T?
    public var message: String?

    public init(success: Bool, data: T? = nil, message: String? = nil) {
        self.success = success
        self.data = data
        self.message = message
    }

    public func onSuccess<U>(next: () -> Observable<Result<U>>) -> Observable<Result<U>> {
        if success {
            return next()
        } else {
            return Observable.just(Result<U>(success: false, message: message))
        }
    }

    public func transform<U>() -> Result<U> {
        if success {
            return Result<U>(success: success)
        } else {
            return Result<U>(success: false, message: message)
        }
    }
}

public class User: Codable {
    public var id: String
    public var name: String
    public var avatar: String?

    public init(id: String, name: String, avatar: String?) {
        self.id = id
        self.name = name
        self.avatar = avatar
    }
}

public class LocalSetting: Codable {
    public var audienceLatency: Bool

    public init(audienceLatency: Bool = false) {
        self.audienceLatency = audienceLatency
    }
}
