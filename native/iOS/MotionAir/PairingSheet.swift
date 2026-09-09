import SwiftUI
import AVFoundation
import VisionKit
import JoypadCore

struct PairingSheet: View {
    @Bindable var store: PairingStore
    let session: ProbeSession
    @Environment(\.dismiss) private var dismiss
    @State private var discovery = MacDiscovery()
    @State private var code = ""
    @State private var invitation: PairingInvitation?
    @State private var error: String?
    @State private var scanning = false
    @State private var cameraPending = false
    @State private var attemptedPairing = false
    @FocusState private var codeFocused: Bool

    var body: some View {
        NavigationStack {
            ScrollViewReader { scroll in
            Form {
                if let invitation {
                    Section {
                        Label(invitation.name, systemImage: "desktopcomputer")
                            .font(.title2.weight(.semibold)).padding(.vertical, 8)
                        Text("Check that this is the Mac whose QR code you scanned.")
                        MotionAction(title: store.busy ? "Pairing…" : "Pair and connect", icon: "link") { attemptedPairing = true; store.pair(invitation, session: session) { dismiss() } }
                            .disabled(store.busy).frame(minHeight: 44)
                            .accessibilityIdentifier("confirmPairing")
                        Button("Scan a different code") { self.invitation = nil; code = ""; error = nil; attemptedPairing = false }
                            .disabled(store.busy)
                    } header: { Text("Confirm your Mac") } footer: {
                        Text("This Mac will be remembered securely. You can remove a paired phone from the Mac at any time.")
                    }
                    Section {
                        LabeledContent("Connection", value: "Encrypted · local network")
                        LabeledContent("Address", value: invitation.hosts.first ?? "")
                    }
                } else {
                    Section {
                        VStack(alignment: .leading, spacing: 16) {
                            HStack(spacing: 18) {
                                Image("MotionMark").resizable().scaledToFit().frame(width: 72, height: 72)
                                    .accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("A perfect pair.").font(.system(.title, design: .rounded, weight: .bold))
                                    Text("Your next game is a scan away.").font(.subheadline).foregroundStyle(.secondary)
                                }
                            }
                            Text("Open Motion Air.command on your Mac. Keep both devices on the same Wi-Fi or Personal Hotspot.")
                                .foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                            MotionAction(title: cameraPending ? "Opening camera…" : "Scan Mac QR code", icon: "qrcode.viewfinder") { requestCamera() }
                                .disabled(cameraPending).accessibilityIdentifier("scanQRCode")
                        }.padding(.vertical, 12)
                    }
                    Section {
                        TextField("Paste pairing code", text: $code, axis: .vertical)
                            .lineLimit(2...4).textInputAutocapitalization(.never).autocorrectionDisabled()
                            .focused($codeFocused).accessibilityIdentifier("pairingCode")
                        Button("Review code") { review(code) }.accessibilityIdentifier("reviewCode").disabled(code.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                            .frame(minHeight: 44)
                    } header: { Text("Or paste from your Mac") } footer: {
                        Text("Choose Copy pairing code on your Mac, then paste it here. Codes expire after five minutes.")
                    }
                    Section("Nearby Macs") {
                        ForEach(discovery.macs) { mac in Label(mac.name, systemImage: "desktopcomputer") }
                        Text(discovery.message).font(.callout).foregroundStyle(.secondary)
                    }
                }
                if store.busy { Section { ProgressView(store.message) } }
                if let error {
                    Section {
                        Label(error, systemImage: "exclamationmark.circle.fill")
                            .foregroundStyle(.red).fixedSize(horizontal: false, vertical: true)
                            .accessibilityIdentifier("pairingError")
                    }.id("pairing-error")
                }
                if attemptedPairing && invitation != nil && !store.busy { Section { Text(store.message).font(.callout).foregroundStyle(.secondary) } }
            }
            .onChange(of: error) { _, value in
                if value != nil { scroll.scrollTo("pairing-error", anchor: .center) }
            }
            }
            .navigationTitle("Pair a Mac").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { store.cancel(); dismiss() } } }
            .tint(MotionTheme.action)
            .task { discovery.start() }
            .onDisappear { discovery.stop() }
            .sheet(isPresented: $scanning) {
                NavigationStack {
                    PairingScanner(found: { scanning = false; review($0) }, failed: { scanning = false; error = $0 })
                        .ignoresSafeArea(edges: .bottom)
                        .navigationTitle("Scan your Mac’s QR code").navigationBarTitleDisplayMode(.inline)
                        .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { scanning = false } } }
                }
            }
            .interactiveDismissDisabled(store.busy)
        }
    }

    private func review(_ text: String) {
        codeFocused = false
        do { invitation = try PairingInvitation.parse(text); error = nil }
        catch { self.error = error.localizedDescription }
    }
    private func requestCamera() {
        guard DataScannerViewController.isSupported else { error = "QR scanning is unavailable on this device. Paste the pairing code instead."; return }
        cameraPending = true
        Task {
            let allowed: Bool
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: allowed = true
            case .notDetermined: allowed = await AVCaptureDevice.requestAccess(for: .video)
            default: allowed = false
            }
            cameraPending = false
            guard allowed else { error = "Camera access is off. Enable it in Settings, or paste the pairing code."; return }
            guard DataScannerViewController.isAvailable else { error = "Camera scanning is unavailable. Paste the pairing code instead."; return }
            error = nil; scanning = true
        }
    }
}
