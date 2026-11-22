import Foundation
import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xff) / 255,
                  green: Double((hex >> 8) & 0xff) / 255,
                  blue: Double(hex & 0xff) / 255,
                  opacity: alpha)
    }

    init?(hexString: String) {
        var text = hexString.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.hasPrefix("#") { text.removeFirst() }
        guard text.count == 6 || text.count == 8,
              let value = UInt64(text, radix: 16) else { return nil }
        if text.count == 6 {
            self.init(hex: UInt32(value))
        } else {
            let rgb = UInt32((value >> 8) & 0xFFFFFF)
            let alpha = Double(value & 0xFF) / 255
            self.init(hex: rgb, alpha: alpha)
        }
    }

    var hexString: String {
        let color = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "#%02X%02X%02X", Int(round(r * 255)), Int(round(g * 255)), Int(round(b * 255)))
    }

    func blend(_ other: Color, _ amount: Double) -> Color {
        let t = CGFloat(max(0, min(1, amount)))
        let lhs = UIColor(self)
        let rhs = UIColor(other)
        var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
        var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
        lhs.getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
        rhs.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
        return Color(.sRGB,
                     red: Double(r1 + (r2 - r1) * t),
                     green: Double(g1 + (g2 - g1) * t),
                     blue: Double(b1 + (b2 - b1) * t),
                     opacity: Double(a1 + (a2 - a1) * t))
    }

    var luminance: Double {
        let color = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        func linear(_ value: CGFloat) -> Double {
            let v = Double(value)
            return v <= 0.03928 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }
}

enum LandColorPreset: String, CaseIterable, Identifiable, Codable {
    case web, pure, soft, vivid, custom
    var id: String { rawValue }
    var label: String {
        switch self {
        case .web: return "网页淡彩"
        case .pure: return "简约纯白"
        case .soft: return "清透柔色"
        case .vivid: return "鲜明彩卡"
        case .custom: return "自定义"
        }
    }
    var detail: String {
        switch self {
        case .web: return "复刻网页端 6 级土地淡彩"
        case .pure: return "纯白卡片，仅土壤徽章着彩"
        case .soft: return "轻薄柔和着底与细描边"
        case .vivid: return "更高饱和度，土地等级一眼区分"
        case .custom: return "按土地等级自由选色"
        }
    }
}

enum BackgroundTone: String, CaseIterable, Identifiable, Codable {
    case standard, warmSoft = "warm_soft", pureAmoled = "pure_amoled"
    var id: String { rawValue }
    var label: String {
        switch self {
        case .standard: return "默认"
        case .warmSoft: return "暖纸 / 柔灰"
        case .pureAmoled: return "亮白 / AMOLED"
        }
    }
    var detail: String {
        switch self {
        case .standard: return "冷灰底 + 纯白卡；黑底 + 深灰卡"
        case .warmSoft: return "护眼暖米纸底；柔和炭灰底"
        case .pureAmoled: return "清透纯白底；AMOLED 纯黑"
        }
    }
}

enum RanchAnimalColorPreset: String, CaseIterable, Identifiable, Codable {
    case pure, status, pen, custom
    var id: String { rawValue }
    var label: String {
        switch self {
        case .pure: return "简约纯白"
        case .status: return "状态着彩"
        case .pen: return "窝棚区分"
        case .custom: return "自定义"
        }
    }
    var detail: String {
        switch self {
        case .pure: return "纯白卡片，巨大与可收获仅边框提示"
        case .status: return "可收获淡绿、可生产淡蓝、巨大暖金"
        case .pen: return "窝暖米、棚淡青，巨大自动金底"
        case .custom: return "按窝 / 棚 / 巨大动物自由选色"
        }
    }
}

enum ProgressBarStyle: String, CaseIterable, Identifiable, Codable {
    case smooth, segmented, gradient
    var id: String { rawValue }
    var label: String {
        switch self {
        case .smooth: return "平滑圆轨"
        case .segmented: return "分段指示"
        case .gradient: return "渐变流光"
        }
    }
}

enum CornerStyle: String, CaseIterable, Identifiable, Codable {
    case rounded, standard, compact
    var id: String { rawValue }
    var label: String {
        switch self {
        case .rounded: return "柔和圆润"
        case .standard: return "标准"
        case .compact: return "利落方正"
        }
    }
}

