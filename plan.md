# Plán realizace: Wordík

Plán vychází z `task.md`. Kroky jdou za sebou tak, aby se co nejvíc ověřovalo automaticky. Nejdřív vzniká logika bez Androidu (jde ji testovat na PC), potom UI a až nakonec balení a publikace.

**Značení:** 🤖 = udělám sám, 👤 = potřebuji vás (rozhodnutí, instalace, ruční zkouška na zařízení).
**Hotovo u každého kroku znamená:** projekt se sestaví a všechny testy kroku i předchozích kroků projdou. Zaškrtávám až po skutečném ověření.

## Stav prostředí

Na tomto počítači (kde vznikl plán) není Java, Android SDK ani Gradle. Realizace proběhne na jiném stroji, kde bude všechno potřebné nainstalované včetně možnosti spuštění emulátoru nebo zařízení. Nástroje tedy neinstaluji, jen na začátku ověřím, že jsou k dispozici. Git repozitář založí uživatel.

## Rozhodnutí z odpovědí

- Nástroje a zařízení pro zkoušku jsou připravené na cílovém stroji (krok 0 se omezuje na kontrolu).
- Git repozitář vytvoří uživatel. Commity dělám jen na vyžádání.
- Zvuky vygeneruji skriptem (syntetické tóny, bez licenčních omezení).
- Výchozí font: Atkinson Hyperlegible, přibalený v aplikaci.
- Ikona: jednoduchá vektorová adaptivní ikona s písmenem „W“ ve veselých barvách.
- **Krok 16 (Google Play) je zablokovaný**, dokud uživatel nerozhodne o package name (applicationId) a o účtu Google Play. Do té doby se používá dočasný identifikátor `cz.wordik.app`, který lze před publikací změnit.

---

## Krok 0: Příprava prostředí a základní rozhodnutí

Co se dělá: na cílovém stroji se ověří, že jsou k dispozici nástroje pro sestavení a spuštění testů. Nic se neinstaluje.

- [x] 👤 Odpovědět na otázky v `questions.md`
- [x] 👤 Připravit cílový stroj: JDK 17, Android SDK (platforma API 34/35, build-tools), případně emulátor nebo zařízení s API 26 a novějším
  - Ověřeno: JDK 21.0.8 (`JAVA_HOME`, není v `PATH`), SDK platformy 26 a 37.0, build-tools 36.0.0, AVD `Medium_Tablet_API_26`, `Pixel_Tablet_API_35`, `Medium_Phone_API_37.0`. Místo JDK 17 je JDK 21 (stačí pro AGP 8.x); platformy 34/35 chybí, použije se `compileSdk` 36/37 (případně se platforma doinstaluje v kroku 1).
- [x] 👤 Založit git repozitář
- [x] 🤖 Ověřit nástroje na cílovém stroji: `java -version`, `ANDROID_HOME`, `sdkmanager --list`, `adb devices`
  - `java` není v `PATH` (funguje přes `JAVA_HOME`), `adb` a `sdkmanager` také ne (jsou v `ANDROID_HOME\platform-tools` a `cmdline-tools\latest\bin`). `adb devices`: žádné připojené zařízení, emulátor je nutné spustit (AVD viz výše). Gradle není globálně, použije se wrapper.
- [x] 🤖 Přidat `.gitignore` (Gradle, IDE, `local.properties`, keystore)

## Krok 1: Kostra projektu

Co se dělá: prázdná aplikace, která se sestaví, spustí a má nastavené základní nástroje.

- [x] 🤖 Vytvořit Gradle projekt (Kotlin DSL, version catalog `libs.versions.toml`, Gradle wrapper)
- [x] 🤖 Modul `app`: `minSdk 26`, `targetSdk` dle aktuálního požadavku Google Play, Kotlin, Jetpack Compose, Material 3, dočasný `applicationId` `cz.wordik.app` (změnit před krokem 16)
- [x] 🤖 Zvolit strukturu balíčků: `domain` (čistá logika bez Androidu), `data` (načítání sad, nastavení), `ui` (Compose obrazovky, ViewModely)
- [x] 🤖 Nastavit testovací závislosti (JUnit 5 nebo 4, kotlinx-coroutines-test, případně Turbine, Compose UI test)
- [x] 🤖 Prázdná obrazovka „Wordík“ a název aplikace v zdrojích
- [x] 🤖 Ověřit `./gradlew assembleDebug`, `./gradlew test` a `./gradlew lint`
- [x] 🤖 Doplnit do `CLAUDE.md` skutečné příkazy (build, test, spuštění jednoho testu)

