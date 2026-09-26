# Galaxy Ring Battery Check

Ein einfacher Selbsttest für den Akku des Galaxy Ring. Die App verbindet sich direkt per Bluetooth mit einem bereits gekoppelten Ring und zeigt dessen Akkuzustand und Stromverbrauch an.

Die Oberfläche ist auf Deutsch und Englisch verfügbar und richtet sich nach der Spracheinstellung des Smartphones. Die App ist ein unabhängiges Projekt und keine offizielle Samsung-Anwendung.

## Funktionen

- Akkuselbsttest mit einer Schaltfläche starten und wiederholen
- Akkuzustand und Stromverbrauch anzeigen
- Verbindung, Berechtigungen und Fehler verständlich melden
- Auf Deutsch und Englisch nutzbar

## Voraussetzungen

- Ein Galaxy Ring, der bereits mit dem Smartphone gekoppelt ist
- Bluetooth eingeschaltet und der Ring in der Nähe
- Android 12 oder neuer
- Berechtigung für Geräte in der Nähe

Root-Zugriff und Samsung Members werden nicht benötigt.

## App herunterladen

Eine neue Version wird nur gebaut und veröffentlicht, wenn ein Versions-Tag wie `v1.0.0` gepusht wird. Die fertige APK findest du dann auf der [Releases-Seite](https://github.com/Canic/galaxy-ring-battery-check/releases). Normale Pushes und Pull Requests lösen keine Veröffentlichung aus.

Es handelt sich um eine Debug-APK. Android kann beim Installieren eines neuen Builds verlangen, eine bereits installierte lokal gebaute Version dieser App zuerst zu deinstallieren.

## Selbsttest durchführen

1. Kopple den Galaxy Ring in den Bluetooth-Einstellungen des Smartphones.
2. Öffne **Ring Battery Check** beziehungsweise **Ring-Akkucheck**.
3. Erlaube beim ersten Start den Zugriff auf Geräte in der Nähe.
4. Tippe auf **Start test** beziehungsweise **Test starten** und halte den Ring in der Nähe.

Der Test dauert einige Sekunden. Das Ergebnis zeigt, ob der gemeldete Akkuzustand und Stromverbrauch normal sind. Der Ring liefert außerdem einen Kapazitätswert, aber keine Einheit dafür. Die App zeigt diesen deshalb nicht als Ladezustand in Prozent an.

## Entwicklung

Der Android-Build wird über [GitHub Actions](.github/workflows/build-apk.yml) erstellt. Eine Version wird durch einen Tag im Format `v<Major>.<Minor>.<Patch>` ausgelöst, zum Beispiel `v1.0.1`. Für einen lokalen Build steht `build.sh` bereit; es verwendet die Android-Werkzeuge aus `../.tools/android`.

<details>
<summary>Technische Hintergründe und Rohdaten</summary>

### Ergebnis des getesteten Rings

Der Ring antwortete mit:

```text
21 21 45 06 01 07 00 08 04 31 39 2E 35
```

| Feld | Wert | Interpretation des Samsung-Codes |
| --- | --- | --- |
| Batteriezustand (6) | 1 | Maßnahme erforderlich |
| Stromverbrauch (7) | 0 | Normal |
| Kapazitätswert (8) | 19.5 | Einheit nicht angegeben |

Der Kapazitätswert `19.5` ist kein Ladeprozentsatz. Seine Einheit lässt sich aus der Antwort nicht sicher bestimmen.

### Protokollnotizen

Die App setzt den vom Galaxy Ring Manager verwendeten `batteryStatus`-Diagnosebefehl (Nachrichten-ID 5) direkt über Bluetooth LE um. Nach der Verbindung und dem Abonnieren der Benachrichtigungen auf dem Samsung-Datendienst beantwortet der Ring bei einer frischen Verbindung einen 167-Byte-Handshake. Anschließend sendet die App `21 21 05` an die RX-Charakteristik. Die ersten beiden Bytes sind Quell- und Zielkanal `0x21`; `05` enthält Format 0, Typ 0 und die Akku-Test-ID 5. Die Antwort enthält denselben Kanal und die Nachrichten-ID 5 mit Antwort-Typ 1 (`45`).

Der Test wurde auf einem Pixel 9 mit und ohne laufenden Galaxy Ring Manager durchgeführt.

</details>
