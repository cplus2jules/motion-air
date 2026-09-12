import SwiftUI
import UIKit
import JoypadCore

struct NavigationControls: View {
    let session: ProbeSession
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(spacing: 14) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: 12) { leftGrip; rightGrip }
                VStack(spacing: 12) { leftGrip; rightGrip }
            }
            HStack(spacing: 10) {
                button(.dpadLeft, "←", "D-pad left")
                button(.dpadUp, "↑", "D-pad up")
                button(.dpadDown, "↓", "D-pad down")
                button(.dpadRight, "→", "D-pad right")
            }
            DisclosureGroup {
                VStack(spacing: 10) {
                    HStack(spacing: 10) {
                        button(.sl, "SL", "SL")
                        button(.sr, "SR", "SR")
                    }
                    HStack(spacing: 10) {
                        button(.l, "L", "Left shoulder")
                        button(.zl, "ZL", "Left trigger")
                        button(.zr, "ZR", "Right trigger")
                        button(.r, "R", "Right shoulder")
                    }
                }.padding(.top, 12)
            } label: { Text("More buttons").frame(minHeight: 44) }
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 8).padding(.vertical, 4)
        }
    }

    private var leftGrip: some View {
        VStack(spacing: 14) {
            HStack { button(.minus, "−", "Minus").frame(width: 48); Spacer(); gripLabel("MOVE") }
            MenuJoystick(enabled: session.controlsEnabled, move: session.moveStick, nudge: session.nudgeStick)
                .padding(.vertical, 8)
        }
        .frame(minWidth: 138, maxWidth: .infinity, minHeight: 246)
        .padding(10)
        .background(MotionTheme.red, in: UnevenRoundedRectangle(topLeadingRadius: 38, bottomLeadingRadius: 48, bottomTrailingRadius: 28, topTrailingRadius: 28))
        .foregroundStyle(MotionTheme.ink)
    }

    private var rightGrip: some View {
        VStack(spacing: 14) {
            HStack { gripLabel("PLAY"); Spacer(); button(.plus, "+", "Plus / pause").frame(width: 48) }
            VStack(spacing: 3) {
                button(.x, "X", "X").frame(width: 48)
                HStack(spacing: 36) {
                    button(.y, "Y", "Y").frame(width: 48)
                    button(.a, "A", "A / select").frame(width: 48)
                }
                button(.b, "B", "B / back").frame(width: 48)
            }.frame(width: 132)
        }
        .frame(minWidth: 138, maxWidth: .infinity, minHeight: 246)
        .padding(10)
        .background(MotionTheme.blue, in: UnevenRoundedRectangle(topLeadingRadius: 28, bottomLeadingRadius: 28, bottomTrailingRadius: 48, topTrailingRadius: 38))
        .foregroundStyle(MotionTheme.ink)
    }

    private func gripLabel(_ label: String) -> some View {
        Text(label).font(.system(.caption2, design: .rounded, weight: .heavy)).tracking(1.5)
            .accessibilityHidden(true)
    }

    private func button(_ id: ControllerButtonID, _ title: String, _ label: String) -> some View {
        ControllerPressButton(title: title, label: label, identifier: "control-\(id.rawValue)",
                              enabled: session.controlsEnabled,
                              changed: { session.setButton(id, down: $0) },
                              activate: { session.tapButton(id) })
            .frame(minWidth: 44, minHeight: 48, maxHeight: 48)
    }
}

/// UIKit touch cancellation keeps simultaneous shoulder/face/stick input independent of Form scrolling.
struct ControllerPressButton: UIViewRepresentable {
    let title: String
    let label: String
    let identifier: String
    let enabled: Bool
    let changed: (Bool) -> Void
    let activate: () -> Void

    func makeUIView(context: Context) -> TouchButton {
        let button = TouchButton(type: .system)
        button.isExclusiveTouch = false
        button.addTarget(button, action: #selector(TouchButton.press), for: [.touchDown, .touchDragEnter])
        button.addTarget(button, action: #selector(TouchButton.endPress), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        return button
    }

    func updateUIView(_ button: TouchButton, context: Context) {
        button.changed = changed
        button.activate = activate
        if !enabled { button.endPress() }
        button.isEnabled = enabled
        button.accessibilityLabel = label
        button.accessibilityIdentifier = identifier
        var configuration = UIButton.Configuration.filled()
        configuration.title = title
        configuration.cornerStyle = .capsule
        configuration.baseForegroundColor = .white
        configuration.baseBackgroundColor = UIColor(red: 0.125, green: 0.149, blue: 0.188, alpha: 1)
        configuration.background.strokeColor = UIColor.white.withAlphaComponent(0.20)
        configuration.background.strokeWidth = 1
        button.alpha = enabled ? 1 : 0.60
        configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
            var attributes = incoming
            // Physical key glyphs stay inside their fixed hit areas at accessibility sizes.
            attributes.font = UIFontMetrics(forTextStyle: .headline).scaledFont(
                for: .systemFont(ofSize: 17, weight: .semibold), maximumPointSize: 26
            )
            return attributes
        }
        button.configuration = configuration
    }

    static func dismantleUIView(_ button: TouchButton, coordinator: ()) { button.endPress() }

    @MainActor final class TouchButton: UIButton {
        var changed: ((Bool) -> Void)?
        var activate: (() -> Void)?
        private var down = false

        override var isHighlighted: Bool {
            didSet {
                // Game input is frequent: feedback is immediate, with no animation or input delay.
                transform = isHighlighted && !UIAccessibility.isReduceMotionEnabled
                    ? CGAffineTransform(scaleX: 0.96, y: 0.96) : .identity
            }
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            guard window != nil else { endPress(); return }
            // Form delays UIKit touchDown while deciding whether to scroll.
            // Deliver game controls immediately; retain normal cancellation
            // when a drag actually turns into scrolling.
            var ancestor = superview
            while let view = ancestor {
                if let scroll = view as? UIScrollView { scroll.delaysContentTouches = false }
                ancestor = view.superview
            }
        }

        @objc func press() {
            guard isEnabled, !down else { return }
            down = true
            changed?(true)
        }
        @objc func endPress() {
            guard down else { return }
            down = false
            changed?(false)
        }
        override func accessibilityActivate() -> Bool {
            guard isEnabled else { return false }
            activate?()
            return true
        }
    }
}
