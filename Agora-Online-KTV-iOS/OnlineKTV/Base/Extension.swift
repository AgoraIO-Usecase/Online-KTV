//
//  Extension.swift
//  InteractivePodcast
//
//  Created by XUCH on 2021/3/3.
//

import UIKit

extension String {
    var localized: String { NSLocalizedString(self, comment: "") }
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

var screenWidth: CGFloat { return UIScreen.main.bounds.width }

extension UIView {
    enum Relation {
        case equal
        case greaterOrEqual
        case lessOrEqual
    }

    func roundCorners(_ corners: UIRectCorner, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: bounds, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        let mask = CAShapeLayer()
        mask.path = path.cgPath
        layer.mask = mask
    }

    func rounded(color: String? = nil, borderWidth: CGFloat = 0, radius: CGFloat? = nil) {
        layer.cornerRadius = radius ?? min(bounds.width, bounds.height) / 2
        if let borderColor = color {
            layer.borderColor = UIColor(hex: borderColor).cgColor
        }
        layer.borderWidth = borderWidth
    }

    func shadow(color: String = "#000", radius: CGFloat = 10, offset: CGSize = .zero, opacity: Float = 0.65) {
        layer.shadowRadius = radius
        layer.shadowColor = UIColor(hex: color).cgColor
        layer.shadowOffset = offset
        layer.shadowOpacity = opacity
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

    public func removeAllConstraints() {
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
    func showToast(message: String?, type: Notification = .info, duration: CGFloat = 1.5) {
        DispatchQueue.main.async { [unowned self] in
            guard let _message = message else {
                return
            }
            self.show(message: _message, type: type, duration: duration)
        }
    }
}

extension UIView {
    class func loadFromNib<T>(name: String, bundle: Bundle? = nil) -> T? {
        return UINib(
            nibName: name,
            bundle: bundle
        ).instantiate(withOwner: nil, options: nil)[0] as? T
    }
}

extension UILabel {
    private enum AssociatedKeys {
        static var padding = UIEdgeInsets()
    }

    var padding: UIEdgeInsets? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.padding) as? UIEdgeInsets
        }
        set {
            if let newValue = newValue {
                objc_setAssociatedObject(self, &AssociatedKeys.padding, newValue as UIEdgeInsets?, objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            }
        }
    }

    override open func draw(_ rect: CGRect) {
        if let insets = padding {
            drawText(in: rect.inset(by: insets))
        } else {
            drawText(in: rect)
        }
    }

    override open var intrinsicContentSize: CGSize {
        var contentSize = super.intrinsicContentSize
        if let insets = padding {
            contentSize.height += insets.top + insets.bottom
            contentSize.width += insets.left + insets.right
        }
        return contentSize
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
