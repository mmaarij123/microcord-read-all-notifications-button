# Read All Notifications Button for Microcord

Mark unread server channels and joined threads as read with a single tap above Microcord’s server list.

## What you can do

- Tap **Read All** to clear unread messages across your servers, including text channels, voice and stage chats, and active threads you’ve joined.
- See a confirmation after the request finishes. If a batch fails, tap **Read All** again to retry.

## Install from GitHub Releases

1. Open the [latest GitHub release](https://github.com/mmaarij123/microcord-read-all-notifications-button/releases/latest) and download **`microcord-read-all-notifications-button.apk`** under **Assets**.
2. Install the APK on the Android device where Microcord is installed.
3. In **Microcord Settings → Plugins**, enable **Read All Notifications Button** and approve it when prompted.
4. Open the server list and tap **Read All** at the top.

## Build from source

Install JDK 21 and Android SDK Platform 36.1. Clone this repository and run the Gradle wrapper from its root.

```bash
bash ./gradlew assembleDebug lintDebug
```

On Windows PowerShell, run `.\gradlew.bat assembleDebug lintDebug`.

The APK is generated at `build/outputs/apk/debug/MicrocordReadAllNotificationsButton-debug.apk`. For a signed release build, copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties`, fill in your signing details, and run `assembleRelease` with the same wrapper. Upload the signed APK from `build/outputs/apk/release/` to GitHub Releases as `microcord-read-all-notifications-button.apk`. Reuse the release signing key for updates.

The plugin is built with Kotlin against Microcord Plugin API `1.0.6`. It attaches a native Android button above Microcord’s guild list and removes it when the plugin is disabled. It checks channel access and compares known message IDs with Microcord’s read markers. It sends the latest message ID for each unread channel through Microcord’s signed-in REST client in sequential batches of up to 100. Microcord processes the read state updates from Discord. The Android package is `io.github.mmaarij123.readallnotificationsbutton`.

## License

The original Microcord plugin code is licensed under MIT. Copyright © 2026 Maarij. You may use, copy, modify, and redistribute it with the copyright and permission notices. The code is provided without warranty. [Read the MIT license](LICENSE)

See the notices for the Vencord feature that inspired this plugin and the upstream tools and libraries. [Third-party notices](THIRD_PARTY_NOTICES.md)
