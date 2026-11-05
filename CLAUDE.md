# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

Gradle projekt (Kotlin DSL, version catalog `gradle/libs.versions.toml`, AGP 9.4.1 s vestavěným Kotlinem, Gradle wrapper 9.6.0), jeden modul `app`. Plán realizace je v `plan.md` (zaškrtává se až po ověření), specifikace v `task.md`.

- `applicationId` je dočasně `cz.wordik.app` (změnit před krokem 16), `minSdk 26`, `compileSdk`/`targetSdk` 37, Compose + Material 3, testy JUnit 4 + coroutines-test + Turbine.
- Balíčky v `app/src/main/java/cz/wordik/app/`: `domain` (čistá logika bez Androidu), `data` (úložiště a správa sad, nastavení), `ui` (Compose obrazovky, ViewModely).
- `domain`: model a parser sad, `TestSession` (průchody/konec/statistiky), `ChoiceTest` (režim 1), `AssemblyTest`/`AssemblyRound` (režim 2), `SettingsValidator`, `ConfigParser`. `data`: `SetStorage`/`FileSetStorage` (soubory sad ve `filesDir/sets`), `SetLibrary` (načtení, vytvoření, uložení, smazání; jednorázové zkopírování přibalených sad), `SetRepository`, `ConfigRepository`, `SettingsStore` (DataStore). `ui`: `AppViewModel` + `Screen` (zásobník obrazovek, bez navigation-compose), `setup`, `test`, `results`, `settings`, `theme`, `components`.
- Konfigurace je v `app/src/main/assets/config.properties`; přibalené sady v `app/src/main/assets/sets/*.txt` se jen při prvním spuštění zkopírují do úložiště aplikace, dál se pracuje se `SetLibrary`.
- Správa sad (krok 14X): editor a QR import jsou v `ui/setup` (`SetEditorScreens.kt`, `CameraQrPreview.kt`). Skener používá CameraX a ML Kit Barcode Scanning (přibalený model, offline) a oprávnění `CAMERA`; náhled kamery je parametr `QrScanScreen`, jde nahradit. Skutečné čtení QR kódu se na emulátoru neověřilo (ruční zkouška na zařízení). Chybná sada nebo konfigurace se na startu zobrazí jako `StartupErrorScreen`.
- Instrumentované Compose UI testy celého toku jsou v `app/src/androidTest/.../FlowTest.kt` (testTagy `prompt`, `option`, `tile`, `slot`, `pass_progress`; vyžadují Espresso 3.7.0 kvůli API 37 a rozsvícený displej emulátoru, např. `adb shell svc power stayon true`). Zvuky: balíček `sound` (`SoundPlayer`, `SoundPool`; WAV generuje `tools/generate_sounds.py`). Kroky 15 a 16 plánu nejsou hotové; chyby z `bugs.md` (B1–B4) jsou opravené.
- Režim 2 po písmenkách: `AssemblyRound.hint()` (napověda, +1 chyba) a `giveUp()` (přeskočení, chyby za zbývající písmena); neúplné „Potvrdit“/„Dál“ jde přes potvrzovací dialog v `AssemblyTestScreen`.
- Znak BOM ve zdrojácích zapisujte jako `0xFEFF.toChar()`, ne literálem (lint `ByteOrderMark`).
- `local.properties` (cesta k SDK) je mimo git; na Windows musí mít `sdk.dir=C\:\\Users\\...` (zdvojená zpětná lomítka) (jinak selže lint `PropertyEscape`). `JAVA_HOME` ukazuje na JDK 21.

- Testy `SettingsStore` používají převážně in-memory DataStore: souborový DataStore na Windows JVM selže při druhém zápisu (přejmenování přes existující soubor), na Androidu ne.

## Commands

Na Windows `gradlew.bat` (v Git Bash `./gradlew.bat`).

- Sestavení debug APK: `./gradlew.bat assembleDebug`
- Unit testy: `./gradlew.bat test`
- Jeden test: `./gradlew.bat testDebugUnitTest --tests "cz.wordik.app.SanityTest"`
- Lint: `./gradlew.bat lint`
- Vše najednou: `./gradlew.bat assembleDebug test lint`
- Instrumentované testy (potřebují běžící emulátor/zařízení, při více zařízeních jen jedno): `./gradlew.bat connectedDebugAndroidTest`
- Emulátory (AVD): `Medium_Tablet_API_26`, `Pixel_Tablet_API_35`, `Medium_Phone_API_37.0`; `adb` je v `%ANDROID_HOME%\platform-tools`.

## What is being built (from `task.md`)

An Android app for children to practise vocabulary ("slovíčka"). The default language pair is CZ-EN, but the design should support any foreign language.

- **Target platform:** Android phones, minimum Android 8 (API 26). A feature may require a higher version if it needs one.
- **Word sets:** the user gets sets of words into the app in some way that is not yet specified ("nějak"). The import mechanism is an open design decision. The user picks a set to be tested on. Words are shown in random order.
- **Test settings:** each test is configured with a parameter `N`. Its meaning depends on the test mode (see below).

### Test modes

1. **Multiple choice.**
   - The app picks a random word and a random side of it (Czech or foreign).
   - It shows N other options from the same set, and the user taps the correct one.
   - The test ends when either the user has gone through all words N times, or the user reaches a target success rate of N % after going through the whole set.
2. **Letter assembly.**
   - The app picks a random word and shows its Czech form.
   - It offers the letters the target word is made of, optionally plus N extra random letters.
   - The user taps letters to build the word.
   - Evaluation is configurable in one of two ways:
     - Evaluate the whole word when the user presses a confirm button, giving success or failure.
     - Evaluate per letter: a correctly placed letter counts as a success, a wrong one as a failure.

Note that `N` is used for several different things in the spec (number of distractors, number of passes, success-rate percentage, number of extra letters). Model them as separate, named settings rather than one shared field.
