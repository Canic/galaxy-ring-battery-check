# Galaxy Ring Battery Check

A simple battery self-check for the Galaxy Ring. The app connects to a ring already paired with your phone and displays its reported battery health and current consumption.

The interface follows your phone's language and is available in English and German. This is an independent project and is not an official Samsung app.

## Features

- Start or repeat the ring's battery self-check
- See battery health and current consumption
- Get clear connection, permission, and error messages
- Use the app in English or German

## Requirements

- A Galaxy Ring paired with your phone
- Bluetooth enabled and the ring nearby
- Android 12 or later
- Nearby devices permission

Root access and Samsung Members are not required.

## Download

A new APK is built and published only when a version tag such as `v1.0.0` is pushed. Download it from the [GitHub Releases page](https://github.com/Canic/galaxy-ring-battery-check/releases). Regular pushes and pull requests do not publish a release.

The download is a debug APK. Android may ask you to uninstall a locally built copy before installing it.

## Run a battery check

1. Pair your Galaxy Ring in your phone's Bluetooth settings.
2. Open **Ring Battery Check** or **Ring-Akkucheck**.
3. Grant Nearby devices permission when asked.
4. Tap **Start test** or **Test starten** and keep the ring nearby.

The check takes a few seconds. The result shows whether the ring reports normal battery health and current consumption. The ring also returns a capacity value without a unit, so the app does not present it as a charge percentage.

## Deutsche Version

<details>
<summary>Deutsch</summary>

### Galaxy Ring Akkucheck

Ein einfacher Selbsttest für den Akku des Galaxy Ring. Die App verbindet sich mit einem bereits gekoppelten Ring und zeigt den gemeldeten Akkuzustand und Stromverbrauch an.

Die Oberfläche richtet sich nach der Spracheinstellung des Smartphones. Dieses unabhängige Projekt ist keine offizielle Samsung-Anwendung.

### Voraussetzungen

- Ein Galaxy Ring, der mit dem Smartphone gekoppelt ist
- Bluetooth ist eingeschaltet und der Ring in der Nähe
- Android 12 oder neuer
- Berechtigung für Geräte in der Nähe

Root-Zugriff und Samsung Members werden nicht benötigt.

### Herunterladen und verwenden

Eine neue APK wird nur bei einem Versions-Tag wie `v1.0.0` erstellt und veröffentlicht. Du findest sie auf der [GitHub-Releases-Seite](https://github.com/Canic/galaxy-ring-battery-check/releases). Normale Pushes und Pull Requests veröffentlichen keine neue Version.

Kopple den Ring in den Bluetooth-Einstellungen, öffne **Ring-Akkucheck**, erlaube beim ersten Start den Zugriff auf Geräte in der Nähe und tippe auf **Test starten**. Halte den Ring dabei in der Nähe. Der Test dauert einige Sekunden.

Die APK ist ein Debug-Build. Android kann verlangen, eine lokal gebaute Version der App vor der Installation zu deinstallieren.

</details>

## Development

GitHub Actions builds and publishes the APK from [version tags](.github/workflows/build-apk.yml). Create tags in the `v<Major>.<Minor>.<Patch>` format, for example `v1.0.1`. For a local build, `build.sh` uses the Android tools in `../.tools/android`.

<details>
<summary>Protocol notes and raw diagnostic data / Protokollnotizen und Rohdaten</summary>

### Tested ring response / Antwort des getesteten Rings

```text
21 21 45 06 01 07 00 08 04 31 39 2E 35
```

| Field / Feld | Value / Wert | Samsung code interpretation / Interpretation |
| --- | --- | --- |
| Battery health / Batteriezustand (6) | 1 | Attention required / Maßnahme erforderlich |
| Current consumption / Stromverbrauch (7) | 0 | Normal |
| Capacity value / Kapazitätswert (8) | 19.5 | Unit not provided / Einheit nicht angegeben |

The value `19.5` is not a charge percentage; its unit cannot be determined from the response. / Der Wert `19.5` ist kein Ladeprozentsatz; seine Einheit lässt sich aus der Antwort nicht sicher bestimmen.

### Protocol / Protokoll

The app implements the `batteryStatus` diagnostic command (message ID 5) used by the inspected Galaxy Ring Manager, sent directly over Bluetooth LE. After connecting and subscribing to notifications on the Samsung data service, the ring sends a 167-byte handshake on a fresh connection. The app then writes `21 21 05` to the RX characteristic. The first two bytes are source and destination channel `0x21`; `05` encodes format 0, type 0, and battery test ID 5. The response uses the same channel and message ID 5 with response type 1 (`45`).

Die App setzt den `batteryStatus`-Diagnosebefehl (Nachrichten-ID 5) des untersuchten Galaxy Ring Managers direkt über Bluetooth LE um. Nach dem Verbinden und Abonnieren der Benachrichtigungen auf dem Samsung-Datendienst sendet der Ring bei einer frischen Verbindung einen 167-Byte-Handshake. Danach schreibt die App `21 21 05` an die RX-Charakteristik. Die ersten beiden Bytes sind Quell- und Zielkanal `0x21`; `05` codiert Format 0, Typ 0 und Akku-Test-ID 5. Die Antwort enthält denselben Kanal und die Nachrichten-ID 5 mit Antwort-Typ 1 (`45`).

The test was run on a Pixel 9, both with and without Galaxy Ring Manager running. / Der Test lief auf einem Pixel 9 mit und ohne laufenden Galaxy Ring Manager.

</details>
