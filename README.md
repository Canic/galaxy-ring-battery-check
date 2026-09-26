# Galaxy Ring Akku-Selbsttest

Die App sendet denselben `batteryStatus`-Diagnosebefehl (Nachrichten-ID 5) wie der untersuchte Galaxy Ring Manager direkt über Bluetooth LE an einen bereits gekoppelten Galaxy Ring. Sie benötigt weder Root noch Samsung Members. Auf dem Pixel 9 wurde der Test mit und ohne laufenden Ring Manager erfolgreich ausgeführt.

## Verwendung

Die fertige APK liegt unter [`build/ring-battery-test.apk`](build/ring-battery-test.apk). Sie ist auf dem Pixel bereits installiert. Öffne **Ring Battery Check** und tippe auf **Start test**. Auf einem Gerät mit deutscher Systemsprache heißt sie **Ring-Akkucheck** und die Schaltfläche **Test starten**. Beim ersten Test muss die Bluetooth-Berechtigung zugelassen werden. Danach zeigt die App Fortschritt, Ergebnis und einzelne Akkuwerte an; mit der Schaltfläche lässt sich der Test wiederholen. Die Oberfläche folgt automatisch der Systemsprache (Englisch oder Deutsch).

## APK von GitHub Actions

Bei jedem Push, Pull Request oder manuellen Start baut der Workflow unter `.github/workflows/build-apk.yml` eine Debug-APK. Nach dem Lauf findest du sie im GitHub Actions-Lauf unter **Artifacts** als `ring-battery-check-…`. Die APK ist mit dem temporären Debug-Schlüssel des jeweiligen Builds signiert. Beim Wechsel von der lokal gebauten APK kann Android deshalb verlangen, die vorhandene App zuerst zu deinstallieren.

## Gemessenes Ergebnis

Der getestete Ring antwortete mit `21 21 45 06 01 07 00 08 04 31 39 2E 35`:

| Feld | Wert | Interpretation des Samsung-Codes |
| --- | --- | --- |
| Batteriezustand (6) | 1 | Maßnahme erforderlich |
| Stromverbrauch (7) | 0 | Normal |
| Kapazitätswert (8) | 19.5 | Einheit wird in der Antwort nicht angegeben |

Die App meldet deshalb insgesamt **Maßnahme erforderlich**. Der Wert `19.5` ist kein Ladeprozentsatz; die Einheit lässt sich aus dem untersuchten Code nicht sicher bestimmen.

## Technik

Nach BLE-Verbindung und GATT-Subscription auf den Samsung-Datendienst beantwortet die App bei einer frischen Verbindung den 167-Byte-Handshake. Danach schreibt sie `21 21 05` an die RX-Charakteristik. Die ersten zwei Bytes sind Quell- und Zielkanal `0x21`; `05` enthält Format 0, Typ 0 und die Akku-Test-ID 5. Die Antwort hat denselben Kanal und die Nachrichten-ID 5 mit Antwort-Typ 1 (`45`).

`build.sh` baut die APK aus dem Java-Quelltext mit den lokal unter `../.tools/android` abgelegten Android-Werkzeugen. Die APK ist mit einem lokalen Testschlüssel signiert.
