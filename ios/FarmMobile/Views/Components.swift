import Foundation
import UIKit
import CoreImage.CIFilterBuiltins
import SwiftUI

struct Card<Content: View>: View {
    @Environment(\.palette) private var p
    var padding: CGFloat = 16
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(p.card)
            .clipShape(RoundedRectangle(cornerRadius: p.cardRadius, style: .continuous))
            .overlay {
                if p.cardSurfaceStyle == .bordered {
                    RoundedRectangle(cornerRadius: p.cardRadius, style: .continuous)
                        .stroke(p.separator.opacity(0.75), lineWidth: 0.8)
                }
            }
            .shadow(color: p.cardSurfaceStyle == .elevated
                    ? Color.black.opacity(p.isDark ? 0.35 : 0.08) : .clear,
                    radius: 10, x: 0, y: 4)
    }
}

struct SectionTitle: View {
    @Environment(\.palette) private var p
    var title: String
    var trailing: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(p.text)
            Spacer()
            if let trailing {
                Button(action: { action?() }) {
                    Text(trailing).font(.system(size: 14, weight: .semibold)).foregroundColor(p.emerald)
                }
                .disabled(action == nil)
            }
        }
        .padding(.horizontal, 2)
        .padding(.top, 4)
    }
}

struct Pill: View {
    @Environment(\.palette) private var p
    var text: String
    var color: Color? = nil
    var filled = false
    var dot = false

    var body: some View {
        let tint = color ?? p.secondary
        HStack(spacing: 4) {
            if dot {
                Circle().fill(tint).frame(width: 5, height: 5)
            }
            Text(text)
                .font(.system(size: 11, weight: .semibold))
                .lineLimit(1)
        }
        .foregroundColor(filled ? .white : tint)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(filled ? tint : tint.opacity(0.13))
        .clipShape(Capsule())
    }
}

struct AvatarView: View {
    @Environment(\.palette) private var p
    var url: String
    var name: String
    var size: CGFloat = 52

    var body: some View {
        Group {
            if url.isEmpty {
                placeholder
            } else {
                AsyncImage(url: URL(string: url)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else {
                        placeholder
                    }
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(p.separator, lineWidth: 0.5))
    }

    private var placeholder: some View {
        ZStack {
            p.track
            Image(systemName: "person.fill").font(.system(size: size * 0.4)).foregroundColor(p.emerald)
        }
    }
}

struct RemoteIcon: View {
    @Environment(\.palette) private var p
    var url: String
    var fallback: String = "leaf.fill"
    var size: CGFloat = 36

    var body: some View {
        Group {
            if url.isEmpty {
                fallbackView
            } else {
                AsyncImage(url: URL(string: url)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFit()
                    } else {
                        fallbackView
                    }
                }
            }
        }
        .frame(width: size, height: size)
    }

    private var fallbackView: some View {
        ZStack {
            p.track
            Image(systemName: fallback).font(.system(size: size * 0.45)).foregroundColor(p.secondary)
        }
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }
}

struct ProgressLine: View {
    @Environment(\.palette) private var p
    var value: Double
    var color: Color? = nil
    var height: CGFloat = 8

    var body: some View {
        let clamped = CGFloat(max(0, min(1, value)))
        GeometryReader { geo in
            switch p.progressStyle {
            case .smooth:
                ZStack(alignment: .leading) {
                    Capsule().fill(p.track)
                    Capsule().fill(color ?? p.emerald)
                        .frame(width: clamped * geo.size.width)
                }
            case .segmented:
                HStack(spacing: 2) {
                    ForEach(0..<16, id: \.self) { index in
                        Capsule()
                            .fill(CGFloat(index) / 15.0 <= clamped ? (color ?? p.emerald) : p.track)
                    }
                }
            case .gradient:
                ZStack(alignment: .leading) {
                    Capsule().fill(p.track)
                    Capsule()
                        .fill(LinearGradient(colors: [(color ?? p.emerald).opacity(0.55), color ?? p.emerald],
                                             startPoint: .leading, endPoint: .trailing))
                        .frame(width: clamped * geo.size.width)
                }
            }
        }
        .frame(height: height)
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? 320
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = proposal.width ?? bounds.width
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: bounds.minX + x, y: bounds.minY + y), proposal: ProposedViewSize(width: size.width, height: size.height))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

struct StatTile: View {
    @Environment(\.palette) private var p
    var icon: String
    var tint: Color
    var label: String
    var value: String
    var subtitle: String? = nil
    var exact: Bool = false
    var onTap: (() -> Void)? = nil

