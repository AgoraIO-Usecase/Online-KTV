//
//  ServiceLocator.swift
//  Core
//
//  Created by XC on 2021/6/3.
//

import Foundation

public protocol Resolver {
    func resolve<ServiceType>(_ type: ServiceType.Type) -> ServiceType
}

protocol ServiceFactory {
    associatedtype ServiceType
    func resolve(_ resolver: Resolver) -> ServiceType
}

struct BasicServiceFactory<ServiceType>: ServiceFactory {
    private let factory: (Resolver) -> ServiceType

    init(_: ServiceType.Type, factory: @escaping (Resolver) -> ServiceType) {
        self.factory = factory
    }

    func resolve(_ resolver: Resolver) -> ServiceType {
        return factory(resolver)
    }
}

final class AnyServiceFactory {
    private let _resolve: (Resolver) -> Any
    private let _supports: (Any.Type) -> Bool

    init<T: ServiceFactory>(_ serviceFactory: T) {
        _resolve = { resolver in
            serviceFactory.resolve(resolver)
        }
        _supports = { type in
            type == T.ServiceType.self
        }
    }

    func resolve<ServiceType>(_ resolver: Resolver) -> ServiceType {
        return _resolve(resolver) as! ServiceType
    }

    func supports<ServiceType>(_ type: ServiceType.Type) -> Bool {
        return _supports(type)
    }
}

struct Container: Resolver {
    let factories: [AnyServiceFactory]

    init() {
        factories = []
    }

    private init(factories: [AnyServiceFactory]) {
        self.factories = factories
    }

    func register<T>(_ type: T.Type, instance: T) -> Container {
        return register(type) { _ in
            instance
        }
    }

    func register<ServiceType>(_ type: ServiceType.Type, _ factory: @escaping (Resolver) -> ServiceType) -> Container {
        assert(!factories.contains(where: { $0.supports(type) }))
        let newFactory = BasicServiceFactory<ServiceType>(type) { resolver in
            factory(resolver)
        }
        return .init(factories: factories + [AnyServiceFactory(newFactory)])
    }

    func resolve<ServiceType>(_ type: ServiceType.Type) -> ServiceType {
        guard let factory = factories.first(where: { $0.supports(type) }) else {
            fatalError("No suitable factory found")
        }
        return factory.resolve(self)
    }
}

public class InjectionService: Resolver {
    private var container = Container()
    public static let shared = InjectionService()

    private init() {}

    public func register<T>(_ type: T.Type, instance: T) -> InjectionService {
        container = container.register(type) { _ in instance }
        return self
    }

    public func register<ServiceType>(_ type: ServiceType.Type, _ factory: @escaping (Resolver) -> ServiceType) -> InjectionService {
        container = container.register(type, factory)
        return self
    }

    public func resolve<ServiceType>(_ type: ServiceType.Type) -> ServiceType {
        return container.resolve(type)
    }
}