## Krok 2: Model slovíček a načítání sad

Co se dělá: datový model, parser souboru `česky;cizí` a načtení přibalených sad. Všechno v čistém Kotlinu, testy běží na PC.

- [ ] 🤖 Model: `Word` (česky, cizí, příznak „rozlišovat velikost písmen“), `WordSet` (název, seznam slovíček)
- [ ] 🤖 Parser: řádek `česky;cizí`, ignorování prázdných řádků, odmítnutí řádku bez `;` nebo s více `;`, ořezání mezer
- [ ] 🤖 Zpracování uvozovek kolem cizího slovíčka (příznak se nastaví, uvozovky se odstraní)
- [ ] 🤖 Zpracování chyb: srozumitelná chyba s číslem řádku, aby se špatná sada dala opravit
- [ ] 🤖 Dvě ukázkové sady CZ-EN po cca 12 slovíčkách v `assets/sets/` (název sady = název souboru bez přípony), mezi nimi fráze, apostrof a slovíčko v uvozovkách
- [ ] 🤖 `SetRepository`: načtení všech přibalených sad z assets
- [ ] 🤖 Unit testy parseru (běžný řádek, mezery, prázdné řádky, uvozovky, diakritika, UTF-8, chybné řádky)
- [ ] 🤖 Test, že ukázkové sady z assets jdou načíst bez chyb

## Krok 3: Konfigurace a nastavení testu

Co se dělá: konfigurační soubor s výchozími hodnotami a rozsahy a model nastavení testu.

- [ ] 🤖 Konfigurační soubor v `assets` (nebo `res/raw`): výchozí hodnoty, rozsahy, interval automatického přechodu, font
- [ ] 🤖 Model `TestSettings` (režim, počet nesprávných variant, minimální průchody, minimální úspěšnost, písmena navíc, způsob hodnocení, směr)
- [ ] 🤖 Načtení konfigurace a převod na výchozí `TestSettings`
- [ ] 🤖 Validace nastavení podle rozsahů a podle vybraných sad (režim 1 vyžaduje alespoň N+1 slovíček celkem)
- [ ] 🤖 Unit testy: načtení konfigurace, hranice rozsahů, chybějící nebo chybná hodnota v konfiguraci, validace N+1

## Krok 4: Ukládání nastavení

Co se dělá: pamatování posledního nastavení pro kombinaci sad a nastavení aplikace (Normální / Velké).

- [ ] 🤖 Zvolit úložiště: Jetpack DataStore (historie se neukládá, databáze není potřeba)
- [ ] 🤖 Klíč kombinace sad (stabilní nezávisle na pořadí výběru, např. seřazené názvy sad)
- [ ] 🤖 `SettingsStore`: uložení a načtení `TestSettings` pro kombinaci sad, pro dosud nepoužitou kombinaci vrátit výchozí
- [ ] 🤖 Uložení volby „Normální / Velké“ pro celou aplikaci
- [ ] 🤖 Ošetřit poškozená nebo nekompatibilní uložená data (návrat k výchozím hodnotám)
- [ ] 🤖 Testy: ukládání a načtení, jiné pořadí sad = stejná kombinace, nová kombinace = výchozí hodnoty, poškozená data

## Krok 5: Jádro testu (průchody, konec, statistiky)

Co se dělá: stavový automat testu nezávislý na režimu: pořadí slovíček, průchody, podmínka konce, statistiky a výsledky.