    var body: some View {
        Button(action: { onTap?() }) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Image(systemName: icon)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 22, height: 22)
                        .background(tint)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    Text(label).font(.system(size: 13)).foregroundColor(p.secondary)
                    Spacer(minLength: 0)
                }
                Text(value)
                    .font(.system(size: exact ? 20 : 24, weight: .bold, design: .rounded))
                    .foregroundColor(p.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 11))
                        .foregroundColor(subtitle.hasPrefix("本次 -") ? p.red : p.green)
                        .lineLimit(1)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 96, alignment: .topLeading)
            .background(p.card)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct ActionPill: View {
    @Environment(\.palette) private var p
    var title: String
    var icon: String? = nil
    var tint: Color? = nil
    var enabled = true
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                if let icon { Image(systemName: icon).font(.system(size: 12, weight: .semibold)) }
                Text(title).font(.system(size: 13, weight: .semibold)).lineLimit(1)
            }
            .foregroundColor(enabled ? (tint ?? p.emerald) : p.secondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background((enabled ? (tint ?? p.emerald) : p.secondary).opacity(0.10))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct PrimaryButton: View {
    @Environment(\.palette) private var p
    var title: String
    var icon: String? = nil
    var tint: Color? = nil
    var enabled = true
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon { Image(systemName: icon) }
                Text(title).font(.system(size: 15, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(enabled ? (tint ?? p.emerald) : p.secondary.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: p.controlRadius, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct LoadingState: View {
    @Environment(\.palette) private var p
    var text = "加载中"
    var body: some View {
        VStack(spacing: 10) {
            ProgressView().tint(p.emerald)
            Text(text).font(.system(size: 13)).foregroundColor(p.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 120)
    }
}

struct EmptyStateView: View {
    @Environment(\.palette) private var p
    var icon = "leaf"
    var title: String
    var subtitle: String = ""
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon).font(.system(size: 30)).foregroundColor(p.secondary)
            Text(title).font(.system(size: 16, weight: .semibold)).foregroundColor(p.text)
            if !subtitle.isEmpty {
                Text(subtitle).font(.system(size: 13)).foregroundColor(p.secondary).multilineTextAlignment(.center)
            }
            if let actionTitle, let action {
                Button(actionTitle, action: action).font(.system(size: 14, weight: .semibold)).foregroundColor(p.emerald)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 130)
        .padding(16)
    }
}

struct SpaceSwitcher: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore

    var body: some View {
        HStack(spacing: 2) {
            ForEach(GameSpace.allCases) { item in
                let selected = store.space == item
                Button {
                    withAnimation(.easeOut(duration: 0.18)) { store.space = item }
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: item.icon).font(.system(size: 13, weight: .semibold))
                        Text(item.label).font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(selected ? .white : p.secondary)
                    .padding(.horizontal, 13)
                    .padding(.vertical, 7)
                    .background(selected ? item.accent : Color.clear)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(p.card.opacity(0.9))
        .clipShape(Capsule())
    }
}

struct TopBar: View {
    @Environment(\.palette) private var p
    @EnvironmentObject private var store: AppStore
    var showSpace = true
    var showAccount = true
    @State private var showAccounts = false

    var body: some View {
        HStack {
            if showSpace { SpaceSwitcher() }
            Spacer()
            if showAccount {
                Button { showAccounts = true } label: {
                    HStack(spacing: 7) {
                        Circle().fill(store.isAccountOnline ? p.green : p.secondary)
                            .frame(width: 6, height: 6)
                        Text(store.currentAccount?.displayTitle ?? "选择账号")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(p.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                            .truncationMode(.middle)
                            .frame(maxWidth: 190, alignment: .trailing)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(p.secondary)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(p.card)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .sheet(isPresented: $showAccounts) {
            AccountManagerSheet().environmentObject(store)
        }
    }
}


enum QRImageFactory {
    static func make(_ text: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 10, y: 10)),
              let cg = CIContext().createCGImage(output, from: output.extent) else { return nil }
        return UIImage(cgImage: cg)
    }

    static func decodeDataURL(_ text: String) -> UIImage? {
        guard let comma = text.firstIndex(of: ",") else { return nil }
        let base64 = String(text[text.index(after: comma)...])
        guard let data = Data(base64Encoded: base64) else { return nil }
        return UIImage(data: data)
    }
}

struct QRCodeView: View {
    @Environment(\.palette) private var p
    var content: String
    var size: CGFloat = 220

    var body: some View {
        Group {
            if content.isEmpty {
                ZStack {
                    p.elevated
                    VStack(spacing: 8) {
                        ProgressView().tint(p.emerald)
                        Text("正在获取二维码").font(.system(size: 12)).foregroundColor(p.secondary)
                    }
                }
            } else if content.lowercased().hasPrefix("data:image"), let image = QRImageFactory.decodeDataURL(content) {
                Image(uiImage: image).resizable().interpolation(.none).scaledToFit()
            } else if content.lowercased().hasPrefix("http"), let url = URL(string: content) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().interpolation(.none).scaledToFit()
                    } else {
                        ProgressView().tint(p.emerald)
                    }
                }
            } else if let data = Data(base64Encoded: content), let image = UIImage(data: data) {
                Image(uiImage: image).resizable().interpolation(.none).scaledToFit()
            } else if let image = QRImageFactory.make(content) {
                Image(uiImage: image).resizable().interpolation(.none).scaledToFit()
            } else {
                ZStack { p.elevated; Image(systemName: "qrcode").font(.system(size: 60)).foregroundColor(p.secondary) }
            }
        }
        .frame(width: size, height: size)
        .padding(10)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(p.separator, lineWidth: 0.5))
    }
}

struct AmountPrompt: Identifiable {
    let id = UUID()
    var title: String
    var caption: String = ""
    var maxCount: Int
    var confirmLabel = "确认"
    var unit = ""
    var onConfirm: (Int) -> Void
}

struct AmountPromptView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    var prompt: AmountPrompt
    @State private var text = "1"
    @FocusState private var focused: Bool

    var maxCount: Int { max(1, prompt.maxCount) }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(prompt.title).font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                    if !prompt.caption.isEmpty {
                        Text(prompt.caption).font(.system(size: 13)).foregroundColor(p.secondary)
                    }
                    HStack(spacing: 10) {
                        TextField("数量", text: $text)
                            .keyboardType(.numberPad)
                            .focused($focused)
                            .font(.system(size: 24, weight: .semibold, design: .rounded))
                            .padding(12)
                            .background(p.elevated)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Text(prompt.unit).font(.system(size: 14)).foregroundColor(p.secondary)
                    }
                    HStack(spacing: 8) {
                        ForEach([1, 5, 10, 50], id: \.self) { n in
                            ActionPill(title: "\(n)", enabled: n <= maxCount) { text = "\(n)" }
                        }
                        Spacer()
                        ActionPill(title: "最大", enabled: true) { text = "\(maxCount)" }
                    }
                    Text("可输入 1 – \(maxCount)").font(.system(size: 12)).foregroundColor(p.secondary)
                    PrimaryButton(title: prompt.confirmLabel) {
                        let n = min(maxCount, max(1, Int(text) ?? 1))
                        dismiss()
                        prompt.onConfirm(n)
                    }
                    .padding(.top, 4)
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
            .onAppear {
                text = "1"
                focused = true
            }
        }
    }
}

struct QuotePrompt: Identifiable {
    let id = UUID()
    var quote: RanchQuote
    var onConfirm: () -> Void
}

struct QuotePromptView: View {
    @Environment(\.palette) private var p
    @Environment(\.dismiss) private var dismiss
    var prompt: QuotePrompt
    @State private var busy = false

    var q: RanchQuote { prompt.quote }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(q.label.isEmpty ? "确认操作" : q.label)
                        .font(.system(size: 22, weight: .bold)).foregroundColor(p.text)
                    if q.count > 0 {
                        Text("数量：\(q.count)").font(.system(size: 14)).foregroundColor(p.secondary)
                    }
                    if !q.message.isEmpty {
                        Text(q.message).font(.system(size: 13)).foregroundColor(p.orange)
                    }
                    if !q.costItems.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("将消耗").font(.system(size: 14, weight: .semibold)).foregroundColor(p.secondary)
                            ForEach(q.costItems) { item in
                                HStack {
                                    Text(item.name).foregroundColor(p.text)
                                    Spacer()
                                    Text("×\(Fmt.compact(item.count))").foregroundColor(p.red)
                                }.font(.system(size: 14))
                            }
                        }
                        .padding(12).background(p.elevated)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    if !q.rewards.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("将获得").font(.system(size: 14, weight: .semibold)).foregroundColor(p.secondary)
                            ForEach(q.rewards) { item in
                                HStack {
                                    Text(item.name).foregroundColor(p.text)
                                    Spacer()
                                    Text("×\(Fmt.compact(item.count))").foregroundColor(p.green)
                                }.font(.system(size: 14))
                            }
                        }
                        .padding(12).background(p.elevated)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    Text("服务端已生成一次性确认签名，确认后立即执行。")
                        .font(.system(size: 12)).foregroundColor(p.secondary)
                    PrimaryButton(title: "确认执行", icon: "checkmark.shield.fill") {
                        busy = true
                        dismiss()
                        prompt.onConfirm()
                    }
                    .disabled(busy)
                }
                .padding(20)
            }
            .background(p.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
        }
        .interactiveDismissDisabled(busy)
    }
}

