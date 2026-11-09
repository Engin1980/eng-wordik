Tento soubor obsahuje nalezené chyby uživatelem při reálném testování.

## Při spuštění více sad, které mají stejné slovo

Pokud má při spuštěném testu více sad společné slovo (ať už české nebo anglické), tak uživatel nebude vědět, které má doplnit.

Řešení: zatím zahlásit chybu při spuštění testu, test po potvrzení chyby spustit, ale taková slova vyhodit.

## Ujasnit, že při editaci/importu slov se ořezávají mezery na krajích

Ověřit, že při editaci slov, stejně jako při importu slov, se ořezávají mezery kolem slov, tj: 
```
 strom ; tree 
```
je totéž jako
```
strom;tree
```