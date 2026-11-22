import Foundation

struct RemotePushSettings {
    var enabled = false
    var relayURL = ""
    var secret = ""
}

enum RemotePushStore {
    private static let enabledKey = "farm_ios_remote_push_enabled"
    private static let urlKey = "farm_ios_remote_push_url"
    private static let secretKey = "farm_ios_remote_push_secret"

    static var settings: RemotePushSettings {
        let d = UserDefaults.standard
        return RemotePushSettings(enabled: d.bool(forKey: enabledKey),
                                  relayURL: d.string(forKey: urlKey) ?? "",
                                  secret: d.string(forKey: secretKey) ?? "")
    }

    static func save(_ settings: RemotePushSettings) {
        let d = UserDefaults.standard
        d.set(settings.enabled, forKey: enabledKey)
        d.set(settings.relayURL, forKey: urlKey)
        d.set(settings.secret, forKey: secretKey)
    }

    static var deviceId: String {
        let key = "farm_ios_push_device_id"
        if let existing = UserDefaults.standard.string(forKey: key), !existing.isEmpty { return existing }
        let value = UUID().uuidString
        UserDefaults.standard.set(value, forKey: key)
        return value
    }
}

enum PushRelayClient {
    static func normalize(_ raw: String) -> String? {
        var text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return nil }
        if !text.contains("://") { text = "http://" + text }
        guard let url = URL(string: text), let host = url.host, !host.isEmpty else { return nil }
        var result = url.absoluteString
        while result.hasSuffix("/") { result.removeLast() }
        return result
    }

    static func register(settings: RemotePushSettings, token: String, accountId: String,
                         farmServerURL: String, bundleId: String) async throws -> String {
        let body: [String: Any] = [
            "token": token,
            "deviceId": RemotePushStore.deviceId,
            "platform": "ios",
            "accountId": accountId,
            "farmServerUrl": farmServerURL,
            "bundleId": bundleId,
        ]
        let json = try await request(settings: settings, path: "/api/devices/register", body: body)
        return json.str("message").isEmpty ? "远程推送已注册" : json.str("message")
    }

    static func unregister(settings: RemotePushSettings, token: String) async throws {
        let body: [String: Any] = ["token": token, "deviceId": RemotePushStore.deviceId]
        _ = try await request(settings: settings, path: "/api/devices/unregister", body: body)
    }

    static func test(settings: RemotePushSettings, token: String) async throws -> String {
        let body: [String: Any] = ["token": token, "deviceId": RemotePushStore.deviceId]
        let json = try await request(settings: settings, path: "/api/test", body: body)
        return json.str("message").isEmpty ? "测试推送已发送" : json.str("message")
    }

    private static func request(settings: RemotePushSettings, path: String, body: [String: Any]) async throws -> JSONValue {
        guard let base = normalize(settings.relayURL), let url = URL(string: base + path) else {
            throw APIError(message: "请填写有效的推送中继地址")
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("FarmMobile-iOS", forHTTPHeaderField: "User-Agent")
        if !settings.secret.isEmpty {
            request.setValue(settings.secret, forHTTPHeaderField: "x-relay-secret")
        }
        request.httpBody = JSONBody.data(body)
        let (data, response) = try await URLSession.shared.data(for: request)
        let http = response as? HTTPURLResponse
        let root = (try? JSONSerialization.jsonObject(with: data)).map { JSONValue(any: $0) } ?? .null
        if let http, http.statusCode >= 400 || root.has("ok") && !root.flag("ok", true) {
            let message = root.str("error").isEmpty ? root.str("message") : root.str("error")
            throw APIError(message: message.isEmpty ? "推送中继请求失败 (\(http.statusCode))" : message,
                           httpCode: http.statusCode)
        }
        return root
    }
}
