# BikeMusic

Android向け音楽再生アプリケーション。  
アプリ内で独立した音量調整やお気に入りリスト、バックグラウンド再生などをサポートします。

---

## 🎯 概要

- **個別音量調整**  
  アプリ内のSeekBarでシステム音量に影響を与えずに再生音量をコントロール  
- **プレイリスト**  
  端末内の音楽ファイルを自動でスキャンし、プレイリストを生成  
- **お気に入り登録**  
  Roomを使ってお気に入り曲を永続化し、Homeタブで一覧表示  
- **バックグラウンド再生**  
  Foreground Serviceによりアプリを閉じても再生を継続  
- **通知コントロール**  
  通知領域から停止が可能  

## 🚀 主な機能

1. **音量調整**  
   - 再生中にリアルタイムで音量を±0.1ずつ変更し、Roomに保存  
2. **曲の絞り込み**  
   - 検索バー（曲名・アーティスト部分一致）＋アーティストプルダウンで高速フィルタリング  
3. **お気に入り**  
   - SongAdapterのボタンでお気に入り登録/解除、Homeタブで永続化されたリストを表示  
4. **バックグラウンド再生**  
   - `MusicService`（Foreground Service）でプレイリストをループ再生  
   - Notificationにてタイトル表示＋停止ボタン  
5. **Service ↔ Fragment 通信**  
   - LocalBroadcastManagerで現在再生中の曲タイトルをHomeFragmentに通知  

## 🛠️ セットアップ・ビルド

1. **リポジトリをクローン**  
   ```bash
   git clone https://github.com/yourname/BikeMusic.git
   cd BikeMusic

2. **リリースビルド**

メニュー → Build → Generate Signed Bundle / APK… を選択

3. **実機インストール（AABの場合）**

bundletool を使って .apks を生成、adb install-apks でインストール


# 楽曲一覧画面
<img src="https://github.com/user-attachments/assets/08bb11bb-d571-41a4-84ac-2fbc0b4ad8e1" width="300px">

# お気に入り画面
<img src="https://github.com/user-attachments/assets/1ba102b4-45bb-4a41-a5d7-7f308dd8a556" width="300px">