- [ ] 🤖 `TestSession`: fronta slovíček na průchod, každé právě jednou, v náhodném pořadí; náhodnost vložená zvenku (seed pro testy)
- [ ] 🤖 Zaznamenání výsledku odpovědi (správně / špatně, u písmen počty správných a chybných)
- [ ] 🤖 Podmínka konce kontrolovaná po každém slovíčku: dokončené průchody ≥ minimum A celková úspěšnost ≥ minimum
- [ ] 🤖 Výpočet úspěšnosti: správné odpovědi / všechny odpovědi, resp. správná písmena / (správná + chybná)
- [ ] 🤖 Pokračování dalšími průchody, dokud nejsou podmínky splněny
- [ ] 🤖 Předčasné ukončení a příznak „Test nebyl dokončen“
- [ ] 🤖 `TestResult`: počty, procento, chybná slovíčka, doba trvání (hodiny vložené zvenku), seznam slovíček podle úspěšnosti vzestupně, při shodě podle abecedy
- [ ] 🤖 Unit testy: konec uprostřed třetího průchodu, procento dosaženo dřív než průchody, procento nedosaženo → další průchod, předčasné ukončení, řazení výsledků, sloučení více sad, deterministické pořadí při pevném seedu

## Krok 6: Logika režimu 1 (výběr z variant)

Co se dělá: generování otázek a vyhodnocení.

- [ ] 🤖 Volba strany podle směru (CZ→cizí, cizí→CZ, mix náhodně)
- [ ] 🤖 Generování N nesprávných variant náhodně ze všech slovíček vybraných sad, bez shodného textu se správnou odpovědí a bez duplicit
- [ ] 🤖 Zamíchání N+1 možností
- [ ] 🤖 Vyhodnocení odpovědi a propojení s `TestSession`
- [ ] 🤖 Unit testy: počet možností N+1, správná odpověď je vždy mezi možnostmi, žádné duplicity, směr respektuje nastavení, mix dává obě strany, málo slovíček (chyba při startu), více sad

## Krok 7: Logika režimu 2 (skládání z písmen)

Co se dělá: nabídka písmen, hodnocení po písmenkách i celého slova.

- [ ] 🤖 Rozklad cizího slovíčka na znaky, nabídka včetně opakovaných znaků, mezery, apostrofu a pomlčky, náhodné pořadí
- [ ] 🤖 Písmena navíc: jen písmena, která se vyskytují v cizích slovíčkách vybraných sad, počet dle nastavení
- [ ] 🤖 Porovnání znaků: bez rozlišení velikosti písmen, s rozlišením u slovíčka v uvozovkách, diakritika je jiné písmeno
- [ ] 🤖 Hodnocení po písmenkách: kontrola jen dalšího očekávaného znaku, správný se vloží, špatný se započítá jako chyba a nevloží se; počítání správných a chybných písmen
- [ ] 🤖 Hodnocení celého slova: potvrzení kdykoliv (i nevyplněného slova), špatné slovo = chyba za průchod, bez opravy
- [ ] 🤖 Vrácení vloženého písmena do nabídky před potvrzením (jen v hodnocení celého slova)
- [ ] 🤖 Informace pro UI, zda je potřeba povinné „Dál“ (slovo s chybou) nebo se přechází automaticky
- [ ] 🤖 Unit testy: slovo „book“ (dvě písmena „o“), mezera ve frázi, „c“ ≠ „č“, velikost písmen s uvozovkami i bez nich, písmena navíc nikdy nejsou mimo povolenou množinu, chyba nezvýší počet správných písmen nad délku slova, nevyplněné potvrzení, vrácení písmena

## Krok 8: Základ UI (téma, navigace, nastavení aplikace)

Co se dělá: vzhled a kostra obrazovek.

- [ ] 🤖 Téma: veselé, ale střídmé barvy, typografie s fontem z konfigurace (musí jasně rozlišovat „l“, „I“, „1“)
- [ ] 🤖 Úrovně velikosti „Normální / Velké“ aplikované na tlačítka a písmo v celé aplikaci
- [ ] 🤖 Navigace: výběr sad → nastavení testu → test → výsledky; návrat a ukončení testu
- [ ] 🤖 Obrazovka „Nastavení“ (velikost tlačítek a písma), uložení do `SettingsStore`
- [ ] 🤖 Adaptivní rozvržení pro telefon i tablet, na výšku i na šířku
- [ ] 🤖 Compose preview pro klíčové komponenty
- [ ] 🤖 Ověřit sestavení a lint

