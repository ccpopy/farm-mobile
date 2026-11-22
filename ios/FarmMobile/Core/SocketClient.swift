import Foundation

@MainActor
final class SocketClient {
    var onConnectionChanged: ((Bool) -> Void)?
    var onStatus: ((String, StatusSnapshot) -> Void)?
    var onLog: ((RunLogEntry) -> Void)?
    var onLogsSnapshot: ((String, [RunLogEntry]) -> Void)?

    private var task: URLSessionWebSocketTask?
    private var session: URLSession?
    private var desired = false
    private var activeBase = ""
    private var activeToken = ""
    private var subscribedId = ""
    private var reconnectItem: DispatchWorkItem?
    private var reconnectDelay: TimeInterval = 2
    private var connected = false

    func connect(baseURL: String, token: String, accountId: String) {
        let base = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        let tok = token.trimmingCharacters(in: .whitespacesAndNewlines)
        if base.isEmpty || tok.isEmpty {
            disconnect()
            return
        }
        desired = true
        if !activeBase.isEmpty && activeBase == base && activeToken == tok, let task {
            subscribedId = accountId.trimmingCharacters(in: .whitespacesAndNewlines)
            if connected {
                send("42" + arrayJSON(["subscribe", ["accountId": subscribedId.isEmpty ? "all" : subscribedId]]))
                return
            }
            // A task that previously failed/completed cannot be revived by
            // calling resume(). Recreate the socket in that case.
            if task.state == .running || task.state == .suspended {
                task.resume()
                return
            }
        }
        closeSocket()
        activeBase = base
        activeToken = tok
        subscribedId = accountId.trimmingCharacters(in: .whitespacesAndNewlines)
        openSocket()
    }

    func subscribe(accountId: String) {
        subscribedId = accountId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard connected else { return }
        send("42" + arrayJSON(["subscribe", ["accountId": subscribedId.isEmpty ? "all" : subscribedId]]))
    }

    func disconnect() {
        desired = false
        reconnectItem?.cancel()
        reconnectItem = nil
        closeSocket()
        activeBase = ""
        activeToken = ""
        setConnected(false)
    }

    private func openSocket() {
        guard desired, !activeBase.isEmpty, let baseURL = URL(string: activeBase),
              var comps = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else { return }
        comps.scheme = comps.scheme == "https" ? "wss" : "ws"
        var path = comps.path
        if !path.hasSuffix("/") { path += "/" }
        // Socket.IO 的默认挂载点是 /socket.io/，尾斜杠不能省。
        // 少这个斜杠时 nginx/Express 会返回 502，WebSocket 握手直接失败。
        comps.path = path + "socket.io/"
        comps.query = "EIO=4&transport=websocket"
        guard let url = comps.url else { return }

        let cfg = URLSessionConfiguration.ephemeral
        cfg.timeoutIntervalForRequest = 60
        cfg.httpShouldSetCookies = false
        let session = URLSession(configuration: cfg)
        self.session = session
        let task = session.webSocketTask(with: url)
        self.task = task
        task.resume()
        receive(on: task)
    }

    private func receive(on task: URLSessionWebSocketTask) {
        task.receive { [weak self] result in
            Task { @MainActor in
                guard let self, self.task === task else { return }
                switch result {
                case .success(let message):
                    switch message {
                    case .string(let text): self.handlePacket(text)
                    case .data(let data):
                        if let text = String(data: data, encoding: .utf8) { self.handlePacket(text) }
                    @unknown default: break
                    }
                    if self.task === task { self.receive(on: task) }
                case .failure:
                    self.handleFailure(task)
                }
            }
        }
    }

    private func handlePacket(_ text: String) {
        if text == "2" || text.hasPrefix("2") && text.count <= 2 {
            send("3")
            return
        }
        if text.hasPrefix("0") {
            var auth: [String: Any] = ["token": activeToken]
            auth["accountId"] = subscribedId.isEmpty ? "all" : subscribedId
            send("40" + objectJSON(auth))
            return
        }
        if text.hasPrefix("40") {
            setConnected(true)
            reconnectDelay = 2
            let id = subscribedId.isEmpty ? "all" : subscribedId
            send("42" + arrayJSON(["subscribe", ["accountId": id]]))
            return
        }
        if text.hasPrefix("41") || text.hasPrefix("44") {
            setConnected(false)
            scheduleReconnect()
            return
        }
        if text.hasPrefix("42") {
            var payload = String(text.dropFirst(2))
            if let bracket = payload.firstIndex(of: "[") { payload = String(payload[bracket...]) }
            handleEvent(payload)
        }
    }

    private func handleEvent(_ payload: String) {
        guard let data = payload.data(using: .utf8),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [Any],
              arr.count >= 1, let event = arr[0] as? String else { return }
        let arg: JSONValue = arr.count > 1 ? JSONValue(any: arr[1]) : .null
        switch event {
        case "status:update":
            let id = arg.str("accountId")
            guard !id.isEmpty, let status = arg.obj("status") else { return }
            let snapshot = StatusSnapshot(accountId: id, data: status, serverTimeHint: arg.l("serverTime"))
            onStatus?(id, snapshot)
        case "log:new":
            onLog?(RunLogEntry(arg))
        case "logs:snapshot":
            let id = arg.str("accountId")
            let logs = arg.arr("logs").map { RunLogEntry($0) }
            onLogsSnapshot?(id, logs)
        default:
            break
        }
    }

    private func handleFailure(_ task: URLSessionWebSocketTask) {
        guard self.task === task else { return }
        setConnected(false)
        scheduleReconnect()
    }

    private func scheduleReconnect() {
        guard desired else { return }
        reconnectItem?.cancel()
        let delay = reconnectDelay
        reconnectDelay = min(reconnectDelay * 1.6, 12)
        let item = DispatchWorkItem { [weak self] in
            guard let self, self.desired else { return }
            self.closeSocket()
            self.openSocket()
        }
        reconnectItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: item)
    }

    private func closeSocket() {
        task?.cancel(with: .goingAway, reason: nil)
        task = nil
        session?.invalidateAndCancel()
        session = nil
    }

    private func setConnected(_ value: Bool) {
        guard connected != value else { return }
        connected = value
        onConnectionChanged?(value)
    }

    private func send(_ text: String) {
        guard let task else { return }
        task.send(.string(text)) { _ in }
    }

    private func objectJSON(_ obj: [String: Any]) -> String {
        guard JSONSerialization.isValidJSONObject(obj),
              let data = try? JSONSerialization.data(withJSONObject: obj) else { return "{}" }
        return String(data: data, encoding: .utf8) ?? "{}"
    }

    private func arrayJSON(_ arr: [Any]) -> String {
        guard JSONSerialization.isValidJSONObject(arr),
              let data = try? JSONSerialization.data(withJSONObject: arr) else { return "[]" }
        return String(data: data, encoding: .utf8) ?? "[]"
    }
}
