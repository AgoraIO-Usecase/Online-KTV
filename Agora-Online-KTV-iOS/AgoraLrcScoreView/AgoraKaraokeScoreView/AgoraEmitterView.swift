//
//  AgoraEmitterView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/17.
//

import UIKit

class AgoraEmitterView: UIView {
    private lazy var emitter: CAEmitterLayer = {
        // 1.创建发射器
        let emitter = CAEmitterLayer()
        // 2.设置发射器的位置
        emitter.emitterPosition = center
        // 3.开启三维效果
        emitter.preservesDepth = true
        return emitter
    }()

    public var config: AgoraScoreItemConfigModel?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupUI() {
        // 5.将粒子设置到发射器中
        emitter.emitterCells = [createEmitterCell(name: "1"),
                                createEmitterCell(name: "2"),
                                createEmitterCell(name: "3"),
                                createEmitterCell(name: "4")]
        // 6.将发射器的layer添加到父layer中
//        layer.addSublayer(emitter)
    }
    
    private func createEmitterCell(name: String) -> CAEmitterCell {
        // 4.创建粒子, 并且设置例子相关的属性
        let cell = CAEmitterCell()
        // 4.2.设置粒子速度
        cell.velocity = 1
        cell.velocityRange = 1
        // 4.3.设置例子的大小
        cell.scale = 0.6
        cell.scaleRange = 0.3
        // 4.4.设置粒子方向
        cell.emissionLongitude = CGFloat.pi * 3
        cell.emissionRange = CGFloat.pi / 6
        // 4.5.设置例子的存活时间
        cell.lifetime = 0.7
        cell.lifetimeRange = 1
        // 4.6.设置粒子旋转
        cell.spin = CGFloat.pi / 2
        cell.spinRange = CGFloat.pi / 4
        // 4.6.设置例子每秒弹出的个数
        cell.birthRate = 1
        cell.alphaRange = 0.9
        cell.alphaSpeed = -0.35
        // 4.7.设置粒子展示的图片
//        cell.contents = UIImage()?.cgImage
        // 设置发射器的位置
        cell.birthRate = 5
        // 初始速度
        cell.velocity = 90
        
        cell.name = name
        return cell
    }
    private var isStop: Bool = false
    private var isStart: Bool = false

    func setupEmitterPoint(point: CGPoint) {
        emitter.emitterPosition = point
    }

    func startEmittering() {
        if isStart == true { return }
        layer.addSublayer(emitter)
        if config?.emitterImages == nil {
            emitter.emitterCells?.forEach({
                let image = UIImage(color: config?.emitterColors.randomElement() ?? .red,
                                    size: CGSize(width: 10, height: 10))?.toCircle()
                $0.contents = image?.cgImage
            })
        } else {
            emitter.emitterCells?.forEach({
                let images = config?.emitterImages?.map({ $0.cgImage })
                $0.contents = images?.randomElement
            })
        }
        emitter.lifetime = 1.0
        isStart = true
        isStop = false
    }

    /// 移除CAEmitterLayer
    func stopEmittering() {
        if isStop == true { return }
        emitter.lifetime = 0.0
        isStop = true
        isStart = false
    }
}

private extension UIImage {
    convenience init?(color: UIColor,
                      size: CGSize = CGSize(width: 1, height: 1))
    {
        let rect = CGRect(origin: .zero, size: size)
        UIGraphicsBeginImageContextWithOptions(rect.size, false, 0.0)
        color.setFill()
        UIRectFill(rect)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        guard let cgImage = image?.cgImage else { return nil }
        self.init(cgImage: cgImage)
    }

    // 生成圆形图片
    func toCircle() -> UIImage {
        // 取最短边长
        let shotest = min(size.width, size.height)
        // 输出尺寸
        let outputRect = CGRect(x: 0, y: 0, width: shotest, height: shotest)
        // 开始图片处理上下文（由于输出的图不会进行缩放，所以缩放因子等于屏幕的scale即可）
        UIGraphicsBeginImageContextWithOptions(outputRect.size, false, 0)
        let context = UIGraphicsGetCurrentContext()!
        // 添加圆形裁剪区域
        context.addEllipse(in: outputRect)
        context.clip()
        // 绘制图片
        draw(in: CGRect(x: (shotest - size.width) / 2,
                        y: (shotest - size.height) / 2,
                        width: size.width,
                        height: size.height))
        // 获得处理后的图片
        let maskedImage = UIGraphicsGetImageFromCurrentImageContext()!
        UIGraphicsEndImageContext()
        return maskedImage
    }
}
