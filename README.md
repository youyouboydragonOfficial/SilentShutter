# 📸 SilentShutter v2.1.0 - Default Camera Shutter Hacker

[![Release](https://img.shields.io/github/v/release/youyouboydragonOfficial/SilentShutter?color=00F0FF&style=for-the-badge)](https://github.com/youyouboydragonOfficial/SilentShutter/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-00E676?style=for-the-badge&logo=android)](https://github.com/youyouboydragonOfficial/SilentShutter)
[![License](https://img.shields.io/badge/License-MIT-blueviolet?style=for-the-badge)](LICENSE)

**端末のデフォルト（純正）カメラアプリのシャッター音をハッキング技術で徹底消音！**  
自作カメラではなく、Google PixelやXperia、Galaxy等にプリインストールされている**普段使っている純正カメラアプリ**のシャッター音をroot化なしで消音する専用ハッカーユーティリティ。

---

## ❄️ v2.1.0 新機能: アプリ内更新 & 深々と降る雪の演出

- **❄️ 深々と降る雪のパーティクル演出 (Snowfall Animation)**:
  - 更新確認ボタンを押すと、画面いっぱいに美しい雪の結晶がしんしんと舞い落ちる美麗なサイバー演出。
- **🌀 クールな回転サイバースピナー (Multi-Ring Cyber Spinner)**:
  - 外側と内側が逆回転するネオンシアン＆エメラルドグリーンの多重リングとパルスコアによる近未来的スピナー。
- **⏱️ 3秒間の演出保証**:
  - API通信が一瞬で終わっても、必ず3秒間は読み込み画面と降雪演出を堪能できます。
- **📲 アプリ内ワンタップ自動更新 (Auto In-App APK Updater)**:
  - GitHubの最新リリースを自動検知。
  - アプリ内で直接APKを高速ダウンロードし、進捗バー表示後に端末のインストーラーが自動起動！ブラウザを開く手間を完全に省きます。

---

## ⚡ 徹底強化されたデフォルトカメラ消音ハック

1. **🎙️ 全オーディオストリーム強制0 ＆ 受話口（Earpiece）退避ハック (Android 12+)**:
   - `STREAM_SYSTEM`, `STREAM_NOTIFICATION`, `STREAM_RING`, `STREAM_MUSIC`, `STREAM_ALARM`, `STREAM_DTMF`, `STREAM_VOICE_CALL` の全ストリームを完全ゼロ化＆`ADJUST_MUTE`。
   - 通話モード（`MODE_IN_COMMUNICATION`）にハイジャックし、オーディオルーティングを外側のスピーカーから**受話口（耳当て部分の微小スピーカー）**へ強制迂回！
   - 万が一シャッター音が鳴ってもスピーカーから爆音が出るのを物理的・OSレベルで遮断します。
2. **🔄 音量強制ガードループ (Volume Guard Thread)**:
   - 純正カメラが起動時にシステム音量を勝手に元に戻そうとする動作を阻止するため、カメラ起動中は毎秒数十回にわたり音量を0に引き戻し続けるガードスレッドを並列稼働！
3. **⚡ シャッター操作検知 ミリ秒パルス (Shutter Tap Interceptor)**:
   - 画面内のシャッターボタンが押された瞬間を超高速検知し、瞬時消音パルスを発動。
4. **📱 Shizuku API による Galaxy CSCキー直接書き換え**:
   - root不要で `csc_pref_camera_forced_shuttersound_key = 0` を直接注入。

---

## 📲 インストール & 使い方

### 1. 初回インストール
[GitHub Releases (v2.1.0)](https://github.com/youyouboydragonOfficial/SilentShutter/releases/tag/v2.1.0) から `SilentShutter-v2.1.0.apk` を直接ダウンロードして端末にインストールしてください。  
*(※zipファイルは一切使用しておらず、APKバイナリを直接配布しています)*

### 2. 次回以降のアップデート
- アプリ内の **「❄️ アプリ内で更新状況を確認する」** またはヘッダー右上の **「❄️ UPDATE」** ボタンを押すだけで、アプリ内で自動更新が完結します！

---

## 📜 ライセンス

MIT License © 2026 SilentShutter
