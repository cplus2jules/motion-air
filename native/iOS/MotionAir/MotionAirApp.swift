import SwiftUI

@main
struct MotionAirApp: App {
    @State private var session = ProbeSession()
    @State private var pairing = PairingStore()
    @Environment(\.scenePhase) private var scenePhase

    init() {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--ui-preview-connected") || arguments.contains("--ui-preview-motion") || arguments.contains("--ui-preview-dance") {
            _session = State(initialValue: ProbeSession.uiPreview(
                motion: arguments.contains("--ui-preview-motion") || arguments.contains("--ui-preview-dance"),
                locked: arguments.contains("--ui-preview-dance")
            ))
        }
        #endif
    }

    var body: some Scene {
        WindowGroup {
            MotionRootView(session: session, pairing: pairing)
                .onChange(of: scenePhase) { _, phase in
                    if phase == .inactive { session.releaseControls() }
                    if phase == .background { pairing.cancel(); pairing.discovery.stop() }
                    if phase == .background, session.connected || session.connecting {
                        session.disconnect(reason: "Motion Air went into the background. Keep it open while playing, then reconnect to continue.")
                    }
                }
        }
    }
}
