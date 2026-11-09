# ML Kit a CameraX přinášejí vlastní consumer pravidla; sem patří jen pravidla aplikace.
# Sady a konfigurace se čtou jako textové soubory (bez reflexe), další pravidla nejsou potřeba.

# ML Kit Barcode: komponenty se skládají přes ComponentRuntime (reflexe, tovární třídy);
# bez těchto pravidel R8 poškodí inicializaci a BarcodeScanning.getClient() spadne na NPE.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.firebase.components.** { *; }
