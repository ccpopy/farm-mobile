# Farm Mobile（农场助手 Android 客户端）

面向自托管农场服务的原生 Android 控制客户端，基于 **Kotlin + Jetpack Compose + Material 3** 构建，直接对接后端已有 HTTP REST 与 Socket.IO 实时接口，不依赖 Web 响应式套壳。

> **服务端兼容说明**
>
> 本仓库为上游开源服务端 **[liyangpengs/qq-farm-bot](https://github.com/liyangpengs/qq-farm-bot)** 的专用适配版本（初始版本 **v0.0.1**）：
> - 直接配合 **[liyangpengs/qq-farm-bot](https://github.com/liyangpengs/qq-farm-bot)** 使用，**无需修改上游服务端源码**，也不需要安装额外插件扩展。
> - 使用上游的**「自动化设置」**接口管理种植与养殖策略、日常农牧、施肥、好友互动及神秘商人，并可查看定时任务状态。登录支持**管理员账号密码或会话 Token**。
> - 客户端包名为 `io.github.ccpopy.farmmobile`（应用名展示为「农场助手」），可与私有版客户端同时安装在同一台设备中互不冲突。

## 核心能力

- **远程服务器配置与会话授权**
  - 首次连接页服务器地址默认留空并带 `https://` 示例提示，支持局域网 IP、域名与 HTTPS 反向代理，内置 `/api/game-version` 连通性测试。
  - **管理员账号密码登录**：使用与 Web 管理面板相同的用户名和密码登录；服务端重启后重新登录即可。
  - **会话 Token 授权**：也支持直接粘贴已登录 Web 面板的有效 `x-admin-token` 会话凭证。
- **移动端 5-Tab 信息架构与全局空间切换**
  - 底部 5 个标签页：**总览 · 农场/牧场 · 发现 · 好友 · 我的**；顶部全局 **「农场 ↔ 牧场」** 切换器同步总览视图、工作台、发现中心与好友交互。
- **总览与实时动态**
  - Socket.IO 实时同步账号在线状态、经验进度、金币/点券/金豆豆/钻石资产、化肥/罐头桶余量、今日统计、每日礼包及运行日志流；支持一键购买普通/有机化肥。
- **农场与完整牧场工作台**
  - **农场工作台**：3 列土地宫格、土壤等级与升级信息、实时成熟倒计时、一键收获/除草浇水/种植/施肥及单块地详情操作，内置背包与看门狗宠物管理。
  - **牧场工作台**：提供**动物 / 设施 / 背包 / 任务 / 图鉴** 5 个子视图，支持按窝/棚管理动物、牧草与罐头桶维护、设施升级与图鉴奖励领取，关键消费与放生动作均经上游 `quote -> signature` 二次确认。
- **自动化设置中心（对齐上游官方模块）**
  - 从「总览」快捷入口或「我的 → 自动化设置」进入，集中配置**种植策略与优先种子**、**农场/好友巡查间隔与种植节奏**、**日常农场与邮箱领取**、**日常牧场与养殖策略（含指定窝/棚动物）**、**施肥模式/范围土地/自动补肥**、**好友互动/静默时段/自动通过申请规则**以及**神秘商人自动购买策略**，并展示当前账号 Worker 的只读定时任务概览（`/api/scheduler`）。
- **发现中心（活动 · 商城 · 节令日历）**
  - **活动中心**：同步限时、常驻与往期活动目录，内置**牧歌初启**、**奶茶热销中**、**快乐不独享**、**秋祈良愿**、**千星游记**、**七夕鹊桥**、**青酿换万金**、**公益小红花**与**雨落成诗**原生交互界面。
  - **商城系统**：支持游戏商城、限时商店、道具商店、SVIP、神秘商人以及牧场商店 / 仓鼠集市 / 罐头商城。
  - **节令日历**：展示农历/公历节令卡片、当季进度收集与答题领奖。
- **农/牧场好友互动**
  - 支持农场与牧场好友访问、可偷/可帮筛选、偷取/照顾/捣乱、牧场喂蚊子/抚摸动物、访客记录与黑名单管理。
- **简洁防堆积通知推送（默认关闭）**
  - 支持按 13 类农牧与好友动作自主订阅，采用分类折叠更新防止通知栏刷屏，开启时按 Android 13+ 规范请求 `POST_NOTIFICATIONS` 权限。
  - 账号掉线重登提醒基于上游 `/api/account-logs` 与 Socket.IO `account-log:new` 监听登录失效、被踢下线与异常断连。
- **应用内检查更新与下载加速**
  - 支持从本公开仓库 GitHub Releases 检查 `v<版本>` 发布与更新说明，支持默认及自定义 HTTPS 镜像加速下载（`https://ghfast.top`）与安装包完整性及签名校验。

## 构建、签名与发布

1. 本地开发可用 Android Studio 打开仓库根目录，使用 **JDK 17**、**Gradle 8.11.1** 与 **Android SDK Platform 35**。
2. GitHub Actions 自动构建与 Release 发布（[android-build.yml](.github/workflows/android-build.yml)）：
   - 在本仓库 **Settings → Secrets and variables → Actions** 配置：
     - `ANDROID_RELEASE_KEYSTORE_BASE64`：PKCS12 签名证书（key alias 须为 `farm-mobile`）的 Base64 内容
     - `ANDROID_RELEASE_KEYSTORE_PASSWORD`：签名证书密码
   - 核对 `app/build.gradle.kts` 中的 `versionName` 与 `versionCode` 后，创建带更新说明的 `v<版本>` 标签（例如 `v0.0.1`）：
     ```bash
     git tag -a v0.0.1 -F release-notes.md --cleanup=verbatim
     git push origin v0.0.1
     ```
   - 工作流会自动运行 `gradle lintRelease assembleRelease --no-daemon`、签名校验，并以标签说明创建 GitHub Release，上传 `farm-mobile-<版本>-release.apk` 及保留 Actions Artifact。
