#!/usr/bin/env python3
"""Draw the Play listing's high-res icon and feature graphic from the launcher vectors.

    python3 make-listing-graphics.py

Writes ic_launcher_play_store.png (512x512) and feature-graphic.png (1024x500) next to
this file. Both are read straight from res/drawable/ic_launcher_{background,foreground}.xml,
translated to SVG and rendered by rsvg-convert, so they cannot drift from the app's icon.
The feature graphic sets the app name in Cinzel, the display face the app uses for Roll.
Needs rsvg-convert (librsvg2-bin) and Pillow.
"""
import os, subprocess, tempfile, xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")
A = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"
INK = (0x3A, 0x28, 0x16)


def color(argb):
    argb = argb.lstrip("#")
    if len(argb) == 8:
        return f"#{argb[2:]}", int(argb[:2], 16) / 255
    return f"#{argb}", 1.0


def path_svg(p, defs):
    attrs = {"d": p.get(A + "pathData")}
    fill = p.get(A + "fillColor")
    grad = p.find(f"{AAPT}attr/gradient")
    if grad is not None:
        gid = f"g{len(defs)}"
        g = lambda k: grad.get(A + k)
        s, so = color(g("startColor")); e, eo = color(g("endColor"))
        defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{g("startX")}" '
                    f'y1="{g("startY")}" x2="{g("endX")}" y2="{g("endY")}">'
                    f'<stop offset="0" stop-color="{s}" stop-opacity="{so}"/>'
                    f'<stop offset="1" stop-color="{e}" stop-opacity="{eo}"/></linearGradient>')
        attrs["fill"] = f"url(#{gid})"
    elif fill:
        c, o = color(fill)
        attrs["fill"], attrs["fill-opacity"] = c, o * float(p.get(A + "fillAlpha", 1))
    else:
        attrs["fill"] = "none"
    if p.get(A + "strokeColor"):
        c, o = color(p.get(A + "strokeColor"))
        attrs.update({"stroke": c, "stroke-opacity": o, "stroke-width": p.get(A + "strokeWidth", "1"),
                      "stroke-linejoin": p.get(A + "strokeLineJoin", "miter"),
                      "stroke-linecap": p.get(A + "strokeLineCap", "butt")})
    return "<path " + " ".join(f'{k}="{v}"' for k, v in attrs.items()) + "/>"


def node_svg(n, defs):
    out = []
    for c in n:
        if c.tag == "path":
            out.append(path_svg(c, defs))
        elif c.tag == "group":
            px, py = float(c.get(A + "pivotX", 0)), float(c.get(A + "pivotY", 0))
            sx, sy = float(c.get(A + "scaleX", 1)), float(c.get(A + "scaleY", 1))
            t = f"translate({px},{py}) scale({sx},{sy}) translate({-px},{-py})"
            out.append(f'<g transform="{t}">{node_svg(c, defs)}</g>')
    return "".join(out)


def layer(name, defs):
    return node_svg(ET.parse(os.path.join(RES, "drawable", f"{name}.xml")).getroot(), defs)


def render(svg, size, out):
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as f:
        f.write(svg)
    subprocess.run(["rsvg-convert", "-w", str(size[0]), "-h", str(size[1]), f.name, "-o", out], check=True)
    os.unlink(f.name)


def svg(view, body, defs):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view}">'
            f'<defs>{"".join(defs)}</defs>{body}</svg>')


def main():
    defs = []
    bg, fg = layer("ic_launcher_background", defs), layer("ic_launcher_foreground", defs)

    # The 108 canvas rendered 1:1 with the background full-bleed; Play applies its own mask.
    render(svg("0 0 108 108", bg + fg, defs), (512, 512), os.path.join(HERE, "ic_launcher_play_store.png"))

    # Feature graphic: the background wash stretched to 1024x500, the die on the left third.
    out = os.path.join(HERE, "feature-graphic.png")
    view_h = 108 * 500 / 1024
    wash = bg.replace("M0,0h108v108h-108z", f"M0,0h108v{view_h}h-108z")
    defs_fg = [d.replace('y2="108"', f'y2="{view_h}"') for d in defs]
    die = f'<g transform="translate(-12,{(view_h - 108 * 0.62) / 2}) scale(0.62)">{fg}</g>'
    render(svg(f"0 0 108 {view_h}", wash + die, defs_fg), (1024, 500), out)

    img = Image.open(out).convert("RGB")
    d = ImageDraw.Draw(img)
    fonts = os.path.join(RES, "font")
    title = ImageFont.truetype(os.path.join(fonts, "cinzel_bold.ttf"), 92)
    d.text((400, 250), "Dice Roller", font=title, fill=INK, anchor="lm")
    img.save(out)


main()
