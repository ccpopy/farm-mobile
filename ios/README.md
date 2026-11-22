# Farm Mobile iOS（农场助手 iOS 端）

面向开源自托管农场服务 **liyangpengs/qq-farm-bot** 的原生 iOS 客户端，使用 **SwiftUI + async/await + URLSession WebSocket** 实现，与同仓库 Android 端共用同一套 REST 与 Socket.IO 实时协议。

- 运行环境：**iOS 16.0+**
- 开发环境：**Xcode 16+ / Swift 5.9**
- Bundle ID：**io.github.ccpopy.farmmobile**
- 许可协议：与上游一致，**CC BY-NC-SA 4.0**（署名 · 非商业性使用 · 相同方式共享）

## 已实现功能

- **服务器与授权**
  - 支持局域网 IP、域名、HTTPS 反向代理，自动规范化 http/https 地址。
  - 支持管理员账号密码登录，或直接粘贴 Web 管理面板的 x-admin-token。
  - 多服务器保存、切换、重命名、删除；Token 存在 **iOS Keychain**，服务器列表与偏好存在 UserDefaults。
  - 账号添加支持手动凭证、**微信扫码**与 **QQ 扫码**，扫码状态每 1.4 秒轮询并自动保存。
  - 登录前可通过 /api/game-version 测试连通性并显示 Bot 版本。
- **实时总览**
  - 底部 5 标签：总览 · 农场/牧场 · 发现 · 好友 · 我的，顶部全局「农场 ↔ 牧场」切换。
  - Socket.IO 实时同步在线状态、等级、经验进度、经验速率、升级预测、农场/好友巡查倒计时。
  - 金币、点券、金豆豆、钻石紧凑显示，点击数据卡切换精确值。
  - 化肥/收藏点库存、今日礼包、今日收益、运行日志实时刷新与清空。
  - 牧场模式展示等级、牧草、待处理、可收、动物数量、牧场生涯与收偷比。
- **农场工作台**
  - 3 列土地宫格、土壤等级、成熟倒计时、缺水/草/虫标记、合种标记、化肥剩余次数。
  - 一键收获/护理/种植/普通肥/有机肥/升级土地，单块土地详情与操作。
  - 农场背包使用、出售、锁定/解锁；系统物品库存汇总。
  - 看门狗出战/收回、狗粮喂养、守护时长与礼物领取。
- **牧场工作台**
  - 动物/设施/背包/任务 4 个子视图。
  - 按窝/棚分组动物，展示阶段、巨型体重、产物进度、下次时间；一键收获/生产/清理/喂养。
  - 设施补牧草、升级、完成升级、沙漏加速、激活棚。
  - 牧场背包放养、罐头补充、出售、批量锁定/解锁、出售全部产物。
  - 关键消费与放生执行服务端 quote → signature → execute 二次确认。
- **发现中心**
  - 活动目录（农场/牧场两套）；原生实现牧歌初启、快乐不独享、秋祈良愿、千星游记、鹊桥寄情的核心领取/分享/祈愿/通行证/星座/兑换/赠礼流程。
  - 奶茶提供实时服务端数据面板；青酿换万金、公益小红花、雨落成诗已提供原生操作界面。
  - 商城：游戏商城、限时商店、道具商店、SVIP、神秘商人；牧场商店、仓鼠集市、礼包商城；购买/兑换走服务端校验。
  - 节令日历：季节进度、节令奖励、趣味问答。
- **好友**
  - 农场：全部/可偷/可帮筛选，偷取、帮务农、捣乱、好友土地网格、访客记录、黑名单。
  - 牧场：好友列表、清理、偷取、放蚊子、抚摸动物，好友牧场生涯与货架状态。
- **设置与自动化**
  - 自动化设置完整表单：种植策略、背包种子顺序、巡查间隔、施肥与补肥、好友自动接受、养殖策略、动物指定、牧场/好友/神秘商人开关、静默时段。
  - 只读定时任务概览（/api/scheduler）。
  - 图鉴：作物/超变进度与领取、种子购买、蓝晶藏品评分奖励、升星/陈列。
  - 浅色/深色/跟随系统；通知分类订阅与测试通知；日志清理。

## 目录结构

    FarmMobile/
    ├── FarmMobileApp.swift          # App 入口
    ├── Theme.swift                  # Apple 风格色板、格式化工具
    ├── Info.plist                   # ATS / 本地网络 / 启动屏
    ├── Core/
    │   ├── JSONValue.swift          # 宽松 JSON 解析（兼容 int/double/string/bool 混用）
    │   ├── Models.swift             # 账号、土地、背包、牧场、好友、商城、活动、图鉴模型
    │   ├── APIClient.swift          # 全部 REST 接口与错误归一化
    │   ├── SocketClient.swift       # Engine.IO v4 + Socket.IO 文本协议客户端
    │   ├── Persistence.swift        # Keychain + UserDefaults 多服务器配置
    │   ├── Notifications.swift      # 本地通知权限与投递
    │   └── AppStore.swift           # @MainActor 会话状态、轮询、实时事件合并
    └── Views/
        ├── RootView.swift           # 启动 / 连接 / 5 标签主壳
        ├── ServerSetupView.swift    # 服务器地址与授权
        ├── OverviewView.swift       # 总览
        ├── FarmWorkspaceView.swift  # 农场 + 背包 + 看门狗
        ├── RanchWorkspaceView.swift # 牧场 + 设施 + 背包 + 任务
        ├── FriendsView.swift        # 农牧好友与互动
        ├── DiscoverView.swift       # 活动中心与活动详情
        ├── MallView.swift           # 农场/牧场商城
        ├── SolarCalendarView.swift  # 节令日历与问答
        ├── SettingsView.swift       # 我的 / 通知 / 服务器
        ├── AutomationView.swift     # 自动化设置
        ├── IllustratedView.swift    # 图鉴与藏品
        ├── AccountSheets.swift      # 账号/服务器管理
        └── Components.swift         # 卡片、指标、弹窗、确认签名等通用组件

