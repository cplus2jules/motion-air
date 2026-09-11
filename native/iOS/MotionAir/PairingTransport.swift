import Foundation
import Security
import CryptoKit
import JoypadCore

/// Only the exact leaf certificate scanned from the Mac can authenticate this connection.
nonisolated final class PinnedTLSDelegate: NSObject, URLSessionTaskDelegate {
    let fingerprint: String
    init(fingerprint: String) { self.fingerprint = fingerprint.lowercased() }

    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping @Sendable (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        evaluate(challenge, completionHandler)
    }
    func urlSession(_ session: URLSession, task: URLSessionTask, didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping @Sendable (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        evaluate(challenge, completionHandler)
    }
    private func evaluate(_ challenge: URLAuthenticationChallenge,
                          _ completion: @escaping @Sendable (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust else {
            completion(.performDefaultHandling, nil); return
        }
        guard let trust = challenge.protectionSpace.serverTrust,
              let certificates = SecTrustCopyCertificateChain(trust) as? [SecCertificate], let certificate = certificates.first else {
            completion(.cancelAuthenticationChallenge, nil); return
        }
        let digest = SHA256.hash(data: SecCertificateCopyData(certificate) as Data).map { String(format: "%02x", $0) }.joined()
        guard digest == fingerprint else { completion(.cancelAuthenticationChallenge, nil); return }
        completion(.useCredential, URLCredential(trust: trust))
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping @Sendable (URLRequest?) -> Void) {
        completionHandler(nil) // Pairing credentials must never follow an unexpected redirect.
    }
}

@MainActor
enum PairingTransport {
    static func session(fingerprint: String) -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 4
        configuration.timeoutIntervalForResource = 8
        configuration.httpCookieStorage = nil
        return URLSession(configuration: configuration, delegate: PinnedTLSDelegate(fingerprint: fingerprint), delegateQueue: nil)
    }

    static func pair(_ invitation: PairingInvitation, phoneName: String) async throws -> SavedMac {
        let session = session(fingerprint: invitation.fingerprint)
        defer { session.invalidateAndCancel() }
        for host in invitation.hosts {
            try Task.checkCancellation()
            guard let base = BridgeEndpoint.url(host: host, port: String(invitation.port)),
                  var parts = URLComponents(url: base, resolvingAgainstBaseURL: false) else { continue }
            parts.scheme = "https"; parts.path = "/api/pairing/claim"; parts.query = nil
            guard let url = parts.url else { continue }
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder().encode(Claim(code: invitation.code, name: phoneName))
            do {
                let (data, response) = try await session.data(for: request)
                guard let response = response as? HTTPURLResponse else { continue }
                guard response.statusCode == 200 else {
                    let message = (try? JSONDecoder().decode(ServerError.self, from: data))?.error ?? "Pairing was declined. Generate a new QR code on the Mac."
                    throw PairingError.unavailable(message)
                }
                return try SavedMac(invitation: invitation, reply: JSONDecoder().decode(PairingReply.self, from: data), connectedHost: host)
            } catch let error as PairingError { throw error }
            catch is CancellationError { throw CancellationError() }
            catch { continue }
        }
        throw PairingError.unavailable("Could not reach or verify this Mac. Keep both devices on the same network and scan its current QR code.")
    }

    static func reachableMac(for mac: SavedMac, discovered: SavedMac? = nil) async throws -> SavedMac {
        let session = session(fingerprint: mac.fingerprint)
        defer { session.invalidateAndCancel() }
        // Race address hints, never claims: the QR's one-use code is submitted only by pair().
        var candidates = mac.hosts.compactMap { mac.relocating(to: $0, port: mac.port) }
        if let discovered { candidates.insert(discovered, at: 0) }
        let verified: SavedMac? = await withTaskGroup(of: SavedMac?.self) { group in
            var urls: Set<URL> = []
            for candidate in candidates {
                guard let host = candidate.hosts.first, let url = candidate.url(host: host, path: "/"),
                      urls.insert(url).inserted else { continue }
                group.addTask {
                    do {
                        let (_, response) = try await session.data(from: url)
                        // Even a 404 verifies reachability, but only after the saved TLS pin matches.
                        return response is HTTPURLResponse ? candidate : nil
                    } catch { return nil }
                }
            }
            for await result in group {
                if let result { group.cancelAll(); return result }
            }
            return nil
        }
        try Task.checkCancellation()
        if let verified { return verified }
        throw PairingError.unavailable("Could not reach or verify your Mac. Open Motion Air.command and use the same Wi-Fi or Personal Hotspot. On Wi-Fi, avoid a guest network and check that the router allows devices to connect to each other. If the Mac was reset, scan its new QR code.")
    }

    private struct Claim: Encodable { let code: String; let name: String }
    private struct ServerError: Decodable { let error: String }
}
