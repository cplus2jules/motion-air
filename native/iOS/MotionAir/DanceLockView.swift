import SwiftUI

struct DanceLockView: View {
    let session: ProbeSession
    @State private var holding = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 28) {
                    Spacer(minLength: 24)
                    ZStack {
                        RoundedRectangle(cornerRadius: 48).fill(MotionTheme.red)
                            .frame(width: 112, height: 178).rotationEffect(.degrees(-12)).offset(x: -48, y: 10)
                        RoundedRectangle(cornerRadius: 48).fill(MotionTheme.blue)
                            .frame(width: 112, height: 178).rotationEffect(.degrees(12)).offset(x: 48, y: -10)
                        Image(systemName: "lock.fill")
                            .font(.system(size: 48, weight: .semibold)).foregroundStyle(.white)
                            .frame(width: 108, height: 108)
                            .background(MotionTheme.ink, in: Circle())
                    }
                    .frame(height: 220).accessibilityHidden(true)
                    VStack(spacing: 12) {
                        Text("All moves. No mis-taps.").font(.system(.largeTitle, design: .rounded, weight: .bold))
                            .accessibilityAddTraits(.isHeader)
                        Text("Controls locked. Keep Motion Air open, hold your iPhone securely and enjoy the dance.")
                            .font(.body).foregroundStyle(.white.opacity(0.7))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    TimelineView(.periodic(from: .now, by: 0.5)) { _ in
                        Label(session.motionIsFresh ? "Motion streaming" : "Waiting for motion",
                              systemImage: session.motionIsFresh ? "waveform" : "exclamationmark.circle")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(session.motionIsFresh ? Color.green : Color.orange)
                    }
                    Spacer(minLength: 32)
                    VStack(spacing: 12) {
                        Text(holding ? "Keep holding…" : "Hold to unlock controls")
                            .font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 64)
                            .background {
                                Capsule().fill(.white.opacity(0.10))
                                GeometryReader { size in
                                    Capsule().fill(.white.opacity(0.15))
                                        .frame(width: size.size.width)
                                        .scaleEffect(x: holding ? 1 : 0, y: 1, anchor: .leading)
                                }.clipShape(Capsule())
                            }
                            .contentShape(Capsule())
                            .onLongPressGesture(minimumDuration: 1.5, maximumDistance: 32) {
                                session.unlockControls()
                            } onPressingChanged: { pressing in
                                withAnimation(pressing && !reduceMotion ? .linear(duration: 1.5) : nil) {
                                    holding = pressing
                                }
                            }
                            .accessibilityAddTraits(.isButton)
                            .accessibilityLabel("Unlock controls")
                            .accessibilityHint("Double-tap to return to the controller. Motion stays on.")
                            .accessibilityAction { session.unlockControls() }
                            .accessibilityIdentifier("danceUnlock")
                        Text("Hold for 1.5 seconds. Motion stays on.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.65))
                    }
                    Text("Song progress and scores are on your Mac.")
                        .font(.footnote).foregroundStyle(.white.opacity(0.65))
                }
                .multilineTextAlignment(.center)
                .padding(28)
                .frame(maxWidth: 480)
                .frame(minHeight: geometry.size.height)
                .frame(maxWidth: .infinity)
            }
        }
        .foregroundStyle(.white)
        .background(MotionTheme.ink.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .interactiveDismissDisabled()
        .persistentSystemOverlays(.hidden)
        .onDisappear { holding = false }
    }
}
