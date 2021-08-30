//
//  LeanCloudHelper.swift
//  Core
//
//  Created by XC on 2021/6/1.
//
#if false
    import Core
    import Foundation
    import LeanCloud
    import RxSwift

    public enum Database {
        public static func initConfig() {
            do {
                try LCApplication.default.set(
                    id: BuildConfig.LeanCloudAppId,
                    key: BuildConfig.LeanCloudAppKey,
                    serverURL: BuildConfig.LeanCloudServerUrl
                )
            } catch {
                Logger.log(message: error.localizedDescription, level: .error)
            }
        }

        static let completionQueue = DispatchQueue(label: "Database")

        public static func save(
            transform: @escaping () throws -> LCObject
        ) -> Observable<Result<String>> {
            return Single.create { single in
                do {
                    let object = try transform()
                    let acl = LCACL()
                    // deafult allow all user can read write
                    acl.setAccess([.read, .write], allowed: true)
                    object.ACL = acl

                    object.save(completionQueue: Database.completionQueue, completion: { result in
                        switch result {
                        case .success:
                            single(.success(Result(success: true, data: object.objectId?.value)))
                        case let .failure(error: error):
                            single(.success(Result(success: false, message: error.description)))
                        }
                    })
                } catch {
                    single(.success(Result(success: false, message: error.localizedDescription)))
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func query<T>(
            className: String,
            objectId: String,
            queryWhere: ((LCQuery) throws -> Void)?,
            transform: @escaping (LCObject) throws -> T
        ) -> Observable<Result<T>> {
            return Single.create { single in
                let _query = LCQuery(className: className)
                do {
                    if let _where = queryWhere {
                        try _where(_query)
                    }
                    _query.get(objectId, completionQueue: Database.completionQueue, completion: { result in
                        do {
                            switch result {
                            case let .success(object: data):
                                single(.success(Result(success: true, data: try transform(data))))
                            case let .failure(error: error):
                                single(.success(Result(success: false, message: error.description)))
                            }
                        } catch {
                            single(.success(Result(success: false, message: error.localizedDescription)))
                        }
                    })
                } catch {
                    single(.success(Result(success: false, message: error.localizedDescription)))
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func query<T>(
            className: String,
            queryWhere: ((LCQuery) throws -> Void)?,
            transform: @escaping ([LCObject]) throws -> T
        ) -> Observable<Result<T>> {
            return Single.create { single in
                let _query = LCQuery(className: className)
                do {
                    if let _where = queryWhere {
                        try _where(_query)
                    }
                    _query.find(completionQueue: Database.completionQueue, completion: { result in
                        do {
                            switch result {
                            case let .success(objects: list):
                                single(.success(Result(success: true, data: try transform(list))))
                            case let .failure(error: error):
                                single(.success(Result(success: false, message: error.description)))
                            }
                        } catch {
                            Logger.log(message: "query0 \(className) error:\(error)", level: .error)
                            single(.success(Result(success: false, message: "unknown error".localized)))
                        }
                    })
                } catch {
                    Logger.log(message: "query1 \(className) error:\(error)", level: .error)
                    single(.success(Result(success: false, message: "unknown error".localized)))
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func delete(
            className: String,
            objectId: String
        ) -> Observable<Result<Void>> {
            return Single.create { single in
                let object = LCObject(className: className, objectId: objectId)
                object.delete { _result in
                    switch _result {
                    case .success:
                        single(.success(Result(success: true)))
                    case let .failure(error: error):
                        single(.success(Result(success: false, message: error.description)))
                    }
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func delete(
            className: String,
            queryWhere: ((LCQuery) throws -> Void)?
        ) -> Observable<Result<Void>> {
            return Single.create { single in
                let _query = LCQuery(className: className)
                do {
                    if let _where = queryWhere {
                        try _where(_query)
                    }
                    _query.find(completionQueue: Database.completionQueue, completion: { result in
                        switch result {
                        case let .success(objects: list):
                            LCObject.delete(list) { _result in
                                switch _result {
                                case .success:
                                    single(.success(Result(success: true)))
                                case let .failure(error: error):
                                    single(.success(Result(success: false, message: error.description)))
                                }
                            }
                        case let .failure(error: error):
                            if error.code == 101 {
                                single(.success(Result(success: true)))
                            } else {
                                single(.success(Result(success: false, message: error.description)))
                            }
                        }
                    })
                } catch {
                    Logger.log(message: "delete \(className) error:\(error)", level: .error)
                    single(.success(Result(success: false, message: "unknown error".localized)))
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func subscribe<T>(
            className: String,
            queryWhere: ((LCQuery) throws -> Void)?,
            onEvent: @escaping ((LiveQuery.Event) throws -> T?)
        ) -> Observable<Result<T>> {
            return Observable.create { observer -> Disposable in
                let query = LCQuery(className: className)
                var liveQuery: LiveQuery?
                do {
                    if let _where = queryWhere {
                        try _where(query)
                    }
                    liveQuery = try LiveQuery(query: query, eventHandler: { _, event in
                        Logger.log(message: "liveQueryEvent event:\(event)", level: .info)
                        do {
                            let result = try onEvent(event)
                            observer.onNext(Result<T>(success: result != nil, data: result))
                        } catch {
                            observer.onNext(Result<T>(success: false, message: "unknown error".localized))
                        }

                    })
                    liveQuery!.subscribe { result in
                        switch result {
                        case .success:
                            observer.onNext(Result(success: true))
                            Logger.log(message: "----- subscribe \(className) success -----", level: .info)
                            return
                        case let .failure(error: error):
                            Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                            observer.onNext(Result(success: false, message: error.reason))
                            observer.onCompleted()
                            return
                        }
                    }
                } catch {
                    Logger.log(message: "subscribe0 \(className) error:\(error)", level: .error)
                    observer.onNext(Result(success: false, message: "unknown error".localized))
                    observer.onCompleted()
                }
                return Disposables.create {
                    liveQuery?.unsubscribe { result in
                        switch result {
                        case .success:
                            Logger.log(message: "----- unsubscribe \(className) success -----", level: .info)
                        case let .failure(error: error):
                            Logger.log(message: "----- unsubscribe \(className) error:\(error) -----", level: .error)
                        }
                    }
                }
            }
        }
    }
#endif
