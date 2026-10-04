#!/usr/bin/env python3
"""Reshoot every Play Store screenshot, in every listing language, on one emulator.

    python3 reshoot-store.py [serial]

Writes store-assets/screenshots/<locale>/: five phone shots (1080x2160, the tallest
2:1 Play accepts, so a roll's total is on screen), and the same five at each
tablet size under tablet-7/ and tablet-10/. The debug build has to be installed first
(./gradlew installDebug). The emulator is resized for each form factor and put back as it
was at the end; the phone AVD is resized rather than booting tablet AVDs, as in Todolist.

Each session starts from a preferences file written by make-demo-prefs.py, so language,
theme, colour, custom dice and history are fixed. The roll itself is a real roll, so
look at every image before publishing.
"""
import os, re, shutil, subprocess, sys, tempfile, time

TOOLS = os.path.dirname(os.path.abspath(__file__))
SCREENSHOTS = os.path.join(os.path.dirname(TOOLS), "screenshots")
SDK = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/Android/Sdk")
ADB = f"{SDK}/platform-tools/adb"
PKG = "fr.mandarine.diceroller"
PREFS = "files/datastore/dice_settings.preferences_pb"

WORDS = {
    "en-US": {"flags": [], "inc": "Increase {} count", "roll": "Roll",
              "history": "Recent rolls", "settings": "Settings"},
    "fr-FR": {"flags": ["--fr"], "inc": "Augmenter le nombre de {}", "roll": "Lancer",
              "history": "Lancers récents", "settings": "Paramètres"},
}

FORM_FACTORS = [
    ("phone", "1080x2160", "420", ""),
    ("tablet7", "1200x1920", "320", "tablet-7"),
    ("tablet10", "1600x2560", "360", "tablet-10"),
]

SHOTS = ["01-pool", "02-roll", "03-history", "04-dark", "05-settings"]


def adb(dev, *args, out=None):
    return subprocess.run([ADB, "-s", dev, *args], check=True, text=out is None,
                          capture_output=out is None, stdout=out).stdout


def shell(dev, cmd):
    return adb(dev, "shell", cmd)


def tidy(dev, cmd):
    subprocess.run([ADB, "-s", dev, "shell", cmd], check=False)


def screencap(dev, path):
    with open(path, "wb") as f:
        adb(dev, "exec-out", "screencap", "-p", out=f)


def dump(dev):
    for _ in range(5):
        shell(dev, "uiautomator dump /sdcard/win.xml")
        xml = shell(dev, "cat /sdcard/win.xml")
        if "<hierarchy" in xml:
            return xml
        time.sleep(1)
    raise RuntimeError("no ui dump")


def find(xml, *, text=None, desc=None):
    for node in re.finditer(r"<node [^>]*>", xml):
        n = node.group(0)
        if text is not None and f'text="{text}"' not in n:
            continue
        if desc is not None and f'content-desc="{desc}' not in n:  # prefix match
            continue
        x1, y1, x2, y2 = map(int, re.search(
            r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups())
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def tap(dev, **kw):
    p = find(dump(dev), **kw)
    if p is None:
        raise RuntimeError(f"not found: {kw}")
    shell(dev, f"input tap {p[0]} {p[1]}")
    time.sleep(0.4)


def demo_mode(dev):
    shell(dev, "settings put global sysui_demo_allowed 1")
    b = lambda a: shell(dev, f"am broadcast -a com.android.systemui.demo {a}")
    b("-e command exit")
    b("-e command enter")
    b("-e command clock -e hhmm 0930")
    b("-e command battery -e level 100 -e plugged false -e powersave false")
    b("-e command network -e wifi hide -e mobile hide -e airplane hide")
    b("-e command network -e wifi show -e level 4 -e fully true")
    b("-e command notifications -e visible false")
    b("-e command status -e volume hide -e bluetooth hide -e location hide -e alarm hide"
      " -e sync hide -e mute hide -e zen hide -e vpn hide -e cast hide -e hotspot hide")
    for key in shell(dev, "cmd notification list").split():
        shell(dev, f"cmd notification snooze --for 86400000 '{key}'")


def session(dev, work, words, *extra):
    """Force-stops the app, gives it a fresh preferences file, and launches it."""
    prefs = os.path.join(work, "prefs.pb")
    subprocess.run(["python3", f"{TOOLS}/make-demo-prefs.py", prefs, *words["flags"], *extra],
                   check=True)
    shell(dev, f"am force-stop {PKG}")
    adb(dev, "push", prefs, "/data/local/tmp/dice_settings.preferences_pb")
    shell(dev, f"run-as {PKG} sh -c 'mkdir -p files/datastore; "
               f"cp /data/local/tmp/dice_settings.preferences_pb {PREFS}'")
    shell(dev, f"am start -n {PKG}/.MainActivity")
    for _ in range(30):
        if find(dump(dev), text=words["roll"]) is not None:
            break
        time.sleep(1)
    else:
        raise RuntimeError("the screen never drew")
    time.sleep(2)


def add(dev, words, die, n):
    p = find(dump(dev), desc=words["inc"].format(die))
    if p is None:
        raise RuntimeError(f"no stepper for {die}")
    for _ in range(n):
        shell(dev, f"input tap {p[0]} {p[1]}")
        time.sleep(0.3)


def roll(dev, words):
    tap(dev, text=words["roll"])
    time.sleep(3)  # the tumble, the bounce, and the history entry settling


def capture(dev, work, locale, out):
    words = WORDS[locale]
    # Light, amethyst: the pool, its roll, the log open, the settings sheet.
    session(dev, work, words)
    add(dev, words, "D6", 2)
    add(dev, words, "D7", 1)
    screencap(dev, f"{out}/01-pool.png")
    roll(dev, words)
    screencap(dev, f"{out}/02-roll.png")
    tap(dev, desc=words["history"])
    time.sleep(1)
    screencap(dev, f"{out}/03-history.png")
    tap(dev, desc=words["settings"])
    time.sleep(2)
    screencap(dev, f"{out}/05-settings.png")
    # Dark, another colour: a bigger roll.
    session(dev, work, words, "--dark", "--color", "Sapphire")
    add(dev, words, "D20", 2)
    add(dev, words, "D8", 2)
    roll(dev, words)
    screencap(dev, f"{out}/04-dark.png")


def main():
    dev = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
    work = tempfile.mkdtemp(prefix="reshoot-")
    try:
        for locale in WORDS:
            for prefix, size, density, subdir in FORM_FACTORS:
                shell(dev, f"wm size {size}")
                shell(dev, f"wm density {density}")
                time.sleep(2)
                demo_mode(dev)  # after the resize, which restarts parts of SystemUI
                out = os.path.join(work, locale, prefix)
                os.makedirs(out)
                capture(dev, work, locale, out)
                target = os.path.join(SCREENSHOTS, locale, subdir)
                os.makedirs(target, exist_ok=True)
                for shot in SHOTS:
                    name = f"phone-{shot}" if prefix == "phone" else shot
                    shutil.copyfile(f"{out}/{shot}.png", f"{target}/{name}.png")
                print("captured", locale, prefix)
    finally:
        tidy(dev, f"am force-stop {PKG}")
        tidy(dev, "wm size reset")
        tidy(dev, "wm density reset")
        tidy(dev, "am broadcast -a com.android.systemui.demo -e command exit")
        shutil.rmtree(work)
    print("screenshots written to", SCREENSHOTS)


main()
