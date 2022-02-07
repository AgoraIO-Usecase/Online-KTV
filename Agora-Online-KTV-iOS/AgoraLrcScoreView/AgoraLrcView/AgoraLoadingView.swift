//
//  AgoraLoadingView.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2022/1/10.
//

import UIKit

protocol AgoraLoadViewDelegate {
    func getCurrentTime() -> TimeInterval
}

class AgoraLoadingView: UIView {
    
    var delegate: AgoraLoadViewDelegate?
    
    var lrcConfig: AgoraLrcConfigModel? {
        didSet {
            updateUI()
        }
    }
    
    private enum AgoraLoadingType: Int, CaseIterable {
        case first = 1
        case second = 2
        case third = 3
    }
    
    private lazy var statckView: UIStackView = {
        let stackView = UIStackView()
        stackView.alignment = .center
        stackView.axis = .horizontal
        stackView.distribution = .equalCentering
        stackView.spacing = 10
        return stackView
    }()
    
    private lazy var loadViews: [UIView] = AgoraLoadingType.allCases.map({
        let view = UIView()
        view.backgroundColor = .gray
        view.layer.cornerRadius = 5
        view.layer.masksToBounds = true
        view.tag = $0.rawValue
        return view
    })
    
    private lazy var timer = GCDTimer()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func beginAnimation() {
        timer.scheduledSecondsTimer(withName: "loadView", timeInterval: 120, queue: .main) { [weak self] _, time in
            guard let self = self else { return }
            let duration = self.delegate?.getCurrentTime() ?? 0
            if duration >= 3 {
                let view = self.statckView.arrangedSubviews.last
                self.hiddenView(view: view, isHidden: time.truncatingRemainder(dividingBy: 2) == 0)
                
            } else if duration >= 2 {
                let lastView = self.statckView.arrangedSubviews.last
                self.hiddenView(view: lastView, isHidden: true)
                let view = self.statckView.arrangedSubviews[1]
                self.hiddenView(view: view, isHidden: true)
                
            } else if duration >= 1 {
                let lastView = self.statckView.arrangedSubviews.last
                self.hiddenView(view: lastView, isHidden: true)
                let view = self.statckView.arrangedSubviews[1]
                self.hiddenView(view: view, isHidden: true)
                let firstView = self.statckView.arrangedSubviews.first
                self.hiddenView(view: firstView, isHidden: true)
                
            } else if duration < 1 && time < 115 {
                self.timer.destoryTimer(withName: "loadView")
                UIView.animate(withDuration: 0.25) {
                    self.isHidden = true
                }
            }
        }
    }
    
    func hiddenLoadView() {
        timer.destoryTimer(withName: "loadView")
        self.isHidden = true
    }
    
    private func hiddenView(view: UIView?, isHidden: Bool) {
        view?.alpha = isHidden ? 0.0 : 1.0
    }
    
    private var constrs: [NSLayoutConstraint] = [NSLayoutConstraint]()
    private func setupUI() {
        setContentHuggingPriority(.defaultHigh, for: .vertical)
        addSubview(statckView)
        loadViews.forEach({
            $0.translatesAutoresizingMaskIntoConstraints = false
            statckView.addArrangedSubview($0)
            let w = $0.widthAnchor.constraint(equalToConstant: 10)
            let h = $0.heightAnchor.constraint(equalToConstant: 10)
            w.isActive = true
            h.isActive = true
            constrs.append(w)
            constrs.append(h)
        })
        statckView.translatesAutoresizingMaskIntoConstraints = false
        statckView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        statckView.topAnchor.constraint(equalTo: topAnchor, constant: 5).isActive = true
        statckView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    private func updateUI() {
        loadViews.forEach({
            $0.backgroundColor = lrcConfig?.waitingViewBgColor
            $0.layer.cornerRadius = (lrcConfig?.waitingViewSize ?? 5) * 0.5
            constrs.forEach({
                $0.constant = (lrcConfig?.waitingViewSize ?? 10)
                $0.isActive = true
            })
        })
    }
}
