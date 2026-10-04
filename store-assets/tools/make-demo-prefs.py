#!/usr/bin/env python3
"""Write the DataStore preferences file the store screenshots start from.

    python3 make-demo-prefs.py <out> [--fr] [--dark] [--color NAME] [--now MILLIS]

The file is `dice_settings.preferences_pb`, the one `Context.diceDataStore` reads. It is a
`PreferenceMap` protobuf, encoded here by hand so the tool needs nothing beyond Python.
It sets the language and theme explicitly (so the device's own never leaks into a shot),
turns sound and shake off, defines two custom dice and writes a short roll history.
"""
import argparse, time


def varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def field(number, wire, payload):
    tag = varint((number << 3) | wire)
    if wire == 0:
        return tag + varint(payload)
    return tag + varint(len(payload)) + payload


def value(v):
    # Value: boolean = 1, integer = 3, string = 5
    if isinstance(v, bool):
        return field(1, 0, int(v))
    if isinstance(v, int):
        return field(3, 0, v)
    return field(5, 2, v.encode())


def preference_map(prefs):
    out = b""
    for k, v in prefs.items():
        entry = field(1, 2, k.encode()) + field(2, 2, value(v))
        out += field(1, 2, entry)
    return out


def history(now):
    minute = 60_000
    records = [
        (now - 2 * minute, "20:17*1"),
        (now - 9 * minute, "6:6*2,4*1,2*1|7:5*1"),
        (now - 26 * minute, "8:8*1,3*1|12:11*1"),
        (now - 3 * 60 * minute, "4:4*2,1*1|20:20*1"),
    ]
    return "\n".join(f"{t};{g}" for t, g in records)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("out")
    p.add_argument("--fr", action="store_true")
    p.add_argument("--dark", action="store_true")
    p.add_argument("--color", default="Amethyst")
    p.add_argument("--now", type=int, default=int(time.time() * 1000))
    a = p.parse_args()
    prefs = {
        "app_language_tag": "fr" if a.fr else "en",
        "app_theme": "Dark" if a.dark else "Light",
        "selected_color": a.color,
        "sound_enabled": False,
        "shake_to_roll_enabled": False,
        "custom_dice": "7,100",
        "roll_history": history(a.now),
    }
    with open(a.out, "wb") as f:
        f.write(preference_map(prefs))


main()
