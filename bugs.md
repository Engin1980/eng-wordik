Tento soubor obsahuje nalezené chyby uživatelem při reálném testování.

## B1- špatné shrnutí hodnocení

Uživatel při hodnocení po písmenkách udělá chybu. I když potom napíše písmenka správně a slovo tedy na konci je zobrazeno v pořádku, ve výsledku se napíše "Špatně. Správně se to píše xxx".

Očekávané chování: Nyní je to správně.

## B2 - Nemožnost doplnit slovíčko
Uživatel při vyplňování písmenek dostane slovíčko, které vůbec neví.

Aktuálně musí hádat písmenka po písmenku.

Správně chceme, aby mohl ihned stisknout tlačítko "Dál", slovo se započítá za tolik chyb, kolik je v něm písmen a zobrazí se samozřejmě správné řešení.

Zároveň u této varianty přidej tlačítko "Napověz", které doplní další písmenko a započítá jeden chybný pokus.

## B3 - při testu zobrazovat průběh

Uživatel chce při testu vidět, kolik slovíček z daného průchodu je hotovo.

## B4 - zabránit double-click na "potvrdit"

Když se kliká na potvrdit a není vybraná odpověď (nebo nejsou doplněna všechna písmenka), tak je třeba potvrzovací dialog,  zda se má opravud pokračovat. Jinak uživatel může 2x kliknout na "Dál" a vlastně tak klikne na "Dál" a podruhé na ihned následně zobrazené tlačítko "Potvrdit".