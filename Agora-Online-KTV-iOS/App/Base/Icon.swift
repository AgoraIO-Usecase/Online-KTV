//
//  Icon.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Foundation
import UIKit

public enum Icon {
    public static func get(named: String) -> UIImage? {
        return UIImage(named: named, in: Bundle(identifier: "io.agora.LiveKtv"), with: nil)
    }
}