struct CountdownLabel: View {
    var date: Date?
    var prefix: String = ""
    var quiet: Bool = false
    var expiredText: String? = nil
    var emptyText: String = "等待排程"

    var body: some View {
        if quiet {
            Text("\(prefix)静默时段")
                .foregroundColor(.secondary)
        } else if let date, date.timeIntervalSince1970 > 1 {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                if let expiredText, date <= context.date {
                    Text(prefix + expiredText)
                } else {
                    Text(prefix + Fmt.countdown(to: date))
                }
            }
        } else {
            Text(prefix + emptyText).foregroundColor(.secondary)
        }
    }
}

struct ToastView: View {
    @Environment(\.palette) private var p
    var message: String
    var body: some View {
        Text(message)
            .font(.system(size: 13, weight: .medium))
            .foregroundColor(.white)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.black.opacity(0.82))
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
            .padding(.top, 8)
    }
}

struct ErrorBanner: View {
    @Environment(\.palette) private var p
    var message: String
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill").foregroundColor(p.orange)
            Text(message).font(.system(size: 12)).foregroundColor(p.text)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(p.orange.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct FieldLabel: View {
    @Environment(\.palette) private var p
    var text: String
    var body: some View {
        Text(text).font(.system(size: 13, weight: .medium)).foregroundColor(p.secondary)
    }
}

struct NumberField: View {
    @Environment(\.palette) private var p
    var title: String
    @Binding var value: Int
    var range: ClosedRange<Int> = 0...99999
    var body: some View {
        HStack {
            Text(title).font(.system(size: 14)).foregroundColor(p.text)
            Spacer()
            TextField("", value: $value, format: .number)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 70)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(p.text)
            Stepper("", value: $value, in: range).labelsHidden()
        }
    }
}

struct SwitchRow: View {
    @Environment(\.palette) private var p
    var title: String
    var subtitle: String? = nil
    @Binding var isOn: Bool
    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 14)).foregroundColor(p.text)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle).font(.system(size: 11)).foregroundColor(p.secondary)
                }
            }
        }
        .tint(p.emerald)
    }
}
