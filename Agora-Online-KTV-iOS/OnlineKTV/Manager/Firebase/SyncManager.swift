//
//  SyncManagerProxy.swift
//  Scene-Examples_LeanCloud
//
//  Created by XC on 2021/6/1.
//
#if FIREBASE
    import Core
    import Firebase
    import FirebaseFirestoreSwift
    import Foundation

    class AgoraObject: IAgoraObject {
        private let document: DocumentSnapshot

        init(document: DocumentSnapshot) {
            self.document = document
        }

        func getId() throws -> String {
            return document.documentID
        }

        func getValue(key: String, type: Any.Type) throws -> Any? {
            let data = document.data()!
            if type == String.self {
                return data[key] as? String
            } else if type == Int.self {
                return data[key] as? Int
            } else if type == Bool.self {
                return data[key] as? Bool
            } else if type == UInt.self {
                return data[key] as? UInt
            } else if type == AgoraDocumentReference.self {
                if let obj = data[key] as? DocumentReference {
                    return AgoraDocumentReference(parent: nil, id: obj.documentID)
                }
            }
            return nil
        }

        func toObject<T>() throws -> T where T: Decodable {
            return try document.data(as: T.self)!
        }
    }

    class AgoraLiveQuery: ISyncManagerLiveQuery {
        private var listenerRegistration: ListenerRegistration?
        private let className: String

        init(className: String, listenerRegistration: ListenerRegistration?) {
            self.className = className
            self.listenerRegistration = listenerRegistration
        }

        func unsubscribe() {
            listenerRegistration?.remove()
            listenerRegistration = nil
            Logger.log(message: "----- unsubscribe \(className) success -----", level: .info)
        }
    }

    class FirebaseSyncManager: ISyncManager {
        func createAgoraRoom(_ room: AgoraRoom, _ delegate: IAgoraObjectDelegate) {
            let className = AgoraRoom.TABLE
            var data = room.toDictionary()
            data["createdAt"] = Timestamp()
            // let data: [String: Any？] = ["createdAt": Timestamp()] + room.toDictionary()
            var ref: DocumentReference?
            ref = Database.db.collection(className).addDocument(data: data) { err in
                if let err = err {
                    delegate.onFailed(code: -1, msg: err.localizedDescription)
                } else {
                    ref!.getDocument { document, error in
                        if let error = error {
                            delegate.onFailed(code: -1, msg: error.localizedDescription)
                        } else if let document = document, document.exists {
                            delegate.onSuccess(result: AgoraObject(document: document))
                        } else {
                            delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref!.documentID)) does not exist!")
                        }
                    }
                }
            }
        }

        func getAgoraRooms(_ delegate: IAgoraObjectListDelegate) {
            let className = AgoraRoom.TABLE
            let query = Database.db.collection(className).order(by: "createdAt", descending: true)
            query.getDocuments { (querySnapshot: QuerySnapshot?, error: Error?) in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else if let querySnapshot = querySnapshot {
                    delegate.onSuccess(result: querySnapshot.documents.map { snapshot in
                        AgoraObject(document: snapshot)
                    })
                } else {
                    delegate.onFailed(code: -1, msg: "getAgoraRooms querySnapshot is nil!")
                }
            }
        }

        func get(_ reference: AgoraDocumentReference, _ delegate: IAgoraObjectDelegate) {
            let className = reference.className
            let objectId = reference.id
            let query = Database.db.collection(className).document(objectId)
            query.getDocument { document, error in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else if let document = document, document.exists {
                    delegate.onSuccess(result: AgoraObject(document: document))
                } else {
                    delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(objectId)) does not exist!")
                }
            }
        }

        func get(_ reference: AgoraCollectionReference, _ delegate: IAgoraObjectListDelegate) {
            let roomId = reference.parent.id
            let roomObject = Database.document(table: AgoraRoom.TABLE, id: roomId)
            let query = Database.db.collection(reference.className)
                .whereField("roomId", isEqualTo: roomObject)
                .order(by: "createdAt", descending: false)
            query.getDocuments { (querySnapshot: QuerySnapshot?, error: Error?) in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else if let querySnapshot = querySnapshot {
                    delegate.onSuccess(result: querySnapshot.documents.map { snapshot in
                        AgoraObject(document: snapshot)
                    })
                } else {
                    delegate.onFailed(code: -1, msg: "get AgoraCollectionReference querySnapshot is nil!")
                }
            }
        }

        func add(_ reference: AgoraCollectionReference, _ data: [String: Any?], _ delegate: IAgoraObjectDelegate) {
            let className = reference.className
            let roomId = reference.parent.id
            let roomObject = Database.document(table: AgoraRoom.TABLE, id: roomId)
            var ref: DocumentReference?
            var data = data
            data["createdAt"] = Timestamp()
            data["roomId"] = roomObject
            ref = Database.db.collection(className).addDocument(data: data) { error in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else {
                    ref!.getDocument { document, error in
                        if let error = error {
                            delegate.onFailed(code: -1, msg: error.localizedDescription)
                        } else if let document = document, document.exists {
                            delegate.onSuccess(result: AgoraObject(document: document))
                        } else {
                            delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref!.documentID)) does not exist!")
                        }
                    }
                }
            }
        }

        func update(_ reference: AgoraDocumentReference, _ data: [String: Any?], _ delegate: IAgoraObjectDelegate) {
            let className = reference.className
            let ref = Database.document(table: className, id: reference.id)
            ref.updateData(data as [AnyHashable: Any]) { error in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else {
                    ref.getDocument { document, error in
                        if let error = error {
                            delegate.onFailed(code: -1, msg: error.localizedDescription)
                        } else if let document = document, document.exists {
                            delegate.onSuccess(result: AgoraObject(document: document))
                        } else {
                            delegate.onFailed(code: -1, msg: "Document(class:\(className), id:\(ref.documentID)) does not exist!")
                        }
                    }
                }
            }
        }

        func delete(_ reference: AgoraDocumentReference, _ delegate: IAgoraDocumentReferenceDelegate) {
            let className = reference.className
            if reference.id != "" {
                let ref = Database.document(table: className, id: reference.id)
                ref.delete { error in
                    if let error = error {
                        delegate.onFailed(code: -1, msg: error.localizedDescription)
                    } else {
                        delegate.onSuccess()
                    }
                }
            } else if reference.whereEQ.count > 0 {
                let batch = Database.db.batch()
                let completion = { (querySnapshot: QuerySnapshot?, error: Error?) in
                    if let error = error {
                        delegate.onFailed(code: -1, msg: error.localizedDescription)
                    } else {
                        querySnapshot!.documents.forEach { document in
                            batch.deleteDocument(document.reference)
                        }
                        batch.commit { error in
                            if let error = error {
                                delegate.onFailed(code: -1, msg: error.localizedDescription)
                            } else {
                                delegate.onSuccess()
                            }
                        }
                    }
                }
                var query: Query = Database.db.collection(className)
                for eqs in reference.whereEQ {
                    let key = eqs.key
                    let value = eqs.value
                    if let ref = value as? AgoraDocumentReference {
                        query = query.whereField(key, isEqualTo: Database.document(table: ref.className, id: ref.id))
                    } else {
                        query = query.whereField(key, isEqualTo: value)
                    }
                }
                query.getDocuments(completion: completion)
            } else {
                delegate.onFailed(code: -1, msg: "can not delete reference!")
            }
        }

        func delete(_ reference: AgoraCollectionReference, _ delegate: IAgoraDocumentReferenceDelegate) {
            let batch = Database.db.batch()
            let className = reference.className
            let completion = { (querySnapshot: QuerySnapshot?, error: Error?) in
                if let error = error {
                    delegate.onFailed(code: -1, msg: error.localizedDescription)
                } else {
                    querySnapshot!.documents.forEach { document in
                        batch.deleteDocument(document.reference)
                    }
                    batch.commit { error in
                        if let error = error {
                            delegate.onFailed(code: -1, msg: error.localizedDescription)
                        } else {
                            delegate.onSuccess()
                        }
                    }
                }
            }
            var query: Query = Database.db.collection(className)
            query = query.whereField("roomId", isEqualTo: Database.document(table: reference.parent.className, id: reference.parent.id))
            query.getDocuments(completion: completion)
        }

        func subscribe(_ reference: AgoraDocumentReference, _ delegate: ISyncManagerEventDelegate) -> ISyncManagerLiveQuery {
            let listenerRegistration: ListenerRegistration
            let className = reference.className

            if reference is AgoraRoomReference {
                listenerRegistration = Database.db.collection(className).document(reference.id).addSnapshotListener { querySnapshot, error in
                    if let error = error {
                        Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                        delegate.onError(code: -1, msg: error.localizedDescription)
                    } else if let querySnapshot = querySnapshot {
                        Logger.log(message: "liveQueryEvent \(className) event", level: .info)
                        if querySnapshot.exists {
                            delegate.onUpdated(object: AgoraObject(document: querySnapshot))
                        } else {
                            delegate.onDeleted(objectId: querySnapshot.documentID)
                        }
                    } else {
                        Logger.log(message: "subscribe0 \(className) error: snapshot is nil", level: .error)
                        delegate.onError(code: -1, msg: "unknown error")
                    }
                }
            } else {
                var query: Query = Database.db.collection(className)
                for eqs in reference.whereEQ {
                    let key = eqs.key
                    let value = eqs.value
                    if let ref as? AgoraDocumentReference {
                        query = query.whereField(key, isEqualTo: Database.document(table: ref.className, id: ref.id))
                    } else {
                        query = query.whereField(key, isEqualTo: value)
                    }
                }
                if let parent = reference.parent {
                    query.whereField("roomId", isEqualTo: Database.document(table: AgoraRoom.TABLE, id: parent.parent.id))
                }
                listenerRegistration = query.addSnapshotListener { (querySnapshot: QuerySnapshot?, error: Error?) in
                    if let error = error {
                        Logger.log(message: "subscribe1 \(className) error:\(error)", level: .error)
                        delegate.onError(code: -1, msg: error.localizedDescription)
                    } else if let querySnapshot = querySnapshot {
                        Logger.log(message: "liveQueryEvent \(className) event", level: .info)
                        querySnapshot.documentChanges.forEach { change in
                            switch change.type {
                            case .added:
                                delegate.onCreated(object: AgoraObject(document: change.document))
                            case .modified:
                                delegate.onUpdated(object: AgoraObject(document: change.document))
                            case .removed:
                                delegate.onDeleted(objectId: change.document.documentID)
                            default: break
                            }
                        }
                    } else {
                        Logger.log(message: "subscribe0 \(className) error: snapshot is nil", level: .error)
                        delegate.onError(code: -1, msg: "unknown error")
                    }
                }
            }
            return AgoraLiveQuery(className: className, listenerRegistration: listenerRegistration)
        }
    }
#endif