enum CardSurfaceStyle: String, CaseIterable, Identifiable, Codable {
    case bordered, flat, elevated
    var id: String { rawValue }
    var label: String {
        switch self {
        case .bordered: return "细腻描边"
        case .flat: return "纯平无框"
        case .elevated: return "轻盈阴影"
        }
    }
}

struct CustomAppearanceConfig: Codable, Equatable {
    var baseThemeMode = "system"
    var backgroundTone: BackgroundTone = .standard
    var accentHex: String?
    var ranchAccentFollowPrimary = false
    var ranchAccentHex: String?
    var landPreset: LandColorPreset = .web
    var customLandColors: [String: String] = [:]
    var landDetailTinted = true
    var ranchAnimalPreset: RanchAnimalColorPreset = .status
    var customRanchColors: [String: String] = [:]
    var landGridColumns = 3
    var ranchAnimalColumns = 2
    var progressBarStyle: ProgressBarStyle = .smooth
    var cornerStyle: CornerStyle = .standard
    var cardSurfaceStyle: CardSurfaceStyle = .bordered

    init() {}

    init(baseThemeMode: String = "system",
         backgroundTone: BackgroundTone = .standard,
         accentHex: String? = nil,
         ranchAccentFollowPrimary: Bool = false,
         ranchAccentHex: String? = nil,
         landPreset: LandColorPreset = .web,
         customLandColors: [String: String] = [:],
         landDetailTinted: Bool = true,
         ranchAnimalPreset: RanchAnimalColorPreset = .status,
         customRanchColors: [String: String] = [:],
         landGridColumns: Int = 3,
         ranchAnimalColumns: Int = 2,
         progressBarStyle: ProgressBarStyle = .smooth,
         cornerStyle: CornerStyle = .standard,
         cardSurfaceStyle: CardSurfaceStyle = .bordered) {
        self.baseThemeMode = baseThemeMode
        self.backgroundTone = backgroundTone
        self.accentHex = accentHex
        self.ranchAccentFollowPrimary = ranchAccentFollowPrimary
        self.ranchAccentHex = ranchAccentHex
        self.landPreset = landPreset
        self.customLandColors = customLandColors
        self.landDetailTinted = landDetailTinted
        self.ranchAnimalPreset = ranchAnimalPreset
        self.customRanchColors = customRanchColors
        self.landGridColumns = landGridColumns == 4 ? 4 : 3
        self.ranchAnimalColumns = ranchAnimalColumns == 3 ? 3 : 2
        self.progressBarStyle = progressBarStyle
        self.cornerStyle = cornerStyle
        self.cardSurfaceStyle = cardSurfaceStyle
    }

    enum CodingKeys: String, CodingKey {
        case baseThemeMode, backgroundTone, accentHex, ranchAccentFollowPrimary, ranchAccentHex
        case landPreset, customLandColors, landDetailTinted, ranchAnimalPreset, customRanchColors
        case landGridColumns, ranchAnimalColumns, progressBarStyle, cornerStyle, cardSurfaceStyle
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        baseThemeMode = try c.decodeIfPresent(String.self, forKey: .baseThemeMode) ?? "system"
        backgroundTone = try c.decodeIfPresent(BackgroundTone.self, forKey: .backgroundTone) ?? .standard
        accentHex = try c.decodeIfPresent(String.self, forKey: .accentHex)
        ranchAccentFollowPrimary = try c.decodeIfPresent(Bool.self, forKey: .ranchAccentFollowPrimary) ?? false
        ranchAccentHex = try c.decodeIfPresent(String.self, forKey: .ranchAccentHex)
        landPreset = try c.decodeIfPresent(LandColorPreset.self, forKey: .landPreset) ?? .web
        customLandColors = try c.decodeIfPresent([String: String].self, forKey: .customLandColors) ?? [:]
        landDetailTinted = try c.decodeIfPresent(Bool.self, forKey: .landDetailTinted) ?? true
        ranchAnimalPreset = try c.decodeIfPresent(RanchAnimalColorPreset.self, forKey: .ranchAnimalPreset) ?? .status
        customRanchColors = try c.decodeIfPresent([String: String].self, forKey: .customRanchColors) ?? [:]
        landGridColumns = (try c.decodeIfPresent(Int.self, forKey: .landGridColumns) ?? 3) == 4 ? 4 : 3
        ranchAnimalColumns = (try c.decodeIfPresent(Int.self, forKey: .ranchAnimalColumns) ?? 2) == 3 ? 3 : 2
        progressBarStyle = try c.decodeIfPresent(ProgressBarStyle.self, forKey: .progressBarStyle) ?? .smooth
        cornerStyle = try c.decodeIfPresent(CornerStyle.self, forKey: .cornerStyle) ?? .standard
        cardSurfaceStyle = try c.decodeIfPresent(CardSurfaceStyle.self, forKey: .cardSurfaceStyle) ?? .bordered
    }
}

