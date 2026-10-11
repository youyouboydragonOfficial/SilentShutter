# 📸 SilentShutter (サイレントシャッター)

[![Release](https://img.shields.io/github/v/release/youyouboydragonOfficial/SilentShutter?color=00F0FF&style=for-the-badge)](https://github.com/youyouboydragonOfficial/SilentShutter/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-00E676?style=for-the-badge&logo=android)](https://github.com/youyouboydragonOfficial/SilentShutter)
[![License](https://img.shields.io/badge/License-MIT-blueviolet?style=for-the-badge)](LICENSE)

**root化不要**で、カメラ起動時のみ一時的にシャッター音・通知音・システム音を無音化するAndroidアプリ。  
カメラを閉じると自動的に元の音量・元のマナーモードにミリ秒で復元されます。  
サイバーパンク/ネオンダークを基調とした洗練されたHUDインターフェースを搭載。

---

## ✨ 主な機能

- ⚡ **ワンタップ マスタースイッチ (ON / OFF)**
  - 巨大なネオン発光トグルスイッチでいつでも即座に無音化エンジンを起動・停止。
- 🎯 **スマート自動消音 (Smart Shutter Mute)**
  - ユーザー補助サービス（Accessibility Service）により、カメラアプリの起動をリアルタイム検出。
  - カメラ起動時「のみ」一時的に消音し、カメラを閉じたら元の音量・マナーモードに瞬時に復帰。
  - 普段の着信音・通知音・音楽再生には一切影響を与えません。
- 🎛️ **クイック設定タイル (Quick Settings Tile)**
  - Androidの通知シェード（コントロールセンター）からワンタップでON/OFFの切り替えが可能。
- 🔮 **フローティングHUDボタン (Overlay Trigger)**
  - 画面端に吸着するサイバーネオン調のHUDボタン。撮影中でも画面隅から瞬時に消音状態を手動トグル。
- 📱 **Samsung Galaxy & ADB 直接消音モード**
  - Samsung端末等のCSCカメラ設定（`csc_pref_camera_forced_shuttersound_key`）を無効化するADBコマンドの案内＆直接適用機能を内蔵。
- 📋 **対象カメラアプリ管理 & カスタム追加**
  - Googleカメラ、Galaxyカメラ、Xperiaカメラ、LINEカメラ、Instagram、TikTok、各種サードパーティカメラアプリに標準対応。任意のパッケージ名も自由に追加可能。
- 🖥️ **リアルタイム・サイバーコンソール**
  - 検知したカメライベントと音量復旧ログをターミナル風にリアルタイムストリーミング表示。

---

## 🚀 インストール & 使い方

### 1. APKのダウンロード
[GitHub Releases](https://github.com/youyouboydragonOfficial/SilentShutter/releases/latest) から `SilentShutter-v1.0.0.apk` を直接ダウンロードして端末にインストールします。
*(※zipではなくAPKファイルを直接配布しています)*

### 2. 初期セットアップ
1. **ユーザー補助サービス (Accessibility Service) の有効化**
   - アプリを起動し、「ユーザー補助設定を開く」をタップ。
   - 「ダウンロード済みのアプリ」一覧から **SilentShutter** を選択してONにします。
2. **通知へのアクセス (DND / マナーモード権限) の許可**
   - 「マナーモード / DND 制御権限」の「許可する」をタップし、SilentShutterをONにします。

### 3. 使用方法
- 中央の **巨大な電源スイッチ** をタップして「ON」に設定するだけ！
- カメラアプリを開くと、自動的に「MUTED」状態となりシャッター音が無効化されます。
- カメラを終了すると、自動的に「RESTORED」となり元の音量に戻ります。

---

## 🛠️ 技術スタック & アーキテクチャ

- **言語**: Kotlin 2.0.20
- **UI フレームワーク**: Jetpack Compose (Material 3)
- **非同期処理**: Kotlin Coroutines & StateFlow
- **システム制御**:
  - `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`)
  - `AudioManager` (Stream Volume & Ringer Mode Restore)
  - `NotificationManager` (InterruptionFilter DND Mute)
  - `TileService` (Quick Settings Tile)
  - `WindowManager` (Floating Overlay HUD)

---

## 🔒 プライバシー & セキュリティ

- **個人情報の収集は一切行いません**
- インターネット通信権限（`android.permission.INTERNET`）を使用せず、端末ローカル内のみで完結して動作します。
- ユーザー補助サービスは、カメラアプリの前面起動状態（パッケージ名）の検知にのみ使用され、画面上のコンテンツやテキストの取得・読み取りは行いません。

---

## 📜 ライセンス

MIT License © 2026 SilentShutter
