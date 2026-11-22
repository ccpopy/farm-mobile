import Foundation
import Network

struct APIError: LocalizedError, Equatable {
    var message: String
    var httpCode: Int = 0
    var errorCode: String = ""
    var errorType: String = ""
    var remainingMs: Int64 = 0
    var errorDescription: String? { message }
}

struct LoginResult {
    var ok: Bool
    var token = ""
    var username = ""
    var mustChangePassword = false
    var error = ""
    var errorType = ""
    var remainingMs: Int64 = 0
}

private final class RedirectFollowerDelegate: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        guard let scheme = request.url?.scheme?.lowercased(), ["http", "https"].contains(scheme) else {
            completionHandler(nil)
            return
        }
        var next = request
        if let original = task.originalRequest {
            // Some self-hosted deployments redirect http -> https with 301/302.
            // URLSession normally changes POST to GET in that case; restore the
            // original method/body so login and settings saves still work.
            if original.httpMethod != "GET" && original.httpMethod != "HEAD" && next.httpMethod != original.httpMethod {
                next.httpMethod = original.httpMethod
                next.httpBody = original.httpBody
            }
            if next.httpBody == nil, let body = original.httpBody {
                next.httpBody = body
            }
            for (key, value) in original.allHTTPHeaderFields ?? [:] where next.value(forHTTPHeaderField: key) == nil {
                next.setValue(value, forHTTPHeaderField: key)
            }
        }
        completionHandler(next)
    }
}

final class FarmAPI {
    private(set) var baseURL = ""
    private(set) var token = ""
    private(set) var accountId = ""
    var onUnauthorized: ((String) -> Void)?

    private let redirectDelegate = RedirectFollowerDelegate()
    private let session: URLSession

    init() {
        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 60
        cfg.timeoutIntervalForResource = 90
        cfg.httpShouldSetCookies = false
        cfg.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = URLSession(configuration: cfg, delegate: redirectDelegate, delegateQueue: nil)
    }

    func configure(serverURL: String, token: String, accountId: String) {
        baseURL = Self.normalizeBaseURL(serverURL) ?? ""
        self.token = token.trimmingCharacters(in: .whitespacesAndNewlines)
        self.accountId = accountId.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func setAccount(_ id: String) { accountId = id }

    static func normalizeBaseURL(_ raw: String) -> String? {
        var text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return nil }
        if !text.contains("://") { text = "http://" + text }
        guard var comps = URLComponents(string: text),
              let scheme = comps.scheme?.lowercased(),
              ["http", "https"].contains(scheme),
              let host = comps.host, !host.isEmpty,
              comps.user == nil, comps.password == nil, comps.query == nil, comps.fragment == nil,
              comps.url != nil else { return nil }
        comps.scheme = scheme
        guard let normalizedURL = comps.url else { return nil }
        var result = normalizedURL.absoluteString
        while result.hasSuffix("/") { result.removeLast() }
        return result
    }

