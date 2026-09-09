import SwiftUI

struct MotionRootView: View {
    let session: ProbeSession
    let pairing: PairingStore
    @AppStorage("joypad.onboarding.v1.complete") private var completed = false
    @State private var openMacs = false
    @State private var previewingTour = false

    init(session: ProbeSession, pairing: PairingStore) {
        self.session = session
        self.pairing = pairing
        #if DEBUG
        _previewingTour = State(initialValue: ProcessInfo.processInfo.arguments.contains("--ui-preview-onboarding"))
        #endif
    }

    private var skipTourForPreview: Bool {
        #if DEBUG
        session.isUIPreview || ProcessInfo.processInfo.arguments.contains("--ui-skip-onboarding")
        #else
        false
        #endif
    }

    var body: some View {
        if previewingTour || (!completed && !skipTourForPreview) {
            OnboardingView(completionTitle: pairing.macs.isEmpty ? "Pair my Mac" : "Choose my Mac") { destination in
                openMacs = destination == .pairMac
                completed = true
                previewingTour = false
            }
        } else {
            ContentView(session: session, pairing: pairing, openMacsInitially: openMacs)
        }
    }
}

enum OnboardingDestination { case controller, pairMac }

private enum WelcomeStep: Int, CaseIterable {
    case welcome, controls, motion

    var title: String {
        switch self {
        case .welcome: "Small screen.\nBig game."
        case .controls: "Feels familiar.\nPlays your way."
        case .motion: "Bring your\nbest moves."
        }
    }

    var detail: String {
        switch self {
        case .welcome: "Your iPhone is the controller. Your Mac runs the game. Let's get them together."
        case .controls: "Move the stick to navigate. A selects. B goes back. Give them a try."
        case .motion: "Enable Motion after connecting. Then use Dance Lock to keep accidental taps out of your game."
        }
    }
}

