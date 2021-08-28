//
//  RoundButton.swift
//  InteractivePodcast
//
//  Created by XC on 2021/3/12.
//

import Foundation
import UIKit

open class RoundButton: UIButton {
    open var borderColor: String?
    open var borderWidth: CGFloat = 0

    override open func layoutSubviews() {
        super.layoutSubviews()
        rounded(color: borderColor, borderWidth: borderWidth)
    }

    override open func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)
        alpha = 0.65
    }

    override open func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        alpha = 1
    }

    override open func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesCancelled(touches, with: event)
        alpha = 1
    }
}
