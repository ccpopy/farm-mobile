import Foundation
import CoreFoundation

enum JSONValue {
    case null
    case bool(Bool)
    case int(Int)
    case double(Double)
    case string(String)
    case array([JSONValue])
    case object([String: JSONValue])

    init(any: Any) {
        switch any {
        case is NSNull:
            self = .null
        case let n as NSNumber:
            if CFGetTypeID(n) == CFBooleanGetTypeID() {
                self = .bool(n.boolValue)
            } else {
                let d = n.doubleValue
                if d.rounded() == d && abs(d) < 9_000_000_000_000_000 {
                    self = .int(Int(d))
                } else {
                    self = .double(d)
                }
            }
        case let s as String:
            self = .string(s)
        case let a as [Any]:
            self = .array(a.map(JSONValue.init(any:)))
        case let d as [String: Any]:
            self = .object(d.mapValues(JSONValue.init(any:)))
        default:
            self = .null
        }
    }

    var stringValue: String? {
        switch self {
        case .string(let s): return s
        case .int(let i): return String(i)
        case .double(let d): return String(d)
        case .bool(let b): return b ? "true" : "false"
        default: return nil
        }
    }

    var intValue: Int? {
        switch self {
        case .int(let i): return i
        case .double(let d): return Int(d)
        case .string(let s): return Int(s)
        case .bool(let b): return b ? 1 : 0
        default: return nil
        }
    }

    var int64Value: Int64? {
        switch self {
        case .int(let i): return Int64(i)
        case .double(let d): return Int64(d)
        case .string(let s):
            if let i = Int64(s) { return i }
            if let d = Double(s) { return Int64(d) }
            return nil
        case .bool(let b): return b ? 1 : 0
        default: return nil
        }
    }

    var doubleValue: Double? {
        switch self {
        case .int(let i): return Double(i)
        case .double(let d): return d
        case .string(let s): return Double(s)
        case .bool(let b): return b ? 1 : 0
        default: return nil
        }
    }

    var boolValue: Bool? {
        switch self {
        case .bool(let b): return b
        case .int(let i): return i != 0
        case .double(let d): return d != 0
        case .string(let s):
            let lower = s.lowercased()
            if ["true", "1", "yes", "y"].contains(lower) { return true }
            if ["false", "0", "no", "n", ""].contains(lower) { return false }
            return nil
        default: return nil
        }
    }

    var arrayValue: [JSONValue]? {
        if case .array(let a) = self { return a }
        return nil
    }

    var objectValue: [String: JSONValue]? {
        if case .object(let o) = self { return o }
        return nil
    }

    subscript(key: String) -> JSONValue? {
        objectValue?[key]
    }

    func has(_ key: String) -> Bool { objectValue?[key] != nil }
    func isNull(_ key: String) -> Bool {
        guard let v = objectValue?[key] else { return true }
        if case .null = v { return true }
        return false
    }

    func str(_ key: String, _ def: String = "") -> String { self[key]?.stringValue ?? def }
    func optStr(_ key: String) -> String? { self[key]?.stringValue }
    func i(_ key: String, _ def: Int = 0) -> Int { self[key]?.intValue ?? def }
    func l(_ key: String, _ def: Int64 = 0) -> Int64 { self[key]?.int64Value ?? def }
    func d(_ key: String, _ def: Double = 0) -> Double { self[key]?.doubleValue ?? def }
    func flag(_ key: String, _ def: Bool = false) -> Bool { self[key]?.boolValue ?? def }
    func arr(_ key: String) -> [JSONValue] { self[key]?.arrayValue ?? [] }
    func obj(_ key: String) -> JSONValue? { self[key] }

    var foundation: Any {
        switch self {
        case .null: return NSNull()
        case .bool(let b): return b
        case .int(let i): return i
        case .double(let d): return d
        case .string(let s): return s
        case .array(let a): return a.map(\.foundation)
        case .object(let o): return o.mapValues(\.foundation)
        }
    }

    var pretty: String {
        guard JSONSerialization.isValidJSONObject(foundation),
              let data = try? JSONSerialization.data(withJSONObject: foundation, options: [.prettyPrinted, .sortedKeys]),
              let text = String(data: data, encoding: .utf8) else { return "" }
        return text
    }
}

struct AnyEncodable: Encodable {
    let value: Any
    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch value {
        case is NSNull: try c.encodeNil()
        case let v as Bool: try c.encode(v)
        case let v as Int: try c.encode(v)
        case let v as Int64: try c.encode(v)
        case let v as Double: try c.encode(v)
        case let v as String: try c.encode(v)
        case let v as [Any]: try c.encode(v.map(AnyEncodable.init(value:)))
        case let v as [String: Any]: try c.encode(v.mapValues(AnyEncodable.init(value:)))
        default: try c.encodeNil()
        }
    }
}

enum JSONBody {
    static func data(_ body: [String: Any]?) -> Data {
        guard let body, JSONSerialization.isValidJSONObject(body) else { return Data("{}".utf8) }
        return (try? JSONSerialization.data(withJSONObject: body)) ?? Data("{}".utf8)
    }
}