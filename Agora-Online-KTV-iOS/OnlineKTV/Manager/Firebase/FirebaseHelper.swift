//
//  FirebaseHelper.swift
//  Core
//
//  Created by XC on 2021/6/2.
//
#if FIREBASE
    import Core
    import Firebase
    import FirebaseFirestoreSwift
    import Foundation
    import RxSwift

    public enum Database {
        public static let db = Firestore.firestore()

        public static func initConfig() {
            FirebaseApp.configure()
        }

        public static func document(table: String, id: String) -> DocumentReference {
            return db.collection(table).document(id)
        }

        public static func save(
            transform: @escaping () throws -> (String, data: [String: Any], String?)
        ) -> Observable<Result<String>> {
            return Single.create { single in
                do {
                    var (table, data, id) = try transform()
                    if let id = id {
                        db.collection(table).document(id).updateData(data) { err in
                            if let err = err {
                                single(.success(Result(success: false, message: err.localizedDescription)))
                            } else {
                                single(.success(Result(success: true, data: id)))
                            }
                        }
                    } else {
                        var ref: DocumentReference?
                        data["createdAt"] = Timestamp()
                        ref = db.collection(table).addDocument(data: data) { err in
                            if let err = err {
                                single(.success(Result(success: false, message: err.localizedDescription)))
                            } else {
                                single(.success(Result(success: true, data: ref!.documentID)))
                            }
                        }
                    }
                } catch {
                    single(.success(Result(success: false, message: error.localizedDescription)))
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func query<T>(
            className: String,
            objectId: String,
            transform: @escaping (DocumentSnapshot) throws -> T
        ) -> Observable<Result<T>> {
            return Single.create { single in
                let query = db.collection(className).document(objectId)
                query.getDocument { document, error in
                    if let error = error {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    } else if let document = document, document.exists {
                        do {
                            single(.success(Result(success: true, data: try transform(document))))
                        } catch {
                            single(.success(Result(success: false, message: error.localizedDescription)))
                        }
                    } else {
                        single(.success(Result(success: false, message: "Document(class:\(className), id:\(objectId)) does not exist!")))
                    }
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func query<T>(
            className: String,
            queryWhere: ((CollectionReference) -> Query)?,
            transform: @escaping ([DocumentSnapshot]) throws -> T
        ) -> Observable<Result<T>> {
            return Single.create { single in
                let completion = { (querySnapshot: QuerySnapshot?, error: Error?) in
                    if let error = error {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    } else {
                        do {
                            single(.success(Result(success: true, data: try transform(querySnapshot!.documents))))
                        } catch {
                            single(.success(Result(success: false, message: error.localizedDescription)))
                        }
                    }
                }
                if let queryWhere = queryWhere {
                    let query = queryWhere(db.collection(className))
                    query.getDocuments(completion: completion)
                } else {
                    db.collection(className).getDocuments(completion: completion)
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func delete(
            className: String,
            objectId: String
        ) -> Observable<Result<Void>> {
            return Single.create { single in
                db.collection(className).document(objectId).delete { error in
                    if let error = error {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    } else {
                        single(.success(Result(success: true)))
                    }
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func delete(
            className: String,
            queryWhere: ((CollectionReference) -> Query)?
        ) -> Observable<Result<Void>> {
            return Single.create { single in
                let batch = db.batch()
                let completion = { (querySnapshot: QuerySnapshot?, error: Error?) in
                    if let error = error {
                        single(.success(Result(success: false, message: error.localizedDescription)))
                    } else {
                        querySnapshot!.documents.forEach { document in
                            batch.deleteDocument(document.reference)
                        }
                        batch.commit { error in
                            if let error = error {
                                single(.success(Result(success: false, message: error.localizedDescription)))
                            } else {
                                single(.success(Result(success: true)))
                            }
                        }
                    }
                }
                if let queryWhere = queryWhere {
                    let query = queryWhere(db.collection(className))
                    query.getDocuments(completion: completion)
                } else {
                    db.collection(className).getDocuments(completion: completion)
                }
                return Disposables.create()
            }.asObservable()
        }

        public static func subscribe(
            className: String,
            queryWhere: ((CollectionReference) -> Query)?
        ) -> Observable<Result<QuerySnapshot>> {
            return Observable.create { observer -> Disposable in
                let completion = { (querySnapshot: QuerySnapshot?, error: Error?) in
                    if let error = error {
                        Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                        observer.onNext(Result(success: false, message: error.localizedDescription))
                        observer.onCompleted()
                    } else if let querySnapshot = querySnapshot {
                        observer.onNext(Result(success: true, data: querySnapshot))
                        Logger.log(message: "liveQueryEvent \(className) event", level: .info)
                    } else {
                        Logger.log(message: "subscribe0 \(className) error: snapshot is nil", level: .error)
                        observer.onNext(Result(success: false, message: "unknown error".localized))
                        observer.onCompleted()
                    }
                }
                let listenerRegistration: ListenerRegistration
                if let queryWhere = queryWhere {
                    let query = queryWhere(db.collection(className))
                    listenerRegistration = query.addSnapshotListener(completion)
                } else {
                    listenerRegistration = db.collection(className).addSnapshotListener(completion)
                }
                Logger.log(message: "----- subscribe \(className) success -----", level: .info)

                return Disposables.create {
                    listenerRegistration.remove()
                    Logger.log(message: "----- unsubscribe \(className) success -----", level: .info)
                }
            }
        }
    }
#endif
