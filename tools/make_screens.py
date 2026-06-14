#!/usr/bin/env python3
"""Render UI preview images matching the app's actual Compose screens
(same DOS palette, same layout, real extracted data). Output -> screenshots/."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SH = ROOT / "screenshots"; SH.mkdir(exist_ok=True)
DS = json.load(open(ROOT / "data" / "pcleague.json"))

# DOS palette (matches ui/theme/Theme.kt)
BG = (0, 0, 0x80); PANEL = (0, 0, 0xAA); FRAME = (0x55, 0xFF, 0xFF)
STATUS = (0, 0xAA, 0xAA); BLACK = (0, 0, 0); YELLOW = (0xFF, 0xFF, 0x55)
TEXT = (0xAA, 0xAA, 0xAA); WHITE = (0xFF, 0xFF, 0xFF); BGREEN = (0x55, 0xFF, 0x55)
BCYAN = (0x55, 0xFF, 0xFF); BMAG = (0xFF, 0x55, 0xFF); GREEN = (0, 0xAA, 0)
BRED = (0xFF, 0x55, 0x55)
ROLECOL = {"GK": YELLOW, "DEF": BCYAN, "MID": BGREEN, "ATT": BMAG}
ROLELBL = {"GK": "Kpr", "DEF": "Def", "MID": "Mdf", "ATT": "Frw"}

W, H = 720, 1480
FD = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
FB = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"


def f(sz, bold=False):
    return ImageFont.truetype(FB if bold else FD, sz)


def canvas():
    img = Image.new("RGB", (W, H), BG)
    return img, ImageDraw.Draw(img)


def frame(d, x, y, w, h, title, right=None):
    bar = 56
    d.rectangle([x, y, x + w, y + h], fill=PANEL, outline=FRAME, width=4)
    d.rectangle([x, y, x + w, y + bar], fill=FRAME)
    d.text((x + 16, y + 14), title.upper(), font=f(28, True), fill=BLACK)
    if right:
        rb = d.textlength(right, font=f(24, True))
        d.text((x + w - rb - 16, y + 16), right, font=f(24, True), fill=BLACK)
    return y + bar + 12


def status(d, text):
    d.rectangle([0, H - 56, W, H], fill=STATUS)
    d.text((20, H - 44), text, font=f(22, True), fill=BLACK)


def squad_screen():
    img, d = canvas()
    me = DS["teams"][0]  # Arsenal
    y = frame(d, 16, 16, W - 32, H - 96, "Squad", right=str(len(me["players"])))
    cols = [(40, "POS", YELLOW), (90, "NAME", YELLOW)]
    d.text((40, y), "POS", font=f(22, True), fill=YELLOW)
    d.text((96, y), "NAME", font=f(22, True), fill=YELLOW)
    d.text((430, y), "AGE", font=f(22, True), fill=YELLOW)
    d.text((520, y), "SH", font=f(22, True), fill=YELLOW)
    d.text((600, y), "GLS", font=f(22, True), fill=YELLOW)
    y += 40
    order = {"GK": 0, "DEF": 1, "MID": 2, "ATT": 3}
    roster = sorted(me["players"], key=lambda p: (order[p["role"]], -p["skillSum"]))
    for i, p in enumerate(roster):
        if y > H - 130: break
        d.text((40, y), ROLELBL[p["role"]], font=f(22, True), fill=ROLECOL[p["role"]])
        d.text((96, y), p["name"][:18], font=f(22), fill=TEXT)
        d.text((440, y), str(p["age"]), font=f(22), fill=TEXT)
        d.text((520, y), str(p["skillSum"]), font=f(22, True), fill=BGREEN)
        d.text((615, y), "0", font=f(22), fill=TEXT)
        y += 42
    status(d, "ARSENAL  ·  Premier League  ·  1=Lineup 2=Trans")
    img.save(SH / "squad.png")


def table_screen():
    img, d = canvas()
    teams = [DS["teams"][i] for i in DS["divisions"][0]["teamIds"]]
    # fabricate a plausible opening-day-ish ordering by skill for the preview
    teams = sorted(teams, key=lambda t: -sum(p["skillSum"] for p in t["players"]))
    y = frame(d, 16, 16, W - 32, H - 96, "Premier League")
    for lbl, x in [("NAME", 96), ("P", 430), ("W", 480), ("D", 530), ("GD", 580), ("PTS", 650)]:
        d.text((x, y), lbl, font=f(22, True), fill=YELLOW)
    y += 40
    pts = [42, 39, 37, 34, 33, 30, 28, 27, 25, 24, 23, 21, 20, 18, 17, 16, 15, 14, 12, 10, 8, 6]
    for i, t in enumerate(teams):
        if y > H - 130: break
        mine = i == 3
        if i < 3: d.rectangle([20, y - 4, W - 36, y + 34], fill=(6, 66, 15))
        if i >= len(teams) - 3: d.rectangle([20, y - 4, W - 36, y + 34], fill=(74, 6, 6))
        if mine: d.rectangle([20, y - 4, W - 36, y + 34], fill=GREEN)
        rc = BGREEN if i < 3 else (BRED if i >= len(teams) - 3 else TEXT)
        d.text((36, y), str(i + 1), font=f(22, True), fill=rc)
        d.text((96, y), t["name"][:18], font=f(22, mine), fill=WHITE if mine else TEXT)
        pp = pts[i] if i < len(pts) else 5
        d.text((430, y), "21", font=f(22), fill=TEXT)
        d.text((480, y), str(pp // 3), font=f(22), fill=TEXT)
        d.text((530, y), "3", font=f(22), fill=TEXT)
        d.text((575, y), f"+{22 - i}", font=f(22), fill=TEXT)
        d.text((655, y), str(pp), font=f(22, True), fill=BGREEN)
        y += 38
    status(d, "Premier League")
    img.save(SH / "table.png")


def office_screen():
    img, d = canvas()
    y = frame(d, 16, 16, W - 32, 150, "Arsenal", right="Season 1")
    d.text((32, y), "Premier League", font=f(24), fill=BCYAN)
    d.text((W - 200, y), "POS 4/22", font=f(24, True), fill=YELLOW)
    y += 40
    d.text((32, y), "Round 8/42", font=f(22), fill=TEXT)
    d.text((W - 280, y), "16 PTS (5-1-2)", font=f(22), fill=TEXT)
    y = frame(d, 16, 180, W - 32, H - 320, "Manager's Office")
    items = [("1", "Squad"), ("2", "Line-up"), ("3", "Transfers"), ("4", "Youth Squad"),
             ("5", "League Tables"), ("6", "Fixtures"), ("7", "Top Scorers"),
             ("8", "Finances"), ("9", "FA Cup"), ("0", "News")]
    for k, lbl in items:
        d.text((40, y), k, font=f(30, True), fill=YELLOW)
        d.text((100, y), lbl, font=f(30), fill=WHITE)
        y += 54
    # play button bar
    by = H - 150
    d.rectangle([20, by, 250, by + 60], fill=FRAME)
    d.text((60, by + 16), "Save", font=f(26, True), fill=BLACK)
    d.rectangle([266, by, W - 20, by + 60], fill=BGREEN)
    d.text((300, by + 16), "▶ Play Next Match", font=f(26, True), fill=BLACK)
    status(d, "Arsenal  ·  Premier League  ·  Budget: £4.0M")
    img.save(SH / "office.png")


def title_screen():
    img, d = canvas()
    cx = W // 2
    d.text((cx - 230, 120), "┌──────────────────┐", font=f(28), fill=FRAME)
    t = "PcLeague"
    d.text((cx - d.textlength(t, font=f(78, True)) / 2, 160), t, font=f(78, True), fill=YELLOW)
    s = "Soccer Manager"
    d.text((cx - d.textlength(s, font=f(30)) / 2, 260), s, font=f(30), fill=BGREEN)
    d.text((cx - 230, 320), "└──────────────────┘", font=f(28), fill=FRAME)
    y = frame(d, 60, 420, W - 120, 360, "PcLeague")
    for k, lbl, col in [("1", "New Game", BGREEN), ("2", "Continue Saved Game", WHITE),
                        ("3", "Load Game", WHITE), ("4", "Settings", WHITE)]:
        d.text((40 + 60, y), k, font=f(34, True), fill=YELLOW)
        d.text((120 + 60, y), lbl, font=f(32), fill=col)
        y += 64
    # lang buttons
    by = 820
    d.rectangle([60, by, W // 2 - 10, by + 64], fill=YELLOW)
    d.text((140, by + 18), "English", font=f(28, True), fill=BLACK)
    d.rectangle([W // 2 + 10, by, W - 60, by + 64], fill=FRAME)
    d.text((W // 2 + 90, by + 18), "Norsk", font=f(28, True), fill=BLACK)
    status(d, "PcLeague 2.1  ·  (c) Asle Rokstad 1994  ·  Android recreation")
    img.save(SH / "title.png")


if __name__ == "__main__":
    squad_screen(); table_screen(); office_screen(); title_screen()
    print("wrote", *(p.name for p in sorted(SH.glob("*.png"))))
