import SwiftUI
import JoypadCore

private enum ControllerSheet: String, Identifiable {
    case macs, settings
    var id: String { rawValue }
}

enum MotionTheme {
    static let red = Color(red: 1, green: 0.325, blue: 0.392)
    static let blue = Color(red: 0.125, green: 0.647, blue: 0.922)
    static let ink = Color(red: 0.125, green: 0.149, blue: 0.188)
    static let action = Color(red: 0, green: 0.443, blue: 0.89)
}

struct ContentView: View {
    @Bindable var session: ProbeSession
    @Bindable var pairing: PairingStore
    @State private var sheet: ControllerSheet?
    @Environment(\.dynamicTypeSize) private var typeSize

    init(session: ProbeSession, pairing: PairingStore, openMacsInitially: Bool = false) {
        self.session = session
        self.pairing = pairing
        _sheet = State(initialValue: openMacsInitially ? .macs : nil)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    #if DEBUG
                    if session.isUIPreview {
                        Label("UI preview · no game output", systemImage: "eye")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    #endif
                    connectionHeader
                    if session.connectionRecovering {
                        Label("Wi-Fi is catching up. Keep your phone near the Mac or router.", systemImage: "wifi.exclamationmark")
                            .font(.footnote).foregroundStyle(.secondary)
                            .accessibilityIdentifier("connectionRecoveryNotice")
                    }
                    if session.connected && session.motionReceivers == 0 {
                        Label("No motion receiver yet. Open Ryujinx through Motion Air.command and start your game.", systemImage: "desktopcomputer.trianglebadge.exclamationmark")
                            .font(.footnote).foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityIdentifier("motionReceiverNotice")
                    }
                    NavigationControls(session: session)
                    if !session.connected {
                        Label(session.notice, systemImage: "info.circle")
                            .font(.footnote).foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityIdentifier("sessionNotice")
                    }
                }
                .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 24)
                .frame(maxWidth: 620)
                .frame(maxWidth: .infinity)
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .safeAreaInset(edge: .bottom, spacing: 0) { playActions }
            .disabled(session.danceLocked)
            .allowsHitTesting(!session.danceLocked)
            .accessibilityHidden(session.danceLocked)
            .navigationTitle("Motion Air")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 6) {
                        Image("MotionMark").resizable().scaledToFit().frame(width: 28, height: 28)
                            .accessibilityHidden(true)
                        Text("Motion Air").font(.system(.headline, design: .rounded))
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("Motion Air").accessibilityAddTraits(.isHeader)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { sheet = .settings } label: {
                        Image(systemName: "slider.horizontal.3").frame(width: 44, height: 44)
                    }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Controller settings").accessibilityIdentifier("settingsButton")
                }
            }
            .sheet(item: $sheet) { destination in
                switch destination {
                case .macs: MacConnectionsView(pairing: pairing, session: session)
                case .settings: ControllerSettingsView(session: session, pairing: pairing)
                }
            }
            .fullScreenCover(isPresented: Binding(
                get: { session.danceLocked },
                set: { if !$0 { session.unlockControls() } }
            )) { DanceLockView(session: session) }
            .tint(MotionTheme.action)
        }
    }

    private var connectionHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 5) {
                    Text(session.connected ? "Let's play." : "Ready, player one?")
                        .font(.system(.largeTitle, design: .rounded, weight: .bold))
                        .fixedSize(horizontal: false, vertical: true)
                        .accessibilityAddTraits(.isHeader)
                    Text(session.connected ? "A selects. B goes back. You know the drill." : "Your iPhone. Your Mac. One more game.")
                        .font(.subheadline).foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            Button { sheet = .macs } label: {
                HStack(spacing: 10) {
                    Image(systemName: session.connected ? "desktopcomputer" : "link")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(session.connected ? Color.green : MotionTheme.action)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(session.connectedMacName ?? (session.connecting ? "Connecting to your Mac…" : "Connect your Mac"))
                            .font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                            .lineLimit(typeSize.isAccessibilitySize ? nil : 1)
                        Text(session.connected ? "Connected · Player 1" : "Pair once, then tap to reconnect")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 4)
                    if session.connecting { ProgressView() }
                    else { Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.secondary) }
                }
                .padding(14).frame(maxWidth: .infinity, minHeight: 60)
                .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 20))
                .contentShape(RoundedRectangle(cornerRadius: 20))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("macsButton")
        }
    }

    private var playActions: some View {
        VStack(spacing: 8) {
            if #available(iOS 26, *) {
                GlassEffectContainer(spacing: 12) { actionRow }
            } else { actionRow }
            Text(session.connected ? "Keep Motion Air open while you play." : "Open Motion Air.command on your Mac to start.")
                .font(.caption).foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 8)
        .frame(maxWidth: .infinity)
        .background(.bar)
    }

    @ViewBuilder private var actionRow: some View {
        if !session.connected {
            MotionAction(title: session.connecting ? "Connecting…" : "Pair a Mac", icon: "qrcode.viewfinder") { sheet = .macs }
                .disabled(session.connecting).accessibilityIdentifier("pairMacButton")
        } else {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 12) { motionAction; lockAction }
                VStack(spacing: 10) { motionAction; lockAction }
            }
        }
    }

    private var motionAction: some View {
        MotionAction(title: session.motionPending ? "Checking…" : session.motionEnabled ? "Motion on" : "Enable Motion",
                     icon: session.motionEnabled ? "waveform" : "waveform.slash", prominent: !session.motionEnabled) {
            session.setMotion(!session.motionEnabled)
        }
        .disabled(!session.compatible || session.motionPending || !session.sensorAvailable)
        .accessibilityValue(session.motionEnabled ? "On" : "Off")
        .accessibilityHint(session.sensorAvailable ? "Send your iPhone movement to the Mac." : "Motion needs a physical iPhone.")
        .accessibilityIdentifier("motionToggle")
    }

    private var lockAction: some View {
        MotionAction(title: "Dance Lock", icon: "lock.shield", prominent: session.motionEnabled) { session.lockForDance() }
            .disabled(!session.motionEnabled || session.motionPending)
            .accessibilityHint("Protect buttons and stick while motion continues.")
            .accessibilityIdentifier("danceLock")
    }
}

