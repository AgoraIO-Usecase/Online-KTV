//
//  HomeController.swift
//  LiveKtv
//
//  Created by XC on 2021/6/4.
//

import Core
import Foundation
import UIKit

public class LiveKtvHomeController: BaseViewContoller {
    @IBOutlet var backButton: UIButton! {
        didSet {
            backButton.isHidden = navigationController?.viewControllers.count == 1
        }
    }

    @IBOutlet var reloadButton: RoundButton!
    @IBOutlet var emptyView: UIView!
    @IBOutlet var createRoomButton: UIButton!
    @IBOutlet var listView: UICollectionView! {
        didSet {
            let layout = WaterfallLayout()
            layout.delegate = self
            layout.sectionInset = UIEdgeInsets(top: 15, left: 15, bottom: 0, right: 15)
            layout.minimumLineSpacing = 15
            layout.minimumInteritemSpacing = 10

            listView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: 70, right: 0)
            listView.collectionViewLayout = layout
            listView.register(HomeCardView.self, forCellWithReuseIdentifier: NSStringFromClass(HomeCardView.self))
            listView.dataSource = self
        }
    }

    private let refreshControl: UIRefreshControl = {
        let view = UIRefreshControl()
        view.tintColor = UIColor(hex: Colors.Text).withAlphaComponent(0.7)
        return view
    }()

    private let viewModel = HomeViewModel()

    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return [.portrait]
    }

    private var isEmpty: Bool = false {
        didSet {
            emptyView.isHidden = !isEmpty
            listView.isHidden = isEmpty
        }
    }

    private var isRefreshing: Bool = false {
        didSet {
            if isRefreshing {
                refreshControl.beginRefreshing()
            } else {
                refreshControl.endRefreshing()
            }
        }
    }

    private var initAppFinished: Bool = false

    override public func viewDidLoad() {
        super.viewDidLoad()
        listView.refreshControl = refreshControl

        checkNetworkPermission()
        if !reloadButton.isHidden {
            reloadButton.addTarget(self, action: #selector(onTapReloadButton), for: .touchUpInside)
        }
        subcribeUIEvent()
    }

    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if initAppFinished { refreshControl.refreshManually() }
    }

    private func subcribeUIEvent() {
        backButton.addTarget(self, action: #selector(onTapBackButton), for: .touchUpInside)
        createRoomButton.addTarget(self, action: #selector(onTapCreateRoomButton), for: .touchUpInside)
        refreshControl.addTarget(self, action: #selector(onRefresh), for: .valueChanged)
    }

    @objc func onTapBackButton() {
        navigationController?.popViewController(animated: true)
    }

    @objc func onTapReloadButton() {
        checkNetworkPermission()
        if !reloadButton.isHidden {
            show(message: "Needs Network permission".localized, type: .error)
        }
    }

    @objc func onTapCreateRoomButton() {
        guard let instance = CreateRoomController.instance() else { return }
        navigationController?.pushViewController(instance, animated: true)
    }

    @objc func onRefresh() {
        isRefreshing = true
        loadRoomList()
    }

    private func checkNetworkPermission() {
        reloadButton.isHidden = Utils.checkNetworkPermission()
        if reloadButton.isHidden {
            initAppData()
        }
    }

    private func initAppData() {
        viewModel.setup { [weak self] waiting in
            guard let self = self else { return }
            self.show(processing: waiting)
        } onSuccess: { [weak self] in
            guard let self = self else { return }
            self.refreshControl.tintColor = UIColor(hex: Colors.Text).withAlphaComponent(0.7)
            self.refreshControl.refreshManually()
        } onError: { [weak self] message in
            guard let self = self else { return }
            self.show(message: message, type: .error)
        }
    }

    private func loadRoomList() {
        viewModel.refreshRoomList { [weak self] waiting in
            guard let self = self else { return }
            if !waiting {
                self.isRefreshing = false
            }
        } onSuccess: { [weak self] in
            guard let self = self else { return }
            self.initAppFinished = true
            self.emptyView.isHidden = self.viewModel.roomList.count > 0
            self.listView.reloadData()
        } onError: { [weak self] message in
            guard let self = self else { return }
            self.show(message: message, type: .error)
        }
    }

    deinit {
        Logger.log(self, message: "deinit", level: .info)
    }

    public static func instance() -> LiveKtvHomeController? {
        let storyBoard = UIStoryboard(name: "Main", bundle: Utils.bundle)
        return storyBoard.instantiateViewController(withIdentifier: "HomeController") as? LiveKtvHomeController
    }
}

extension LiveKtvHomeController: UICollectionViewDataSource {
    public func numberOfSections(in _: UICollectionView) -> Int {
        return 1
    }

    public func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let card: HomeCardView = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(HomeCardView.self), for: indexPath) as? HomeCardView else {
            let cell = HomeCardView()
            return cell
        }
        card.delegate = self
        card.room = viewModel.roomList[indexPath.item]
        return card
    }

    public func collectionView(_: UICollectionView, numberOfItemsInSection _: Int) -> Int {
        return viewModel.roomList.count
    }
}

extension LiveKtvHomeController: WaterfallLayoutDelegate {
    public func collectionView(_: UICollectionView, layout: WaterfallLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = (layout.collectionViewContentSize.width - 15 * 2 - 15) / 2
        return HomeCardView.sizeForItem(room: viewModel.roomList[indexPath.item], width: width)
    }

    public func collectionViewLayout(for _: Int) -> WaterfallLayout.Layout {
        return .waterfall(column: 2, distributionMethod: .balanced)
    }
}

extension LiveKtvHomeController: HomeCardDelegate {
    func onTapCard(with room: LiveKtvRoom) {
        guard let controller = RoomController.instance() else { return }
        viewModel.join(room: room) { [weak self] waiting in
            guard let self = self else { return }
            self.show(processing: waiting)
        } onSuccess: { [weak self] in
            guard let self = self else { return }
            self.navigationController?.pushViewController(controller, animated: true)
        } onError: { [weak self] message in
            guard let self = self else { return }
            self.show(message: message, type: .error)
        }
    }
}