    static func q(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .alphanumerics.union(CharacterSet(charactersIn: "-._~"))) ?? value
    }

    private func normalizeMessage(_ raw: String) -> String {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return "" }
        let pattern = #"^[A-Za-z0-9_-]+\.[A-Za-z0-9._-]+.*\bcode=\d+\b"#
        if let regex = try? NSRegularExpression(pattern: pattern),
           let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
           let range = Range(match.range, in: text) {
            let rest = String(text[range.upperBound...]).trimmingCharacters(in: .whitespacesAndNewlines)
            if !rest.isEmpty { return rest }
        }
        return text
    }

    func request(_ method: String,
                 _ path: String,
                 body: [String: Any]? = nil,
                 accountOverride: String? = nil,
                 tokenOverride: String? = nil,
                 baseOverride: String? = nil) async throws -> JSONValue {
        let targetBase: String
        if let baseOverride {
            guard let normalized = Self.normalizeBaseURL(baseOverride) else { throw APIError(message: "请输入有效的服务器地址") }
            targetBase = normalized
        } else {
            targetBase = baseURL
        }
        guard !targetBase.isEmpty else { throw APIError(message: "请先配置远程服务器地址") }
        let fullURL = path.hasPrefix("/") ? targetBase + path : targetBase + "/" + path
        guard let url = URL(string: fullURL) else { throw APIError(message: "服务器地址无效") }

        let httpMethod = method.uppercased()
        var req = URLRequest(url: url)
        req.httpMethod = httpMethod
        let effectiveToken = tokenOverride ?? (baseOverride == nil ? token : "")
        if !effectiveToken.isEmpty {
            req.setValue(effectiveToken, forHTTPHeaderField: "x-admin-token")
        }
        let effectiveAccount = accountOverride ?? (baseOverride == nil ? accountId : "")
        if !effectiveAccount.isEmpty {
            req.setValue(effectiveAccount, forHTTPHeaderField: "x-account-id")
        }
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            req.httpBody = JSONBody.data(body)
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        } else if httpMethod == "POST" || httpMethod == "PUT" {
            req.httpBody = Data("{}".utf8)
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        } else if httpMethod == "DELETE" {
            req.httpBody = Data()
        }

        var data: Data
        var response: URLResponse
        if url.scheme?.lowercased() == "http" {
            // ATS 会在 URLSession 层直接拦截明文 http（NSURLError -1022），
            // 而且 NSAllowsArbitraryLoads 会被同一字典里的其它键覆盖。
            // 自托管服务通常只有 http://IP:80，这里直接使用 Network.framework
            // 发送原始 HTTP 请求，彻底绕开 ATS，可同时用于 REST 与轮询。
            do {
                let raw = try await RawHTTPClient.request(
                    method: httpMethod,
                    url: url,
                    headers: req.allHTTPHeaderFields ?? [:],
                    body: req.httpBody
                )
                data = raw.body
                guard let httpResponse = HTTPURLResponse(
                    url: url,
                    statusCode: raw.statusCode,
                    httpVersion: "HTTP/1.1",
                    headerFields: raw.headers
                ) else {
                    throw APIError(message: "HTTP 服务器响应无效")
                }
                response = httpResponse
            } catch let rawError as APIError {
                throw rawError
            } catch {
                throw APIError(message: "无法连接到服务器，请检查地址与网络 (\(error.localizedDescription))")
            }
        } else {
            do {
                let pair = try await session.data(for: req)
                data = pair.0
                response = pair.1
            } catch let error as NSError {
                throw APIError(message: "无法连接到服务器，请检查地址与网络 (\(error.localizedDescription))")
            }
        }
        guard let http = response as? HTTPURLResponse else { throw APIError(message: "服务器响应无效") }
        if (300...399).contains(http.statusCode) {
            throw APIError(message: "服务器地址发生重定向，请填写最终访问地址", httpCode: http.statusCode)
        }

        var root: JSONValue = .null
        let trimmed = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.hasPrefix("[") {
            if let obj = try? JSONSerialization.jsonObject(with: data) {
                root = .object(["_array": JSONValue(any: obj), "ok": .bool(http.statusCode < 400)])
            }
        } else if !trimmed.isEmpty {
            if let obj = try? JSONSerialization.jsonObject(with: data) {
                root = JSONValue(any: obj)
            } else if http.statusCode >= 400 {
                throw APIError(message: "服务器返回异常 (\(http.statusCode))", httpCode: http.statusCode)
            } else {
                throw APIError(message: "服务器响应格式无效")
            }
        } else {
            root = .object(["ok": .bool(http.statusCode < 400)])
        }

        if http.statusCode == 401 && baseOverride == nil && tokenOverride == nil && !token.isEmpty {
            onUnauthorized?(effectiveToken)
        }
        if http.statusCode >= 400 || root.has("ok") && !root.flag("ok", true) {
            let msg = root.str("errorMessage").isEmpty
                ? (root.str("error").isEmpty ? (root.str("message").isEmpty ? "请求失败 (\(http.statusCode))" : root.str("message")) : root.str("error"))
                : root.str("errorMessage")
            throw APIError(message: normalizeMessage(msg), httpCode: http.statusCode,
                           errorCode: root.str("errorCode").isEmpty ? root.str("code") : root.str("errorCode"),
                           errorType: root.str("errorType"), remainingMs: root.l("remainingMs"))
        }
        return root
    }

    // MARK: Auth & Accounts

    func fetchGameVersion(baseOverride: String? = nil) async throws -> ServerVersionInfo {
        let json = try await request("GET", "/api/game-version", baseOverride: baseOverride)
        let info = ServerVersionInfo(json)
        if info.botVersion.isEmpty { throw APIError(message: "该地址未返回 QQ 农场助手版本信息") }
        return info
    }

    func login(base: String, username: String, password: String) async -> LoginResult {
        do {
            let json = try await request("POST", "/api/login",
                                         body: ["username": username.trimmingCharacters(in: .whitespacesAndNewlines), "password": password],
                                         tokenOverride: "", baseOverride: base)
            let data = json.obj("data") ?? .null
            let user = data.obj("user") ?? .null
            return LoginResult(ok: true, token: data.str("token"),
                               username: user.str("username").isEmpty ? username : user.str("username"),
                               mustChangePassword: data.flag("mustChangePassword"))
        } catch let e as APIError {
            return LoginResult(ok: false, error: e.message, errorType: e.errorType, remainingMs: e.remainingMs)
        } catch {
            return LoginResult(ok: false, error: error.localizedDescription)
        }
    }

    func validateToken(baseOverride: String? = nil, tokenOverride: String? = nil) async -> Bool {
        do {
            let json = try await request("GET", "/api/auth/validate", tokenOverride: tokenOverride ?? "", baseOverride: baseOverride)
            return json.obj("data")?.flag("valid") == true
        } catch { return false }
    }

    func fetchUserMe(baseOverride: String? = nil, tokenOverride: String? = nil) async -> String {
        do {
            let json = try await request("GET", "/api/user/me", tokenOverride: tokenOverride ?? "", baseOverride: baseOverride)
            return json.obj("data")?.str("username") ?? ""
        } catch { return "" }
    }

    func logout() async { _ = try? await request("POST", "/api/logout") }

    func fetchAccounts() async throws -> [AccountItem] {
        let json = try await request("GET", "/api/accounts")
        return (json.obj("data")?.arr("accounts") ?? []).map(AccountItem.init)
    }

    func startAccount(_ id: String) async throws { _ = try await request("POST", "/api/accounts/\(Self.q(id))/start", accountOverride: id) }
    func stopAccount(_ id: String) async throws { _ = try await request("POST", "/api/accounts/\(Self.q(id))/stop") }
    func deleteAccount(_ id: String) async throws { _ = try await request("DELETE", "/api/accounts/\(Self.q(id))") }

    func updateAccount(id: String, name: String, code: String) async throws -> String? {
        var body: [String: Any] = ["id": id]
        if code.isEmpty {
            body["remark"] = name
            _ = try await request("POST", "/api/account/remark", body: body, accountOverride: id)
            return nil
        }
        body["name"] = name
        body["code"] = code
        let response = try await request("POST", "/api/accounts", body: body, accountOverride: id)
        guard let data = response.obj("data") else { throw APIError(message: "服务器未确认账号更新") }
        let exists = (data.arr("accounts").map(AccountItem.init)).contains { $0.id == id }
        if !exists { throw APIError(message: "账号已不存在，请刷新账号列表", httpCode: 404) }
        do { try await startAccount(id); return nil }
        catch let e as APIError { return e.message }
    }

    func saveAccount(name: String, platform: String, code: String, loginType: String = "manual", wxLoginTaskId: String? = nil) async throws {
        var body: [String: Any] = ["name": name, "platform": platform, "loginType": loginType]
        if !code.isEmpty { body["code"] = code }
        if let wxLoginTaskId, !wxLoginTaskId.isEmpty { body["wxLoginTaskId"] = wxLoginTaskId }
        _ = try await request("POST", "/api/accounts", body: body)
    }

    func fetchLoginConfig() async throws -> LoginConfigModel {
        let json = try await request("GET", "/api/settings/login-config")
        return LoginConfigModel(json.obj("data") ?? .null)
    }

    func createWxQrTask() async throws -> WxQrTask {
        let json = try await request("POST", "/api/wx-login/tasks", body: [:])
        return WxQrTask(json.obj("data") ?? .null)
    }

    func pollWxQrStatus(_ taskId: String) async throws -> WxQrTask {
        let json = try await request("GET", "/api/wx-login/tasks/\(Self.q(taskId))/status")
        return WxQrTask(json.obj("data") ?? .null)
    }

    func confirmWxQr(_ taskId: String) async throws {
        _ = try await request("POST", "/api/wx-login/tasks/\(Self.q(taskId))/confirm", body: [:])
    }

    func claimWxQrCode(_ taskId: String) async throws -> String {
        let json = try await request("POST", "/api/wx-login/tasks/\(Self.q(taskId))/code", body: [:])
        return json.obj("data")?.str("code") ?? ""
    }

    func cancelWxQrTask(_ taskId: String) async {
        _ = try? await request("DELETE", "/api/wx-login/tasks/\(Self.q(taskId))")
    }

    func createQqQrTask() async throws -> QqQrTask {
        let json = try await request("POST", "/api/qq-login/tasks", body: [:])
        return QqQrTask(json.obj("data") ?? .null)
    }

    func pollQqQrStatus(_ taskId: String) async throws -> QqQrTask {
        let json = try await request("POST", "/api/qq-login/tasks/\(Self.q(taskId))/status", body: [:])
        return QqQrTask(json.obj("data") ?? .null)
    }

    func claimQqQrCode(_ taskId: String) async throws -> String {
        let json = try await request("POST", "/api/qq-login/tasks/\(Self.q(taskId))/code", body: [:])
        return json.obj("data")?.str("code") ?? ""
    }

    func cancelQqQrTask(_ taskId: String) async {
        _ = try? await request("POST", "/api/qq-login/tasks/\(Self.q(taskId))/cancel", body: [:])
    }

    // MARK: Overview

    func fetchStatus(accountId: String) async throws -> StatusSnapshot {
        let json = try await request("GET", "/api/status", accountOverride: accountId)
        return StatusSnapshot(accountId: accountId, data: json.obj("data") ?? .null, serverTimeHint: json.l("serverTime"))
    }

    func fetchDiamond(accountId: String) async throws -> Int64 {
        let json = try await request("GET", "/api/diamond", accountOverride: accountId)
        return json.obj("data")?.l("diamond") ?? 0
    }

    func fetchDailyGifts(accountId: String) async throws -> DailyGiftsSummary {
        let json = try await request("GET", "/api/daily-gifts", accountOverride: accountId)
        return DailyGiftsSummary(json.obj("data") ?? .null)
    }

    func fetchLogs(accountId: String, limit: Int = 120, timeFrom: String = "") async throws -> [RunLogEntry] {
        var params = ["limit=\(limit)"]
        if accountId.isEmpty || accountId == "all" { params.append("accountId=all") }
        if !timeFrom.isEmpty { params.append("timeFrom=\(Self.q(timeFrom))") }
        let json = try await request("GET", "/api/logs?" + params.joined(separator: "&"),
                                     accountOverride: (accountId.isEmpty || accountId == "all") ? nil : accountId)
        return json.arr("data").map { RunLogEntry($0) }
    }

    func clearLogs(accountId: String) async throws {
        _ = try await request("DELETE", "/api/logs", accountOverride: accountId)
    }

    // MARK: Farm / Bag / Pets

    func fetchLands(accountId: String) async throws -> LandsPayload {
        let json = try await request("GET", "/api/lands", accountOverride: accountId)
        return LandsPayload(json.obj("data") ?? .null)
    }

    func operateFarm(accountId: String, op: String, landId: Int? = nil) async throws -> String {
        var body: [String: Any] = ["opType": op]
        if let landId { body["landId"] = landId }
        let json = try await request("POST", "/api/farm/operate", body: body, accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let hadWork = data.flag("hadWork")
        if op == "clear" && landId != nil { return hadWork ? "护理完成" : "当前无需操作" }
        let actions = data.arr("actions").compactMap(\.stringValue).map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        if !actions.isEmpty { return actions.joined(separator: " · ") }
        return hadWork ? "操作完成" : "当前无需操作"
    }

    func fertilizeLand(accountId: String, landId: Int, type: String) async throws {
        _ = try await request("POST", "/api/farm/fertilize", body: ["landId": landId, "fertilizerType": type], accountOverride: accountId)
    }

    func fertilizeAll(accountId: String, type: String) async throws -> Int {
        let json = try await request("POST", "/api/farm/fertilize-all", body: ["fertilizerType": type], accountOverride: accountId)
        return json.obj("data")?.i("successCount") ?? 0
    }

    func fetchBag(accountId: String) async throws -> BagPayload {
        let json = try await request("GET", "/api/bag", accountOverride: accountId)
        return BagPayload(json.obj("data") ?? .null)
    }

    func useBagItem(accountId: String, item: BagItem, count: Int) async throws -> String {
        let json = try await request("POST", "/api/bag/use",
                                     body: ["itemId": item.id, "count": count, "uid": item.uid],
                                     accountOverride: accountId)
        let rewards = json.obj("data")?.arr("rewards") ?? []
        let text = rewards.map { "\($0.str("name")) ×\($0.l("count"))" }.joined(separator: "、")
        return text.isEmpty ? "使用成功" : text
    }

    func sellBagItem(accountId: String, item: BagItem, count: Int64) async throws {
        let entry: [String: Any] = ["id": item.id, "count": count, "uid": item.uid]
        _ = try await request("POST", "/api/bag/sell", body: ["items": [entry]], accountOverride: accountId)
    }

    func lockBagItems(accountId: String, uids: [Int64], locked: Bool) async throws {
        _ = try await request("POST", "/api/bag/lock", body: ["itemUids": uids, "locked": locked], accountOverride: accountId)
    }

    func fetchPets(accountId: String) async throws -> PetSnapshot {
        let json = try await request("GET", "/api/pets", accountOverride: accountId)
        return PetSnapshot(json.obj("data") ?? .null)
    }

    func deployDog(accountId: String, dogId: Int) async throws -> PetSnapshot {
        let json = try await request("POST", "/api/pets/deploy", body: ["dogId": dogId], accountOverride: accountId)
        return PetSnapshot(json.obj("data") ?? .null)
    }

    func withdrawDog(accountId: String) async throws -> PetSnapshot {
        let json = try await request("POST", "/api/pets/withdraw", body: [:], accountOverride: accountId)
        return PetSnapshot(json.obj("data") ?? .null)
    }

    func useDogFood(accountId: String, itemId: Int, count: Int) async throws -> PetSnapshot {
        let json = try await request("POST", "/api/pets/food/use", body: ["itemId": itemId, "count": count, "uid": 0], accountOverride: accountId)
        return PetSnapshot(json.obj("data") ?? .null)
    }

    func claimDogGifts(accountId: String) async throws -> Int {
        let json = try await request("POST", "/api/dog/skill-gifts/claim", body: [:], accountOverride: accountId)
        return json.obj("data")?.i("pending") ?? 0
    }

    // MARK: Ranch

    func fetchRanchView(accountId: String, view: String, shopId: Int? = nil, hostGid: String? = nil) async throws -> RanchViewSnapshot {
        var params = ["view=\(Self.q(view))"]
        if let shopId { params.append("shopId=\(shopId)") }
        if let hostGid, !hostGid.isEmpty { params.append("hostGid=\(Self.q(hostGid))") }
        do {
            let json = try await request("GET", "/api/ranch?" + params.joined(separator: "&"), accountOverride: accountId)
            guard let data = json.obj("data") else { throw APIError(message: "服务器未返回牧场数据") }
            return RanchViewSnapshot(data)
        } catch let e as APIError where e.httpCode == 200 && e.message == "未知牧场页面" {
            // Older servers only know the legacy view names. Do not fall back
            // for every unknown view: passing "animals" back into this same
            // method used to recurse forever when a server rejected it.
            let legacy: String
            switch view {
            case "mall": legacy = "cans"
            case "overview": legacy = "animals"
            default: throw e
            }
            return try await fetchRanchView(accountId: accountId, view: legacy, shopId: shopId, hostGid: hostGid)
        }
    }

    func ranchDirect(accountId: String, action: String, params: [String: Any] = [:]) async throws -> String {
        var body = params
        body["direct"] = true
        let json = try await request("POST", "/api/ranch/\(Self.q(action))", body: body, accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let notice = data.str("notice")
        if !notice.isEmpty { return notice }
        return data.str("status") == "skipped" ? "当前没有可操作项目" : "操作完成"
    }

    func quoteRanch(accountId: String, action: String, input: [String: Any] = [:]) async throws -> RanchQuote {
        let json = try await request("POST", "/api/ranch/\(Self.q(action))/quote", body: input, accountOverride: accountId)
        let quote = RanchQuote(json.obj("data") ?? .null)
        if !quote.skipped && quote.signature.isEmpty { throw APIError(message: "牧场报价缺少确认凭据，请刷新后重试") }
        return quote
    }

    func executeRanch(accountId: String, action: String, input: [String: Any], signature: String) async throws -> RanchOperateResult {
        var body = input
        body["expected"] = ["signature": signature]
        let json = try await request("POST", "/api/ranch/\(Self.q(action))", body: body, accountOverride: accountId)
        let result = RanchOperateResult(json.obj("data") ?? .null)
        if result.status != "confirmed" && result.status != "skipped" {
            throw APIError(message: "牧场操作结果未确认，请刷新核对", errorCode: "RANCH_RESULT_UNKNOWN")
        }
        return result
    }

    // MARK: Friends

    func fetchFriends(accountId: String, forceSync: Bool = false) async throws -> [FriendItem] {
        let path = forceSync ? "/api/friends?forceSync=true" : "/api/friends"
        let json = try await request("GET", path, accountOverride: accountId)
        if let status = json.obj("status"), let state = status.str("state").nilIfEmpty, state != "ready" {
            let err = status.str("error")
            if !err.isEmpty { throw APIError(message: err) }
        }
        return json.arr("data").map(FriendItem.init)
    }

    func fetchFriendLands(accountId: String, gid: Int64) async throws -> FriendLandsPayload {
        let json = try await request("GET", "/api/friend/\(gid)/lands", accountOverride: accountId)
        return FriendLandsPayload(json.obj("data") ?? .null)
    }

    func operateFriend(accountId: String, gid: Int64, op: String) async throws -> String {
        let json = try await request("POST", "/api/friend/\(gid)/op", body: ["opType": op], accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let msg = data.str("message")
        if !data.flag("ok", true) {
            throw APIError(message: msg.isEmpty ? "好友农场操作失败" : msg)
        }
        return msg.isEmpty ? "操作已执行" : msg
    }

    func fetchInteractRecords(accountId: String) async throws -> [InteractRecord] {
        let json = try await request("GET", "/api/interact-records", accountOverride: accountId)
        return json.arr("data").enumerated().map { InteractRecord($0.element, index: $0.offset) }
    }

    func fetchBlacklist(accountId: String) async throws -> [FriendBlacklistItem] {
        let json = try await request("GET", "/api/friend-blacklist", accountOverride: accountId)
        return json.arr("data").map(FriendBlacklistItem.init)
    }

    func toggleBlacklist(accountId: String, gid: Int64) async throws -> [FriendBlacklistItem] {
        let json = try await request("POST", "/api/friend-blacklist/toggle", body: ["gid": gid], accountOverride: accountId)
        return json.arr("data").map(FriendBlacklistItem.init)
    }

    // MARK: Automation & Settings

    func fetchScheduler(accountId: String) async throws -> SchedulerSummary {
        let json = try await request("GET", "/api/scheduler", accountOverride: accountId)
        return SchedulerSummary(json.obj("data") ?? .null)
    }

    func fetchRanchOptions() async throws -> [RanchAnimalOption] {
        let json = try await request("GET", "/api/ranch/automation-options")
        return json.arr("data").map(RanchAnimalOption.init).filter { $0.value > 0 && !$0.label.isEmpty }
    }

    func fetchBagSeeds(accountId: String) async throws -> [BagPriorityItem] {
        let json = try await request("GET", "/api/bag/seeds", accountOverride: accountId)
        if !json.has("data") { throw APIError(message: "服务器未返回背包种子列表") }
        return json.arr("data").map(BagPriorityItem.init)
    }

    func fetchSeedCatalog(accountId: String) async throws -> [SeedOption] {
        let json = try await request("GET", "/api/seeds", accountOverride: accountId)
        return json.arr("data").map(SeedOption.init)
    }

    func fetchSettings(accountId: String) async throws -> AccountSettings {
        let json = try await request("GET", "/api/settings", accountOverride: accountId)
        return AccountSettings(json.obj("data") ?? .null)
    }

    func saveSettings(accountId: String, model: AccountSettings) async -> (Bool, String) {
        let farmMin = max(1, model.farmMinSec)
        let farmMax = max(farmMin, model.farmMaxSec)
        let friendMin = max(1, model.friendMinSec)
        let friendMax = max(friendMin, model.friendMaxSec)

        var intervals = (try? JSONSerialization.jsonObject(with: Data(model.rawIntervalsJson.utf8))) as? [String: Any] ?? [:]
        intervals["farm"] = farmMin; intervals["farmMin"] = farmMin; intervals["farmMax"] = farmMax
        intervals["friendMin"] = friendMin; intervals["friendMax"] = friendMax

        var automation = (try? JSONSerialization.jsonObject(with: Data(model.rawAutomationJson.utf8))) as? [String: Any] ?? [:]
        automation["fertilizer"] = model.fertilizerMode
        automation["fertilizer_smart_seconds"] = min(3600, max(30, model.fertilizerSmartSeconds))
        automation["fertilizer_land_types"] = model.fertilizerLandTypes
        automation["ranch_strategy"] = model.ranchStrategy
        automation["ranch_bag_fallback"] = model.ranchBagFallback
        // Only send priority lists when they contain data. An empty list is
        // usually caused by a failed /api/bag/seeds or an older server response;
        // sending [] here would silently erase the saved priority order.
        if !model.ranchCoopCubPriority.isEmpty {
            automation["ranch_coop_cub_priority"] = model.ranchCoopCubPriority
        }
        if !model.ranchShedCubPriority.isEmpty {
            automation["ranch_shed_cub_priority"] = model.ranchShedCubPriority
        }
        automation["ranch_coop_animal"] = model.ranchCoopAnimal
        automation["ranch_shed_animal"] = model.ranchShedAnimal
        automation["ranch_grass_threshold_percent"] = min(100, max(1, model.ranchGrassThresholdPercent))
        automation["ranch_check_minutes"] = min(60, max(1, model.ranchCheckMinutes))
        for (k, v) in model.automationBooleans { automation[k] = v }
        func putCanStage(_ key: String, _ after: String?) {
            let before = automation[key] as? String
            if after == nil || after == before {
                automation.removeValue(forKey: key)
            } else if let after {
                automation[key] = after
            }
        }
        putCanStage("ranch_feed_normal_stage", model.ranchFeedNormalStage)
        putCanStage("ranch_feed_organic_stage", model.ranchFeedOrganicStage)

        let quiet: [String: Any] = [
            "enabled": model.quietHoursEnabled,
            "start": model.quietHoursStart,
            "end": model.quietHoursEnd,
            "continueFarm": model.quietContinueFarm,
        ]
        var payload: [String: Any] = [
            "plantingStrategy": model.plantingStrategy,
            "preferredSeedId": model.preferredSeedId,
            "intervals": intervals,
            "friendQuietHours": quiet,
            "stealDelaySeconds": min(300, max(0, model.stealDelaySeconds)),
            "plantOrderRandom": model.plantOrderRandom,
            "plantDelaySeconds": min(60, max(0, model.plantDelaySeconds)),
            "fertilizerBuyOrganicCount": min(10000, max(0, model.fertilizerBuyOrganicCount)),
            "fertilizerBuyOrganicThresholdHours": min(990, max(0, model.fertilizerBuyOrganicThresholdHours)),
            "fertilizerBuyNormalCount": min(10000, max(0, model.fertilizerBuyNormalCount)),
            "fertilizerBuyNormalThresholdHours": min(990, max(0, model.fertilizerBuyNormalThresholdHours)),
            "fertilizerBuyCheckIntervalMinutes": min(1440, max(1, model.fertilizerBuyCheckIntervalMinutes)),
            "autoAcceptFriendMinLevel": min(200, max(0, model.autoAcceptFriendMinLevel)),
            "autoAcceptRequireOwnLevel": model.autoAcceptRequireOwnLevel,
            "autoAcceptHarvestStealEnabled": model.autoAcceptHarvestStealEnabled,
            "autoAcceptHarvestStealHarvest": min(9999, max(0, model.autoAcceptHarvestStealHarvest)),
            "autoAcceptHarvestStealSteal": min(9999, max(1, model.autoAcceptHarvestStealSteal)),
            "automation": automation,
            "bagSeedFallbackStrategy": model.bagSeedFallbackStrategy,
            "bagSeedMultiLandReservationEnabled": model.bagSeedMultiLandReservationEnabled,
        ]
        if !model.bagSeedPriority.isEmpty {
            payload["bagSeedPriority"] = model.bagSeedPriority
        }
        if let hour = model.emailClaimHour { payload["emailClaimHour"] = min(23, max(0, hour)) }
        do {
            _ = try await request("POST", "/api/settings/save", body: payload, accountOverride: accountId)
            return (true, "自动化设置已保存")
        } catch let e as APIError {
            if e.httpCode == 202 { return (false, e.message) }
            return (false, e.message)
        } catch {
            return (false, error.localizedDescription)
        }
    }

    // MARK: Solar Calendar

    func fetchSolarCalendar(accountId: String) async throws -> SolarCalendarState {
        let json = try await request("GET", "/api/solar-calendar", accountOverride: accountId)
        return SolarCalendarState(json.obj("data") ?? .null)
    }

    func operateSolar(accountId: String, action: String, body: [String: Any] = [:]) async throws -> SolarActionResult {
        let json = try await request("POST", "/api/solar-calendar/\(Self.q(action))", body: body, accountOverride: accountId)
        return SolarActionResult(json.obj("data") ?? .null)
    }

    // MARK: Mall

    private func mergeDiamond(accountId: String, catalog: MallCatalog) async -> MallCatalog {
        guard catalog.currencies.contains(where: { $0.id == 1004 }) else { return catalog }
        guard let diamond = try? await fetchDiamond(accountId: accountId) else { return catalog }
        var copy = catalog
        copy.currencies = copy.currencies.map { c in
            var v = c
            if v.id == 1004 { v.count = diamond; v.balanceKnown = true }
            return v
        }
        copy.goods = copy.goods.map { g in
            var v = g
            if v.priceId == 1004 { v.priceBalance = diamond }
            return v
        }
        return copy
    }

    func fetchMallCatalog(accountId: String, slotType: Int, propsShop: Bool = false, subSlotType: Int = 1) async throws -> MallCatalog {
        let path = propsShop ? "/api/props-shop" : "/api/game-mall?slotType=\(slotType)&subSlotType=\(subSlotType)"
        let json = try await request("GET", path, accountOverride: accountId)
        let catalog = MallCatalog(json.obj("data") ?? .null)
        return await mergeDiamond(accountId: accountId, catalog: catalog)
    }

    func purchaseMallGoods(accountId: String, goods: MallGoods, slotType: Int, propsShop: Bool, count: Int) async throws -> MallPurchaseResult {
        let rewards = goods.rewards.map { ["id": $0.id, "count": $0.count] as [String: Any] }
        let expectedPrice: [String: Any] = ["id": goods.priceId, "count": goods.priceCount, "rewards": rewards]
        var payload: [String: Any] = [
            "goodsId": goods.id,
            "expectedPrice": expectedPrice,
            "slotType": slotType,
            "count": count,
        ]
        if propsShop {
            payload["expectedOffer"] = [
                "price": ["id": goods.priceId, "count": goods.priceCount],
                "rewards": rewards,
            ]
        }
        let path = propsShop ? "/api/props-shop/purchase" : "/api/game-mall/purchase"
        let json = try await request("POST", path, body: payload, accountOverride: accountId)
        var result = MallPurchaseResult(json.obj("data") ?? .null)
        if let catalog = result.catalog {
            result.catalog = await mergeDiamond(accountId: accountId, catalog: catalog)
        }
        return result
    }

    func fetchMysteryShop(accountId: String) async throws -> MysteryShopState {
        let json = try await request("GET", "/api/mystery-shop", accountOverride: accountId)
        return MysteryShopState(json.obj("data") ?? .null)
    }

    func purchaseMystery(accountId: String, npcId: Int, expectedOffer: JSONValue) async throws -> CommerceItem? {
        let json = try await request("POST", "/api/mystery-shop/purchase",
                                     body: ["npcId": npcId, "expectedOffer": expectedOffer.foundation],
                                     accountOverride: accountId)
        let purchase = json.obj("data")?.obj("purchase")
        if let reward = purchase?.obj("reward") { return CommerceItem(reward) }
        return nil
    }

    // MARK: Activity Center

    func fetchRaw(accountId: String, path: String) async throws -> JSONValue {
        try await request("GET", path, accountOverride: accountId)
    }

    func fetchActivityDirectory(accountId: String, isRanch: Bool) async throws -> [ActivityDirectoryItem] {
        let path = isRanch ? "/api/activity-center/ranch-activities" : "/api/activity-center/activities"
        let json = try await request("GET", path, accountOverride: accountId)
        return json.obj("data")?.arr("activities").map(ActivityDirectoryItem.init) ?? []
    }

    func fetchMuge(accountId: String) async throws -> MugeState {
        let json = try await request("GET", "/api/activities/muge", accountOverride: accountId)
        return MugeState(json.obj("data") ?? .null)
    }

    func operateMuge(accountId: String, action: String, body: [String: Any] = [:]) async throws -> (MugeState?, String) {
        let json = try await request("POST", "/api/activities/muge/\(Self.q(action))", body: body, accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let state = data.obj("activity").map { MugeState($0) }
        let rewards = CommerceItem.list(data.arr("rewards"))
        let text = rewards.map(\.text).joined(separator: "、")
        let msg = text.isEmpty ? (action == "settle" ? "牧场人格已生成" : "操作已完成") : text
        let suffix = data.flag("refreshRequired") ? "（请刷新确认最新进度）" : ""
        return (state, msg + suffix)
    }

    func fetchShareReward(accountId: String, activityId: String) async throws -> ShareRewardState {
        let json = try await request("GET", "/api/activities/share-reward/\(Self.q(activityId))", accountOverride: accountId)
        return ShareRewardState(json.obj("data") ?? .null)
    }

    func operateShareReward(accountId: String, activityId: String, action: String, body: [String: Any] = [:]) async throws -> (ShareRewardState?, String) {
        let json = try await request("POST", "/api/activities/share-reward/\(Self.q(activityId))/\(Self.q(action))",
                                     body: body, accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let state = data.obj("activity").map { ShareRewardState($0) }
        let rewards = CommerceItem.list(data.arr("rewards"))
        let text = rewards.map(\.text).joined(separator: "、")
        let score = data.obj("result")?.l("granted_score") ?? 0
        let msg = text.isEmpty ? (score > 0 ? "获得 \(score) 快乐值" : "领取成功") : text
        return (state, msg + (data.flag("refreshRequired") ? "（请刷新确认最新进度）" : ""))
    }

    func fetchAutumnWish(accountId: String) async throws -> AutumnWishState {
        let json = try await request("GET", "/api/activities/autumn/wish", accountOverride: accountId)
        return AutumnWishState(json.obj("data") ?? .null)
    }

    func operateAutumnWish(accountId: String, action: String, body: [String: Any] = [:]) async throws -> (AutumnWishState?, String) {
        let json = try await request("POST", "/api/activities/autumn/wish/\(Self.q(action))", body: body, accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let state = data.obj("activity").map { AutumnWishState($0) }
        let rewards = CommerceItem.list(data.arr("rewards"))
        let text = rewards.map(\.text).joined(separator: "、")
        let msg = text.isEmpty ? (action == "draw" ? "签文已揭晓，请领取好运奖励" : "操作已完成") : text
        return (state, msg + (data.flag("refreshRequired") ? "（请刷新确认最新进度）" : ""))
    }

    func fetchStellar(accountId: String) async throws -> StellarSnapshot {
        let json = try await request("GET", "/api/activity-center/stellar", accountOverride: accountId)
        return StellarSnapshot(json.obj("data") ?? .null)
    }

    func simpleActivityAction(accountId: String, path: String, body: [String: Any] = [:]) async throws -> String {
        let json = try await request("POST", path, body: body, accountOverride: accountId)
        let rewards = CommerceItem.list(json.obj("data")?.arr("rewards") ?? [])
        let text = rewards.map(\.text).joined(separator: "、")
        return text.isEmpty ? "操作完成" : text
    }

    func claimStellarPass(accountId: String) async throws -> String {
        try await simpleActivityAction(accountId: accountId, path: "/api/activity-center/pass/claim")
    }

    func lightStellarConstellation(accountId: String) async throws -> String {
        try await simpleActivityAction(accountId: accountId, path: "/api/activity-center/constellation/light")
    }

    func exchangeStarSand(accountId: String, goodsId: String, count: Int) async throws -> String {
        try await simpleActivityAction(accountId: accountId, path: "/api/activity-center/shop/exchange",
                                       body: ["goodsId": goodsId, "count": count])
    }

    func claimStellarTerm(accountId: String, termId: String) async throws -> String {
        try await simpleActivityAction(accountId: accountId,
                                       path: "/api/activity-center/solar-terms/\(Self.q(termId))/claim")
    }

    func fetchQixi(accountId: String) async throws -> QixiState {
        let json = try await request("GET", "/api/activity-center/qixi", accountOverride: accountId)
        return QixiState(json.obj("data") ?? .null)
    }

    func claimQixiBridge(accountId: String) async throws -> String {
        try await simpleActivityAction(accountId: accountId, path: "/api/activity-center/qixi/bridge/claim")
    }

    func giftQixiSachet(accountId: String, friendGid: String, messageTextId: Int = 15) async throws -> String {
        let json = try await request("POST", "/api/activity-center/qixi/gift",
                                     body: ["friendGid": friendGid, "messageTextId": messageTextId],
                                     accountOverride: accountId)
        return json.str("message").isEmpty ? "鹊羽香囊赠送成功" : json.str("message")
    }

    func fetchQingMei(accountId: String) async throws -> QingMeiState {
        let json = try await request("GET", "/api/activity-center/qingmei", accountOverride: accountId)
        return QingMeiState(json.obj("data") ?? .null)
    }

    func fetchCharity(accountId: String) async throws -> CharityState {
        let json = try await request("GET", "/api/activity-center/charity-red-flower", accountOverride: accountId)
        return CharityState(json.obj("data") ?? .null)
    }

    func fetchWeather(accountId: String) async throws -> WeatherState {
        let json = try await request("GET", "/api/activity-center/weather", accountOverride: accountId)
        return WeatherState(json.obj("data") ?? .null)
    }

    func operateSeasonal(accountId: String, path: String, body: [String: Any] = [:]) async throws -> String {
        try await simpleActivityAction(accountId: accountId, path: path, body: body)
    }

    // MARK: Illustrated & Collections

    func fetchIllustrated(accountId: String) async throws -> IllustratedSnapshot {
        let json = try await request("GET", "/api/illustrated", accountOverride: accountId)
        guard let data = json.obj("data") else { throw APIError(message: "服务端未返回图鉴数据") }
        return IllustratedSnapshot(data)
    }

    func claimIllustrated(accountId: String, type: Int) async throws -> (IllustratedSnapshot?, String) {
        let json = try await request("POST", "/api/illustrated/claim", body: ["type": type], accountOverride: accountId)
        let data = json.obj("data") ?? .null
        let levels = data.arr("claimedLevels").count
        if levels == 0 { throw APIError(message: "图鉴领取结果未确认，请刷新后核对") }
        let rewards = CommerceItem.list(data.arr("rewards"))
        let text = rewards.map(\.text).joined(separator: "、")
        let msg = text.isEmpty ? "已领取 \(levels) 档图鉴奖励" : text
        let snapshot = data.obj("snapshot").map { IllustratedSnapshot($0) }
        return (snapshot, data.flag("refreshRequired") ? msg + "（请刷新确认最新进度）" : msg)
    }

    func purchaseIllustratedSeed(accountId: String, goodsId: Int, seedId: Int, count: Int) async throws -> Int64 {
        let json = try await request("POST", "/api/seeds/purchase",
                                     body: ["goodsId": goodsId, "count": count], accountOverride: accountId)
        let items = json.obj("data")?.arr("get_items") ?? []
        let received = items.filter { $0.i("id") == seedId }.reduce(Int64(0)) { $0 + max(0, $1.l("count")) }
        if received <= 0 { throw APIError(message: "种子购买结果未确认，请刷新背包后核对") }
        return received
    }

    func fetchCollections(accountId: String) async throws -> CollectionSnapshot {
        let json = try await request("GET", "/api/collections", accountOverride: accountId)
        guard let data = json.obj("data") else { throw APIError(message: "服务端未返回蓝晶藏品数据") }
        return CollectionSnapshot(data)
    }

    func operateCollections(accountId: String, action: String, body: [String: Any] = [:], label: String) async throws -> CollectionOperateResult {
        let json = try await request("POST", "/api/collections/\(Self.q(action))", body: body, accountOverride: accountId)
        return CollectionOperateResult(json.obj("data") ?? .null, fallback: label)
    }
}

private struct RawHTTPResponse {
    var statusCode: Int
    var headers: [String: String]
    var body: Data
}

private enum RawHTTPClient {
    // RawHTTPOperation 由 NWConnection 的异步回调驱动，调用方必须一直持有它；
    // 否则 start() 返回后实例立刻释放，continuation 永远不会 resume，界面会
    // 一直停在“正在连接…”。用静态表在请求期间保活，结束时再移除。
    private static let runningLock = NSLock()
    private static var running: [UUID: RawHTTPOperation] = [:]

    static func request(method: String,
                        url: URL,
                        headers: [String: String],
                        body: Data?,
                        timeout: TimeInterval = 30) async throws -> RawHTTPResponse {
        var current = url
        for _ in 0..<5 {
            let response = try await one(method: method, url: current, headers: headers, body: body, timeout: timeout)
            guard (300...399).contains(response.statusCode),
                  let location = response.headers["location"],
                  let next = URL(string: location, relativeTo: current)?.absoluteURL else {
                return response
            }
            if next.scheme?.lowercased() == "https" {
                return try await secureRequest(method: method, url: next, headers: headers, body: body)
            }
            current = next
        }
        throw APIError(message: "HTTP 重定向次数过多")
    }

    private static func one(method: String,
                            url: URL,
                            headers: [String: String],
                            body: Data?,
                            timeout: TimeInterval) async throws -> RawHTTPResponse {
        let operation = try RawHTTPOperation(method: method, url: url, headers: headers, body: body, timeout: timeout)
        let token = UUID()
        runningLock.lock()
        running[token] = operation
        runningLock.unlock()
        defer {
            runningLock.lock()
            running[token] = nil
            runningLock.unlock()
        }
        return try await withCheckedThrowingContinuation { continuation in
            operation.start(continuation)
        }
    }

    private static func secureRequest(method: String,
                                      url: URL,
                                      headers: [String: String],
                                      body: Data?) async throws -> RawHTTPResponse {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body
        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw APIError(message: "HTTPS 服务器响应无效")
        }
        var responseHeaders: [String: String] = [:]
        for (key, value) in http.allHeaderFields {
            responseHeaders[String(describing: key).lowercased()] = String(describing: value)
        }
        return RawHTTPResponse(statusCode: http.statusCode, headers: responseHeaders, body: data)
    }
}

private final class RawHTTPOperation {
    private let connection: NWConnection
    private let queue = DispatchQueue(label: "farm.raw-http")
    private let method: String
    private let url: URL
    private let headers: [String: String]
    private let body: Data?
    private let hostHeader: String
    private var continuation: CheckedContinuation<RawHTTPResponse, Error>?
    private var timeoutWork: DispatchWorkItem?
    private var buffer = Data()
    private var finished = false

    init(method: String, url: URL, headers: [String: String], body: Data?, timeout: TimeInterval) throws {
        guard let host = url.host, !host.isEmpty,
              let portValue = url.port ?? Optional(80),
              portValue > 0, portValue <= 65_535,
              let port = NWEndpoint.Port(rawValue: UInt16(portValue)) else {
            throw APIError(message: "HTTP 服务器地址无效")
        }
        self.method = method.uppercased()
        self.url = url
        self.headers = headers
        self.body = body
        var hostHeader = host
        if let userPort = url.port, userPort != 80 {
            hostHeader += ":\(userPort)"
        }
        self.hostHeader = hostHeader
        self.connection = NWConnection(host: NWEndpoint.Host(host), port: port, using: .tcp)
        let work = DispatchWorkItem { [weak self] in
            self?.finish(.failure(APIError(message: "HTTP 服务器响应超时")))
        }
        self.timeoutWork = work
        queue.asyncAfter(deadline: .now() + timeout, execute: work)
    }

    func start(_ continuation: CheckedContinuation<RawHTTPResponse, Error>) {
        self.continuation = continuation
        connection.stateUpdateHandler = { [weak self] state in
            guard let self, !self.finished else { return }
            switch state {
            case .ready:
                self.sendRequest()
            case .failed(let error):
                self.finish(.failure(APIError(message: "HTTP 连接失败：\(error.localizedDescription)")))
            case .cancelled:
                break
            default:
                break
            }
        }
        connection.start(queue: queue)
    }

    private func sendRequest() {
        var path = url.path
        if path.isEmpty { path = "/" }
        if let query = url.query, !query.isEmpty { path += "?" + query }
        var head = "\(method) \(path) HTTP/1.1\r\n"
        head += "Host: \(hostHeader)\r\n"
        head += "Connection: close\r\n"
        for (key, value) in headers {
            let lower = key.lowercased()
            if ["host", "connection", "content-length"].contains(lower) { continue }
            head += "\(key): \(value)\r\n"
        }
        if let body {
            head += "Content-Length: \(body.count)\r\n"
        }
        head += "\r\n"
        var packet = Data(head.utf8)
        if let body { packet.append(body) }
        connection.send(content: packet, completion: .contentProcessed { [weak self] error in
            guard let self, !self.finished else { return }
            if let error {
                self.finish(.failure(APIError(message: "HTTP 发送失败：\(error.localizedDescription)")))
            } else {
                self.receive()
            }
        })
    }

    private func receive() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) { [weak self] data, _, isComplete, error in
            guard let self, !self.finished else { return }
            if let error {
                self.finish(.failure(APIError(message: "HTTP 接收失败：\(error.localizedDescription)")))
                return
            }
            if let data { self.buffer.append(data) }
            do {
                if let response = try self.parse(final: isComplete) {
                    self.finish(.success(response))
                } else if isComplete {
                    self.finish(.failure(APIError(message: "HTTP 响应不完整")))
                } else {
                    self.receive()
                }
            } catch {
                self.finish(.failure(error))
            }
        }
    }

    private func parse(final: Bool) throws -> RawHTTPResponse? {
        let separator = Data("\r\n\r\n".utf8)
        guard let headerRange = buffer.range(of: separator) else {
            if final { throw APIError(message: "HTTP 响应缺少头部") }
            return nil
        }
        guard let headerText = String(data: buffer[..<headerRange.lowerBound], encoding: .utf8) else {
            throw APIError(message: "HTTP 响应头部编码无效")
        }
        let lines = headerText.components(separatedBy: "\r\n")
        guard let statusLine = lines.first else { throw APIError(message: "HTTP 响应缺少状态行") }
        let parts = statusLine.split(separator: " ", maxSplits: 2)
        guard parts.count >= 2, let statusCode = Int(parts[1]) else {
            throw APIError(message: "HTTP 状态行无效")
        }
        var responseHeaders: [String: String] = [:]
        for line in lines.dropFirst() {
            guard let colon = line.firstIndex(of: ":") else { continue }
            let key = String(line[..<colon]).trimmingCharacters(in: .whitespaces).lowercased()
            let value = String(line[line.index(after: colon)...]).trimmingCharacters(in: .whitespaces)
            if !key.isEmpty { responseHeaders[key] = value }
        }
        let bodyStart = headerRange.upperBound
        if responseHeaders["transfer-encoding"]?.lowercased().contains("chunked") == true {
            guard let body = decodeChunked(from: bodyStart) else {
                if final { throw APIError(message: "HTTP chunked 响应不完整") }
                return nil
            }
            return RawHTTPResponse(statusCode: statusCode, headers: responseHeaders, body: body)
        }
        if let lengthText = responseHeaders["content-length"], let length = Int(lengthText) {
            guard buffer.count >= bodyStart + length else {
                if final { throw APIError(message: "HTTP 响应正文不完整") }
                return nil
            }
            return RawHTTPResponse(statusCode: statusCode, headers: responseHeaders,
                                   body: Data(buffer[bodyStart..<(bodyStart + length)]))
        }
        if final {
            return RawHTTPResponse(statusCode: statusCode, headers: responseHeaders, body: Data(buffer[bodyStart...]))
        }
        return nil
    }

    private func decodeChunked(from start: Int) -> Data? {
        let crlf = Data("\r\n".utf8)
        var index = start
        var output = Data()
        while index < buffer.count {
            guard let lineRange = buffer.range(of: crlf, options: [], in: index..<buffer.count) else { return nil }
            let line = String(data: buffer[index..<lineRange.lowerBound], encoding: .utf8)?
                .trimmingCharacters(in: .whitespaces) ?? ""
            let sizeText = line.split(separator: ";", maxSplits: 1).first.map(String.init) ?? ""
            guard let size = Int(sizeText, radix: 16) else { return nil }
            let dataStart = lineRange.upperBound
            if size == 0 { return output }
            let dataEnd = dataStart + size
            guard buffer.count >= dataEnd + 2 else { return nil }
            output.append(buffer[dataStart..<dataEnd])
            index = dataEnd + 2
        }
        return nil
    }

    private func finish(_ result: Result<RawHTTPResponse, Error>) {
        queue.async { [weak self] in
            guard let self, !self.finished else { return }
            self.finished = true
            self.timeoutWork?.cancel()
            self.connection.cancel()
            self.continuation?.resume(with: result)
            self.continuation = nil
        }
    }
}

extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
