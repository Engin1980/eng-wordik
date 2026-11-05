"""Generuje zvuky aplikace (syntetické tóny, bez licenčních omezení) do app/src/main/res/raw.

Spuštění z kořene repozitáře:  python tools/generate_sounds.py
Používá jen standardní knihovnu Pythonu.
"""
import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
OUT_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"

# Frekvence not (Hz)
C3, D3, G3 = 130.81, 146.83, 196.00
C5, E5, G5, A5, C6 = 523.25, 659.25, 783.99, 880.00, 1046.50


def tone(freq, seconds, harmonics=((1, 1.0), (2, 0.25)), attack=0.006, decay=5.0):
    """Jeden tón: součet harmonických s rychlým náběhem a exponenciálním doznívání."""
    n = int(SAMPLE_RATE * seconds)
    out = []
    for i in range(n):
        t = i / SAMPLE_RATE
        env = min(1.0, t / attack) * math.exp(-decay * t)
        # krátké zeslabení na konci, aby nepraskalo
        env *= min(1.0, (seconds - t) / 0.01)
        value = sum(a * math.sin(2 * math.pi * freq * h * t) for h, a in harmonics)
        out.append(env * value)
    return out


def mix(*tracks):
    length = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(length)]


def concat(*tracks):
    result = []
    for t in tracks:
        result.extend(t)
    return result


def silence(seconds):
    return [0.0] * int(SAMPLE_RATE * seconds)


def write_wav(name, samples, peak=0.8):
    top = max(abs(s) for s in samples) or 1.0
    scaled = [int(max(-1.0, min(1.0, s / top * peak)) * 32767) for s in samples]
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / name
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(b"".join(struct.pack("<h", s) for s in scaled))
    print(f"{path.name}: {len(samples) / SAMPLE_RATE:.2f} s")


# Správně: dva stoupající veselé tóny.
correct = concat(tone(E5, 0.12, decay=6), tone(A5, 0.22, decay=7))

# Chyba: nižší klesající dvojtón, jemnější (kvinta v harmonických), aby dítě neděsil.
wrong = concat(
    tone(G3, 0.14, harmonics=((1, 1.0), (3, 0.3)), decay=4),
    tone(D3, 0.26, harmonics=((1, 1.0), (3, 0.3)), decay=5),
)

# Dokončení testu: rozložený akord C dur nahoru a závěrečný celý akord.
arpeggio = concat(
    tone(C5, 0.14, decay=3),
    tone(E5, 0.14, decay=3),
    tone(G5, 0.14, decay=3),
    tone(C6, 0.14, decay=3),
    silence(0.02),
)
chord = mix(
    tone(C5, 0.7, decay=3.5),
    tone(E5, 0.7, decay=3.5),
    tone(G5, 0.7, decay=3.5),
    tone(C6, 0.7, decay=3.5),
)
finish = concat(arpeggio, chord)

if __name__ == "__main__":
    write_wav("sound_correct.wav", correct)
    write_wav("sound_wrong.wav", wrong)
    write_wav("sound_finish.wav", finish)
