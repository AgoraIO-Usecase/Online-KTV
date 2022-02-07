//
//  AgoraTriangleView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2022/1/12.
//

import UIKit

class AgoraTriangleView: UIView {
    
    public var config: AgoraScoreItemConfigModel?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .clear
        layer.masksToBounds = false
        alpha = 0
    }
    
    func updateAlpha(at alpha: CGFloat) {
        self.alpha = alpha
    }
    
    override func draw(_ rect: CGRect) {
        if config?.tailAnimateImage != nil {
            layer.contents = config?.tailAnimateImage?.cgImage
            return
        }
        guard let context = UIGraphicsGetCurrentContext() else { return }
        
        context.beginPath()
        context.move(to: CGPoint(x: rect.maxX, y: rect.minY))
        context.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        context.addLine(to: CGPoint(x: (rect.minX), y: rect.maxY / 2.0))
        context.closePath()
        
        let color = config?.tailAnimateColor ?? .red

        let shadowColor = color.cgColor
        let shadowOffet = CGSize(width: -1.0, height: 0.0)
        let shadowBlurRadius: CGFloat = 2.5
        
        context.setShadow(offset: shadowOffet, blur: shadowBlurRadius, color: shadowColor)
        
        context.setFillColor(color.withAlphaComponent(0.8).cgColor)
        
        context.fillPath()
    }
}
