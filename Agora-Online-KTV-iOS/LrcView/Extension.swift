//
//  Extension.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import UIKit

extension String {
    var localized: String { NSLocalizedString(self, bundle: Bundle(identifier: "io.agora.Core")!, comment: "") }
    var doubleValue: Double {
        return (self as NSString).doubleValue
    }

    var floatValue: Float {
        return (self as NSString).floatValue
    }
}

extension UserDefaults {
    func set<T: Encodable>(encodable: T, forKey key: String) {
        if let data = try? JSONEncoder().encode(encodable) {
            set(data, forKey: key)
        }
    }

    func value<T: Decodable>(_ type: T.Type, forKey key: String) -> T? {
        if let data = object(forKey: key) as? Data,
           let value = try? JSONDecoder().decode(type, from: data)
        {
            return value
        }
        return nil
    }
}

extension UIColor {
    convenience init(hex: String, alpha: CGFloat? = nil) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            red: CGFloat(r) / 255,
            green: CGFloat(g) / 255,
            blue: CGFloat(b) / 255,
            alpha: alpha ?? CGFloat(a) / 255
        )
    }
}

public var screenWidth: CGFloat { return UIScreen.main.bounds.width }

extension UIView {
    enum Relation {
        case equal
        case greaterOrEqual
        case lessOrEqual
    }

    func highlight() {
        UIView.animate(withDuration: 0.1) {
            self.alpha = 0.5
        }
    }

    func unhighlight() {
        UIView.animate(withDuration: 0.1) {
            self.alpha = 1
        }
    }

    func roundCorners(_ corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: bounds, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        let mask = CAShapeLayer()
        mask.path = path.cgPath
        layer.mask = mask
        clipsToBounds = true
    }

    func rounded(color: String? = nil, borderWidth: CGFloat = 0, radius: CGFloat? = nil) {
        layer.cornerRadius = radius ?? min(bounds.width, bounds.height) / 2
        if let borderColor = color {
            layer.borderColor = UIColor(hex: borderColor).cgColor
        } else {
            layer.borderColor = UIColor.clear.cgColor
        }
        layer.borderWidth = borderWidth
    }

    func shadow(color: String = "#000", radius: CGFloat = 10, offset: CGSize = .zero, opacity: Float = 0.65) {
        layer.shadowRadius = radius
        layer.shadowColor = UIColor(hex: color).cgColor
        layer.shadowOffset = offset
        layer.shadowOpacity = opacity
    }

    func shadowAnimation(color: String = "#000") {
        shadow(color: color, radius: 0, offset: .zero, opacity: 0)
        UIView.animate(withDuration: 2, delay: 0, options: [.curveEaseOut, .autoreverse, .repeat]) { [weak self] in
            self?.shadow(color: color)
        }
    }

    func stopShadowAnimation() {
        layer.removeAllAnimations()
        shadow(color: "#000", radius: 0, offset: .zero, opacity: 0)
    }

    func animateTo(frame: CGRect, withDuration duration: TimeInterval, completion: ((Bool) -> Void)? = nil) {
        guard let _ = superview else {
            return
        }

        let xScale = frame.size.width / self.frame.size.width
        let yScale = frame.size.height / self.frame.size.height
        let x = frame.origin.x + (self.frame.width * xScale) * layer.anchorPoint.x
        let y = frame.origin.y + (self.frame.height * yScale) * layer.anchorPoint.y

        UIView.animate(withDuration: duration, delay: 0, options: .curveLinear, animations: {
            self.layer.position = CGPoint(x: x, y: y)
            self.transform = self.transform.scaledBy(x: xScale, y: yScale)
        }, completion: completion)
    }

    func width(constant: CGFloat, relation: Relation = .equal) -> UIView {
        switch relation {
        case .equal:
            widthAnchor.constraint(equalToConstant: constant).isActive = true
        case .greaterOrEqual:
            widthAnchor.constraint(greaterThanOrEqualToConstant: constant).isActive = true
        case .lessOrEqual:
            widthAnchor.constraint(lessThanOrEqualToConstant: constant).isActive = true
        }
        return self
    }

    func width(equalTo: NSLayoutDimension, multiplier: CGFloat = 1, constant: CGFloat = 0) -> UIView {
        widthAnchor.constraint(equalTo: equalTo, multiplier: multiplier, constant: constant).isActive = true
        return self
    }

    func height(constant: CGFloat, relation: Relation = .equal) -> UIView {
        switch relation {
        case .equal:
            heightAnchor.constraint(equalToConstant: constant).isActive = true
        case .greaterOrEqual:
            heightAnchor.constraint(greaterThanOrEqualToConstant: constant).isActive = true
        case .lessOrEqual:
            heightAnchor.constraint(lessThanOrEqualToConstant: constant).isActive = true
        }
        return self
    }

