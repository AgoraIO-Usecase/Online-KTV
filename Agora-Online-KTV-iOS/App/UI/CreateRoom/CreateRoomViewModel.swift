//
//  CreateRoomViewModel.swift
//  LiveKtv
//
//  Created by XC on 2021/6/8.
//

import Core
import Foundation
import RxSwift

class CreateRoomViewModel {
    private let disposeBag: DisposeBag

    init(viewController: BaseViewContoller) {
        disposeBag = viewController.disposeBag
    }

    func account() -> User? {
        return RoomManager.shared().account
    }

    func create(with name: String,
                cover: String,
                onWaiting: @escaping (Bool) -> Void,
                onSuccess: @escaping () -> Void,
                onError: @escaping (String) -> Void)
    {
        if let user = account() {
            let room = LiveKtvRoom(id: "", userId: user.id, channelName: name, cover: cover, mv: LiveKtvRoom.randomMV())
            RoomManager.shared()
                .create(room: room)
                .concatMap { result in
                    result.onSuccess {
                        RoomManager.shared().join(room: room)
                    }
                }
                .observe(on: MainScheduler.instance)
                .do(onSubscribe: {
                    onWaiting(true)
                }, onDispose: {
                    onWaiting(false)
                })
                .subscribe { result in
                    if result.success {
                        onSuccess()
                    } else {
                        onError(result.message ?? "unknown error".localized)
                    }
                } onDisposed: {
                    onWaiting(false)
                }
                .disposed(by: disposeBag)
        } else {
            onError("account is nil!")
        }
    }
}
