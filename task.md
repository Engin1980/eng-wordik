# Program na zkoušení slovíček

Pracovní název aplikace: **Wordík** (zatím nezávazný).

Cílem je vytvořit android aplikaci pro zkoušení slovíček pro děti (a rodiče, kteří je zkoušejí). Aplikace obsahuje sady slovíček. Uživatel si vybere, z jaké sady (nebo z více sad najednou) chce být zkoušen, a program zobrazuje zkoušená slovíčka v náhodném pořadí. Jedna strana slovíčka je vždy česká, druhá je libovolný cizí jazyk (typicky angličtina). Podporují se jen jazyky v latince.

Tento dokument popisuje **co má aplikace dělat**, ne jak se bude vyvíjet. Plán realizace vznikne zvlášť. Otevřené otázky se vedou v `questions.md` (aktuálně žádné).

## Uživatel

- Existuje jediná role „uživatel“. Nerozlišují se rodiče, děti ani učitelé, nejsou profily ani PIN.
- Aplikace je určena spíš pro rodiče a děti, ne jako nástroj pro učitele.

## Sady slovíček

- Jedna sada = jeden textový soubor v UTF-8. Název sady je název souboru bez přípony. Soubor nemá hlavičku, prázdné řádky se ignorují.
- Každý řádek je jedno slovíčko ve tvaru `česky;cizí` (levá strana česky, pravá cizí jazyk, oddělovač `;`). Znak `;` uvnitř slovíčka není povolen.
- Slovíčko má vždy právě jeden překlad. Synonyma a více správných odpovědí se neřeší.
- Slovíčko může být i fráze: může obsahovat mezery, interpunkci, velká písmena a závorky (např. „to be“, „I’m“, „ice cream“).
- Cizí slovíčko může být celé uzavřeno v uvozovkách. Uvozovky jsou jen příznak, že se v režimu 2 rozlišuje velikost písmen. Nezobrazují se ani se neskládají.
- Jazyk cizí strany se v sadě neurčuje (doplní se až při budoucím importu sad).
- Typická sada má kolem 12 slovíček, maximálně nižší desítky.
- **Zdroj sad:** sady jsou uložené v úložišti aplikace (jeden soubor `název.txt` na sadu). Při prvním spuštění se do něj zkopírují 2 přibalené ukázkové sady po cca 12 slovíčkách (CZ-EN); po smazání se znovu neobjeví.
- **Správa sad:** uživatel může sadu vytvořit, upravit a smazat; přejmenování sady zatím není.
  - **Název sady** musí být neprázdný, unikátní (bez ohledu na velikost písmen), nejvýše 60 znaků, bez znaků `\ / : * ? " < > |` a nesmí končit tečkou.
  - **Nová sada:** po zadání názvu vznikne prázdná sada a otevře se editace.
  - **Editace:** jednoduchý textový editor se slovíčky jako volný text `česky;cizí` po řádcích (slovíčko s rozlišením velikosti písmen má cizí část v uvozovkách). Uživatel změny uloží, nebo zahodí (zahození neuložených změn se potvrzuje). Text s chybou se neuloží; zobrazí se chyby s čísly řádků. Prázdnou sadu uložit lze.
  - **Smazání** sady vyžaduje potvrzení.
  - **Import přes QR kód:** po zadání názvu se otevře skener (kamera, vyžaduje povolení). QR kód obsahuje UTF-8 text `česky;cizí` po řádcích. Lze naskenovat více kódů za sebou (bez pořadí); slova se sloučí a duplicity (shodné české i cizí znění) se vynechají. Po „Hotovo“ se pokračuje editací naskenovaného textu, sada se vytvoří až uložením.
- Do jednoho testu lze vybrat více sad najednou.

## Test

### Společná pravidla

- **Průchod:** každé slovíčko vybraných sad se zeptá právě jednou, v náhodném pořadí.
- **Konec testu:** test končí, až jsou splněny obě podmínky současně:
  1. proběhl alespoň zadaný počet průchodů,
  2. celková úspěšnost za celý test dosáhla alespoň zadaného procenta.
- Podmínky se kontrolují po každém slovíčku, test tedy může skončit i uprostřed průchodu (např. uprostřed třetího, pokud byly požadovány minimálně 2 průchody).
- Úspěšnost se počítá za celý test, ne za poslední průchod. Počítá se jako:
  - počet správných odpovědí / počet všech odpovědí (režim 1 a režim 2 s hodnocením celého slova),
  - počet správných písmen / (správná + chybná písmena) (režim 2 s hodnocením po písmenkách).
