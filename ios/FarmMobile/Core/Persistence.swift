import Foundation
import Security

struct ServerProfile: Identifiable, Codable, Equatable {
    var id: String
    var name: String
    var serverURL: String
    var authMode: String
    var username: String
    var currentAccountId: String
    var token: String = ""

    init(id: String = UUID().uuidString,
         name: String = "",
         serverURL: String,
         authMode: String = "password",
         username: String = "",
         currentAccountId: String = "") {
        self.id = id
        self.name = name
        self.serverURL = serverURL
        self.authMode = authMode
        self.username = username
        self.currentAccountId = currentAccountId
    }

    enum CodingKeys: String, CodingKey {
        case id, name, serverURL, authMode, username, currentAccountId
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        serverURL = try c.decodeIfPresent(String.self, forKey: .serverURL) ?? ""
        authMode = try c.decodeIfPresent(String.self, forKey: .authMode) ?? "password"
        username = try c.decodeIfPresent(String.self, forKey: .username) ?? ""
        currentAccountId = try c.decodeIfPresent(String.self, forKey: .currentAccountId) ?? ""
        token = ""
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(name, forKey: .name)
        try c.encode(serverURL, forKey: .serverURL)
        try c.encode(authMode, forKey: .authMode)
        try c.encode(username, forKey: .username)
        try c.encode(currentAccountId, forKey: .currentAccountId)
    }

    var isAuthenticated: Bool {
        !serverURL.trimmingCharacters(in: .whitespaces).isEmpty && !token.isEmpty
    }
    var displayName: String {
        if !name.isEmpty { return name }
        guard let host = URLComponents(string: serverURL)?.host else { return serverURL }
        if let port = URLComponents(string: serverURL)?.port { return "\(host):\(port)" }
        return host
    }
}

struct AppPrefs: Codable {
    var activeProfileId: String = ""
    var themeMode: String = "system"
    var customAppearance = CustomAppearanceConfig()
    var activityPushEnabled: Bool = false
    var offlineAlertsEnabled: Bool = false
    var enabledNotifyTypes: [String] = ["farm_harvest", "friend_steal", "ranch_harvest", "warehouse_sell"]

    init() {}

    enum CodingKeys: String, CodingKey {
        case activeProfileId, themeMode, customAppearance, activityPushEnabled, offlineAlertsEnabled, enabledNotifyTypes
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        activeProfileId = try c.decodeIfPresent(String.self, forKey: .activeProfileId) ?? ""
        themeMode = try c.decodeIfPresent(String.self, forKey: .themeMode) ?? "system"
        customAppearance = try c.decodeIfPresent(CustomAppearanceConfig.self, forKey: .customAppearance) ?? CustomAppearanceConfig()
        activityPushEnabled = try c.decodeIfPresent(Bool.self, forKey: .activityPushEnabled) ?? false
        offlineAlertsEnabled = try c.decodeIfPresent(Bool.self, forKey: .offlineAlertsEnabled) ?? false
        enabledNotifyTypes = try c.decodeIfPresent([String].self, forKey: .enabledNotifyTypes)
            ?? ["farm_harvest", "friend_steal", "ranch_harvest", "warehouse_sell"]
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(activeProfileId, forKey: .activeProfileId)
        try c.encode(themeMode, forKey: .themeMode)
        try c.encode(customAppearance, forKey: .customAppearance)
        try c.encode(activityPushEnabled, forKey: .activityPushEnabled)
        try c.encode(offlineAlertsEnabled, forKey: .offlineAlertsEnabled)
        try c.encode(enabledNotifyTypes, forKey: .enabledNotifyTypes)
    }
}

enum KeychainStore {
    private static let service = "io.github.ccpopy.farmmobile.ios"

    static func set(_ value: String, for account: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(add as CFDictionary, nil)
    }

    static func get(_ account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func delete(_ account: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
    }
}

enum LocalStore {
    private static let key = "farm_mobile_ios_state_v1"
    // Unsigned/sideloaded builds may not have a usable keychain access group.
    // Keep a UserDefaults fallback so a previously authorized server can still
    // reconnect after the app is terminated and relaunched.
    private static let tokenFallbackKey = "farm_mobile_ios_token_fallback_v1"

    struct State: Codable {
        var profiles: [ServerProfile] = []
        var prefs = AppPrefs()

        init() {}

        enum CodingKeys: String, CodingKey { case profiles, prefs }

        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            profiles = try c.decodeIfPresent([ServerProfile].self, forKey: .profiles) ?? []
            prefs = try c.decodeIfPresent(AppPrefs.self, forKey: .prefs) ?? AppPrefs()
        }
    }

    static func load() -> State {
        guard let data = UserDefaults.standard.data(forKey: key),
              var state = try? JSONDecoder().decode(State.self, from: data) else {
            return State()
        }
        var fallback = UserDefaults.standard.dictionary(forKey: tokenFallbackKey) as? [String: String] ?? [:]
        state.profiles = state.profiles.map { profile in
            var p = profile
            if let token = KeychainStore.get(profile.id), !token.isEmpty {
                p.token = token
                fallback[profile.id] = token
            } else {
                p.token = fallback[profile.id] ?? ""
            }
            return p
        }
        let activeIds = Set(state.profiles.map(\.id))
        fallback = fallback.filter { activeIds.contains($0.key) }
        UserDefaults.standard.set(fallback, forKey: tokenFallbackKey)
        return state
    }

    static func save(profiles: [ServerProfile], prefs: AppPrefs) {
        var clean = profiles
        var fallbackTokens: [String: String] = [:]
        for i in clean.indices {
            let token = clean[i].token
            if token.isEmpty {
                KeychainStore.delete(clean[i].id)
            } else {
                KeychainStore.set(token, for: clean[i].id)
                fallbackTokens[clean[i].id] = token
            }
            clean[i].token = ""
        }
        var state = State()
        state.profiles = clean
        state.prefs = prefs
        if let data = try? JSONEncoder().encode(state) {
            UserDefaults.standard.set(data, forKey: key)
        }
        UserDefaults.standard.set(fallbackTokens, forKey: tokenFallbackKey)
    }
}
