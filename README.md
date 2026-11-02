# ⚡ 充电计时 (ChargeTimer)

一款基于 **Kotlin + Jetpack Compose (Material 3)** 构建的现代 Android 智能充电监控应用。专注于精确统计**每次充电从接入到断开的全过程时长、充入电量、实时充电功率（瓦数 W）**，支持达到目标电量自动停止计时、Webhook 自动化推送通知以及全量数据导出与深度图表分析。

---

## ✨ 核心特性

- **⚡ 全自动后台无感运行**：
  - 注册系统级电源插拔广播与开机自启（`BOOT_COMPLETED`）。
  - 插上充电器（快充/USB/无线充）系统自动唤起前台轻量服务开启高精度跑秒与功率采样，**完全无需手动打开 App**。
  - 拔掉充电器时自动结算总用时并持久化保存至本地 SQLite / Room 数据库，随即可靠关闭前台服务。
- **🔌 实时充电功率（W）与电流采样**：
  - 结合实时电压（$U$）与电流（$I$）动态计算实时充电功率（如 `65.0W`、`45.2W`）。
  - 自动记录单次充电周期的**最高峰值功率**与**全程平均功率**。
- **🎯 充满到设定电量自动停止计时**：
  - 支持将目标电量自由设定在 `50% ~ 100%`（默认推荐 `80%` 锂电池黄金养护区间）。
  - 电量充至设定值时**自动锁定充至目标所耗时长**，并支持震动提示拔除电源，防止过度浮充。
- **🤖 Webhook 机器人自动化推送**：
  - 原生兼容**钉钉机器人**、**企业微信群机器人**、**飞书自定义机器人**、**Bark**、**Server酱**及自建 HTTP POST Webhook。
  - 支持分别勾选：**开始充电推送**、**达到目标电量推送**、**断开完成推送**。
  - 提供一键发送测试通知功能，即刻反馈连通状态。
- **📊 充电深度数据分析与习惯看板**：
  - 核心指标：累计充电总时长、平均单次时长、平均充入电量、平均充电功率、充至目标平均耗时。
  - 单次充电柱状趋势图：直观对比近期充电周期的耗时走势与充入电量。
  - 习惯规律分析：AC 快充 / USB / 无线充电的使用次数占比，早中晚夜常用充电时段统计。
- **📤 数据全量导出与分享**：
  - **CSV 表格**：全字段明细（含起止时间戳、有效耗时、目标达成耗时、电量增幅、功率、电压、温度、备注等），支持 Excel / WPS / Numbers 打开。
  - **JSON 格式**：标准格式便于二次量化分析或个人知识库归档。
  - **格式化文本简报**：一键生成并复制总结摘要到剪贴板，方便发送到聊天或备忘录。
- **🎛️ 内置模拟测试控制器**：
  - 面向模拟器或桌面环境，可自由拖动电量滑块、选择充电器类型并一键模拟插拔电源，即时验证跑秒与结算归档逻辑。

---

## 🚀 GitHub Actions 自动构建与 APK 生成

项目已预置自动化构建脚本（`.github/workflows/build-apk.yml`），无需在本地安装繁琐的 Android SDK 即可自动获取 APK 安装包：

### 方式一：推送到分支自动构建
1. 每次 `git push` 到 `main` 或 `master` 分支，GitHub Actions 将自动执行单元测试并编译 Debug APK。
2. 进入仓库页面的 **Actions** 标签页，点击最新一条运行记录。
3. 在页面底部的 **Artifacts（工件）** 区域，直接点击下载 **`ChargeTimer-Debug-APK`** 解压即可得到安装包。

### 方式二：网页端手动一键触发（workflow_dispatch）
1. 在 GitHub 仓库页面点击 **Actions** 标签。
2. 在左侧列表中选择 **“Build & Release Android APK”**。
3. 点击右侧 **“Run workflow”** 按钮即可手动触发打包。

### 方式三：打 Tag 自动发布 Release
当你想发布正式版本时，只需在本地推送一个版本标签：
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions 会自动编译并创建对应的 **GitHub Release**，APK 将作为 Release 附件直接提供下载。

---

## 🛠️ 本地编译与运行指南

### 环境要求
- **Android Studio**：Ladybug (2024.2.1) 或更高版本
- **JDK**：OpenJDK 17 或 21
- **Gradle**：8.x / 9.x（项目已自带 Gradle Wrapper）
- **系统要求**：Android 7.0（API 级别 24）及以上

### 命令行编译步骤
```bash
# 1. 克隆代码仓库
git clone <your-repo-url>
cd <project-directory>

# 2. 赋予 Gradle Wrapper 执行权限 (Linux / macOS)
chmod +x gradlew

# 3. 运行本地自动化单元测试与 Robolectric 测试
./gradlew testDebugUnitTest

# 4. 编译 Debug APK
./gradlew assembleDebug

# 编译产物路径位于：
# app/build/outputs/apk/debug/app-debug.apk
```

### 安装到手机
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 核心架构与技术栈

- **开发语言**：100% Kotlin
- **UI 框架**：Jetpack Compose + Material 3 设计规范
- **架构模式**：MVVM (Model - View - ViewModel) + Repository 模式
- **异步响应式**：Kotlin Coroutines + StateFlow / Flow
- **本地数据库**：AndroidX Room (SQLite)
- **网络组件**：OkHttp 4（轻量异步 Webhook 推送）
- **后台机制**：BroadcastReceiver (`ACTION_POWER_CONNECTED/DISCONNECTED`, `BOOT_COMPLETED`) + Foreground Service (带有前台动态跑秒与功率常驻通知)
- **单元测试**：JUnit4 + Robolectric（本地 JVM 快速驱动测试）

---

## 🔒 权限与隐私声明

应用始终践行最小权限与本地离线优先原则：
- `POST_NOTIFICATIONS`：Android 13+ 用于在前台常驻通知中展示充电时长与功率，以及完成时提醒。
- `RECEIVE_BOOT_COMPLETED`：用于手机开机重启后维持充电插拔监听。
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`：在系统后台锁屏期间维持精准计时与功率采样。
- `INTERNET`：仅用于向用户自主配置的 Webhook URL 推送充电消息，绝无第三方追踪或遥测数据上传。
- `VIBRATE`：充至设定目标电量时的触感提醒。

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
