//
//  Header.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Foundation
import UIKit

class Header: UIView {
    override func layoutSubviews() {
        super.layoutSubviews()
        shadow(color: "#0D000000", radius: 4, offset: CGSize(width: 0, height: 2), opacity: 1)
    }
}
