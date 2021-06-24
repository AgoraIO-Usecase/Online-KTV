//
//  ListenerToolbar.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import UIKit

class ListenerToolbar {
    weak var delegate: RoomController!
    weak var root: UIView!
    var member: LiveKtvMember? {
        didSet {
            if let member = member {
                switch member.toLiveKtvRoomRole() {
                case .listener:
                    root.isHidden = false
                default:
                    root.isHidden = true
                }
            } else {
                root.isHidden = true
            }
        }
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }
}