struct MotionAction: View {
    let title: String
    let icon: String
    var prominent = true
    let action: () -> Void
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        if #available(iOS 26, *), !reduceTransparency {
            if prominent { button.buttonStyle(.glassProminent) }
            else { button.buttonStyle(.glass) }
        } else {
            if prominent { button.buttonStyle(.borderedProminent) }
            else { button.buttonStyle(.bordered) }
        }
    }

    private var button: some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.body.weight(.semibold))
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, minHeight: 36)
        }
        .buttonBorderShape(.capsule).controlSize(.large)
    }
}

private struct MacConnectionsView: View {
    @Bindable var pairing: PairingStore
    let session: ProbeSession
    @Environment(\.dismiss) private var dismiss
    @State private var showingPairing = false
    @State private var removingMac: SavedMac?

    var body: some View {
        NavigationStack {
            List {
                if session.connected || session.connecting {
                    Section("Current Mac") {
                        Label {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(session.connectedMacName ?? "Direct connection").font(.headline)
                                Text(session.connecting ? "Connecting…" : "Connected · Player 1")
                                    .font(.subheadline).foregroundStyle(.secondary)
                            }
                        } icon: { Image(systemName: "desktopcomputer").foregroundStyle(.tint) }
                        Button(session.connecting ? "Cancel connection" : "Disconnect", role: .destructive) {
                            pairing.cancel(); session.disconnect()
                        }.frame(minHeight: 44).accessibilityIdentifier("connectionButton")
                    }
                }
                if !session.connected && !session.connecting {
                    Section {
                        if pairing.macs.isEmpty {
                            ContentUnavailableView {
                                Label("Meet your Mac", systemImage: "desktopcomputer")
                            } description: {
                                Text("Open Motion Air.command on your Mac, then scan its QR code here.")
                            }
                        }
                        ForEach(pairing.macs) { mac in
                            HStack(spacing: 8) {
                                Button { pairing.connect(mac, session: session) } label: {
                                    Label {
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(mac.name).foregroundStyle(.primary)
                                            Text(pairing.discovery.contains(mac.id) ? "Nearby · Tap to connect" : "Find and connect")
                                                .font(.caption).foregroundStyle(.secondary)
                                        }
                                    } icon: { Image(systemName: "desktopcomputer") }
                                    .frame(minHeight: 44)
                                }.buttonStyle(.plain).disabled(pairing.busy)
                                Spacer()
                                Button { removingMac = mac } label: {
                                    Image(systemName: "ellipsis").frame(width: 44, height: 44)
                                }.buttonStyle(.borderless).accessibilityLabel("Manage \(mac.name)")
                            }
                        }
                        if pairing.busy { ProgressView(pairing.message) }
                        else if !pairing.message.isEmpty { Text(pairing.message).font(.callout).foregroundStyle(.secondary) }
                    } header: { if !pairing.macs.isEmpty { Text("Saved Macs") } }
                    Section("Nearby Macs") {
                        ForEach(pairing.discovery.macs.filter { nearby in !pairing.macs.contains { $0.id.lowercased() == nearby.id } }) { mac in
                            Button { showingPairing = true } label: {
                                Label {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(mac.name).foregroundStyle(.primary)
                                        Text("Scan its QR code to pair").font(.caption).foregroundStyle(.secondary)
                                    }
                                } icon: { Image(systemName: "desktopcomputer") }
                                .frame(minHeight: 44)
                            }.disabled(pairing.busy)
                        }
                        Label(pairing.discovery.message, systemImage: pairing.discovery.unavailable ? "wifi.exclamationmark" : "dot.radiowaves.left.and.right")
                            .font(.callout).foregroundStyle(.secondary)
                            .accessibilityIdentifier("macDiscoveryStatus")
                    }
                    Section {
                        Button { showingPairing = true } label: { Label("Pair a Mac", systemImage: "qrcode.viewfinder").frame(minHeight: 44) }
                            .disabled(pairing.busy).accessibilityIdentifier("scanPairMac")
                    } footer: { Text("Use the same Wi-Fi or Personal Hotspot. Connecting takes the Player 1 slot.") }
                }
            }
            .navigationTitle("Your Mac").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
            .sheet(isPresented: $showingPairing) { PairingSheet(store: pairing, session: session) }
            .task { pairing.discovery.start() }
            .onDisappear { if !pairing.busy { pairing.discovery.stop() } }
            .confirmationDialog("Forget this Mac?", isPresented: Binding(get: { removingMac != nil }, set: { if !$0 { removingMac = nil } }), titleVisibility: .visible) {
                if let mac = removingMac { Button("Forget Mac", role: .destructive) { pairing.forget(mac, session: session); removingMac = nil } }
            } message: { Text("Scan its QR code to pair again.") }
        }
    }
}

