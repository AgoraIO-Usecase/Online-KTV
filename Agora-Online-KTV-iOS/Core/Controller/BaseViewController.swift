//
//  BaseViewController.swift
//  Core
//
//  Created by XC on 2021/4/19.
//

import Foundation
import RxCocoa
import RxSwift
import UIKit

open class BaseViewContoller: UIViewController {
    public let disposeBag = DisposeBag()
    private var dialogBackgroundMaskView: UIView?
    private var onDismiss: (() -> Void)?
    public var enableSwipeGesture: Bool = true
    private let cancelSignal: PublishRelay<UITapGestureRecognizer> = PublishRelay()

    override open var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }

    override open func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
    }

    override open func viewDidLoad() {
        super.viewDidLoad()
        if enableSwipeGesture {
            navigationController?.interactivePopGestureRecognizer?.delegate = nil
        }
    }

    override open func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if dialogBackgroundMaskView != nil {
            cancelSignal.accept(UITapGestureRecognizer())
        }
    }

    private func _showMaskView(dialog: UIView, alpha: CGFloat = 0.3) {
        if dialogBackgroundMaskView == nil {
            dialogBackgroundMaskView = UIView()
            dialogBackgroundMaskView!.backgroundColor = UIColor.black
            dialogBackgroundMaskView!.alpha = 0

            let root = addViewTop(dialogBackgroundMaskView!)
            dialogBackgroundMaskView!.fill(view: root).active()
        }
        if let mask = dialogBackgroundMaskView {
            Observable.merge([
                mask.onTap().rx.event.asObservable(),
                cancelSignal.asObservable(),
            ])
                .concatMap { [unowned self] _ in
                    self.dismiss(dialog: dialog)
                }
                .subscribe()
                .disposed(by: disposeBag)
        }
        if let maskView: UIView = dialogBackgroundMaskView {
            maskView.alpha = 0
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                maskView.alpha = alpha
            })
        }
    }

    private func _hiddenMaskView() {
        if let maskView = dialogBackgroundMaskView {
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                maskView.alpha = 0
            }, completion: { _ in
                maskView.removeFromSuperview()
                self.dialogBackgroundMaskView = nil
            })
        }
    }

    open func show(dialog: UIView,
                   style: DialogStyle = .center,
                   padding: CGFloat = 0,
                   relation: UIView.Relation = .equal,
                   completion: ((Bool) -> Void)? = nil,
                   onDismiss: (() -> Void)? = nil)
    {
        self.onDismiss = onDismiss
        dialog.tag = style.rawValue

        switch style {
        case .bottom:
            _showMaskView(dialog: dialog)
            let root = addViewTop(dialog)
            // self.view.addSubview(dialog)
            dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                .centerX(anchor: root.centerXAnchor)
                .marginBottom(anchor: root.bottomAnchor)
                .active()

            dialog.alpha = 0
            let translationY = view.frame.height
            dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.alpha = 1
                dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            }, completion: { finish in
                if let completion = completion {
                    completion(finish)
                }
            })
        case .center:
            _showMaskView(dialog: dialog, alpha: 0.65)
            let root = addViewTop(dialog)
            // self.view.addSubview(dialog)
            dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                .centerX(anchor: root.centerXAnchor)
                .centerY(anchor: root.centerYAnchor, constant: -50)
                .active()

            dialog.alpha = 0
            dialog.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.alpha = 1
                dialog.transform = CGAffineTransform(scaleX: 1, y: 1)
            }, completion: { finish in
                if let completion = completion {
                    completion(finish)
                }
            })
        case .top:
            _showMaskView(dialog: dialog)
            let root = addViewTop(dialog)
            // self.view.addSubview(dialog)
            dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                .centerX(anchor: root.centerXAnchor)
                .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: padding, relation: relation)
                .active()

            dialog.alpha = 0
            let translationY = view.frame.height
            dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.alpha = 1
                dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            }, completion: { finish in
                if let completion = completion {
                    completion(finish)
                }
            })
        case .topNoMask:
            let root = addViewTop(dialog, window: false)
            // self.view.addSubview(dialog)
            dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                .centerX(anchor: root.centerXAnchor)
                .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: padding, relation: relation)
                .active()

            dialog.alpha = 0
            let translationY = view.frame.height
            dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.alpha = 1
                dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            }, completion: { finish in
                if let completion = completion {
                    completion(finish)
                }
            })
        case .bottomNoMask:
            let root = addViewTop(dialog, window: false)
            // self.view.addSubview(dialog)
            if padding > 0 {
                dialog.marginLeading(anchor: root.leadingAnchor, constant: padding, relation: relation)
                    .centerX(anchor: root.centerXAnchor)
                    .marginBottom(anchor: root.safeAreaLayoutGuide.bottomAnchor, constant: padding, relation: relation)
                    .active()
            } else {
                dialog.marginLeading(anchor: root.leadingAnchor)
                    .centerX(anchor: root.centerXAnchor)
                    .marginBottom(anchor: root.bottomAnchor)
                    .active()
            }

            dialog.alpha = 0
            let translationY = view.bounds.height
            dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.alpha = 1
                dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            }, completion: { finish in
                if let completion = completion {
                    completion(finish)
                }
            })
        }
    }

    open func dismiss(dialog: UIView, completion: ((Bool) -> Void)? = nil) {
        _hiddenMaskView()
        let style = DialogStyle.valueOf(style: dialog.tag)
        switch style {
        case .bottom:
            // dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            // dialog.alpha = 1
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                let translationY = dialog.bounds.height
                dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                dialog.alpha = 0
            }, completion: { finish in
                dialog.removeFromSuperview()
                if let onDismiss = self.onDismiss {
                    onDismiss()
                }
                self.onDismiss = nil
                if let completion = completion {
                    completion(finish)
                }
            })
        case .center:
            // dialog.transform = CGAffineTransform(scaleX: 1, y: 1)
            // dialog.alpha = 1
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                dialog.transform = CGAffineTransform(scaleX: 1.2, y: 1.2)
                dialog.alpha = 0
            }, completion: { finish in
                dialog.removeFromSuperview()
                if let onDismiss = self.onDismiss {
                    onDismiss()
                }
                self.onDismiss = nil
                if let completion = completion {
                    completion(finish)
                }
            })
        case .top:
            // dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            // dialog.alpha = 1
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                let translationY = dialog.bounds.height
                dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                dialog.alpha = 0
            }, completion: { finish in
                dialog.removeFromSuperview()
                if let onDismiss = self.onDismiss {
                    onDismiss()
                }
                self.onDismiss = nil
                if let completion = completion {
                    completion(finish)
                }
            })
        case .bottomNoMask:
            // dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            // dialog.alpha = 1
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                let translationY = dialog.bounds.height
                dialog.transform = CGAffineTransform(translationX: 0, y: translationY)
                dialog.alpha = 0
            }, completion: { finish in
                dialog.removeFromSuperview()
                if let onDismiss = self.onDismiss {
                    onDismiss()
                }
                self.onDismiss = nil
                if let completion = completion {
                    completion(finish)
                }
            })
        case .topNoMask:
            // dialog.transform = CGAffineTransform(translationX: 0, y: 0)
            // dialog.alpha = 1
            UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                let translationY = dialog.bounds.height
                dialog.transform = CGAffineTransform(translationX: 0, y: -translationY)
                dialog.alpha = 0
            }, completion: { finish in
                dialog.removeFromSuperview()
                if let onDismiss = self.onDismiss {
                    onDismiss()
                }
                self.onDismiss = nil
                if let completion = completion {
                    completion(finish)
                }
            })
        }
    }

    open func dismiss(completion: ((Bool) -> Void)? = nil) {
        if let navigationController = self.navigationController {
            navigationController.popViewController(animated: true)
            if let completion = completion {
                completion(true)
            }
        } else {
            dismiss(animated: true, completion: {
                if let completion = completion {
                    completion(true)
                }
            })
        }
    }

    open func showAlert(title: String, message: String, onCancel: @escaping () -> Void, onOk: @escaping () -> Void) {
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let cancel = UIAlertAction(title: "Cancel".localized, style: .cancel) { _ in
            onCancel()
        }
        alertController.addAction(cancel)
        let ok = UIAlertAction(title: "Ok".localized, style: .default) { _ in
            onOk()
        }
        alertController.addAction(ok)
        present(alertController, animated: true, completion: nil)
    }

    open func pop(completion: ((Bool) -> Void)? = nil) {
        if let navigationController = self.navigationController {
            Logger.log(message: "pop with navigationController", level: .info)
            UIView.transition(with: self.navigationController!.view!, duration: 0.3, options: .curveEaseOut) {
                let transition = CATransition()
                transition.duration = 0
                transition.type = CATransitionType.push
                transition.subtype = CATransitionSubtype.fromBottom
                transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
                self.navigationController?.view.layer.add(transition, forKey: kCATransition)
            } completion: { _ in
                Logger.log(message: "pop with navigationController finish", level: .info)
                navigationController.popViewController(animated: false)
                if let completion = completion {
                    completion(true)
                }
            }
        } else {
            Logger.log(message: "pop with dismiss", level: .info)
            dismiss(animated: true, completion: {
                if let completion = completion {
                    completion(true)
                }
            })
        }
    }

    open func push(controller: UIViewController) {
        UIView.transition(with: navigationController!.view!, duration: 0.3, options: .curveEaseOut) {
            let transition = CATransition()
            transition.duration = 0
            transition.type = CATransitionType.push
            transition.subtype = CATransitionSubtype.fromTop
            transition.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            self.navigationController?.view.layer.add(transition, forKey: kCATransition)
            self.navigationController?.pushViewController(controller, animated: false)
        }
    }

    open func show(message: String?, type: NotificationType, duration: CGFloat = 1.5) {
        guard let message = message else {
            return
        }
        DispatchQueue.main.async { [weak self] in
            if let self = self {
                let view = NotificationView.create(message: message, type: type)
                view.alpha = 0
                let root = self.addViewTop(view)
                view.marginLeading(anchor: root.leadingAnchor, constant: 16)
                    .centerX(anchor: root.centerXAnchor)
                    .marginTop(anchor: root.safeAreaLayoutGuide.topAnchor, constant: 16)
                    .active()
                let translationY: CGFloat = 40
                view.transform = CGAffineTransform(translationX: 0, y: -translationY)
                UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                    view.alpha = 1
                    view.transform = CGAffineTransform(translationX: 0, y: 0)
                }, completion: { _ in
                    let translationY = view.bounds.height
                    UIView.animate(withDuration: 0.3, delay: TimeInterval(duration), options: .curveEaseInOut, animations: {
                        view.alpha = 0
                        view.transform = CGAffineTransform(translationX: 0, y: -translationY)
                    }, completion: { _ in
                        view.removeFromSuperview()
                    })
                })
            }
        }
    }

    open func show(processing: Bool) {
        DispatchQueue.main.async { [weak self] in
            if let self = self {
                var oldView = self.view.viewWithTag(233)
                if processing {
                    if oldView == nil {
                        oldView = UIView(frame: self.view.frame)
                    }
                    guard let backgroundView = oldView else {
                        return
                    }
                    backgroundView.tag = 233
                    let view = ProcessingView.create()
                    view.center = backgroundView.center

                    backgroundView.addSubview(view)
                    backgroundView.alpha = 0
                    self.view.addSubview(backgroundView)

                    UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                        backgroundView.alpha = 1
                    })
                } else if let backgroundView = oldView {
                    UIView.animate(withDuration: 0.3, delay: 0, options: .curveEaseInOut, animations: {
                        backgroundView.alpha = 0
                    }, completion: { _ in
                        backgroundView.removeFromSuperview()
                    })
                }
            }
        }
    }

    open func show(dialog: UIView,
                   style: DialogStyle = .center,
                   padding: CGFloat = 0,
                   relation: UIView.Relation = .equal,
                   onDismiss: (() -> Void)? = nil) -> Single<Bool>
    {
        return Single.create { [unowned self] single in
            let completion = { finish in
                single(.success(finish))
            }
            self.show(dialog: dialog, style: style, padding: padding, relation: relation, completion: completion, onDismiss: onDismiss)
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
    }

    open func dismiss(dialog: UIView) -> Single<Bool> {
        return Single.create { [unowned self] single in
            let completion = { finish in
                single(.success(finish))
            }
            self.dismiss(dialog: dialog, completion: completion)
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
    }

    open func dismiss() -> Single<Bool> {
        return Single.create { [unowned self] single in
            let completion = { finish in
                single(.success(finish))
            }
            self.dismiss(completion: completion)
            return Disposables.create()
        }
    }

    open func showAlert(title: String, message: String) -> Observable<Bool> {
        return Single.create { [unowned self] single in
            let onCancel = {
                single(.success(false))
            }
            let onOk = {
                single(.success(true))
            }
            self.showAlert(title: title, message: message, onCancel: onCancel, onOk: onOk)
            return Disposables.create()
        }
        .subscribe(on: MainScheduler.instance)
        .asObservable()
    }

    open func pop() -> Single<Bool> {
        return Single.create { [unowned self] single in
            let completion = { finish in
                single(.success(finish))
            }
            self.pop(completion: completion)
            return Disposables.create()
        }
    }

    open func keyboardHeight() -> Observable<CGFloat> {
        return Observable.from([
            NotificationCenter.default.rx.notification(UIApplication.keyboardDidShowNotification)
                .map { notification in notification.keyboardHeight },
            NotificationCenter.default.rx.notification(UIApplication.keyboardWillHideNotification)
                .map { _ in 0 },
        ])
            .merge()
    }
}