- Pokud požadované procento není dosaženo, test pokračuje dalšími průchody, dokud se nedosáhne nebo ho uživatel neukončí.
- **Průběh průchodu:** během testu (v obou režimech) je vidět, kolik slovíček aktuálního průchodu je hotovo („Hotovo X z Y slovíček v tomto průchodu“) včetně ukazatele.
- Uživatel může test kdykoliv ukončit. Přerušený test nelze později obnovit. Po předčasném ukončení se zobrazí výsledky dosavadního průběhu s označením „Test nebyl dokončen“.
- Při spuštění testu se vždy vybírá sada nebo sady a nastavení testu. Aplikace si pamatuje naposledy použité nastavení pro přesnou kombinaci vybraných sad (jedna sada je také kombinace). Pro dosud nepoužitou kombinaci se nabídnou výchozí hodnoty z konfiguračního souboru.

### Nastavení testu

| Nastavení | Platí pro | Výchozí |
|---|---|---|
| Režim testu (1 / 2) | oba | 2 |
| Počet nesprávných variant | režim 1 | 4 |
| Minimální počet průchodů | oba | 2 |
| Minimální úspěšnost v % | oba | 50 |
| Počet písmen navíc | režim 2 | 0 |
| Způsob hodnocení (celé slovo / po písmenkách) | režim 2 | po písmenkách |
| Směr (CZ→cizí / cizí→CZ / mix) | režim 1 | mix |

- V režimu 2 je směr vždy CZ → cizí jazyk.
- Způsob hodnocení se volí jen před začátkem testu, za běhu se nemění.

### Konfigurační soubor

Soubor je součástí zdrojů aplikace (změna znamená nové sestavení). Obsahuje:

- výchozí hodnoty nastavení testu (viz tabulka výše),
- povolené rozsahy: počet nesprávných variant 1–8, počet průchodů 1–10, úspěšnost 1–100 %, písmena navíc 0–10,
- interval automatického přechodu na další slovíčko po správné odpovědi (výchozí cca 1 s),
- font použitý pro slovíčka. Měl by jasně rozlišovat znaky „l“, „I“ a „1“, se kterými děti často bojují.

### Režim 1: výběr z variant

- Aplikace vybere náhodné slovíčko a podle nastaveného směru jeho stranu (česká / cizí; při směru „mix“ náhodně).
- Pro toto slovíčko zobrazí N nesprávných variant (N je nastavení „počet nesprávných variant“) a k nim správnou, tedy N+1 možností. Nesprávné varianty se losují náhodně ze všech slovíček vybraných sad dohromady. Text shodný se správnou odpovědí se z nabídky vynechá.
- Uživatel klikem vybere správnou možnost.
- **Po správné odpovědi** se zobrazí krátká zpětná vazba (barva a zvuk) a po intervalu z konfigurace se automaticky přejde na další slovíčko. Dříve lze přejít tlačítkem „Dál“.
- **Po chybné odpovědi** se zvýrazní správná odpověď a pokračuje se až po klepnutí na „Dál“, aby ji dítě stihlo přečíst. Uživatel nemůže odpovědět znovu.
- Pokud vybrané sady dohromady obsahují méně než N+1 slovíček, aplikace při pokusu o spuštění testu ohlásí chybu a test se nespustí.

### Režim 2: skládání slova z písmen

- Aplikace vybere náhodné slovíčko a zobrazí jeho **české** znění. Uživatel skládá **cizí** slovo.
- Aplikace nabídne písmena, ze kterých se slovo skládá. Opakující se písmena se nabízejí opakovaně (např. „book“ nabídne dvě písmena „o“). Pořadí nabízených písmen je náhodné.
- Nabízejí se všechny znaky slovíčka, tedy i mezera, apostrof a pomlčka. Mezera je samostatná „klávesa“.
- Vložené písmeno z nabídky zešedne a nelze ho použít znovu.
- Volitelně se přidá N náhodných písmen navíc. Losují se jen z písmen (ne z mezery ani interpunkce), která se vyskytují v cizích slovíčkách všech vybraných sad. Losované písmeno se může shodovat s písmenem, které ve slově už je.
- Uživatel klikáním na písmena slovo sestavuje. Rozpracované slovo se zobrazuje jako prázdná místa podle délky slova (včetně mezer), která se postupně plní.
- Písmena s diakritikou jsou jiná písmena než bez ní (c ≠ č).
- Velikost písmen se nerozlišuje. Bez uvozovek nabídka zobrazuje písmena tak, jak jsou zapsána v sadě, a „I“ i „i“ se považují za stejné písmeno. Výjimkou je cizí slovíčko zapsané v uvozovkách: u něj se velikost písmen rozlišuje.
- **Hodnocení celého slova:**
  - Slovo se vyhodnotí po stisku tlačítka „Potvrdit“. Tlačítko je dostupné kdykoliv, i když slovo není zaplněné, aby mohl uživatel pokračovat dál, když slovo vůbec nezná. Nevyplněné slovo se vyhodnotí jako chyba.
  - Před potvrzením lze vložené písmeno vzít zpět klepnutím na něj (vrátí se do nabídky).
  - Při chybě se zobrazí informace o chybě a správné znění slova a **pokračuje se dalším slovem**. Aktuální slovo se neopravuje.
  - Slovo se počítá jako jeden úspěch nebo jedna chyba za průchod.