struct AppearanceQuickPreset: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let preview: [Color]
    let config: CustomAppearanceConfig
}

let appearanceQuickPresets: [AppearanceQuickPreset] = [
    AppearanceQuickPreset(
        id: "web_classic", name: "网页复刻", subtitle: "暖纸护眼 · 网页淡彩",
        preview: [Color(hex: 0xF6F4EC), Color(hex: 0xEEF8FD), Color(hex: 0xFF9500)],
        config: CustomAppearanceConfig(
            baseThemeMode: "light", backgroundTone: .warmSoft, accentHex: nil,
            ranchAccentFollowPrimary: false, ranchAccentHex: nil, landPreset: .web,
            customLandColors: [:], landDetailTinted: true, ranchAnimalPreset: .pen,
            customRanchColors: [:], landGridColumns: 3, ranchAnimalColumns: 2,
            progressBarStyle: .smooth, cornerStyle: .standard, cardSurfaceStyle: .bordered
        )
    ),
    AppearanceQuickPreset(
        id: "pure_white", name: "原版纯白", subtitle: "极简纯白 · 平滑圆轨",
        preview: [Color.white, Color(hex: 0x1B8A5A), Color(hex: 0xFF9500)],
        config: CustomAppearanceConfig(
            baseThemeMode: "light", backgroundTone: .standard, accentHex: nil,
            ranchAccentFollowPrimary: false, ranchAccentHex: nil, landPreset: .pure,
            customLandColors: [:], landDetailTinted: false, ranchAnimalPreset: .pure,
            customRanchColors: [:], landGridColumns: 3, ranchAnimalColumns: 2,
            progressBarStyle: .smooth, cornerStyle: .standard, cardSurfaceStyle: .bordered
        )
    ),
    AppearanceQuickPreset(
        id: "compact_pro", name: "高密效率", subtitle: "4 列农田 · 3 列牧场",
        preview: [Color(hex: 0x007AFF), Color(hex: 0xF97316), Color(hex: 0xEEF8FD)],
        config: CustomAppearanceConfig(
            baseThemeMode: "system", backgroundTone: .standard, accentHex: "#007AFF",
            ranchAccentFollowPrimary: false, ranchAccentHex: "#F97316", landPreset: .soft,
            customLandColors: [:], landDetailTinted: true, ranchAnimalPreset: .status,
            customRanchColors: [:], landGridColumns: 4, ranchAnimalColumns: 3,
            progressBarStyle: .smooth, cornerStyle: .compact, cardSurfaceStyle: .bordered
        )
    ),
    AppearanceQuickPreset(
        id: "amoled_glow", name: "暗夜流光", subtitle: "AMOLED 纯黑 · 渐变",
        preview: [Color.black, Color(hex: 0x6E56CF), Color(hex: 0xD97706)],
        config: CustomAppearanceConfig(
            baseThemeMode: "dark", backgroundTone: .pureAmoled, accentHex: "#6E56CF",
            ranchAccentFollowPrimary: false, ranchAccentHex: "#D97706", landPreset: .vivid,
            customLandColors: [:], landDetailTinted: true, ranchAnimalPreset: .status,
            customRanchColors: [:], landGridColumns: 3, ranchAnimalColumns: 2,
            progressBarStyle: .gradient, cornerStyle: .rounded, cardSurfaceStyle: .bordered
        )
    ),
]

struct LandCardColors {
    var background: Color
    var border: Color
    var badgeBackground: Color
    var badgeText: Color
}

struct RanchAnimalCardColors {
    var background: Color
    var border: Color
    var highlightBorder: Bool
}

private struct WebSoilPalette {
    var bg: Color
    var border: Color
    var chipBg: Color
    var ink: Color
    var darkTint: Color
    var baseBadge: Color
}

