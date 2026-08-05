# OllamaServer · 项目介绍

<div align="right">

[**中文**](README.md) | [English](README.en.md)

</div>

> 基于 [sunshine0523/OllamaServer](https://github.com/sunshine0523/OllamaServer) 的 Android 端 Ollama 客户端增强版：内置 Ollama 引擎、模型下载/上传、对话管理，并新增长按菜单、对话参数设置、视觉模型图片输入与 Ollama Cloud 云端模型等能力。

---

## 📋 目录

- [项目概览](#项目概览)
- [功能特性（点击跳转详情）](#功能特性点击跳转详情)
- [分支改动](#分支改动)
- [功能详情](#功能详情)
- [上游 PR](#上游-pr)
- [技术栈](#技术栈)
- [构建说明](#构建说明)

---

## 项目概览

**OllamaServer** 是在 Android 设备上运行 `ollama serve` 的客户端应用，内置 Ollama 二进制与模型管理界面。本项目在保留上游全部能力的基础上，完成了以下工作：

- 🔧 内置 Ollama 引擎 **0.6.7 → 0.31.2** 升级（Termux Android/bionic 构建，含多模态与 7 个 CPU 指令集变体）
- 📋 长按消息**自定义操作菜单**（复制 / 分享 / 选择文本），替代原生 Alert 弹窗
- ⚙️ **对话设置弹窗**：温度 / Top P / 上下文长度，AsyncStorage 持久化
- 🖼️ 视觉模型**自动识别与图片输入**（系统相册 → 压缩 base64 → 预览 → 随消息发送）
- ☁️ **Ollama Cloud 云端模型**：设备密钥签名认证 + 浏览器授权登录
- 🐞 多项崩溃与构建修复（设置页 Animated 崩溃、加号红屏、图片 OOM、图标字体、debug 自动打包 JS）

---

## 功能特性（点击跳转详情）

| # | 功能 | 一句话描述 | 状态 |
|---|------|-----------|------|
| [F-01](#f01) | **Ollama 引擎升级** | 0.6.7 → 0.31.2，Termux 官方 Android 构建 | ✅ 已上线 |
| [F-02](#f02) | **长按消息菜单** | 复制 / 分享 / 选择文本，主题风格浮层 | ✅ 已上线 |
| [F-03](#f03) | **对话参数设置** | 温度 / Top P / 上下文长度 | ✅ 已上线 |
| [F-04](#f04) | **选择文本弹窗** | 只读可选择的全文复制 | ✅ 已上线 |
| [F-05](#f05) | **视觉模型图片输入** | 自动识别 + 系统相册 + 压缩发送 | ✅ 已上线 |
| [F-06](#f06) | **Ollama Cloud 云端模型** | 设备密钥签名 + 浏览器授权登录 | ✅ 已上线 |
| [F-07](#f07) | **云端 API Key 设置** | 设置页安全存储（🔑） | ✅ 辅助功能 |
| [F-08](#f08) | **崩溃修复合集** | 设置页 / 加号 / 图片 OOM / 安卓 12 | ✅ 已修复 |
| [F-09](#f09) | **图标字体修复** | debug 版图标不显示 | ✅ 已修复 |
| [F-10](#f10) | **Debug 自动打包 JS** | 不再手动维护 index.android.bundle | ✅ 已修复 |

---

## 分支改动

相对上游 `sunshine0523/OllamaServer`（master 停留在 `16e4a92`），本地按主题拆分为多个分支，并已推送上游 3 个 PR：

| 分支 | 改动内容 | 对应 PR |
|------|---------|---------|
| `master` | 本地主线：全部功能与修复合入（领先上游 25+ 提交） | — |
| `pr/features` | 下载稳定性修复 + 长按菜单 + 复制/分享 + 对话设置（10 个文件，+977/−234） | [sunshine0523/OllamaServer#31](https://github.com/sunshine0523/OllamaServer/pull/31) |
| `pr/ollama-0.31.2` | Ollama 0.6.7 → 0.31.2 升级：新二进制 + 依赖库 + OllamaExecutor 适配（21 个文件，含 54MB 二进制） | [sunshine0523/OllamaServer#30](https://github.com/sunshine0523/OllamaServer/pull/30) |
| `pr/build-fixes` | expo 模块依赖修复 + 构建配置 + GitHub Actions CI（5 个文件） | [sunshine0523/OllamaServer#32](https://github.com/sunshine0523/OllamaServer/pull/32) |
| `fix/issues-bc` | 早期历史遗留分支（含 `44775d5` / `d1b3f55` 两个提交，未推送上游） | — |

---

## 功能详情

<a id="f01"></a>

### F-01 · Ollama 引擎升级 0.6.7 → 0.31.2 `已上线`

**背景**：内置 Ollama 0.6.7 过旧——新模型（如 qwen2.5）加载崩溃，且不支持视觉多模态模型。官方 Linux 版是 glibc 构建，无法在 Android 上运行。

**方案**：

- 从 Termux 包源获取官方 Android/bionic 构建 `ollama_0.31.1_aarch64.deb`（含主程序 35MB + `lib/ollama/` 依赖库 19MB：7 个 libggml CPU 变体、libmtmd 多模态库、llama-server/llama-quantize）
- Python 脚本递归验证 19 个 ELF 的 NEEDED 依赖全部满足（无 glibc 依赖）
- OllamaExecutor 适配：递归复制 assets、注入 `LD_LIBRARY_PATH` / `OLLAMA_LIBRARY_PATH` / `OLLAMA_TMPDIR` / `HOME`
- 关键坑：assets 复制不保留可执行位 → 手动 `setExecutable(true)`；Android 无 `/tmp` → 显式 `OLLAMA_TMPDIR`；`version.txt` 版本机制强制重初始化

**涉及文件**：`OllamaExecutor.kt` · `android/app/src/main/assets/arm64-v8a/`（二进制 + lib + version.txt）

**验证**：✅ 模型下载、加载、推理均正常；llama-server `permission denied` 已通过可执行位修复解决。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f02"></a>

### F-02 · 长按消息自定义操作菜单 `已上线`

**背景**：原长按消息弹出的是 Android 原生 Alert 弹窗，与软件 MD3 主题风格不匹配。

**方案**：

- 新增 `MessageActionMenu.tsx`：在长按位置弹出主题风格浮层（复制 / 分享）
- 二级菜单：点「复制」→ 选择文本 / 复制全部；分享走系统分享面板
- 替换 HomePage 中原有的 `Alert.alert` 调用

**涉及文件**：`MessageActionMenu.tsx` · `SelectTextModal.tsx` · `HomePage.tsx`

**验证**：✅ 复制、选择文本、分享均正常。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f03"></a>

### F-03 · 对话参数设置 `已上线`

**背景**：需要控制生成参数（温度 / Top P / 上下文长度）以适配不同模型与任务。

**方案**：

- 右上角设置按钮（⚙️）→ `ChatSettingsModal.tsx`：温度 0.0–2.0、Top P 0.0–1.0、上下文 0/2048/4096/8192/16384
- `Storage.ts` 新增 `ChatSettings` + `loadChatSettings/saveChatSettings`（AsyncStorage 持久化，默认 0.8 / 0.9 / 0）
- `OllamaApi.chat()` 支持 `options`（temperature / top_p / num_ctx，只传显式值）；发送前按 num_ctx 粗略估算（1 token ≈ 2 字符）截断历史

**涉及文件**：`ChatSettingsModal.tsx` · `Storage.ts` · `OllamaApi.ts` · `HomePage.tsx`

**验证**：✅ 设置即时生效并持久化，重启后保留。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f04"></a>

### F-04 · 选择文本只读弹窗 `已上线`

**背景**：选择文本弹窗最初可编辑/删除 AI 回复内容；尝试 `editable={false}` 后又发现 Android 上会同时禁用文本选择。

**方案**：

- 保持可编辑状态但内容不可变：受控 `value` + 忽略 `onChangeText`
- 追加 `showSoftInputOnFocus={false}`：点击文本不弹键盘干扰选区

**涉及文件**：`SelectTextModal.tsx`

**验证**：✅ 可长按拖动选择、全选、复制选中，内容不可修改。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f05"></a>

### F-05 · 视觉模型图片输入 `已上线`

**背景**：加载视觉模型（llava / qwen2.5-vl）后没有图片输入入口，无法进行多模态对话。

**方案**：

- 模型切换时调 `/api/show` 读取 capabilities，自动识别视觉模型；仅视觉模型显示输入栏左侧「+」加号（为未来其他模态留扩展点）
- 原生 `FileUploadModule.pickImage()`：Android 13+ 系统 Photo Picker，旧版回退图库（兼容 ClipData 返回值）
- `readImageAsBase64()`：采样到 1024px 内 + JPEG 70 压缩 → base64（`catch Throwable` 防 OOM 闪退）
- 输入栏上方预览（可 ✕ 删除）；发送时 `Message.images=[base64]` 随 `/api/chat` 发送，气泡内渲染

**涉及文件**：`Chat.ts` · `OllamaApi.ts` · `FileUploadModule.kt` · `HomePage.tsx`

**验证**：✅ 加号 → 系统相册 → 选图 → 预览 → 发送全链路可用（含安卓 12 崩溃修复）。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f06"></a>

### F-06 · Ollama Cloud 云端模型 `已上线`

**背景**：手机本地跑不动大模型。Ollama Cloud 把推理 offload 到 ollama.com，本地 API 完全兼容（如 `gpt-oss:120b-cloud`）。

**关键调研结论**：

- `OLLAMA_API_KEY` 只对「直连 ollama.com API」的客户端生效（Claude Desktop / Qwen 集成），**ollama serve 不读取它**
- serve 的云端认证 = 设备 ed25519 私钥签名（`~/.ollama/id_ed25519`）+ ollama.com 设备关联；私钥只有 CLI 的 `initializeKeypair` 生成，serve 不会自动生成

**方案**：

- 新增 `CloudAuthModule.kt`：生成 ed25519 密钥对（私钥 PKCS8 PEM，ollama 的 x/crypto/ssh 可解析；公钥 authorized_keys 格式，raw32 通过 X.509 SPKI 解析提取——Android 无 JDK15+ 的 EdECPublicKey 接口）
- 设置页「登录 Ollama Cloud」：打开 `https://ollama.com/connect?name=android&key=<base64url公钥>` 浏览器授权，将设备关联到 ollama.com 账号
- OllamaExecutor 启动 serve 前确保密钥存在；移除无效的 `OLLAMA_API_KEY` 注入

**涉及文件**：`CloudAuthModule.kt` · `OllamaExecutor.kt` · `SettingsPage.tsx` · `AppReactPackage.java`

**验证**：✅ 浏览器授权后云端模型可正常对话（需 ollama.com 账号，云端模型按 token 计费）。

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f07"></a>

### F-07 · 云端 API Key 设置 `辅助功能`

**方案**：

- 设置 → 应用设置 →「Ollama 云端 API Key」（🔑 图标），`secureTextEntry` 输入
- `OllamaConfigModule` 原生桥接 `setCloudApiKey / getCloudApiKey`（SharedPreferences 存储）
- 修复 JS 侧以 callback 方式调用 Promise 方法导致 Key 永远读不出来的问题

**涉及文件**：`SettingsPage.tsx` · `OllamaConfigModule.kt` · `OllamaExecutor.kt`

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f08"></a>

### F-08 · 崩溃修复合集 `已修复`

- **设置页 Animated 崩溃（release 闪退）**：`ModelSelector.tsx` 的 `Animated.Value` 在函数体直接 new，每次渲染重建原生节点，打开设置触发 HomePage 重渲染时节点失效 → 改用 `useRef`
- **加号红屏**：`HomePage.tsx` 使用 `NativeModules` 却未 import，Hermes 运行时报 ReferenceError → 补 import + async/await 方式调用 Promise 方法
- **图片 OOM 闪退（安卓 12）**：采样只缩到 2 倍边长（约 12.5MB bitmap）且 `catch(Exception)` 接不住 `OutOfMemoryError` → 直接采样到 1024px 内 + `catch Throwable`
- **云端登录编译失败**：Android SDK 无 `java.security.interfaces.EdECPublicKey`（JDK15+）→ 改 X.509 SPKI 解析提取公钥

**涉及文件**：`ModelSelector.tsx` · `HomePage.tsx` · `FileUploadModule.kt` · `CloudAuthModule.kt`

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f09"></a>

### F-09 · 图标字体修复 `已修复`

**背景**：debug 版图标不显示（release 正常）——react-native-vector-icons 的字体未打进 debug APK。

**方案**：在 `android/app/build.gradle` 文件末尾 `apply from: file("../../node_modules/react-native-vector-icons/fonts.gradle")`，构建时自动把 .ttf 字体打进 APK assets。

**涉及文件**：`android/app/build.gradle`

[↑ 返回功能特性](#功能特性点击跳转详情)

<a id="f10"></a>

### F-10 · Debug 自动打包 JS Bundle `已修复`

**背景**：此前手动把 `index.android.bundle` 嵌入仓库让 debug 版离线可跑，升级后本地文件丢失被误删，debug 版回到红屏。

**方案**：设置 `debuggableVariants = []`，debug 构建自动执行 `export:embed` 打包 JS，无需再手动维护 bundle 文件。

**涉及文件**：`android/app/build.gradle`

[↑ 返回功能特性](#功能特性点击跳转详情)

---

## 上游 PR

三个主题分支已推送到上游 `sunshine0523/OllamaServer`，均 **mergeable / clean**，等待维护者 review：

| PR | 内容 | 状态 |
|----|------|------|
| [#30](https://github.com/sunshine0523/OllamaServer/pull/30) | Ollama 0.31.2 升级（修复 qwen2.5 崩溃 [#25](https://github.com/sunshine0523/OllamaServer/issues/25)、视觉支持 [#28](https://github.com/sunshine0523/OllamaServer/issues/28)） | ✅ 可合并 |
| [#31](https://github.com/sunshine0523/OllamaServer/pull/31) | 下载稳定性 + 长按菜单 + 对话设置 | ✅ 可合并 |
| [#32](https://github.com/sunshine0523/OllamaServer/pull/32) | expo 模块缺失修复（ExpoAsset 闪退）+ 依赖锁定 + GHA CI | ✅ 可合并 |

---

## 技术栈

`React Native 0.77` · `Expo 53` · `react-native-paper 5.15 (MD3)` · `Kotlin 原生模块` · `Ollama 0.31.2 (Termux bionic)` · `AsyncStorage` · `react-native-vector-icons` · `i18next 中/英` · `GitHub Actions CI`

## 构建说明

- GitHub Actions（xis3794 账户）自动构建 debug / release APK
- 构建产物下载到 `/storage/emulated/0/Download/Operit/ollama_apk/`

---

<div align="center">

**OllamaServer 增强版** · 基于 [sunshine0523/OllamaServer](https://github.com/sunshine0523/OllamaServer) 二次开发

</div>