//
//  CoreData.swift
//  Core
//
//  Created by XC on 2021/4/19.
//

import CoreData
import Foundation
import RxSwift

class CoreDataManager {
    public static let shared = CoreDataManager()

    let identifier: String = "io.agora.Core"
    let model: String = "Model"

    lazy var persistentContainer: NSPersistentContainer = {
        let messageKitBundle = Bundle(identifier: self.identifier)
        let modelURL = messageKitBundle!.url(forResource: self.model, withExtension: "momd")!
        let managedObjectModel = NSManagedObjectModel(contentsOf: modelURL)

        let container = NSPersistentContainer(name: self.model, managedObjectModel: managedObjectModel!)
        container.loadPersistentStores { _, error in
            if let err = error {
                fatalError("Loading of store failed:\(err)")
            }
        }
        return container
    }()

    static func getSingleNSManagedObject(entityName: String, create: Bool = false) throws -> NSManagedObject? {
        let managedContext = shared.persistentContainer.viewContext
        let fetchRequest = NSFetchRequest<NSManagedObject>(entityName: entityName)
        let nSManagedObjects = try managedContext.fetch(fetchRequest)
        if nSManagedObjects.count > 0 {
            return nSManagedObjects[0]
        } else if create {
            let entity = NSEntityDescription.entity(forEntityName: entityName, in: managedContext)!
            return NSManagedObject(entity: entity, insertInto: managedContext)
        } else {
            return nil
        }
    }

    public func saveContext() {
        let context = persistentContainer.viewContext
        if context.hasChanges {
            do {
                try context.save()
            } catch {
                let nserror = error as NSError
                fatalError("Unresolved error \(nserror), \(nserror.userInfo)")
            }
        }
    }

    public static func getAccount() -> User? {
        do {
            let account = try getSingleNSManagedObject(entityName: "Account")
            if let account = account {
                return User(id: account.value(forKey: "id") as! String, name: account.value(forKey: "name") as! String, avatar: nil)
            } else {
                return nil
            }
        } catch {
            Logger.log(message: "CoreData getAccount error:\(error)", level: .error)
            return nil
        }
    }

    public static func saveAccount(user: User) -> Observable<Result<User>> {
        return Single.create { single in
            let managedContext = shared.persistentContainer.viewContext
            do {
                let account = try getSingleNSManagedObject(entityName: "Account", create: true)
                if let account = account {
                    account.setValue(user.id, forKey: "id")
                    account.setValue(user.name, forKey: "name")
                    try managedContext.save()
                    single(.success(Result(success: true, data: user)))
                } else {
                    single(.success(Result(success: false, message: "save accunt error!")))
                }
            } catch let error as NSError {
                Logger.log(message: "CoreData saveAccount error:\(error)", level: .error)
                single(.success(Result(success: false, message: "save accunt error!")))
            }
            return Disposables.create()
        }
        .asObservable()
        .subscribe(on: MainScheduler.instance)
    }

    public static func getSetting() -> LocalSetting? {
        do {
            let setting = try getSingleNSManagedObject(entityName: "Setting")
            if let setting = setting {
                return LocalSetting(audienceLatency: setting.value(forKey: "audienceLatency") as! Bool)
            } else {
                return LocalSetting()
            }
        } catch {
            Logger.log(message: "CoreData getSetting error:\(error)", level: .error)
            return nil
        }
    }

    public static func saveSetting(setting: LocalSetting) -> Observable<Result<LocalSetting>> {
        return Single.create { single in
            let managedContext = shared.persistentContainer.viewContext
            do {
                let _setting = try getSingleNSManagedObject(entityName: "Setting", create: true)
                if let _setting = _setting {
                    _setting.setValue(setting.audienceLatency, forKey: "audienceLatency")
                    try managedContext.save()
                    single(.success(Result(success: true, data: setting)))
                } else {
                    single(.success(Result(success: false, message: "save setting error!")))
                }
            } catch let error as NSError {
                Logger.log(message: "CoreData saveSetting error:\(error)", level: .error)
                single(.success(Result(success: false, message: "save setting error!")))
            }
            return Disposables.create()
        }
        .asObservable()
        .subscribe(on: MainScheduler.instance)
    }
}
