Tento soubor obsahuje nalezené chyby uživatelem při reálném testování.

## B0: Nový mód testování slovíček po písmenech.

**Opraveno** (unit testy, lint a build prošly; na zařízení zatím neověřeno): režim „Regresivní“ (`TestMode.REGRESSIVE`), pravidla jako skládání po písmenkách (včetně Napovědět/Přeskočit a písmen navíc), bez počtu průchodů a úspěšnosti.

Přidej nový mód testování slovíček po písmenech. Tentokrát se doplňují slovíčka po písmenech, hodnotí se postupně po jednotlivých znacích. Pokud je při vyplňování alespoň jedna chyba, slovíčko se uloží do dalšího běhu. Až se projdou všechna slovíčka, opakuje se kolo s neúspěšnými slovíčky z minulého běhu. Opět, pokud v nich je chyba, uloží se do dalšího běhu. Takto se opakuje dokolečka, dokud nezbylo žádné slovíčko (tj. každé slovíčko bylo nakonec napsáno napoprvé zcela špatně). Na konci se může stávat, že se pořád opakuje stejné slovíčko a to je v pořádku.

U této varianty se nespecifikuje počet běhů ani minimální úspěšnost. Vždy se běží celá sada, dokud to není celé dobře.

Tato varianta se bude nazývat jako "Regresivní".

## B1: V nastavení, kde se určuje velikost písmen, udělat škálu X-Y (dle vlastní volby)

**Opraveno** (neověřeno na zařízení): posuvník 80–200 % po 10 %, výchozí 100 %; násobí písmo, výšku tlačítek i klávesy písmen. Dřívější „Normální“ = 100 %, „Velké“ = 130 %.
Uživatel tak v nastavení může sám více progresivně nastavovat správnou  velikost. Hodnoty X a Y zatím zvol sám, případně po testování doporučím další úpravu.


## B10: Při spuštění více sad, které mají stejné slovo

Pokud má při spuštěném testu více sad společné slovo (ať už české nebo anglické), tak uživatel nebude vědět, které má doplnit.

Řešení: zatím zahlásit chybu při spuštění testu, test po potvrzení chyby spustit, ale taková slova vyhodit.

## B11: Ujasnit, že při editaci/importu slov se ořezávají mezery na krajích

Ověřit, že při editaci slov, stejně jako při importu slov, se ořezávají mezery kolem slov, tj: 
```
 strom ; tree 
```
je totéž jako
```
strom;tree
```