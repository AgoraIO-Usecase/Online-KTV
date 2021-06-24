//
//  RoundImageView.swift
//  LiveKtv
//
//  Created by XC on 2021/6/7.
//

import Core
import Foundation
import UIKit

class RoundImageView: UIImageView {
    var color: String?
    var borderWidth: CGFloat = 0
    var radius: CGFloat?

    override func layoutSubviews() {
        super.layoutSubviews()
        clipsToBounds = true
        rounded(color: !isHidden ? color : nil, borderWidth: borderWidth, radius: radius)
    }
}