    func height(equalTo: NSLayoutDimension, multiplier: CGFloat = 1, constant: CGFloat = 0) -> UIView {
        heightAnchor.constraint(equalTo: equalTo, multiplier: multiplier, constant: constant).isActive = true
        return self
    }

    func marginTop(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = topAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }

    func marginBottom(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = bottomAnchor
        UIView.anchor(from, anchor, -constant, relation)
        return self
    }

    func marginLeading(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = leadingAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }

    func marginTrailing(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = trailingAnchor
        UIView.anchor(from, anchor, -constant, relation)
        return self
    }

    func centerX(anchor: NSLayoutXAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = centerXAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }

    func centerY(anchor: NSLayoutYAxisAnchor, constant: CGFloat = 0, relation: Relation = .equal) -> UIView {
        let from = centerYAnchor
        UIView.anchor(from, anchor, constant, relation)
        return self
    }

    func square() -> UIView {
        widthAnchor.constraint(equalTo: heightAnchor).isActive = true
        return self
    }

    func fill(view: UIView, leading: CGFloat = 0, top: CGFloat = 0, trailing: CGFloat = 0, bottom: CGFloat = 0) -> UIView {
        return marginLeading(anchor: view.leadingAnchor, constant: leading)
            .marginTop(anchor: view.topAnchor, constant: top)
            .marginTrailing(anchor: view.trailingAnchor, constant: trailing)
            .marginBottom(anchor: view.bottomAnchor, constant: bottom)
    }

    func active() {
        translatesAutoresizingMaskIntoConstraints = false
    }

    func addConstraints(withFormat format: String, views: UIView...) {
        var viewsDictionary = [String: UIView]()
        for i in 0 ..< views.count {
            let key = "v\(i)"
            views[i].translatesAutoresizingMaskIntoConstraints = false
            viewsDictionary[key] = views[i]
        }
        addConstraints(NSLayoutConstraint.constraints(withVisualFormat: format, options: NSLayoutConstraint.FormatOptions(), metrics: nil, views: viewsDictionary))
    }

    func addConstraints(withFormat format: String, arrayOf views: [UIView]) {
        var viewsDictionary = [String: UIView]()
        for i in 0 ..< views.count {
            let key = "v\(i)"
            views[i].translatesAutoresizingMaskIntoConstraints = false
            viewsDictionary[key] = views[i]
        }
        addConstraints(NSLayoutConstraint.constraints(withVisualFormat: format, options: NSLayoutConstraint.FormatOptions(), metrics: nil, views: viewsDictionary))
    }

    static func anchor(_ from: NSLayoutYAxisAnchor, _ to: NSLayoutYAxisAnchor, _ constant: CGFloat, _ relation: Relation) {
        switch relation {
        case .equal:
            from.constraint(equalTo: to, constant: constant).isActive = true
        case .greaterOrEqual:
            from.constraint(greaterThanOrEqualTo: to, constant: constant).isActive = true
        case .lessOrEqual:
            from.constraint(lessThanOrEqualTo: to, constant: constant).isActive = true
        }
    }

    static func anchor(_ from: NSLayoutXAxisAnchor, _ to: NSLayoutXAxisAnchor, _ constant: CGFloat, _ relation: Relation) {
        switch relation {
        case .equal:
            from.constraint(equalTo: to, constant: constant).isActive = true
        case .greaterOrEqual:
            from.constraint(greaterThanOrEqualTo: to, constant: constant).isActive = true
        case .lessOrEqual:
            from.constraint(lessThanOrEqualTo: to, constant: constant).isActive = true
        }
    }

    func removeAllConstraints() {
        var _superview = superview
        while let superview = _superview {
            for constraint in superview.constraints {
                if let first = constraint.firstItem as? UIView, first == self {
                    superview.removeConstraint(constraint)
                }
                if let second = constraint.secondItem as? UIView, second == self {
                    superview.removeConstraint(constraint)
                }
            }
            _superview = superview.superview
        }
        removeConstraints(constraints)
        translatesAutoresizingMaskIntoConstraints = true
    }
}

extension UIViewController {
    func addViewTop(_ view: UIView, window: Bool = true) -> UIView {
        if window {
            if let window: UIWindow = UIApplication.shared.windows.filter({ $0.isKeyWindow }).first {
                window.addSubview(view)
                return window
            }
        }
        self.view.addSubview(view)
        return self.view
    }
}

extension UITableView {
    func reloadData(_ completion: @escaping () -> Void) {
        UIView.animate(withDuration: 0, animations: {
            self.reloadData()
        }, completion: { _ in
            completion()
        })
    }

