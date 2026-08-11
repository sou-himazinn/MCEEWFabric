# MCEEW Fabric

[![Build](https://github.com/sou-himazinn/MCEEWFabric/actions/workflows/build.yml/badge.svg)](https://github.com/sou-himazinn/MCEEWFabric/actions/workflows/build.yml)
[![License: GPL-3.0-only](https://img.shields.io/badge/License-GPL--3.0--only-blue.svg)](LICENSE)

English | [日本語](README.ja.md)

MCEEW is a Minecraft Fabric mod that delivers Japan Meteorological Agency
(JMA) earthquake early warning (EEW) and earthquake information to players in
singleplayer worlds and Fabric servers.

It connects to the configured WebSocket endpoint, filters stale EEW reports,
and notifies players through Minecraft chat, titles, and sounds. The current
Fabric version is focused on Japan JMA data.

## Features

- Japan JMA EEW notifications
- Japan JMA earthquake information and `/eew info`
- Chat, title, and sound notifications
- Japanese (`ja_jp`) and English (`en_us`) messages
- Configurable notification and sound settings
- Automatic WebSocket reconnects
- `/eew` and `/mceew` command aliases
- JSON configuration at `config/mceew.json`

## Compatibility

| Component | Supported version |
| --- | --- |
| Minecraft | 1.21.10 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | Fabric API for Minecraft 1.21.10 |
| Java | 21 or newer |

This branch targets Fabric singleplayer and dedicated servers. It does not
include the former Spigot, Paper, or Folia implementation, or notifications
for CENC, Sichuan, Chongqing, Taiwan, or Fujian data sources.

## Installation

1. Install Minecraft 1.21.10, Fabric Loader 0.19.3 or newer, and the matching
   Fabric API.
2. Download the MCEEW Fabric `.jar` from the repository's Releases page, or
   [build it from source](#build-from-source).
3. Put the MCEEW `.jar` and Fabric API `.jar` in the `mods` directory.
4. Start the game or server once. MCEEW will create `config/mceew.json`.

For a dedicated server, install MCEEW on the server. The notifications use
vanilla Minecraft packets, so players do not need to install MCEEW on their
clients.

## Build From Source

The project includes the Gradle wrapper and requires Java 21 or newer.

```sh
bash ./gradlew build
```

On Windows, use:

```powershell
.\gradlew.bat build
```

The built mod JAR is written to `build/libs/`.

To run the test suite:

```sh
bash ./gradlew test
```

## Commands

`/mceew` is an alias for `/eew`.

| Command | Permission | Description |
| --- | --- | --- |
| `/eew` | Everyone | Show the command help |
| `/eew info` | Everyone | Show the latest received earthquake information |
| `/eew test forecast` | Permission level 2 | Send a forecast test notification |
| `/eew test alert` | Permission level 2 | Send a warning test notification |
| `/eew reload` | Permission level 2 | Reload `config/mceew.json` and reconnect |

`/eew test` without a subcommand is equivalent to
`/eew test forecast`.

## Configuration

The configuration file is created at `config/mceew.json` on first launch. The
default configuration is:

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

| Key | Description |
| --- | --- |
| `language` | Message language: `ja_jp` or `en_us`. |
| `webSocketUrl` | WebSocket endpoint used to receive data. The default is the Wolfx API endpoint shown above. |
| `enableEew` | Enable JMA EEW notifications. |
| `enableEarthquakeInfo` | Enable receiving and automatically notifying new JMA earthquake information. |
| `broadcastChat` | Send EEW details to player chat. |
| `showTitle` | Show EEW information as a title and subtitle. |
| `playSound` | Play the configured sound for an EEW report. |
| `freshAlertMinutes` | Maximum age, in minutes, for an EEW report to be displayed. |
| `timeFormat` | Java `DateTimeFormatter` pattern used for displayed times. Times are interpreted in Japan Standard Time. |
| `sound.id` | Minecraft sound identifier. |
| `sound.volume` | Sound volume. |
| `sound.pitch` | Sound pitch. |

After editing the file, run `/eew reload` as an operator, or restart the
server. If the JSON is invalid, MCEEW keeps a timestamped backup and writes a
fresh default configuration.

## Data Source and Limitations

By default, MCEEW connects to `wss://ws-api.wolfx.jp/all_eew`. This is an
external service and is not operated by this repository. Availability,
latency, message formats, and usage terms may change independently of MCEEW.
You can change the endpoint with `webSocketUrl` if you operate a compatible
service.

MCEEW is a convenience notification tool, not an official JMA application and
not a life-safety system. Do not use Minecraft notifications as a substitute
for official warnings, emergency instructions, or evacuation guidance.

## Contributing

Bug reports, improvements, and focused pull requests are welcome. See
[CONTRIBUTING.md](CONTRIBUTING.md) for the development setup and pull request
guidelines.

## License

MCEEW is distributed under the GNU General Public License v3.0 only. See
[LICENSE](LICENSE).
