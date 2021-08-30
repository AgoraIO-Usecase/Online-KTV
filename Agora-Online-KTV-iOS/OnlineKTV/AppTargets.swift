//
//  Target.swift
//  Scene-Examples
//
//  Created by XC on 2021/5/20.
//

import app
import Core
import Foundation
import UIKit

protocol IAppTarget {
    func initTarget() -> Void
    func getAppMainViewController() -> UIViewController
}

class AppTarget: IAppTarget {
    func initTarget() {
        InjectionService.shared
            .register(IUserManager.self, instance: LocalUserManager())
    }

    func getAppMainViewController() -> UIViewController {
        return LiveKtvHomeController.instance()
    }
}

enum AppTargets {
    private static let target: IAppTarget = AppTarget()

    static func initTarget() {
        target.initTarget()
    }

    static func getAppMainViewController() -> UIViewController {
        target.getAppMainViewController()
    }
}
