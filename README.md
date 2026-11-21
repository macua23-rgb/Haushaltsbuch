# Haushaltsbuch (Android)

Kotlin, Jetpack Compose (Material 3), Room, Hilt, WorkManager, DataStore, Glance, Vico.

## Starten
1. Ordner in Android Studio (aktuelle Version) öffnen, Gradle-Sync abwarten.
2. Auf Gerät oder Emulator (API 26+) starten. Beim ersten Start werden Beispielkonten,
   Kategorien, Budgets, Sparziele, zwei wiederkehrende Buchungen und Beispielkurse angelegt.
3. APK ohne Android Studio: Projekt in ein GitHub-Repository hochladen. Der Workflow
   `.github/workflows/build.yml` baut die Debug-APK; unter *Actions → Lauf → Artifacts* herunterladen
   und auf dem Handy installieren (Installation aus unbekannten Quellen erlauben).
4. Unit-Tests: `./gradlew testDebugUnitTest`

## Funktionen
- Buchungen erfassen und bearbeiten (Datum, Notiz, Tags, Konto, Kategorie), Löschen mit Rückfrage
- Beleg-Foto (Kamera oder Galerie), liegt nur im App-Speicher
- Konten (Giro, Bar, Kreditkarte, Spar) und Kategorien anlegen, bearbeiten, löschen
- Umbuchungen, auch zwischen Währungen; Kreditkartenabrechnung automatisch am Abrechnungstag
- Wiederkehrende Buchungen (wöchentlich, monatlich, jährlich), verpasste Termine werden nachgeholt
- Budgets und Sparziele bearbeiten; Benachrichtigung bei 80 % und 100 % (je Kategorie und Monat einmal)
- Mehrere Währungen mit selbst gepflegten Kursen und wählbarer Basiswährung
- Export als CSV (Excel-tauglich, Formeln entschärft) und PDF, Speicherort frei wählbar
- Sperre mit Biometrie oder Geräte-PIN (sperrt nach 30 s im Hintergrund), Widget mit Kontostand und Schnelleingabe

## Aufbau
- `data/` Room (Version 2, Migration 1→2), Repository, Einstellungen (DataStore), Wechselkurse, Beleg-Speicher
- `work/DailyWorker` täglich: wiederkehrende Buchungen, Kreditkartenabrechnung, Widget aktualisieren
- `notify/` Budget-Warnungen
- `export/` CSV und PDF
- `security/` Biometrie
- `widget/` Glance-Widget
- `ui/` Theme, Charts, Buchungs-Sheet, Navigation, `screens/`
- Beträge sind Cent als Long. Umbuchungen zählen nie als Einnahme oder Ausgabe.

## Bekannte Grenzen
- Wechselkurse sind manuell. Summen rechnen immer mit dem aktuellen Kurs, nicht mit dem Kurs am Buchungstag.
- Nur Währungen mit 2 Nachkommastellen (Liste in `ui/Format.kt`).
- Kreditkarten-Abrechnung bucht den gesamten offenen Saldo, keine Teilzahlung.
- Der Beleg-Export ist nicht enthalten, Belege sind nur in der App sichtbar.
- Kein Cloud-Backup (`allowBackup=false`), Sicherung nur über den CSV-Export.
- Benachrichtigungs-Icon ist ein Platzhalter (`BudgetNotifier`), App-Icon fehlt.
- Vico-API ist auf 2.0.x ausgelegt, Versionen in den Gradle-Dateien bei Bedarf aktualisieren.

## Tests
`app/src/test`: Format und Zahlenblock, Wechselkurse, Statistik (Umbuchungen, Zeiträume, Saldoverlauf),
CSV-Export, Budget-Warnstufen, Intervalle. Nicht abgedeckt: DAOs und Migration (bräuchten Instrumented Tests),
Compose-Oberfläche, PDF-Layout.