struct OnboardingView: View {
    var completionTitle = "Pair my Mac"
    var isReplay = false
    let finish: (OnboardingDestination) -> Void
    @State private var step = WelcomeStep.welcome
    @State private var feedback = ControllerFeedback()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var typeSize
    @AccessibilityFocusState private var headingFocused: Bool

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { scroll in
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(step.title)
                                .font(typeSize.isAccessibilitySize ? .system(.largeTitle, design: .rounded, weight: .bold) : .system(size: 42, weight: .bold, design: .rounded))
                                .tracking(-1.2)
                                .fixedSize(horizontal: false, vertical: true)
                                .accessibilityAddTraits(.isHeader)
                                .accessibilityFocused($headingFocused)
                                .accessibilityIdentifier("onboardingHeading")
                            Text(step.detail)
                                .font(.body).foregroundStyle(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .id("pageTop")

                        illustration
                            .frame(maxWidth: .infinity)
                            .frame(minHeight: typeSize.isAccessibilitySize ? 230 : min(300, geometry.size.height * 0.48))

                        supportingNote
                    }
                    .padding(.horizontal, 28).padding(.top, 20).padding(.bottom, 24)
                    .frame(maxWidth: 540)
                    .frame(maxWidth: .infinity)
                    .id(step)
                    .transition(reduceMotion ? .opacity : .asymmetric(
                        insertion: .opacity.combined(with: .offset(y: 12)),
                        removal: .opacity.combined(with: .offset(y: -8))
                    ))
                }
                .scrollBounceBehavior(.basedOnSize)
                .onChange(of: step) { _, _ in
                    scroll.scrollTo("pageTop", anchor: .top)
                    headingFocused = true
                }
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .safeAreaInset(edge: .bottom, spacing: 0) { footer }
        .background(Color(uiColor: .systemGroupedBackground))
        .tint(MotionTheme.action)
    }

    private var header: some View {
        HStack(spacing: 10) {
            Image("MotionMark").resizable().scaledToFit().frame(width: 30, height: 30)
                .accessibilityHidden(true)
            Text("Motion Air").font(.system(.headline, design: .rounded)).lineLimit(1).minimumScaleFactor(0.6)
            Spacer(minLength: 8)
            Button { finish(.controller) } label: {
                Text(isReplay ? "Close" : "Skip")
                    .font(.subheadline.weight(.semibold)).frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
                .buttonStyle(.plain).foregroundStyle(.tint)
                .accessibilityIdentifier("onboardingSkip")
        }
        .padding(.horizontal, 28).padding(.top, 8)
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private var footer: some View {
        VStack(spacing: 16) {
            HStack(spacing: 8) {
                ForEach(WelcomeStep.allCases, id: \.rawValue) { item in
                    Capsule().fill(item == step ? Color.primary : Color(uiColor: .tertiaryLabel))
                        .frame(width: item == step ? 28 : 8, height: 6)
                }
                Spacer()
                Text("\(step.rawValue + 1) of 3")
                    .font(.caption.monospacedDigit()).foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Step \(step.rawValue + 1) of 3")
            HStack(spacing: 12) {
                if step != .welcome {
                    Button { changeStep(by: -1) } label: {
                        Image(systemName: "arrow.left").font(.body.weight(.semibold))
                            .frame(width: 48, height: 52)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain).accessibilityLabel("Previous step")
                    .accessibilityIdentifier("onboardingBack")
                }
                MotionAction(title: step == .motion ? completionTitle : typeSize.isAccessibilitySize ? "Next" : step == .welcome ? "Let's try it" : "One more thing",
                             icon: step == .motion && !isReplay ? "qrcode.viewfinder" : "arrow.right") {
                    if step == .motion {
                        feedback.play(.success)
                        finish(isReplay ? .controller : .pairMac)
                    } else { changeStep(by: 1) }
                }
                .accessibilityIdentifier("onboardingNext")
            }
        }
        .padding(.horizontal, 28).padding(.top, 16).padding(.bottom, 12)
        .frame(maxWidth: 540).frame(maxWidth: .infinity)
        .background(.bar)
    }

    @ViewBuilder private var illustration: some View {
        switch step {
        case .welcome: WelcomeArtwork()
        case .controls: PracticeController(feedback: feedback)
        case .motion: MotionLesson(feedback: feedback)
        }
    }

    private var supportingNote: some View {
        Label {
            Text(step == .welcome
                 ? "Open Motion Air.command on your Mac. Keep both devices on the same Wi-Fi or Personal Hotspot."
                 : step == .controls
                 ? "Just practice. These controls stay right here."
                 : "Keep the app open while you play. After reconnecting, turn Motion on again.")
            .fixedSize(horizontal: false, vertical: true)
        } icon: {
            Image(systemName: step == .welcome ? "wifi" : step == .controls ? "hand.tap" : "iphone")
        }
        .font(.footnote).foregroundStyle(.secondary)
        .labelStyle(.titleAndIcon)
    }

    private func changeStep(by amount: Int) {
        guard let next = WelcomeStep(rawValue: step.rawValue + amount) else { return }
        feedback.play(.selection)
        withAnimation(reduceMotion ? nil : OnboardingMotion.page) { step = next }
    }
}

private struct WelcomeArtwork: View {
    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 20)
                    .fill(MotionTheme.ink)
                    .overlay {
                        VStack(spacing: 12) {
                            Image(systemName: "play.fill").font(.system(size: 30, weight: .bold))
                            Text("PRESS PLAY").font(.system(size: 11, weight: .bold, design: .monospaced)).tracking(3)
                        }.foregroundStyle(.white.opacity(0.9))
                    }
                    .frame(width: 232, height: 152)
                UnevenRoundedRectangle(bottomLeadingRadius: 10, bottomTrailingRadius: 10)
                    .fill(Color(uiColor: .systemGray3)).frame(width: 76, height: 16)
            }
            .rotationEffect(.degrees(-6)).offset(x: -10, y: -42)
            Image("MotionMark").resizable().scaledToFit()
                .frame(width: 228, height: 228)
                .rotationEffect(.degrees(9)).offset(x: 28, y: 55)
        }
        .frame(height: 290)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("A red and blue controller in front of your Mac")
    }
}

private struct PracticeController: View {
    let feedback: ControllerFeedback
    @GestureState private var stickOffset = CGSize.zero
    @State private var response = "Your turn, player one."
    @State private var selected = false

