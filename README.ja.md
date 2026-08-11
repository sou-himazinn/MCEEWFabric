# MCEEW Fabric

[![Build](https://github.com/TenkyuChimata/MCEEW/actions/workflows/build.yml/badge.svg)](https://github.com/TenkyuChimata/MCEEW/actions/workflows/build.yml)
[![License: GPL-3.0-only](https://img.shields.io/badge/License-GPL--3.0--only-blue.svg)](LICENSE)

[English](README.md) | 日本語

MCEEW は、Minecraft Fabric 向けの日本の緊急地震速報（EEW）・地震情報通知
Mod です。シングルプレイのワールドと Fabric サーバーで利用できます。

設定した WebSocket エンドポイントから情報を受信し、古い EEW を除外したうえで、
Minecraft のチャット、タイトル、サウンドを使ってプレイヤーに通知します。現在の
Fabric 版は、日本の JMA データに対応しています。

## 主な機能

- 日本の JMA 緊急地震速報を通知
- 日本の地震情報を通知し、`/eew info` で最新情報を表示
- チャット、タイトル、サウンドによる通知
- 日本語（`ja_jp`）と英語（`en_us`）に対応
- 通知方法とサウンドを設定可能
- WebSocket の自動再接続
- `/eew` と `/mceew` のコマンドエイリアス
- `config/mceew.json` による JSON 設定

## 対応環境

| コンポーネント | 対応バージョン |
| --- | --- |
| Minecraft | 1.21.10 |
| Fabric Loader | 0.19.3 以上 |
| Fabric API | Minecraft 1.21.10 対応版 |
| Java | 21 以上 |

このブランチは Fabric のシングルプレイと専用サーバーを対象としています。
以前の Spigot、Paper、Folia 実装、および CENC・四川・重慶・台湾・福建の通知機能は
含まれていません。

## 導入方法

1. Minecraft 1.21.10、Fabric Loader 0.19.3 以上、対応する Fabric API を導入します。
2. リポジトリの Releases ページから MCEEW Fabric の `.jar` をダウンロードするか、
   [ソースからビルド](#ソースからビルド)します。
3. MCEEW の `.jar` と Fabric API の `.jar` を `mods` フォルダーに入れます。
4. ゲームまたはサーバーを一度起動します。MCEEW が `config/mceew.json` を作成します。

専用サーバーでは、MCEEW をサーバー側に導入してください。通知にはバニラの
Minecraft パケットを使用するため、プレイヤー側に MCEEW を導入する必要はありません。

## ソースからビルド

Gradle Wrapper を同梱しています。Java 21 以上が必要です。

```sh
./gradlew build
```

Windows では次を実行します。

```powershell
.\gradlew.bat build
```

ビルドされた Mod の JAR は `build/libs/` に出力されます。

テストを実行する場合は次のコマンドを使います。

```sh
./gradlew test
```

## コマンド

`/mceew` は `/eew` と同じコマンドを実行するエイリアスです。

| コマンド | 権限 | 説明 |
| --- | --- | --- |
| `/eew` | 全員 | コマンドヘルプを表示 |
| `/eew info` | 全員 | 受信済みの最新地震情報を表示 |
| `/eew test forecast` | 権限レベル 2 | 予報のテスト通知を送信 |
| `/eew test alert` | 権限レベル 2 | 警報のテスト通知を送信 |
| `/eew reload` | 権限レベル 2 | `config/mceew.json` を再読み込みして再接続 |

サブコマンドを省略した `/eew test` は、`/eew test forecast` と同じです。

## 設定

初回起動時に `config/mceew.json` が作成されます。初期設定は次のとおりです。

```json
{
  "language": "ja_jp",
  "webSocketUrl": "wss://ws-api.wolfx.jp/all_eew",
  "enableEew": true,
  "enableEarthquakeInfo": true,
  "broadcastChat": true,
  "showTitle": true,
  "playSound": true,
  "freshAlertMinutes": 10,
  "timeFormat": "yyyy/MM/dd HH:mm:ss",
  "sound": {
    "id": "minecraft:block.note_block.pling",
    "volume": 1.0,
    "pitch": 1.0
  }
}
```

| キー | 説明 |
| --- | --- |
| `language` | メッセージの言語。`ja_jp` または `en_us`。 |
| `webSocketUrl` | 情報の受信に使う WebSocket エンドポイント。初期値は上記の Wolfx API です。 |
| `enableEew` | JMA 緊急地震速報の通知を有効化。 |
| `enableEarthquakeInfo` | 新しい JMA 地震情報の受信と自動通知を有効化。 |
| `broadcastChat` | EEW の詳細をプレイヤーのチャットに送信。 |
| `showTitle` | EEW 情報をタイトルとサブタイトルで表示。 |
| `playSound` | EEW を受信したときに設定したサウンドを再生。 |
| `freshAlertMinutes` | 表示対象とする EEW の最大経過時間（分）。 |
| `timeFormat` | 表示時刻に使う Java `DateTimeFormatter` のパターン。時刻は日本標準時として扱います。 |
| `sound.id` | Minecraft のサウンド識別子。 |
| `sound.volume` | サウンドの音量。 |
| `sound.pitch` | サウンドの音程。 |

設定を編集した後、管理者権限で `/eew reload` を実行するか、サーバーを再起動して
ください。JSON が不正な場合、MCEEW はタイムスタンプ付きのバックアップを作成し、
初期設定を書き直します。

## データソースと注意事項

初期設定では `wss://ws-api.wolfx.jp/all_eew` に接続します。この外部サービスはこの
リポジトリが運営するものではありません。可用性、遅延、メッセージ形式、利用条件は
MCEEW とは独立して変更される可能性があります。互換性のあるサービスを運用している
場合は、`webSocketUrl` で接続先を変更できます。

MCEEW は通知を便利にするための Mod であり、気象庁の公式アプリでも、生命の安全を
保証するシステムでもありません。Minecraft の通知を、公式の警報、緊急時の指示、
避難案内の代わりに使用しないでください。

## コントリビューション

バグ報告、改善提案、機能追加のプルリクエストを歓迎します。開発環境の構築方法と
プルリクエストの指針は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。

## ライセンス

MCEEW は GNU General Public License v3.0 only のもとで配布されています。詳しくは
[LICENSE](LICENSE) を参照してください。