struct Palette {
    let isDark: Bool
    let appearance: CustomAppearanceConfig
    let isCustom: Bool

    init(_ scheme: ColorScheme, appearance: CustomAppearanceConfig = CustomAppearanceConfig(), isCustom: Bool = false) {
        self.isDark = scheme == .dark
        self.appearance = appearance
        self.isCustom = isCustom
    }

    private var tone: BackgroundTone { isCustom ? appearance.backgroundTone : .standard }
    private var landPreset: LandColorPreset { isCustom ? appearance.landPreset : .soft }
    private var ranchPreset: RanchAnimalColorPreset { isCustom ? appearance.ranchAnimalPreset : .pure }
    private var accentColor: Color? { isCustom ? appearance.accentHex.flatMap { Color(hexString: $0) } : nil }

    var background: Color {
        switch tone {
        case .standard: return Color(hex: isDark ? 0x000000 : 0xF2F2F7)
        case .warmSoft: return Color(hex: isDark ? 0x131417 : 0xF6F4EC)
        case .pureAmoled: return Color(hex: isDark ? 0x000000 : 0xFFFFFF)
        }
    }
    var card: Color {
        switch tone {
        case .standard: return Color(hex: isDark ? 0x1C1C1E : 0xFFFFFF)
        case .warmSoft: return Color(hex: isDark ? 0x1E2025 : 0xFFFFFDF8)
        case .pureAmoled: return Color(hex: isDark ? 0x101114 : 0xF7F7FA)
        }
    }
    var elevated: Color {
        switch tone {
        case .standard: return Color(hex: isDark ? 0x2C2C2E : 0xF9F9FB)
        case .warmSoft: return Color(hex: isDark ? 0x292C33 : 0xF1EEE4)
        case .pureAmoled: return Color(hex: isDark ? 0x1A1C21 : 0xEFEFF4)
        }
    }
    var track: Color {
        switch tone {
        case .standard: return Color(hex: isDark ? 0x2C2C2E : 0xE8E8ED)
        case .warmSoft: return Color(hex: isDark ? 0x292C33 : 0xEAE6DA)
        case .pureAmoled: return Color(hex: isDark ? 0x1C1E23 : 0xE6E6EC)
        }
    }
    var separator: Color {
        switch tone {
        case .standard: return Color(hex: isDark ? 0x38383A : 0xE5E5EA)
        case .warmSoft: return Color(hex: isDark ? 0x363A42 : 0xE1DCCE)
        case .pureAmoled: return Color(hex: isDark ? 0x262930 : 0xE4E4EA)
        }
    }
    var text: Color { Color(hex: isDark ? 0xFFFFFF : 0x1C1C1E) }
    var secondary: Color { Color(hex: isDark ? 0x98989F : 0x6C6C70) }
    var emerald: Color {
        if let accentColor { return isDark ? accentColor.blend(.white, 0.16) : accentColor }
        return Color(hex: isDark ? 0x30D158 : 0x1B8A5A)
    }
    var ranchAccent: Color {
        guard isCustom else { return Color(hex: isDark ? 0xFF9F0A : 0xFF9500) }
        if appearance.ranchAccentFollowPrimary { return emerald }
        if let hex = appearance.ranchAccentHex, let color = Color(hexString: hex) {
            return isDark ? color.blend(.white, 0.16) : color
        }
        return Color(hex: isDark ? 0xFF9F0A : 0xFF9500)
    }
    var green: Color { Color(hex: isDark ? 0x30D158 : 0x34C759) }
    var blue: Color { Color(hex: isDark ? 0x0A84FF : 0x007AFF) }
    var orange: Color { Color(hex: isDark ? 0xFF9F0A : 0xFF9500) }
    var red: Color { Color(hex: isDark ? 0xFF453A : 0xFF3B30) }
    var indigo: Color { Color(hex: isDark ? 0x5E5CE6 : 0x5856D6) }
    var cyan: Color { Color(hex: isDark ? 0x64D2FF : 0x32ADE6) }
    var amber: Color { Color(hex: 0xE59500) }
    var purple: Color { Color(hex: 0x8E5AE2) }

