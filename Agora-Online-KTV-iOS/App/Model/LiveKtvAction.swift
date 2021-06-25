//
//  LiveKtvAction.swift
//  LiveKtv
//
//  Created by XC on 2021/6/8.
//

import Core
import Foundation
import RxSwift

enum LiveKtvActionType: Int {
    case handsUp = 1
    case invite = 2
    case requestLeft = 3
    case requestRight = 4
    case error

    public static func from(value: Int) -> LiveKtvActionType {
        switch value {
        case 1:
            return .handsUp
        case 2:
            return .invite
        case 3:
            return .requestLeft
        case 4:
            return .requestRight
        default:
            return .error
        }
    }
}

class LiveKtvAction {
    public var id: String
    public var action: LiveKtvActionType

    public var memberId: String
    public var roomId: String

    public init(id: String, action: LiveKtvActionType, memberId: String, roomId: String) {
        self.id = id
        self.action = action
        self.memberId = memberId
        self.roomId = roomId
    }
}

extension LiveKtvAction {
    static let TABLE: String = "ACTION_KTV"
    static let ACTION: String = "action"
    static let MEMBER: String = "memberId"
    static let ROOM: String = "roomId"

    private static var manager: SyncManager {
        SyncManager.shared
    }

    static func delete(roomId: String) -> Observable<Result<Void>> {
        return Single.create { single in
            LiveKtvAction.manager
                .getRoom(id: roomId)
                .collection(className: LiveKtvAction.TABLE)
                .delete(delegate: AgoraDocumentReferenceDelegate(success: {
                    single(.success(Result(success: true)))
                }, failed: { _, message in
                    single(.success(Result(success: false, message: message)))
                }))

            return Disposables.create()
        }.asObservable()
    }
}
