import Foundation
import Network
import Observation

@MainActor @Observable
final class MacDiscovery {
    struct NearbyMac: Identifiable, Sendable { let id: String; let name: String }
    struct Endpoint: Sendable { let host: String; let port: Int }
    private(set) var macs: [NearbyMac] = []
    private(set) var message = "Looking for Motion Air on your network…"
    private(set) var unavailable = false
    @ObservationIgnored private var browser: NWBrowser?
    @ObservationIgnored private var generation = UUID()
    @ObservationIgnored private var endpoints: [String: NWEndpoint] = [:]

    func start() {
        guard browser == nil else { return }
        let id = UUID()
        generation = id
        unavailable = false
        message = "Looking for Motion Air on your network…"
        let browser = NWBrowser(for: .bonjourWithTXTRecord(type: "_joypadair._tcp", domain: nil), using: .tcp)
        self.browser = browser
        browser.browseResultsChangedHandler = { [weak self] results, _ in
            var services: [String: NWEndpoint] = [:]
            let found = results.compactMap { result -> NearbyMac? in
                guard case .service(let name, _, _, _) = result.endpoint,
                      case .bonjour(let txt) = result.metadata,
                      txt["v"] == "1", txt["tls"] == "1", let id = txt["id"], UUID(uuidString: id) != nil else { return nil }
                let key = id.lowercased()
                guard services[key] == nil, services.count < 16 else { return nil }
                services[key] = result.endpoint
                return NearbyMac(id: key, name: name)
            }.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
            let discoveredEndpoints = services
            Task { @MainActor in
                guard let self, self.generation == id else { return }
                self.macs = found
                self.endpoints = discoveredEndpoints
                self.message = found.isEmpty ? "No Mac found yet. Open Motion Air.command on your Mac and use the same Wi-Fi or Personal Hotspot." : "Motion Air is running nearby. Pair a new Mac once with its QR code."
            }
        }
        browser.stateUpdateHandler = { [weak self] state in
            Task { @MainActor in
                guard let self, self.generation == id else { return }
                switch state {
                case .waiting, .failed:
                    self.unavailable = true
                    self.macs = []; self.endpoints = [:]
                    self.message = "Discovery is unavailable. Check Motion Air’s Local Network access in Settings. You can still try a saved Mac or scan a fresh QR code."
                case .ready:
                    self.unavailable = false
                default: break
                }
            }
        }
        browser.start(queue: .main)
    }
    func contains(_ id: String) -> Bool { endpoints[id.lowercased()] != nil }

    /// The service only supplies an address hint. PairingTransport must verify the saved certificate.
    func resolve(_ id: String) async throws -> Endpoint? {
        start()
        let currentGeneration = generation
        for _ in 0..<10 {
            try Task.checkCancellation()
            guard generation == currentGeneration else { return nil }
            if let endpoint = endpoints[id.lowercased()] {
                return try await BonjourResolution(endpoint: endpoint).resolve()
            }
            if unavailable { return nil }
            try await Task.sleep(for: .milliseconds(100))
        }
        return nil
    }

    func stop() { generation = UUID(); browser?.cancel(); browser = nil; macs = []; endpoints = [:] }
}

@MainActor
private final class BonjourResolution {
    private let connection: NWConnection
    private var continuation: CheckedContinuation<MacDiscovery.Endpoint?, Error>?
    private var timeout: Task<Void, Never>?

    init(endpoint: NWEndpoint) {
        let parameters = NWParameters.tcp
        // The bridge listens on IPv4. Do not select an IPv6 service address it cannot accept.
        (parameters.defaultProtocolStack.internetProtocol as? NWProtocolIP.Options)?.version = .v4
        connection = NWConnection(to: endpoint, using: parameters)
    }

    func resolve() async throws -> MacDiscovery.Endpoint? {
        try await withTaskCancellationHandler {
            try Task.checkCancellation()
            return try await withCheckedThrowingContinuation { continuation in
                self.continuation = continuation
                connection.stateUpdateHandler = { [weak self] state in
                    Task { @MainActor in self?.changed(state) }
                }
                connection.start(queue: .main)
                timeout = Task { [weak self] in
                    do { try await Task.sleep(for: .seconds(2)) }
                    catch { return }
                    self?.finish(.success(nil))
                }
            }
        } onCancel: {
            Task { @MainActor in self.finish(.failure(CancellationError())) }
        }
    }

    private func changed(_ state: NWConnection.State) {
        switch state {
        case .ready:
            if case .hostPort(.ipv4(let address), let port) = connection.currentPath?.remoteEndpoint {
                // Host.debugDescription can append %en0, which is not an IPv4 URL host.
                let host = address.rawValue.map { String($0) }.joined(separator: ".")
                finish(.success(MacDiscovery.Endpoint(host: host, port: Int(port.rawValue))))
            } else { finish(.success(nil)) }
        case .failed, .waiting, .cancelled: finish(.success(nil))
        default: break
        }
    }

    private func finish(_ result: Result<MacDiscovery.Endpoint?, Error>) {
        guard let continuation else { return }
        self.continuation = nil
        timeout?.cancel(); timeout = nil
        connection.stateUpdateHandler = nil
        connection.cancel()
        continuation.resume(with: result)
    }
}