    var landColumns: Int { isCustom ? (appearance.landGridColumns == 4 ? 4 : 3) : 3 }
    var ranchColumns: Int { isCustom ? (appearance.ranchAnimalColumns == 3 ? 3 : 2) : 2 }
    var progressStyle: ProgressBarStyle { isCustom ? appearance.progressBarStyle : .smooth }
    var cardSurfaceStyle: CardSurfaceStyle { isCustom ? appearance.cardSurfaceStyle : .flat }
    var cardRadius: CGFloat {
        guard isCustom else { return 14 }
        switch appearance.cornerStyle {
        case .rounded: return 18
        case .standard: return 14
        case .compact: return 8
        }
    }
    var tileRadius: CGFloat {
        guard isCustom else { return 14 }
        switch appearance.cornerStyle {
        case .rounded: return 14
        case .standard: return 12
        case .compact: return 6
        }
    }
    var controlRadius: CGFloat {
        guard isCustom else { return 12 }
        switch appearance.cornerStyle {
        case .rounded: return 12
        case .standard: return 10
        case .compact: return 6
        }
    }

    func spaceAccent(_ space: GameSpace) -> Color { space == .farm ? emerald : ranchAccent }

    func soil(_ level: Int) -> Color {
        switch level {
        case 6...: return Color(hex: 0x2FA4E7)
        case 5: return Color(hex: 0x8E5AE2)
        case 4: return Color(hex: 0xE0A82E)
        case 3: return Color(hex: 0x48484A)
        case 2: return Color(hex: 0xD9534F)
        default: return Color(hex: 0xA67C52)
        }
    }

    private func webPalette(_ level: Int) -> WebSoilPalette {
        switch level {
        case 2: return WebSoilPalette(bg: Color(hex: 0xFDF4F1), border: Color(hex: 0xEECBC1),
                                      chipBg: Color(hex: 0xF8DDD5), ink: Color(hex: 0xB0482F),
                                      darkTint: Color(hex: 0xE08A6E), baseBadge: Color(hex: 0xD9534F))
        case 3: return WebSoilPalette(bg: Color(hex: 0xF4F3F1), border: Color(hex: 0xD3CFC8),
                                      chipBg: Color(hex: 0xE3DFD9), ink: Color(hex: 0x4B4540),
                                      darkTint: Color(hex: 0x8D939B), baseBadge: Color(hex: 0x48484A))
        case 4: return WebSoilPalette(bg: Color(hex: 0xFFFAE8), border: Color(hex: 0xEAD57F),
                                      chipBg: Color(hex: 0xF9E9A8), ink: Color(hex: 0x946600),
                                      darkTint: Color(hex: 0xE6C35A), baseBadge: Color(hex: 0xE0A82E))
        case 5: return WebSoilPalette(bg: Color(hex: 0xF8F3FD), border: Color(hex: 0xD7C6EF),
                                      chipBg: Color(hex: 0xEADFFA), ink: Color(hex: 0x7140B5),
                                      darkTint: Color(hex: 0xB892F0), baseBadge: Color(hex: 0x8E5AE2))
        case 6: return WebSoilPalette(bg: Color(hex: 0xEEF8FD), border: Color(hex: 0xB5D9EB),
                                      chipBg: Color(hex: 0xD7EDF8), ink: Color(hex: 0x367CAA),
                                      darkTint: Color(hex: 0x82BDDF), baseBadge: Color(hex: 0x2FA4E7))
        default: return WebSoilPalette(bg: Color(hex: 0xFBF8F1), border: Color(hex: 0xE6DBC2),
                                       chipBg: Color(hex: 0xF1E7D2), ink: Color(hex: 0x86642E),
                                       darkTint: Color(hex: 0xC9A867), baseBadge: Color(hex: 0xA67C52))
        }
    }

