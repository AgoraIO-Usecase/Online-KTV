//
//  CreateRoomController.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import UIKit

class CreateRoomController: BaseViewContoller {
    @IBOutlet var backButton: UIButton!
    @IBOutlet var roomNameView: UILabel!
    @IBOutlet var randomNameView: UIButton!
    @IBOutlet var createRoomView: UIButton!
    @IBOutlet var roomCoverView: UIImageView!

    @IBOutlet var showTipsView: UIView!
    @IBOutlet var closeTipsView: UIButton!

    private var roomName: String = "" {
        didSet {
            roomNameView.text = roomName
        }
    }

    private var roomCover: String = "" {
        didSet {
            roomCoverView.image = UIImage(named: LiveKtvRoom.getLocalCover(cover: roomCover), in: Utils.bundle, with: nil)
        }
    }

    private var viewModel: CreateRoomViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()
        viewModel = CreateRoomViewModel(viewController: self)
        randomRoom()
        subcribeUIEvent()
    }

    private func subcribeUIEvent() {
        backButton.addTarget(self, action: #selector(onTapBackButton), for: .touchUpInside)
        randomNameView.addTarget(self, action: #selector(onTapRandomView), for: .touchUpInside)
        closeTipsView.addTarget(self, action: #selector(onTapCloseTipsView), for: .touchUpInside)
        createRoomView.addTarget(self, action: #selector(onTapCreateRoomView), for: .touchUpInside)
    }

    @objc func onTapBackButton() {
        navigationController?.popViewController(animated: true)
    }

    @objc func onTapRandomView() {
        randomRoom()
    }

    @objc func onTapCloseTipsView() {
        showTipsView.isHidden = true
    }

    @objc func onTapCreateRoomView() {
        viewModel.create(with: roomName, cover: roomCover) { [weak self] waiting in
            guard let self = self else { return }
            self.show(processing: waiting)
        } onSuccess: { [weak self] in
            guard let self = self else { return }
            self.show(message: "创建歌房成功", type: .done)
            self.navigationController?.replaceTopViewController(with: RoomController.instance(), animated: true)
        } onError: { [weak self] message in
            Logger.log(message: message, level: .error)
            guard let self = self else { return }
            self.show(message: message, type: .error)
        }
    }

    private func randomRoom() {
        roomName = Utils.randomRoomName()
        roomCover = LiveKtvRoom.randomCover()
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }

    public static func instance() -> CreateRoomController {
        let storyBoard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        let controller = storyBoard.instantiateViewController(withIdentifier: "CreateRoomController") as! CreateRoomController
        return controller
    }
}