- **Hodnocení po písmenech:**
  - Každé vložené písmeno se hodnotí hned a hodnotí se vždy jen další očekávané písmeno.
  - Správné písmeno se vloží. Špatné se nevloží, ohlásí se chyba a započítá se chyba.
  - Uživatel pokračuje, dokud slovo není správně doplněné. Na jednom slově tak může nabrat více chyb, ale správných písmen jen tolik, kolik jich slovo má. Každé písmeno se do statistiky počítá samostatně.
  - Pokud slovo po chybě na konci správně doplní, zobrazí se „Nyní je řešení správné.“ (ne „Špatně“); chyby se do statistiky počítají dál.
  - **Přeskočení slova („Dál“):** uživatel, který slovo vůbec nezná, může kdykoliv stisknout „Dál“. Po potvrzení dialogem „Opravdu pokračovat?“ se za každé dosud nedoplněné písmeno započítá chyba (už doplněná písmena zůstanou správná) a zobrazí se správné řešení („Správné řešení: …“). Pak je „Dál“ povinné.
  - **Nápověda („Napověz“):** doplní další písmeno a započítá jednu chybu; samotné doplněné písmeno se jako správné nepočítá. Doplní-li nápověda poslední písmeno, slovo je hotové (s chybou).
- **Pokračování po dokončení slova:** po slově bez chyby se přejde na další automaticky po intervalu z konfigurace, dřív tlačítkem „Dál“. Pokud při slově nastala chyba (špatné slovo při hodnocení celého slova, nebo alespoň jedno špatné písmeno či použitá nápověda/přeskočení při hodnocení po písmenkách), je tlačítko „Dál“ povinné a automatický přechod se nepoužije.
- **Ochrana proti nechtěnému dvojklepnutí:** „Potvrdit“ u neúplně vyplněného slova (celé slovo) a předčasné „Dál“ (po písmenkách) vyžadují potvrzení dialogem „Opravdu pokračovat?“. Úplně vyplněné slovo se potvrdí bez dialogu.
- Dlouhá slova a fráze se zatím předpokládají bez speciálního omezení.

### Výsledky testu

Po skončení testu se zobrazí:

- počet správných a špatných odpovědí,
- celkové procento úspěšnosti,
- seznam chybně zodpovězených slovíček,
- dobu trvání testu,
- seznam všech slovíček seřazený podle úspěšnosti od nejnižší po nejvyšší. Úspěšnost slovíčka se počítá přes všechny průchody: při hodnocení celého slova (a v režimu 1) jako počet dobrých a špatných odpovědí za průchody, při hodnocení po písmenkách jako poměr dobrá / (dobrá + špatná) písmena. Při shodě se řadí podle abecedy.

Historie testů se neukládá (viz TODO).

## Vzhled a ovládání

- Uživatelské rozhraní je pouze česky. Lokalizace a tmavý režim zatím nejsou požadovány.
- Aplikace je pro děti: příjemné barvy a motivační prvky, ale bez složitého vzhledu, který by zhoršoval použitelnost.
- Zvuky: krátký zvuk při správné odpovědi, jiný při chybě a krátká fanfára při dokončení testu. Zvukové soubory budou z volně použitelných zdrojů bez licenčních omezení. Vibrace ne. Vypínání zvuků v aplikaci není potřeba (uživatel ztlumí zařízení).
- Podporují se telefony i tablety, na výšku i na šířku.
- Samostatná obrazovka „Nastavení“ obsahuje volbu velikosti tlačítek a písma: „Normální / Velké“. Platí pro celou aplikaci a pamatuje se. Režim pro dyslektiky se neřeší.

## Architektura

- Android aplikace, minimální verze Android 8 (API 26). Ověřeno: vše výše popsané lze v API 26 realizovat, vyšší verze není potřeba.
- Technologie: Kotlin + Jetpack Compose.
- Aplikace funguje offline a bez účtů. Pro budoucí stahování slovíček se počítá s připojením k internetu (viz TODO).
- Data (nastavení a podobně) se ukládají lokálně v zařízení. Zálohy ani přenos dat mezi zařízeními se zatím nepředpokládají. Protože se zatím neukládá historie, stačí jednoduché úložiště nastavení.
- Distribuce: zatím APK, později Google Play.
- Automatické testy: ověřují se základní věci, především logika testů (průchody, ukončení, hodnocení, výpočet úspěšnosti).

## TODO (budoucí rozšíření, zatím neřešit)

- Stahování sad z internetu a určení jazyka sady (import přes QR kód už je).
- Obrázky u slovíček.
- Přehrání cizího slovíčka po ohodnocení pomocí TTS, pokud je TTS pro daný jazyk k dispozici.
- Opakování obtížných slovíček (spaced repetition).
- Historie testů a graf pokroku.
- Přejmenování sad.
- Publikace na Google Play.
- Lokalizace aplikace a tmavý režim.
