//
//  CustomUITabBarController.swift
//  Scene-Examples
//
//  Created by XC on 2021/4/19.
//

import Foundation
import UIKit

public class CustomTabBarItem: UIButton {
    public enum Mode {
        case Switch
        case Push
    }

    var mode: Mode = .Switch
    var builder: (() -> UIViewController)?
    var tint: Bool = true

    var color = UIColor.lightGray {
        didSet {
            if tint {
                if let image = iconView.image {
                    iconView.image = image.withRenderingMode(.alwaysTemplate)
                }
                iconView.tintColor = color
            } else {
                iconView.tintColor = nil
            }
            label.textColor = color
        }
    }

    private let iconView: UIImageView = {
        let view = UIImageView()
        view.contentMode = .scaleAspectFit
        view.clipsToBounds = true
        return view
    }()

    private let label: UILabel = {
        let view = UILabel()
        view.font = UIFont.systemFont(ofSize: 10)
        view.textColor = .black
        view.textAlignment = .center
        view.numberOfLines = 1
        return view
    }()

    public convenience init(icon: UIImage, title: String, tint: Bool = true, builder: (() -> UIViewController)? = nil) {
        self.init()
        self.tint = tint
        self.builder = builder
        if self.builder != nil {
            mode = .Push
        }
        iconView.image = icon
        label.text = title
        addSubview(iconView)
        addSubview(label)

        iconView.marginTop(anchor: topAnchor, constant: 8)
            .centerX(anchor: centerXAnchor)
            .square()
            .marginBottom(anchor: bottomAnchor, constant: 16)
            .active()

        label.marginBottom(anchor: bottomAnchor, constant: 2)
            .centerX(anchor: centerXAnchor)
            .active()
    }
}

public class CustomTabBar: UITabBar {
    var customItems: [CustomTabBarItem] = []
    override public var tintColor: UIColor! {
        didSet {
            customItems.forEach { (item: CustomTabBarItem) in
                item.color = tintColor
            }
        }
    }

    func setItems(items: [CustomTabBarItem]) {
        customItems = items
        backgroundColor = .white
        if customItems.count == 0 {
            return
        }
//        let line = UIView()
//        line.backgroundColor = UIColor(hex: "#DBDBDB")
//        addSubview(line)
//
//        line.height(constant: 0.5 / UIScreen.main.scale)
//            .marginTop(anchor: topAnchor)
//            .marginLeading(anchor: leadingAnchor)
//            .marginTrailing(anchor: trailingAnchor)
//            .active()

        var horizontalConstraints = "H:|"
        let itemWidth: CGFloat = screenWidth / CGFloat(customItems.count)
        customItems.enumerated().forEach { index, item in
            addSubview(item)
            item.marginTop(anchor: topAnchor)
                .marginBottom(anchor: bottomAnchor)
                .width(constant: itemWidth)
                .active()
            horizontalConstraints += String(format: "[v%d]", index)
            item.color = tintColor
        }
        horizontalConstraints += "|"
        addConstraints(withFormat: horizontalConstraints, arrayOf: customItems)
    }
}

open class CustomTabBarController: UITabBarController {
    var customTabBar = CustomTabBar()
    var selectedColor = UIColor(hex: "#2488FE")
    var normalColor: UIColor = .lightGray {
        didSet {
            customTabBar.tintColor = normalColor
        }
    }

    override open func viewDidLoad() {
        super.viewDidLoad()
        // edgesForExtendedLayout = UIRectEdge(rawValue: 0)
        // tabBar.isHidden = true
        tabBar.backgroundColor = .clear
        setupView()
    }

    open func setupView() {}

    public func setTabBar(items: [CustomTabBarItem], height: CGFloat = 49) {
        guard items.count > 0 else {
            return
        }
        customTabBar.setItems(items: items)
        customTabBar.tintColor = normalColor
        customTabBar.customItems.first?.color = selectedColor

        view.addSubview(customTabBar)
        customTabBar.marginLeading(anchor: view.leadingAnchor)
            .marginTrailing(anchor: view.trailingAnchor)
            .height(constant: height)
            .marginBottom(anchor: view.safeAreaLayoutGuide.bottomAnchor)
            .active()

        items.enumerated().forEach { index, item in
            item.tag = index
            item.addTarget(self, action: #selector(switchTab), for: .touchUpInside)
        }
    }

    @objc func switchTab(button: UIButton) {
        let toIndex = button.tag
        let fromIndex = selectedIndex
        guard fromIndex != toIndex else {
            return
        }

        let fromTab = customTabBar.customItems[fromIndex]
        let toTab = customTabBar.customItems[toIndex]

        if toTab.mode == .Switch {
            fromTab.color = normalColor
            toTab.color = selectedColor
            selectedIndex = toIndex
        } else {
            guard let builder = toTab.builder else {
                return
            }
            navigationController?.pushViewController(builder(), animated: true)
        }
    }
}