    func landColors(level: Int) -> LandCardColors {
        let web = webPalette(level)
        switch landPreset {
        case .pure:
            return LandCardColors(background: card, border: separator,
                                  badgeBackground: web.baseBadge.opacity(0.16), badgeText: web.baseBadge)
        case .web:
            if isDark {
                return LandCardColors(background: card.blend(web.darkTint, 0.11),
                                      border: web.darkTint.opacity(0.30),
                                      badgeBackground: web.darkTint.opacity(0.16),
                                      badgeText: web.darkTint)
            }
            return LandCardColors(background: web.bg, border: web.border,
                                  badgeBackground: web.chipBg, badgeText: web.ink)
        case .soft:
            if isDark {
                return LandCardColors(background: card.blend(web.darkTint, 0.06),
                                      border: separator.blend(web.darkTint, 0.22),
                                      badgeBackground: web.darkTint.opacity(0.14),
                                      badgeText: web.darkTint)
            }
            return LandCardColors(background: Color.white.blend(web.bg, 0.55),
                                  border: separator.blend(web.border, 0.55),
                                  badgeBackground: web.chipBg.opacity(0.72), badgeText: web.ink)
        case .vivid:
            if isDark {
                return LandCardColors(background: card.blend(web.darkTint, 0.20),
                                      border: web.darkTint.opacity(0.45),
                                      badgeBackground: web.darkTint.opacity(0.24),
                                      badgeText: web.darkTint.blend(.white, 0.18))
            }
            return LandCardColors(background: web.bg.blend(web.baseBadge, 0.14),
                                  border: web.border.blend(web.baseBadge, 0.35),
                                  badgeBackground: web.chipBg.blend(web.baseBadge, 0.18),
                                  badgeText: web.ink.blend(.black, 0.12))
        case .custom:
            guard let hex = appearance.customLandColors["\(level)"],
                  let picked = Color(hexString: hex) else {
                var fallback = appearance
                fallback.landPreset = .web
                return Palette(isDark ? .dark : .light, appearance: fallback, isCustom: true).landColors(level: level)
            }
            if picked.hexString.uppercased() == "#FFFFFF" {
                return LandCardColors(background: card, border: separator,
                                      badgeBackground: web.baseBadge.opacity(0.16), badgeText: web.baseBadge)
            }
            if picked.luminance >= 0.72 {
                if isDark {
                    return LandCardColors(background: card.blend(picked, 0.12),
                                          border: picked.opacity(0.30),
                                          badgeBackground: picked.opacity(0.18),
                                          badgeText: picked.blend(.white, 0.18))
                }
                return LandCardColors(background: picked, border: picked.blend(.black, 0.14),
                                      badgeBackground: picked.blend(.black, 0.08),
                                      badgeText: picked.blend(Color(hex: 0x2B2B2E), 0.52))
            }
            if isDark {
                return LandCardColors(background: card.blend(picked, 0.18),
                                      border: picked.opacity(0.42),
                                      badgeBackground: picked.opacity(0.24),
                                      badgeText: picked.blend(.white, 0.36))
            }
            let darkText = picked.luminance > 0.35 ? picked.blend(.black, 0.32) : picked
            return LandCardColors(background: Color.white.blend(picked, 0.14),
                                  border: picked.opacity(0.36),
                                  badgeBackground: picked.opacity(0.18), badgeText: darkText)
        }
    }

