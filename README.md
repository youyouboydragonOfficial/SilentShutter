# 📸 SilentShutter (サイレントシャッター) v1.1.0

[![Release](https://img.shields.io/github/v/release/youyouboydragonOfficial/SilentShutter?color=00F0FF&style=for-the-badge)](https://github.com/youyouboydragonOfficial/SilentShutter/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-00E676?style=for-the-badge&logo=android)](https://github.com/youyouboydragonOfficial/SilentShutter)
[![License](https://img.shields.io/badge/License-MIT-blueviolet?style=for-the-badge)](LICENSE)

**root化不要**で、Android端末のカメラシャッター音問題を完全に解決する最強のカメラユーティリティ。  
国内向けPixelやXperiaなどの「OSレベルで強制されるシャッター音」も、内蔵の**完全無音CameraXハードウェア撮影エンジン (0dB)** により100%確実に無音化。  
サイバーパンク/ネオンHUDをテーマにした近未来的デザインと超高機能インターフェースを搭載！

---

## ✨ 最強のハイブリッド消音アーキテクチャ

### 1. ⚡ 完全無音サイバーカメラ (Zero-Sound Camera Engine)
- **国内版Pixel・Xperia・AQUOSなどの全機種で100%確実に 0dB（完全無音）撮影！**
- CameraX APIによる最高画質写真撮影（MediaStore/Pictures/SilentShutter に自動保存）。
- ネオンHUDビューファインダー（照準レティクル、タップフォーカス、ズームスライダー、フラッシュ切替、イン/アウトカメラ反転）。
- シャッター音は一切鳴りませんが、画面のサイバーフラッシュで撮影完了をスマートに通知。

### 2. 🔇 外部カメラ自動ミュート (Smart Shutter Mute)
- ユーザー補助サービス（Accessibility Service）により、標準カメラやサードパーティカメラ（LINE、Instagram、TikTok等）の起動をリアルタイム検出。
- カメラ起動時**のみ**システム音量・通知音量を一時消音し、カメラ終了時にミリ秒単位で元の音量・設定に自動復元！

### 3. 📱 Samsung Galaxy CSC 直接無音化
- Galaxy端末の強制シャッター音フラグ（`csc_pref_camera_forced_shuttersound_key`）を無効化するADBコマンド案内＆ワンタップ直接適用。

### 4. 🎛️ フローティングHUD & クイック設定タイル
- **フローティングHUDボタン**:
  - タップ: 即座に消音トグル
  - 長押し: どこからでも0.5秒で完全無音サイバーカメラを直接起動！
- **クイック設定タイル**: 通知バーからワンタップでON/OFF切り替え。

---

## 🎨 超クールなサイバーパンクHUDデザイン

- **新アプリアイコン**: 電脳レンズフレーム、照準レティクル、エメラルドオプティカルコア、ネオンブレードをあしらったフューチャリスティックデザイン。
- **メインUI**: ディープOLEDブラック（`#080C14`）、エレクトリックシアン（`#00F0FF`）、マトリックスグリーン（`#00E676`）による発光HUD。
- **脈動する特大パワースイッチ** & **リアルタイム・サイバーコンソール**。

---

## 🚀 インストール & 使い方

### 1. APKダウンロード
[GitHub Releases (v1.1.0)](https://github.com/youyouboydragonOfficial/SilentShutter/releases/tag/v1.1.0) から `SilentShutter-v1.1.0.apk` を直接ダウンロードしてインストールしてください。  
*(※zipファイルは一切使用しておらず、APKファイルを直接配布しています)*

### 2. セットアップ & 撮影
1. アプリを起動し、必要な権限（カメラ、ユーザー補助サービス、マナーモード制御）を画面から許可。
2. **完全無音で撮りたいとき**:  
   アプリ内の「完全無音カメラを起動」または画面端のフローティングHUD長押しで即座に0dB撮影！
3. **純正カメラの自動消音**:  
   中央の巨大スイッチをONにするだけでスタンバイ状態になります。

---

## 📜 ライセンス

MIT License © 2026 SilentShutter
