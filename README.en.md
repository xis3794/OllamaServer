# OllamaServer · Project Intro

<div align="right">

[中文](README.md) | [**English**](README.en.md)

</div>

> An enhanced Android Ollama client forked from [sunshine0523/OllamaServer](https://github.com/sunshine0523/OllamaServer): bundled Ollama engine, model download/upload, conversation management, plus long-press menus, chat parameter settings, vision model image input and Ollama Cloud support.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features (click to jump)](#features-click-to-jump)
- [Branches](#branches)
- [Feature Details](#feature-details)
- [Upstream PRs](#upstream-prs)
- [Tech Stack](#tech-stack)
- [Build](#build)

---

## Overview

**OllamaServer** runs `ollama serve` on Android with a bundled Ollama binary and model management UI. On top of upstream capabilities, this project delivered:

- 🔧 Bundled Ollama engine **0.6.7 → 0.31.2** upgrade (Termux Android/bionic build, multimodal + 7 CPU instruction-set variants)
- 📋 Custom **long-press message menu** (copy / share / select text) replacing native Alert dialogs
- ⚙️ **Chat settings modal**: temperature / Top P / context length, persisted via AsyncStorage
- 🖼️ Vision model **auto-detection and image input** (system gallery → compressed base64 → preview → send with message)
- ☁️ **Ollama Cloud models**: device keypair signature auth + browser connect sign-in
- 🐞 Crash & build fixes (settings Animated crash, attach red screen, image OOM, icon fonts, debug JS bundling)

---

## Features (click to jump)

| # | Feature | One-liner | Status |
|---|---------|-----------|--------|
| [F-01](#f01) | **Ollama Engine Upgrade** | 0.6.7 → 0.31.2, official Termux Android build | ✅ LIVE |
| [F-02](#f02) | **Long-press Message Menu** | Copy / share / select text, themed overlay | ✅ LIVE |
| [F-03](#f03) | **Chat Parameter Settings** | Temperature / Top P / Context length | ✅ LIVE |
| [F-04](#f04) | **Select Text Modal** | Read-only selectable full-text copy | ✅ LIVE |
| [F-05](#f05) | **Vision Image Input** | Auto-detect + gallery + compressed send | ✅ LIVE |
| [F-06](#f06) | **Ollama Cloud Models** | Device keypair sign-in + browser connect | ✅ LIVE |
| [F-07](#f07) | **Cloud API Key Setting** | Secure storage in settings (🔑) | ✅ UTILITY |
| [F-08](#f08) | **Crash Fixes** | Settings / attach / OOM / Android 12 | ✅ FIXED |
| [F-09](#f09) | **Icon Font Fix** | Missing icons in debug builds | ✅ FIXED |
| [F-10](#f10) | **Debug JS Bundling** | No manual index.android.bundle | ✅ FIXED |

---

## Branches

Relative to upstream `sunshine0523/OllamaServer` (master at `16e4a92`), work is split into topic branches; 3 PRs were opened upstream:

| Branch | Changes | PR |
|--------|---------|-----|
| `master` | Local mainline: all features & fixes merged (25+ commits ahead) | — |
| `pr/features` | Download stability + long-press menu + copy/share + chat settings (10 files, +977/−234) | [sunshine0523/OllamaServer#31](https://github.com/sunshine0523/OllamaServer/pull/31) |
| `pr/ollama-0.31.2` | Ollama 0.6.7 → 0.31.2: new binary + libs + OllamaExecutor adaptation (21 files incl. 54MB binaries) | [sunshine0523/OllamaServer#30](https://github.com/sunshine0523/OllamaServer/pull/30) |
| `pr/build-fixes` | expo module deps + build config + GHA CI (5 files) | [sunshine0523/OllamaServer#32](https://github.com/sunshine0523/OllamaServer/pull/32) |
| `fix/issues-bc` | Legacy branch (commits `44775d5` / `d1b3f55`, not pushed upstream) | — |

---

## Feature Details

<a id="f01"></a>

### F-01 · Ollama Engine Upgrade 0.6.7 → 0.31.2 `LIVE`

**Background**: Bundled Ollama 0.6.7 was too old: newer models (e.g. qwen2.5) crashed, and vision models were unsupported. Official Linux builds are glibc-based and cannot run on Android.

**Solution**:

- Fetched official Android/bionic build `ollama_0.31.1_aarch64.deb` from Termux repos (35MB binary + 19MB libs: 7 libggml CPU variants, libmtmd multimodal, llama-server/llama-quantize)
- Python script verified NEEDED deps of all 19 ELFs (no glibc dependency)
- OllamaExecutor adaptation: recursive asset copy, `LD_LIBRARY_PATH` / `OLLAMA_LIBRARY_PATH` / `OLLAMA_TMPDIR` / `HOME` env
- Key pitfalls: assets lose exec bit → manual `setExecutable(true)`; no `/tmp` on Android → explicit `OLLAMA_TMPDIR`; `version.txt` forces re-init

**Files**: `OllamaExecutor.kt` · `android/app/src/main/assets/arm64-v8a/` (binary + lib + version.txt)

**Status**: ✅ Model pull/load/inference work; llama-server 'permission denied' fixed via exec bit.

[↑ Back to features](#features-click-to-jump)

<a id="f02"></a>

### F-02 · Long-press Message Menu `LIVE`

**Background**: Long-press used a native Alert dialog that clashed with the app's MD3 theme.

**Solution**:

- New `MessageActionMenu.tsx`: themed overlay at the long-press position (copy / share)
- Two-level menu: 'Copy' → select text / copy all; share via system share sheet
- Replaced `Alert.alert` in HomePage

**Files**: `MessageActionMenu.tsx` · `SelectTextModal.tsx` · `HomePage.tsx`

**Status**: ✅ Copy, select text and share work.

[↑ Back to features](#features-click-to-jump)

<a id="f03"></a>

### F-03 · Chat Parameter Settings `LIVE`

**Background**: Control generation params (temperature / top_p / context length) for different models and tasks.

**Solution**:

- Top-right gear → `ChatSettingsModal.tsx`: temperature 0.0–2.0, top_p 0.0–1.0, context 0/2048/4096/8192/16384
- `Storage.ts`: `ChatSettings` + load/save via AsyncStorage (defaults 0.8 / 0.9 / 0)
- `OllamaApi.chat()` accepts `options` (temperature / top_p / num_ctx); history trimmed by num_ctx estimate (1 token ≈ 2 chars)

**Files**: `ChatSettingsModal.tsx` · `Storage.ts` · `OllamaApi.ts` · `HomePage.tsx`

**Status**: ✅ Settings apply immediately, persisted across restarts.

[↑ Back to features](#features-click-to-jump)

<a id="f04"></a>

### F-04 · Select Text Read-only Modal `LIVE`

**Background**: The select-text modal was editable (could delete AI replies); `editable={false}` then disabled selection entirely on Android.

**Solution**:

- Keep editable state but lock content: controlled `value` + ignore `onChangeText`
- Plus `showSoftInputOnFocus={false}` to avoid keyboard popping up

**Files**: `SelectTextModal.tsx`

**Status**: ✅ Selectable/select-all/copy works, content unmodifiable.

[↑ Back to features](#features-click-to-jump)

<a id="f05"></a>

### F-05 · Vision Model Image Input `LIVE`

**Background**: No image input for vision models (llava / qwen2.5-vl), blocking multimodal chat.

**Solution**:

- On model switch, query `/api/show` capabilities to auto-detect vision; '+' shown only for vision models (extension point for future modalities)
- Native `pickImage()`: Photo Picker on 13+, gallery fallback (ClipData compatible)
- `readImageAsBase64()`: sample ≤1024px + JPEG 70 → base64 (`catch Throwable` prevents OOM crash)
- Preview above input (removable); `Message.images=[base64]` sent with `/api/chat`, rendered in bubble

**Files**: `Chat.ts` · `OllamaApi.ts` · `FileUploadModule.kt` · `HomePage.tsx`

**Status**: ✅ Attach → gallery → preview → send works end-to-end (incl. Android 12 crash fix).

[↑ Back to features](#features-click-to-jump)

<a id="f06"></a>

### F-06 · Ollama Cloud Models `LIVE`

**Background**: Phones can't run large models locally. Ollama Cloud offloads inference to ollama.com with a fully compatible API (e.g. `gpt-oss:120b-cloud`).

**Key findings**:

- `OLLAMA_API_KEY` only works for direct ollama.com API clients (Claude Desktop / Qwen); **ollama serve ignores it**
- serve authenticates via device ed25519 signature (`~/.ollama/id_ed25519`) + device linkage; only CLI `initializeKeypair` generates the key, serve never does

**Solution**:

- New `CloudAuthModule.kt`: ed25519 keypair (PKCS8 PEM private key parseable by x/crypto/ssh; authorized_keys public key, raw32 extracted from X.509 SPKI — Android lacks JDK15+ EdECPublicKey)
- Settings 'Sign in to Ollama Cloud': opens `ollama.com/connect?name=android&key=<b64url pubkey>` to link device to ollama.com account
- OllamaExecutor ensures keypair before serve; dropped ineffective `OLLAMA_API_KEY` injection

**Files**: `CloudAuthModule.kt` · `OllamaExecutor.kt` · `SettingsPage.tsx` · `AppReactPackage.java`

**Status**: ✅ Cloud models chat after browser connect (ollama.com account required; cloud models billed per token).

[↑ Back to features](#features-click-to-jump)

<a id="f07"></a>

### F-07 · Cloud API Key Setting `UTILITY`

**Solution**:

- Settings → App settings → 'Ollama Cloud API Key' (🔑), `secureTextEntry`
- `OllamaConfigModule` native bridge `setCloudApiKey/getCloudApiKey` (SharedPreferences)
- Fixed JS calling Promise method with callback (key was never read back)

**Files**: `SettingsPage.tsx` · `OllamaConfigModule.kt` · `OllamaExecutor.kt`

[↑ Back to features](#features-click-to-jump)

<a id="f08"></a>

### F-08 · Crash Fixes `FIXED`

- **Settings Animated crash**: `Animated.Value` re-created per render in `ModelSelector.tsx` invalidated native nodes → switched to `useRef`
- **Attach red screen**: `NativeModules` used without import in `HomePage.tsx` → added import + async/await Promise calls
- **Image OOM crash (Android 12)**: sampled to 2x edge (≈12.5MB bitmap), `catch(Exception)` missed `OutOfMemoryError` → sample ≤1024px + `catch Throwable`
- **Cloud sign-in compile fail**: Android lacks `EdECPublicKey` (JDK15+) → X.509 SPKI parsing instead

**Files**: `ModelSelector.tsx` · `HomePage.tsx` · `FileUploadModule.kt` · `CloudAuthModule.kt`

[↑ Back to features](#features-click-to-jump)

<a id="f09"></a>

### F-09 · Icon Font Fix `FIXED`

**Background**: Debug builds showed no icons (release fine) — vector-icons fonts missing from debug APK.

**Solution**: Append `apply from: file("../../node_modules/react-native-vector-icons/fonts.gradle")` to `android/app/build.gradle` so fonts are bundled automatically.

**Files**: `android/app/build.gradle`

[↑ Back to features](#features-click-to-jump)

<a id="f10"></a>

### F-10 · Debug JS Auto-bundling `FIXED`

**Background**: Previously the `index.android.bundle` was manually committed for offline debug; it was accidentally deleted and debug builds red-screened.

**Solution**: Set `debuggableVariants = []` so debug builds auto-bundle JS via `export:embed`; no manual bundle file needed.

**Files**: `android/app/build.gradle`

[↑ Back to features](#features-click-to-jump)

---

## Upstream PRs

Three topic branches pushed upstream to `sunshine0523/OllamaServer`; all **mergeable / clean**, awaiting maintainer review:

| PR | Content | Status |
|----|---------|--------|
| [#30](https://github.com/sunshine0523/OllamaServer/pull/30) | Ollama 0.31.2 upgrade (fixes qwen2.5 crash [#25](https://github.com/sunshine0523/OllamaServer/issues/25), vision support [#28](https://github.com/sunshine0523/OllamaServer/issues/28)) | ✅ MERGABLE |
| [#31](https://github.com/sunshine0523/OllamaServer/pull/31) | Download stability + long-press menu + chat settings | ✅ MERGABLE |
| [#32](https://github.com/sunshine0523/OllamaServer/pull/32) | expo module fix (ExpoAsset crash) + dep lock + GHA CI | ✅ MERGABLE |

---

## Tech Stack

`React Native 0.77` · `Expo 53` · `react-native-paper 5.15 (MD3)` · `Kotlin native modules` · `Ollama 0.31.2 (Termux bionic)` · `AsyncStorage` · `react-native-vector-icons` · `i18next zh/en` · `GitHub Actions CI`

## Build

- GitHub Actions (xis3794) auto-builds debug / release APKs
- Artifacts at `/storage/emulated/0/Download/Operit/ollama_apk/`

---

<div align="center">

**OllamaServer Enhanced** · forked from [sunshine0523/OllamaServer](https://github.com/sunshine0523/OllamaServer)

</div>