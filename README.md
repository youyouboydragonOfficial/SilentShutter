# 📸 SilentShutter v2.0.0 - Default Camera Shutter Hacker

[![Release](https://img.shields.io/github/v/release/youyouboydragonOfficial/SilentShutter?color=00F0FF&style=for-the-badge)](https://github.com/youyouboydragonOfficial/SilentShutter/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-00E676?style=for-the-badge&logo=android)](https://github.com/youyouboydragonOfficial/SilentShutter)
[![License](https://img.shields.io/badge/License-MIT-blueviolet?style=for-the-badge)](LICENSE)

**端末のデフォルト（純正）カメラアプリのシャッター音をハッキング技術で無効化するアプリ**！  
自作カメラではなく、Google PixelやXperia、Galaxy等にプリインストールされている**普段使っている純正カメラアプリ**のシャッター音をroot化なしで遮断・無音化します。

---

## ⚡ デフォルトカメラ無音化ハックの仕組み

日本のAndroid端末ではOSのオーディオ層（`AudioPolicyManager`）に `STREAM_SYSTEM_ENFORCED`（強制システム音）が組み込まれており、通常のマナーモードでは消えません。  
本アプリは以下の**3大ハッキングモジュール**により、純正カメラのシャッター音を無効化します。

### 1. 🎙️ 通話モード & 排他オーディオストリーム乗っ取り (Audio Route Hijacker)
- デフォルトカメラが起動した瞬間、OSのオーディオモードを一時的に **`MODE_IN_COMMUNICATION`（通話中モード）** にハイジャック。
- 同時に無音PCMの排他ストリーム（`AudioTrack`）を最高優先度で占有。
- 通話中のユーザーの聴覚保護のため、OSのオーディオポリシーはスピーカーへの警告音ルートを抑制・迂回させ、**純正カメラのシャッター音出力をOSレベルで遮断**します。
- カメラを閉じるとミリ秒単位で通常の音量・モードに自動復元されます。

### 2. ⚡ シャッター操作検知 ミリ秒パルス (Shutter Tap Interceptor)
- ユーザー補助サービス（Accessibility Service）が、純正カメラ画面内のシャッターボタンのタップ操作をリアルタイム検知。
- シャッターが切られる瞬間に超高速で消音パルスを送り込み、ピンポイントで音を遮断します。

### 3. 📱 Shizuku & システム設定インジェクター (System Injector)
- **Shizuku API** に完全対応。Shizukuが稼働していれば、ワンタップでroot不要の特権シェルを実行し、Samsung Galaxy等の純正カメラシャッター音フラグ（CSCキー `csc_pref_camera_forced_shuttersound_key = 0`）を直接書き換えて永久消音！
- Shizukuがない環境でも、ワイヤレスデバッグ用のワンクリックADBコマンドコピー機能を搭載。

---

## 🚀 主な機能

- **🎯 ターゲット自動検出**:
  - お使いのスマートフォンのデフォルトカメラ（Google Camera, Samsung Camera, Xperia Camera等）を自動検出し、アプリ内からワンタップで起動してテスト可能。
- **⚡ メガハックスイッチ (HACK ON / STANDBY)**:
  - アプリ中央の巨大なネオン発光スイッチで全体をいつでもワンタップON/OFF。
- **🔮 フローティングHUDボタン**:
  - 画面端の半透明ネオンHUD。タップで消音トグル、長押し（0.5秒）で純正カメラを直接起動！
- **🎛️ クイック設定タイル**:
  - 通知バーのコントロールセンターからワンタップでハックのON/OFF切り替え。
- **🖥️ リアルタイム・ハックコンソール**:
  - 純正カメラの検知、オーディオハイジャック、シャッターパルス発動のログがターミナル風にリアルタイムストリーミング。

---

## 📲 インストール & 使い方

### 1. APKのダウンロード
[GitHub Releases (v2.0.0)](https://github.com/youyouboydragonOfficial/SilentShutter/releases/tag/v2.0.0) から `SilentShutter-v2.0.0.apk` を直接ダウンロードして端末にインストールしてください。  
*(※zipファイルは一切使用しておらず、APKバイナリを直接配布しています)*

### 2. セットアップ
1. アプリを起動し、「ユーザー補助サービス」と「マナーモード/DND制御権限」を許可。
2. （Galaxyユーザー / Shizuku利用者の場合）「システム設定を注入」をタップ。
3. 中央の巨大スイッチを **HACK ON** に設定。
4. **「デフォルトカメラを起動してテスト」** をタップして、端末の純正カメラを開いて撮影してください！

---

## 📜 ライセンス

MIT License © 2026 SilentShutter