## Krok 9: Výběr sad a nastavení testu

Co se dělá: obrazovky před začátkem testu.

- [ ] 🤖 Seznam sad s možností vybrat jednu nebo více najednou, počet slovíček u každé sady
- [ ] 🤖 Obrazovka nastavení testu: pole podle režimu (volby, které k režimu nepatří, se skryjí), hodnoty z uložené kombinace nebo výchozí
- [ ] 🤖 Validace v UI s českými hláškami, včetně chyby „málo slovíček pro N+1 možností“ a nespuštění testu
- [ ] 🤖 Uložení nastavení při spuštění testu
- [ ] 🤖 ViewModel testy: výběr více sad, načtení uloženého nastavení, chyba N+1, výchozí hodnoty pro novou kombinaci
- [ ] 🤖 Compose UI testy obrazovek (běží na emulátoru nebo zařízení, viz krok 14)

## Krok 10: Obrazovka režimu 1

Co se dělá: průběh testu s výběrem z variant.

- [ ] 🤖 ViewModel nad `TestSession` a generátorem otázek
- [ ] 🤖 Zobrazení slovíčka a N+1 tlačítek, zpětná vazba (barva) po odpovědi
- [ ] 🤖 Po správné odpovědi automatický přechod po intervalu z konfigurace a tlačítko „Dál“ pro dřívější přechod
- [ ] 🤖 Po chybné odpovědi zvýraznění správné odpovědi a povinné „Dál“, bez možnosti opravy
- [ ] 🤖 Tlačítko „Ukončit test“ (potvrzení) vede na výsledky s označením „Test nebyl dokončen“
- [ ] 🤖 ViewModel testy s řízeným časem (interval, automatický přechod, předčasný konec, konec při splnění podmínek)

## Krok 11: Obrazovka režimu 2

Co se dělá: průběh testu se skládáním slova.

- [ ] 🤖 Zobrazení českého slovíčka, prázdných míst podle délky cizího slova (včetně mezer) a nabídky písmen
- [ ] 🤖 Hodnocení po písmenkách: správné písmeno zaplní další místo, špatné vyvolá hlášku chyby a nevloží se; vložená písmena zešednou v nabídce
- [ ] 🤖 Hodnocení celého slova: klepnutím na vložené písmeno se vrátí do nabídky, tlačítko „Potvrdit“ je dostupné kdykoliv, po chybě se zobrazí informace a správné znění
- [ ] 🤖 Pokračování: po slově bez chyby automaticky po intervalu (+ „Dál“), po slově s chybou povinné „Dál“
- [ ] 🤖 Ukončení testu stejně jako v režimu 1
- [ ] 🤖 Zvládnutí dlouhých frází na malé obrazovce (zalamování míst a tlačítek)
- [ ] 🤖 ViewModel testy: oba způsoby hodnocení, povinné „Dál“ po chybě, automatický přechod po úspěchu

## Krok 12: Výsledky testu

Co se dělá: obrazovka po skončení testu.

- [ ] 🤖 Počet správných a špatných odpovědí, procento, doba trvání
- [ ] 🤖 Seznam chybně zodpovězených slovíček
- [ ] 🤖 Seznam všech slovíček podle úspěšnosti od nejnižší, při shodě podle abecedy
- [ ] 🤖 Označení „Test nebyl dokončen“ při předčasném ukončení
- [ ] 🤖 Tlačítka pro návrat na výběr sad a pro opakování testu se stejným nastavením
- [ ] 🤖 Testy formátování výsledků a Compose UI test obrazovky

## Krok 13: Zvuky a motivační prvky

Co se dělá: zvuková zpětná vazba a drobná vizuální odměna.

- [ ] 🤖 Zvuky (správně, chyba, konec testu) vygenerované skriptem ze syntetických tónů, bez licenčních omezení
- [ ] 🤖 Přehrávání přes `SoundPool`, předběžné načtení, žádné vibrace
- [ ] 🤖 Zvuky respektují hlasitost zařízení, bez vlastního vypínače
- [ ] 🤖 Jednoduché vizuální odměny (barvy, ikona, krátká animace po dokončení), aby nezhoršily použitelnost
- [ ] 🤖 Zvukovou službu oddělit rozhraním, aby šla v testech nahradit