private struct ControllerSettingsView: View {
    @Bindable var session: ProbeSession
    let pairing: PairingStore
    @Environment(\.dismiss) private var dismiss
    @AppStorage("probe.macHost") private var host = ""
    @AppStorage("probe.macPort") private var port = "3001"
    @AppStorage(ControllerFeedback.preferenceKey) private var hapticsEnabled = true
    @FocusState private var addressFocused: Bool
    @State private var showingWelcome = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Haptic feedback", isOn: $hapticsEnabled)
                        .accessibilityIdentifier("hapticsToggle")
                        .onChange(of: hapticsEnabled) { _, enabled in if enabled { session.previewHaptics() } }
                    Button { session.previewHaptics() } label: { Label("Try a little tap", systemImage: "hand.tap") }
                        .disabled(!hapticsEnabled).frame(minHeight: 44)
                } header: { Text("Feel the controls") } footer: {
                    Text("Feel button presses, stick engagement and connection changes. Haptics need a physical iPhone.")
                }
                Section("Connection & diagnostics") {
                    LabeledContent("Connection", value: session.status)
                    Text(session.notice).font(.callout).foregroundStyle(.secondary).accessibilityIdentifier("sessionNotice")
                    if let reason = session.lastDisconnectReason {
                        LabeledContent("Last session", value: String(format: "%.1f seconds", session.lastConnectionDuration))
                        Text(reason).font(.callout).foregroundStyle(.secondary)
                            .accessibilityIdentifier("lastDisconnectReason")
                    }
                    LabeledContent("Keyboard", value: session.keyboardReady)
                    LabeledContent("Sensor delivery", value: String(format: "%.1f Hz", session.sampleRate))
                    LabeledContent("Largest sample gap", value: String(format: "%.1f ms", session.maximumGapMilliseconds))
                    LabeledContent("Round trip", value: session.roundTripMilliseconds.map { String(format: "%.1f ms", $0) } ?? "—")
                    LabeledContent("Motion frames sent", value: "\(session.samplesSent)")
                    LabeledContent("Motion receivers", value: session.motionReceivers.map(String.init) ?? "Update the Mac bridge")
                    LabeledContent("Last motion at Mac", value: session.bridgeMotionAgeMilliseconds.map { String(format: "%.0f ms ago", $0) } ?? "No recent sample")
                    LabeledContent("Unsent frames discarded", value: "\(session.droppedMotion)")
                    LabeledContent("Samples above bridge range", value: "\(session.rangeExceededCount)")
                    if let sample = session.sample {
                        vectorRow("Acceleration · g", vector: sample.accelerationG)
                        vectorRow("Angular velocity · °/s", vector: sample.rotationDegreesPerSecond)
                        LabeledContent("Acceleration magnitude", value: String(format: "%.3f g", sample.accelerationG.magnitude))
                    }
                }
                Section {
                    DisclosureGroup("Direct connection · advanced") {
                        TextField("Mac IPv4 address or name.local", text: $host)
                            .keyboardType(.URL).textInputAutocapitalization(.never).autocorrectionDisabled()
                            .focused($addressFocused).accessibilityLabel("Mac address")
                        TextField("Port", text: $port).keyboardType(.numberPad).accessibilityLabel("Mac port")
                        Button("Connect directly") { addressFocused = false; session.connect(host: host, port: port) }
                            .disabled(session.connected || session.connecting || pairing.busy).frame(minHeight: 44)
                        Text("For a trusted development network only. Direct connections are unencrypted. Use pairing for everyday play.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }
                Section {
                    Label("Keep the app open", systemImage: "iphone")
                    Text("Locking your phone or switching apps disconnects the controller. Song progress and scores stay on your Mac.")
                        .foregroundStyle(.secondary)
                    Button { showingWelcome = true } label: {
                        Label("Take the welcome tour", systemImage: "play.rectangle")
                            .frame(minHeight: 44)
                    }
                    .accessibilityIdentifier("replayOnboarding")
                }
                Section("About Motion Air") {
                    Text("An iPhone controller for Just Dance and other motion games on Mac.")
                    Text("Built with Joypad Air in mind, and based on Joypad Air by David García (mindavidev).")
                        .font(.footnote).foregroundStyle(.secondary)
                    Link("Joypad Air · original project", destination: URL(string: "https://github.com/mindavidev/joypad-air")!)
                    Link("Motion Air · source and acknowledgements", destination: URL(string: "https://github.com/cplus2jules/motion-air")!)
                }
            }
            .navigationTitle("Controller settings").navigationBarTitleDisplayMode(.inline)
            .fullScreenCover(isPresented: $showingWelcome) {
                OnboardingView(completionTitle: "Back to controller", isReplay: true) { _ in showingWelcome = false }
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { addressFocused = false; UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) }
                }
            }
        }
    }
    private func vectorRow(_ title: String, vector: Vector3) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.subheadline)
            Text(String(format: "x % .3f  y % .3f  z % .3f", vector.x, vector.y, vector.z))
                .font(.caption.monospaced()).foregroundStyle(.secondary)
                .textSelection(.enabled)
        }.padding(.vertical, 4)
    }
}

#if DEBUG
#Preview("Controller · disconnected") {
    ContentView(session: ProbeSession(), pairing: PairingStore())
}

#Preview("Controller · connected") {
    ContentView(session: .uiPreview(), pairing: PairingStore())
}
#Preview("Controller · motion") {
    ContentView(session: .uiPreview(motion: true), pairing: PairingStore())
}

#endif
