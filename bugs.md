Tento soubor obsahuje nalezené chyby uživatelem při reálném testování.

## Při spuštění více sad, které mají stejné slovo

Pokud má při spuštěném testu více sad společné slovo (ať už české nebo anglické), tak uživatel nebude vědět, které má doplnit.

Řešení: zatím zahlásit chybu při spuštění testu, test po potvrzení chyby spustit, ale taková slova vyhodit.

## Critical: Otevření kamery pro snímání QR kódu způsobí pád aplikace

**OPRAVENO** (release build, ověřeno na tabletu OSCAL Pad 16, Android 13). Příčina: R8 v release buildu poškodil inicializaci komponent ML Kitu, takže `BarcodeScanning.getClient()` spadl na NPE (debug build fungoval). Oprava: pravidla `-keep` pro ML Kit a `com.google.firebase.components` v `app/proguard-rules.pro`. Skenování QR kódu v opraveném release APK funguje.

Aplikace spadne po potvrzení/povolení použití kamery, tedy v okamžiku, kdy by se mělo zobrazit okno kamery, která by ukázala náhled snímaného qr kódu. Jiné agent dodal:

```

## Chyba při otevření fotoaparátu v aplikaci Wordík

Na tabletu **OSCAL Pad 16 s Androidem 13 (API 33)** aplikace `cz.wordik.app` spadne při pokusu otevřít kameru pro pořízení fotografie.

Z logu vyplývá:

- Dochází k `java.lang.NullPointerException`.
- Chyba nastává při vykreslování Jetpack Compose (`Error was captured in composition`).
- Výjimka vzniká v konstruktoru `mk1.<init>(y8)`, kde se aplikace pokouší číst pole `d43 iz2.a` z objektu, který je `null`.
- Následuje `FATAL EXCEPTION: main` a ukončení aplikace.

**Prosím prověř:**
1. Kód obrazovky a komponent, které se vykreslují při otevření fotoaparátu.
2. Předávání a inicializaci dat potřebných pro kameru.
3. Zda některá hodnota, stav nebo objekt související s kamerou není `null`.
4. Oprávnění ke kameře a správné zpracování výsledku spuštění fotoaparátu.
5. Zda je použitá knihovna nebo implementace kamery kompatibilní s Androidem 13 a daným zařízením.

Najdi konkrétní místo vzniku `NullPointerException` a oprav příčinu, ne pouze potlačení výjimky.

Poznámka: Stack trace je obfuskovaný, takže je potřeba dohledat původní třídy pomocí mapping souboru (pokud je k dispozici). Log sám o sobě zatím nedokazuje, že příčinou je přímo kamera nebo její oprávnění.

```

Log k události:
 ```
11-09 20:32:41.140 E/ComposeInternal(10704): Error was captured in composition.
11-09 20:32:41.140 E/ComposeInternal(10704): java.lang.NullPointerException: Attempt to read from field 'd43 iz2.a' on a null object reference in method 'void mk1.<init>(y8)'
11-09 20:32:41.140 E/ComposeInternal(10704):     at mk1.<init>(...:25)
11-09 20:32:41.140 E/ComposeInternal(10704):     at ub3.a(...:111)
11-09 20:32:41.140 E/ComposeInternal(10704):     at yv.m(...:89)
11-09 20:32:41.140 E/ComposeInternal(10704):     at rv.n(...:39)
11-09 20:32:41.140 E/ComposeInternal(10704):     at rv.m(...:9)
11-09 20:32:41.140 E/AndroidRuntime(10704): FATAL EXCEPTION: main
11-09 20:32:41.141 E/AndroidRuntime(10704): Process: cz.wordik.app, PID: 10704
11-09 20:32:41.141 E/AndroidRuntime(10704): java.lang.NullPointerException: Attempt to read from field 'd43 iz2.a' on a null object reference in method 'void mk1.<init>(y8)'
``