## Krok 14: Testování a ladění

Co se dělá: důkladné ověření celé aplikace.

- [ ] 🤖 Spustit všechny unit testy a lint, opravit nalezené chyby
- [ ] 🤖 Doplnit chybějící testy hraničních případů (jedna sada, více sad, minimální a maximální hodnoty nastavení, prázdná sada, sada s jedním slovíčkem)
- [ ] 🤖 Compose UI testy celého toku: výběr sady → nastavení → test v obou režimech → výsledky (pokud je k dispozici emulátor nebo zařízení)
- [ ] 🤖 Zkouška na emulátoru s API 26 a s novějším API, na telefonu i tabletu, na výšku i na šířku, obě velikosti písma
- [ ] 👤 Ruční zkouška na skutečném zařízení (ovládání dítětem, čitelnost fontu, zvuky, velikost tlačítek)
- [ ] 🤖 Opravit nalezené chyby podle zpětné vazby
- [ ] 🤖 Projít `task.md` bod po bodu a ověřit, že je vše splněno

## Krok 15: Sestavení instalovatelného APK

Co se dělá: ikona, podpis a APK k instalaci.

- [ ] 🤖 Ikona aplikace (adaptivní) a název „Wordík“
- [ ] 🤖 Nastavení verze (`versionName`, `versionCode`)
- [ ] 🤖 Release build s minifikací (R8) a ověření, že funguje stejně jako debug
- [ ] 🤖 Podpis APK vlastním keystorem (keystore mimo repozitář, heslo mimo kód)
- [ ] 🤖 Vytvořit podepsané APK a ověřit instalaci na API 26 i novějším
- [ ] 👤 Instalace APK na vaše zařízení a schválení
- [ ] 🤖 Aktualizovat `CLAUDE.md` (příkazy pro release build, struktura projektu)

## Krok 16: Publikace na Google Play (poslední) – ZABLOKOVÁNO

Co se dělá: příprava a nahrání do obchodu. Velká část vyžaduje váš účet.

**Blokováno, dokud uživatel nerozhodne:** konečný package name (`applicationId`; po publikaci ho nelze změnit), vývojářský účet Google Play a místo pro zásady ochrany soukromí. Do té doby se tento krok nezačíná.

- [ ] 👤 Rozhodnout o konečném `applicationId` a změnit ho v projektu
- [ ] 👤 Vývojářský účet Google Play (jednorázový poplatek, ověření totožnosti)
- [ ] 🤖 Přechod na Android App Bundle (AAB) a podepisování Google Play App Signing
- [ ] 🤖 Zásady ochrany soukromí (aplikace nesbírá žádná data), podklady pro formulář „Data safety“
- [ ] 🤖 Podklady do obchodu: název, krátký a dlouhý popis česky, screenshoty telefonu a tabletu, ikona, grafika
- [ ] 👤 Vyplnit dotazníky v Google Play Console (obsah aplikace, cílová skupina včetně dětí, hodnocení obsahu)
- [ ] 🤖 Ověřit požadavky pro aplikace pro děti (Families Policy) a upravit aplikaci, pokud bude třeba
- [ ] 👤 Nahrát AAB do interního testování, vyzkoušet z obchodu
- [ ] 👤 Odeslat do produkce a vyřešit případné připomínky kontroly

---

## Předpoklady a rizika

- Realizace předpokládá cílový stroj s JDK, Android SDK a Gradle. Pokud tam něco chybí, krok 0 se zastaví a doplní se.
- Compose UI testy a zkoušku na API 26 lze spustit jen s emulátorem nebo připojeným zařízením. Pokud je cílový stroj nebude mít k dispozici, ověří UI uživatel ručně a automaticky zůstanou testy logiky a ViewModelů.
- Aplikace pro děti v Google Play podléhá přísnějším pravidlům. Zjistí se v kroku 16, může vyžadovat úpravy (např. omezení reklam a sběru dat, které aplikace stejně nemá).
