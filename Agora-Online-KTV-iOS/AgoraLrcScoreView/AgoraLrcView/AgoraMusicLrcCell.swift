//
//  AgoraMusicLrcCell.swift
//  AgoraKaraokeScore
//
//  Created by zhaoyongqiang on 2021/12/10.
//

import UIKit

class AgoraMusicLrcCell: UITableViewCell {
    private lazy var lrcLabel: AgoraLrcLabel = {
        let label = AgoraLrcLabel()
        label.text = "歌词"
        label.textColor = .blue
        label.font = .systemFont(ofSize: 15)
        label.textAlignment = .center
        label.backgroundColor = .clear
        label.numberOfLines = 0
        return label
    }()

    var lrcConfig: AgoraLrcConfigModel? {
        didSet {
            lrcLabel.textColor = lrcConfig?.lrcNormalColor
            lrcLabel.lrcDrawingColor = lrcConfig?.lrcDrawingColor
            lrcLabel.font = lrcConfig?.lrcFontSize
        }
    }

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        setupUI()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func setupMusicLrcProgress(with progress: CGFloat) {
        lrcLabel.progress = progress
    }

    func setupCurrentLrcScale() {
        lrcLabel.textColor = lrcConfig?.lrcHighlightColor
        UIView.animate(withDuration: 0.25) {
            let scale = self.lrcConfig?.lrcHighlightScaleSize ?? 0
            self.lrcLabel.transform = CGAffineTransform(scaleX: scale, y: scale)
        }
    }

    func setupMusicXmlLrc(with lrcModel: AgoraMiguLrcSentence?,
                          progress: CGFloat)
    {
        lrcLabel.text = lrcModel?.toSentence()
        lrcLabel.progress = progress
        lrcLabel.textColor = lrcConfig?.lrcNormalColor
        lrcLabel.font = lrcConfig?.lrcFontSize
        UIView.animate(withDuration: 0.25) {
            self.lrcLabel.transform = .identity
        }
    }

    func setupMusicLrc(with lrcModel: AgoraLrcModel?,
                       progress: CGFloat)
    {
        lrcLabel.text = lrcModel?.lrc?.trimmingCharacters(in: .whitespacesAndNewlines)
        lrcLabel.progress = progress
        lrcLabel.textColor = lrcConfig?.lrcNormalColor
        lrcLabel.font = lrcConfig?.lrcFontSize
        UIView.animate(withDuration: 0.25) {
            self.lrcLabel.transform = .identity
        }
    }

    private func setupUI() {
        contentView.backgroundColor = .clear
        backgroundColor = .clear
        lrcLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(lrcLabel)
        lrcLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        lrcLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10).isActive = true
        lrcLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10).isActive = true
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let margin = lrcConfig?.lrcLeftAndRightMargin ?? 0
        lrcLabel.preferredMaxLayoutWidth = contentView.frame.width - (margin * 2)
    }
}

class AgoraLrcLabel: UILabel {
    var lrcDrawingColor: UIColor?
    var progress: CGFloat = 0 {
        didSet {
            setNeedsDisplay()
        }
    }

    override func draw(_ rect: CGRect) {
        super.draw(rect)
        if progress <= 0 {
            return
        }
        let lines = Int(bounds.height / font.lineHeight)
        let padingTop = (bounds.height - CGFloat(lines) * font.lineHeight) / CGFloat(lines)
        let maxWidth = sizeThatFits(CGSize(width: CGFloat(MAXFLOAT),
                                           height: font.lineHeight * CGFloat(lines))).width
        let oneLineProgress = maxWidth <= bounds.width ? 1 : bounds.width / maxWidth
        let path = CGMutablePath()
        for index in 0 ..< lines {
            let leftProgress = min(progress, 1) - CGFloat(index) * oneLineProgress
            let fillRect: CGRect
            if leftProgress >= oneLineProgress {
                fillRect = CGRect(x: 0,
                                  y: padingTop + CGFloat(index) * font.lineHeight,
                                  width: bounds.width,
                                  height: font.lineHeight)
                path.addRect(fillRect)
            } else if leftProgress > 0 {
                if (index != lines - 1) || (maxWidth <= bounds.width) {
                    fillRect = CGRect(x: 0,
                                      y: padingTop + CGFloat(index) * font.lineHeight,
                                      width: maxWidth * leftProgress,
                                      height: font.lineHeight)
                } else {
                    let width = maxWidth.truncatingRemainder(dividingBy: bounds.width)
                    let dw = (bounds.width - width) / CGFloat(lines) + maxWidth * leftProgress
                    fillRect = CGRect(x: 0,
                                      y: padingTop + CGFloat(index) * font.lineHeight,
                                      width: dw,
                                      height: font.lineHeight)
                }
                path.addRect(fillRect)
                break
            }
        }
        if let context = UIGraphicsGetCurrentContext(), !path.isEmpty {
            context.addPath(path)
            context.clip()
            let _textColor = textColor
            textColor = lrcDrawingColor
            super.draw(rect)
            textColor = _textColor
        }
    }
}