## 构建

1. 在 macOS 上用 **Xcode 16** 打开 FarmMobile.xcodeproj。
2. 选择 FarmMobile Target → Signing & Capabilities：勾选 Automatically manage signing，选择 Team；如 Bundle ID 冲突，改成你的唯一 ID。
3. 选择 iOS 16+ 模拟器或真机，Run。
4. 首次启动填写服务器地址，使用管理员账号密码或 x-admin-token 连接。

> 如果手动维护 project.pbxproj 遇到 IDE 兼容问题，仓库同时提供 project.yml，安装 XcodeGen 后执行 xcodegen generate 可重新生成工程。

### 命令行构建

    xcodebuild -project FarmMobile.xcodeproj \
      -scheme FarmMobile \
      -configuration Debug \
      -sdk iphonesimulator \
      -destination 'generic/platform=iOS Simulator' \
      build

## 与 Android 端的协议对照

| 能力 | 接口 |
| --- | --- |
| 版本 / 登录 / Token | /api/game-version、/api/login、/api/auth/validate |
| 账号 | /api/accounts、/start、/stop、DELETE、/api/account/remark |
| 总览 | /api/status、/api/diamond、/api/daily-gifts、/api/logs |
| 农场 | /api/lands、/api/farm/operate、/api/farm/fertilize(-all) |
| 背包 | /api/bag、/use、/sell、/lock、/api/bag/seeds |
| 看门狗 | /api/pets、/deploy、/withdraw、/food/use、/api/dog/skill-gifts/claim |
| 牧场 | /api/ranch?view=...、/api/ranch/{action}/quote、/api/ranch/{action} |
| 好友 | /api/friends、/api/friend/{gid}/lands、/op、/api/interact-records、/api/friend-blacklist |
| 自动化 | /api/settings、/api/settings/save、/api/scheduler、/api/ranch/automation-options |
| 商城 | /api/game-mall、/api/props-shop、/api/mystery-shop、牧场 view=shop/market/mall |
| 节令 | /api/solar-calendar、/api/solar-calendar/{action} |
| 活动 | /api/activity-center/activities、/api/activities/*、/api/activity-center/stellar|qixi|... |
| 图鉴 | /api/illustrated、/claim、/api/seeds/purchase、/api/collections |

实时事件与 Android 端一致：

- Socket.IO 路径：<服务器路径>/socket.io/，仅 WebSocket，EIO=4。
- 握手 auth：{"token": "...", "accountId": "..."}；连接后发送 subscribe。
- 事件：status:update、log:new、logs:snapshot、account-log:new。
- 实时断开后自动指数退避重连，同时保留 5 秒轮询兜底。

## APNs 推送中继

仓库包含可直接部署的 Node.js 中继：`server/push-relay`。

    cd server/push-relay
    cp .env.example .env
    # 填写 FARM_BASE_URL、FARM_ADMIN_TOKEN、RELAY_SECRET 与 APNs .p8 参数
    npm install
    npm start

然后在 App 的「我的 → 消息推送 → 远程推送 (APNs)」填写中继地址与密钥。完整说明、Docker 部署、sandbox/production 区别见 `server/push-relay/README.md`。

APNs 需要真机、付费 Apple Developer 账号，并在 Xcode 中为 Target 开启 Push Notifications 能力；本工程已内置 `FarmMobile.entitlements`、`UIBackgroundModes` 与 BGTask 标识。

## 说明

- 本工程在 Windows 环境完成，已用 Swift AST 解析器做全量语法校验；最终编译、签名与真机验证需在 macOS + Xcode 完成。
- iOS 后台不能常驻 WebSocket。本项目提供三层替代：
  1. **APNs 远程推送**：`server/push-relay` 常驻服务器连接 bot Socket，App 在后台或被系统回收后仍能收到通知。
  2. **BGAppRefresh 兜底**：系统择机唤醒（约 15 分钟起，不保证）拉取日志并发送本地通知。
  3. **前台 Socket**：App 在前台时保持实时状态、倒计时和日志刷新。
- Token 使用 Keychain 存储；Info.plist 已允许自托管常见的 http:// 局域网地址与本地网络访问。
- 上游 Android 项目采用 CC BY-NC-SA 4.0，本 iOS 端保留相同署名与非商业限制，禁止倒卖源码或安装包。
