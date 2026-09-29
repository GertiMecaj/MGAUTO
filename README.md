# MGAUTO for Windows

Windows desktop port of [GertiMecaj/mg-afk-android](https://github.com/GertiMecaj/mg-afk-android), based on Android version 2.4.26.

Download the Windows installer or portable ZIP from [Releases](https://github.com/GertiMecaj/MGAUTO/releases). For the portable version, extract the entire folder and run `MGAUTO.exe`. Keep the app and runtime folders together. Java and .NET are bundled. Windows 10/11 x64 and Microsoft Edge WebView2 Runtime are required. WebView2 is normally installed with Windows/Edge; if missing, MGAUTO offers to install it using the bundled, Microsoft-signed installer (internet required).

## Use

1. Open MGAUTO and choose **Login with Discord** on the Dashboard.
2. Complete login in the Windows browser window. The session is saved automatically.
3. Set your room and connect. Use the menu for Room, Pets, Storage, Garden, Shops, LAB, Social, Alerts, Settings and diagnostic logs.
4. **Play in game** pauses the AFK session, opens the game with its session cookie, and resumes AFK when that window closes. Gemini injection follows the Dashboard toggle.
5. Minimize to keep running. Closing the main window offers running in the system tray or quitting. Use the tray menu to reopen, stop an alarm, or quit.

The desktop port retains the source repository's game protocol, parsing, actions, UI screens, LAB, team management, mutations, stock handling, reconnect behavior and alert detection. Features absent from that source revision (such as the older README's casino screens) are not invented here.

Windows replaces Android services with an application lifetime tray process, native sleep prevention, file-backed preferences, desktop notifications and WAV alarms. Alarm schedules, volume and previews are supported. Data is stored under `%LOCALAPPDATA%\MGAUTO`; browser profiles are isolated per account. Credentials go to the browser over a child-process pipe, never a command-line argument. Preferences are local to your Windows account; do not share the data folder.

## Build and verification

The **Windows executable** GitHub Actions workflow compiles the WebView2 helper, runs Kotlin unit tests, packages the desktop app with Java, launches the packaged executable for a screenshot smoke test, builds the EXE installer and publishes downloads. The smoke test checks startup and rendering, not authenticated gameplay. Live game actions require a user account.

Locally on Windows: install JDK 17, Gradle 8.10.2, .NET 8 SDK and WiX 3, then run:

```powershell
dotnet publish browser/MGAUTO.Browser.csproj -c Release -r win-x64 --self-contained true -o resources/windows/browser
gradle test createDistributable packageExe
```

Installer output: `build/compose/binaries/main/exe/`. Portable app: `build/compose/binaries/main/app/MGAUTO/`.

## Credits

Original Android app and game integration by its upstream contributors; original credits and protocol sources: [MG AFK Android](https://github.com/GertiMecaj/mg-afk-android), [MG-Websocket-Helper](https://github.com/Ariedam64/MG-Websocket-Helper), [Magic-Garden-API](https://github.com/Ariedam64/Magic-Garden-API), [Aries Mod API](https://github.com/Ariedam64/mg-api-front-ariesMod), [Gemini](https://github.com/Ariedam64/Gemini).