    func ranchColors(isShed: Bool, isGiant: Bool, canHarvest: Bool,
                     canProduce: Bool, grassBlocked: Bool) -> RanchAnimalCardColors {
        let gold = Color(hex: 0xE0A82E)
        let defaultBorder = isGiant ? gold : (canHarvest ? green : separator)
        let defaultHighlight = isGiant || canHarvest
        switch ranchPreset {
        case .pure:
            return RanchAnimalCardColors(background: card, border: defaultBorder,
                                         highlightBorder: defaultHighlight)
        case .status:
            if isGiant {
                return RanchAnimalCardColors(background: isDark ? card.blend(gold, 0.14) : Color(hex: 0xFFF9E6),
                                             border: isDark ? gold.opacity(0.72) : gold,
                                             highlightBorder: true)
            }
            if canHarvest {
                return RanchAnimalCardColors(background: isDark ? card.blend(green, 0.13) : Color(hex: 0xEEFBF2),
                                             border: green, highlightBorder: true)
            }
            if canProduce {
                return RanchAnimalCardColors(background: isDark ? card.blend(blue, 0.12) : Color(hex: 0xEEF7FE),
                                             border: blue.opacity(isDark ? 0.45 : 0.55),
                                             highlightBorder: false)
            }
            if grassBlocked {
                return RanchAnimalCardColors(background: isDark ? card.blend(orange, 0.13) : Color(hex: 0xFFFFF4EB),
                                             border: orange.opacity(isDark ? 0.50 : 0.70),
                                             highlightBorder: false)
            }
            return RanchAnimalCardColors(background: card, border: separator, highlightBorder: false)
        case .pen:
            if isGiant {
                return RanchAnimalCardColors(background: isDark ? card.blend(gold, 0.14) : Color(hex: 0xFFFFF8E1),
                                             border: gold, highlightBorder: true)
            }
            if isShed {
                let tint = Color(hex: 0x60B5DF)
                return RanchAnimalCardColors(background: isDark ? card.blend(tint, 0.11) : Color(hex: 0xEFF8FD),
                                             border: canHarvest ? green : (isDark ? tint.opacity(0.32) : Color(hex: 0xB8DAEC)),
                                             highlightBorder: canHarvest)
            }
            let tint = Color(hex: 0xE0A865)
            return RanchAnimalCardColors(background: isDark ? card.blend(tint, 0.11) : Color(hex: 0xFDF7EE),
                                         border: canHarvest ? green : (isDark ? tint.opacity(0.32) : Color(hex: 0xE9D4B5)),
                                         highlightBorder: canHarvest)
        case .custom:
            let key = isGiant ? "giant" : (isShed ? "shed" : "coop")
            let defaultHex = isShed ? "#EFF8FD" : (isGiant ? "#FFF8E1" : "#FDF7EE")
            let picked = appearance.customRanchColors[key].flatMap { Color(hexString: $0) }
                ?? Color(hexString: defaultHex) ?? card
            if picked.hexString.uppercased() == "#FFFFFF" {
                return RanchAnimalCardColors(background: card, border: defaultBorder,
                                             highlightBorder: defaultHighlight)
            }
            if picked.luminance >= 0.72 {
                return RanchAnimalCardColors(background: isDark ? card.blend(picked, 0.13) : picked,
                                             border: isGiant ? gold : (canHarvest ? green : (isDark ? picked.opacity(0.34) : picked.blend(.black, 0.14))),
                                             highlightBorder: defaultHighlight)
            }
            return RanchAnimalCardColors(background: isDark ? card.blend(picked, 0.18) : Color.white.blend(picked, 0.14),
                                         border: isGiant ? gold : (canHarvest ? green : picked.opacity(isDark ? 0.42 : 0.36)),
                                         highlightBorder: defaultHighlight)
        }
    }
}

private struct PaletteKey: EnvironmentKey {
    static let defaultValue = Palette(.light)
}

extension EnvironmentValues {
    var palette: Palette {
        get { self[PaletteKey.self] }
        set { self[PaletteKey.self] = newValue }
    }
}

enum AppThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark, custom
    var id: String { rawValue }
    var label: String {
        switch self {
        case .system: return "跟随系统"
        case .light: return "浅色"
        case .dark: return "深色"
        case .custom: return "自定义"
        }
    }
}

enum GameSpace: String, CaseIterable, Identifiable, Equatable {
    case farm, ranch
    var id: String { rawValue }
    var label: String { self == .farm ? "农场" : "牧场" }
    var icon: String { self == .farm ? "leaf.fill" : "pawprint.fill" }
    var accent: Color { self == .farm ? Color(hex: 0x1B8A5A) : Color(hex: 0xFF9500) }
}

enum Fmt {
    static func compact(_ value: Int64) -> String {
        let v = Double(value)
        let absV = abs(v)
        func trim(_ d: Double) -> String {
            var s = String(format: "%.1f", d)
            if s.hasSuffix(".0") { s.removeLast(2) }
            return s
        }
        if absV >= 1_000_000_000_000 { return trim(v / 1_000_000_000_000) + "万亿" }
        if absV >= 100_000_000 { return trim(v / 100_000_000) + "亿" }
        if absV >= 10_000 { return trim(v / 10_000) + "万" }
        return "\(value)"
    }

    static func full(_ value: Int64) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        return f.string(from: NSNumber(value: value)) ?? "\(value)"
    }

    static func duration(_ seconds: Int64) -> String {
        let s = max(0, seconds)
        if s >= 86400 { return "\(s / 86400)天\(s % 86400 / 3600)小时" }
        if s >= 3600 { return String(format: "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60) }
        if s >= 60 { return String(format: "%02d:%02d", s / 60, s % 60) }
        return "\(s)秒"
    }

    static func countdown(to date: Date?) -> String {
        guard let date else { return "—" }
        let delta = Int64(date.timeIntervalSinceNow)
        if delta <= 0 { return "即将开始" }
        return duration(delta)
    }

    static func time(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        return f.string(from: date)
    }

    static func dateTime(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MM-dd HH:mm"
        return f.string(from: date)
    }
}
