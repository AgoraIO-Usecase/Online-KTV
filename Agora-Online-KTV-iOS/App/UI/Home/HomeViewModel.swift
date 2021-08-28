//
//  HomeViewModel.swift
//  LiveKtv
//
//  Created by XC on 2021/6/4.
//

import Core
import Foundation
import IGListKit
import RxCocoa
import RxRelay
import RxSwift

class HomeViewModel {
    private let disposeBag = DisposeBag()
    private let manager = RoomManager.shared()
    private(set) var roomList: [LiveKtvRoom] = []
    private var scheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "io")

    func account() -> User? {
        return manager.account
    }

    func setup(
        onWaiting: @escaping (Bool) -> Void,
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        return manager.getAccount().map { $0.transform() }
            // UIRefreshControl bug?
            .delay(DispatchTimeInterval.milliseconds(200), scheduler: scheduler)
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe(onNext: { (result: Result<Void>) in
                if result.success {
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            })
            .disposed(by: disposeBag)
    }

    func refreshRoomList(
        onWaiting: @escaping (Bool) -> Void,
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        // _dataSource()
        manager.getRooms()
            .observe(on: MainScheduler.instance)
            .do(onSubscribe: {
                onWaiting(true)
            }, onDispose: {
                onWaiting(false)
            })
            .subscribe(onNext: { result in
                if result.success {
                    self.roomList.removeAll()
                    self.roomList.append(contentsOf: result.data ?? [])
                    onSuccess()
                } else {
                    onError(result.message ?? "unknown error".localized)
                }
            })
            .disposed(by: disposeBag)
    }

    func join(room: LiveKtvRoom,
              onWaiting: @escaping (Bool) -> Void,
              onSuccess: @escaping () -> Void,
              onError: @escaping (String) -> Void)
    {
        manager.join(room: room)
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
    }

    deinit {
        _ = manager.leave().subscribe()
        manager.destory()
    }

    func _dataSource() -> Observable<Result<[LiveKtvRoom]>> {
        return Observable.just(Result(success: true, data: [
            LiveKtvRoom(id: "u01", userId: "u01", channelName: "一锤定音", cover: LiveKtvRoom.randomCover(), mv: LiveKtvRoom.randomMV()),
            LiveKtvRoom(id: "u02", userId: "u02", channelName: "有酒吗", cover: LiveKtvRoom.randomCover(), mv: LiveKtvRoom.randomMV()),
            LiveKtvRoom(id: "u03", userId: "u03", channelName: "早安序曲", cover: LiveKtvRoom.randomCover(), mv: LiveKtvRoom.randomMV()),
            LiveKtvRoom(id: "u03", userId: "u03", channelName: "风情万种的歌房", cover: LiveKtvRoom.randomCover(), mv: LiveKtvRoom.randomMV()),
        ])).delay(DispatchTimeInterval.seconds(1), scheduler: scheduler)
    }
}