    var body: some View {
        VStack(spacing: 24) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 44).fill(MotionTheme.red)
                    Circle().fill(MotionTheme.ink.opacity(0.16)).frame(width: 104, height: 104)
                    Circle().fill(MotionTheme.ink.gradient).frame(width: 70, height: 70)
                        .overlay { Circle().strokeBorder(.white.opacity(0.14), lineWidth: 2).padding(6) }
                        .offset(stickOffset)
                }
                .frame(maxWidth: 136).frame(height: 200)
                .contentShape(RoundedRectangle(cornerRadius: 44))
                .gesture(DragGesture(minimumDistance: 0)
                    .updating($stickOffset) { value, state, _ in
                        let distance = max(1, hypot(value.translation.width, value.translation.height) / 24)
                        state = CGSize(width: value.translation.width / distance, height: value.translation.height / distance)
                    }
                    .onChanged { _ in response = "That's the way." }
                    .onEnded { _ in feedback.play(.selection); response = "Right back to center." })
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Practice stick")
                .accessibilityHint("Drag to practice, or use the Move left and Move right actions.")
                .accessibilityAction(named: "Move right") { response = "Moving right."; feedback.play(.selection) }
                .accessibilityAction(named: "Move left") { response = "Moving left."; feedback.play(.selection) }
                .accessibilityIdentifier("onboardingStick")

                VStack(spacing: 18) {
                    practiceButton("A", label: "Practice A, select") { selected = true; response = "Selected. Nice one." }
                        .offset(x: 14)
                    practiceButton("B", label: "Practice B, go back") { selected = false; response = "And you're back." }
                        .offset(x: -14)
                }
                .frame(maxWidth: 136).frame(height: 200)
                .background(MotionTheme.blue, in: RoundedRectangle(cornerRadius: 44))
            }
            Label(response, systemImage: selected ? "checkmark.circle.fill" : "gamecontroller")
                .font(.system(.subheadline, design: .rounded, weight: .semibold))
                .multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                .accessibilityIdentifier("onboardingPracticeResponse")
        }
    }

    private func practiceButton(_ title: String, label: String, action: @escaping () -> Void) -> some View {
        Button {
            feedback.play(.press)
            action()
        } label: {
            Text(title).font(.system(size: 26, weight: .bold, design: .rounded))
                .foregroundStyle(.white).frame(width: 64, height: 64)
                .background(MotionTheme.ink.gradient, in: Circle())
                .overlay { Circle().strokeBorder(.white.opacity(0.18), lineWidth: 1) }
        }
        .buttonStyle(PracticeKeyStyle()).accessibilityLabel(label)
        .accessibilityIdentifier("onboarding\(title)")
    }
}

private struct PracticeKeyStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.96 : 1)
            .opacity(configuration.isPressed ? 0.78 : 1)
    }
}

private struct MotionLesson: View {
    let feedback: ControllerFeedback
    @State private var locked = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                RoundedRectangle(cornerRadius: 38).fill(MotionTheme.ink)
                RoundedRectangle(cornerRadius: 30).fill(Color(uiColor: .secondarySystemGroupedBackground)).padding(8)
                VStack(spacing: 12) {
                    Image(systemName: locked ? "lock.shield.fill" : "figure.dance")
                        .font(.system(size: 48, weight: .medium)).frame(height: 64)
                        .foregroundStyle(locked ? MotionTheme.action : MotionTheme.red)
                    HStack(alignment: .center, spacing: 5) {
                        ForEach(0..<11, id: \.self) { index in
                            Capsule().fill(MotionTheme.blue)
                                .frame(width: 5, height: CGFloat([10, 18, 28, 14, 22, 34, 22, 14, 28, 18, 10][index]))
                        }
                    }
                    Text(locked ? "Taps locked.\nMotion keeps going." : "Your moves,\nin the game.")
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .multilineTextAlignment(.center)
                }.padding(.top, 14)
            }
            .frame(width: 160, height: 236)
            .overlay(alignment: .top) {
                Capsule().fill(MotionTheme.ink).frame(width: 44, height: 12).padding(.top, 15)
            }
            .rotationEffect(.degrees(reduceMotion || locked ? 0 : -8))
            .animation(reduceMotion ? nil : OnboardingMotion.demonstration, value: locked)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(locked ? "Preview: buttons locked, motion continues" : "Preview: motion controls")
            Button {
                locked.toggle()
                feedback.play(.selection)
            } label: {
                Label(locked ? "Unlock preview" : "Try Dance Lock", systemImage: locked ? "lock.open" : "lock")
                    .font(.subheadline.weight(.semibold)).frame(minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityValue(locked ? "Locked" : "Unlocked")
            .accessibilityIdentifier("onboardingLockPreview")
        }
    }
}

#Preview("Welcome") {
    OnboardingView { _ in }
}