    func scroll(to: ScrollsTo, animated: Bool) {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(300)) {
            let numberOfSections = self.numberOfSections
            let numberOfRows = self.numberOfRows(inSection: numberOfSections - 1)
            switch to {
            case .top:
                if numberOfRows > 0 {
                    let indexPath = IndexPath(row: 0, section: 0)
                    self.scrollToRow(at: indexPath, at: .top, animated: animated)
                }
            case .bottom:
                if numberOfRows > 0 {
                    let indexPath = IndexPath(row: numberOfRows - 1, section: numberOfSections - 1)
                    self.scrollToRow(at: indexPath, at: .bottom, animated: animated)
                }
            }
        }
    }

    enum ScrollsTo {
        case top, bottom
    }
}

extension Notification {
    var keyboardHeight: CGFloat {
        return (userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect)?.height ?? 0
    }
}

extension UIWindow {
    func visibleViewController() -> UIViewController? {
        if let rootViewController: UIViewController = self.rootViewController {
            return UIWindow.getVisibleViewControllerFrom(rootViewController)
        }
        return nil
    }

    class func getVisibleViewControllerFrom(_ vc: UIViewController) -> UIViewController {
        if vc.isKind(of: UINavigationController.self) {
            // TODO:
            let navigationController = vc as! UINavigationController // swiftlint:disable:this force_cast
            return UIWindow.getVisibleViewControllerFrom(navigationController.visibleViewController!) // swiftlint:disable:this force_cast
        } else if vc.isKind(of: UITabBarController.self) {
            // TODO:
            let tabBarController = vc as! UITabBarController // swiftlint:disable:this force_cast
            return UIWindow.getVisibleViewControllerFrom(tabBarController.selectedViewController!) // swiftlint:disable:this force_cast
        } else {
            if let presentedViewController = vc.presentedViewController {
                return UIWindow.getVisibleViewControllerFrom(presentedViewController.presentedViewController!)
            } else {
                return vc
            }
        }
    }
}

extension UINavigationController {
    func replaceTopViewController(with viewController: UIViewController, animated: Bool) {
        var controllers = viewControllers
        controllers[controllers.count - 1] = viewController
        setViewControllers(controllers, animated: animated)
    }
}

extension UIView {
    class func loadFromNib<T>(name: String, bundle: Bundle? = nil) -> T? {
        return UINib(
            nibName: name,
            bundle: bundle
        ).instantiate(withOwner: nil, options: nil)[0] as? T
    }

    func onTap() -> UITapGestureRecognizer {
        let tap = UITapGestureRecognizer()
        addGestureRecognizer(tap)
        return tap
    }
}

extension UIView {
    func gradientLayer(colors: [UIColor], degree: CGFloat = 0) -> CAGradientLayer {
        let layer = CAGradientLayer()
        layer.frame = bounds
        layer.colors = colors.map { (color: UIColor) -> CGColor in
            color.cgColor
        }

        let a = tan((degree + 45) * CGFloat.pi / 180) - 1
        let b = 0.5 - a / 2

        layer.startPoint = CGPoint(x: 0, y: b)
        layer.endPoint = CGPoint(x: 1, y: a + b)
        return layer
    }
}

extension UIImage {
    static func gradient(colors: [UIColor], with frame: CGRect, degree: CGFloat = 0) -> UIImage? {
        let layer = CAGradientLayer()
        layer.frame = frame
        layer.colors = colors.map { (color: UIColor) -> CGColor in
            color.cgColor
        }

        let a = tan((degree + 45) * CGFloat.pi / 180) - 1
        let b = 0.5 - a / 2

        layer.startPoint = CGPoint(x: 0, y: b)
        layer.endPoint = CGPoint(x: 1, y: a + b)

        UIGraphicsBeginImageContext(CGSize(width: frame.width, height: frame.height))
        layer.render(in: UIGraphicsGetCurrentContext()!)
        guard let image = UIGraphicsGetImageFromCurrentImageContext() else { return nil }
        UIGraphicsEndImageContext()
        return image
    }
}

extension UIRefreshControl {
    func refreshManually() {
        if let scrollView = superview as? UIScrollView {
            scrollView.setContentOffset(CGPoint(x: 0, y: scrollView.contentOffset.y - frame.height), animated: false)
        }
        // beginRefreshing()
        sendActions(for: .valueChanged)
    }
}

extension UIColor {
    class var background: UIColor {
        if #available(iOS 13.0, *) {
            return .systemBackground
        } else {
            return .white
        }
    }

    class var secondaryBackground: UIColor {
        if #available(iOS 13.0, *) {
            return .secondarySystemBackground
        } else {
            return .lightGray
        }
    }

    class var defaultSeparator: UIColor {
        if #available(iOS 13.0, *) {
            return UIColor.separator
        } else {
            return UIColor(red: 200 / 255.0,
                           green: 199 / 255.0,
                           blue: 204 / 255.0,
                           alpha: 1)
        }
    }

    class var titleLabel: UIColor {
        if #available(iOS 13.0, *) {
            return .label
        } else {
            return .darkText
        }
    }

    class var detailLabel: UIColor {
        if #available(iOS 13.0, *) {
            return .secondaryLabel
        } else {
            return .lightGray
        }
    }
}
