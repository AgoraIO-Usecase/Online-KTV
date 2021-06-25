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

#if LEANCLOUD
    class LeanCloudAppTarget: IAppTarget {
        func initTarget() {
            Database.initConfig()
            _ = InjectionService.shared
                .register(ISyncManager.self, instance: LeanCloudSyncManager())
                .register(IUserManager.self, instance: LeanCloudUserManager())
        }

        func getAppMainViewController() -> UIViewController {
            return LiveKtvHomeController.instance()
        }
    }

#elseif FIREBASE
    class FirebaseAppTarget: IAppTarget {
        func initTarget() {
            Database.initConfig()
            _ = InjectionService.shared
                .register(ISyncManager.self, instance: FirebaseSyncManager())
                .register(IUserManager.self, instance: FirebaseUserManager())
        }

        func getAppMainViewController() -> UIViewController {
            return LiveKtvHomeController.instance()
        }
    }
#endif

enum AppTargets {
    #if LEANCLOUD
        private static let target: IAppTarget = LeanCloudAppTarget()
    #elseif FIREBASE
        private static let target: IAppTarget = FirebaseAppTarget()
    #endif

    static func initTarget() {
        target.initTarget()
    }

    static func getAppMainViewController() -> UIViewController {
        target.getAppMainViewController()
    }
}
