# Contributing to MCEEW

Thank you for helping improve MCEEW. The current project targets Minecraft
1.21.10 on Fabric and focuses on Japan JMA earthquake information.

## Development Setup

- Java 21 or newer
- Internet access for the first Gradle dependency download

The repository includes the Gradle wrapper, so a separate Gradle installation
is not required.

```sh
bash ./gradlew test
bash ./gradlew build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Pull Requests

- Keep changes focused on the current Fabric implementation.
- Add or update tests when changing behavior.
- Keep user-visible messages available in both `ja_jp` and `en_us` when practical.
- Do not commit generated files from `build/`, local toolchains, or IDE metadata.
- Describe the user-visible behavior and verification steps in the pull request.

Before opening a pull request, run `./gradlew build` locally and make sure the
working tree does not contain generated artifacts.

## Scope Notes

MCEEW consumes data from an external WebSocket service. Changes that alter the
data format, connection lifecycle, notification timing, or displayed warning
level should include a short explanation of the compatibility impact.
