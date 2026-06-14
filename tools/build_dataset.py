#!/usr/bin/env python3
"""Build canonical PcLeague dataset from the original RESOURCE.* files.

Outputs data/pcleague.json with:
  divisions[]  -> {tier, name, teams[]}
  teams[]      -> {id, name, division, divPosition, players[]}
  player       -> name, nat, role, age, skill[4], skillSum, value, fitness,
                  flag, rawStats(hex)   (rawStats preserves the original bytes)
  uiStrings    -> {key: {en, no}}  (best-effort from RESOURCE.006)
  namePools    -> {first[], last[]} youth-generator pools (RESOURCE.011/.012)
"""
import struct, json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXT = ROOT / "_extracted"
OUT = ROOT / "data"
OUT.mkdir(exist_ok=True)
REC = 76
ROLE = {"K": "GK", "F": "DEF", "M": "MID", "A": "ATT"}
DIV_NAMES = ["Premier League", "Division 1", "Division 2", "Division 3"]


def read_name(b):
    n = b[0]
    if n == 0 or n > 19:
        return ""
    return b[1:1 + n].decode("cp865", "replace").rstrip("\x00 ").strip()


def parse_players(fname):
    data = (EXT / fname).read_bytes()
    out = []
    for k in range(len(data) // REC):
        b = data[k * REC:(k + 1) * REC]
        st = b[0x18:0x4c]                       # 52-byte stat block
        role = chr(b[0x17]) if chr(b[0x17]) in ROLE else "?"
        skill = [st[11], st[12], st[13], st[14]]
        out.append({
            "name": read_name(b),
            "nat": b[0x14:0x17].decode("latin1", "replace"),
            "role": ROLE.get(role, "ATT"),
            "age": st[2],
            "skill": skill,
            "skillSum": sum(skill),
            "value": st[6] | (st[7] << 8),     # transfer value (low/high byte)
            "fitness": st[0],
            "flag": st[9],
            "rawStats": st.hex(),
        })
    return out


def parse_teams():
    data = (EXT / "RESOURCE.001").read_bytes()
    recs, i = [], 0
    while i < len(data) - 4:
        ln = struct.unpack(">I", data[i:i + 4])[0]
        if 3 <= ln <= 22 and i + 4 + ln <= len(data):
            s = data[i + 4:i + 4 + ln]
            if all(32 <= c < 127 for c in s) and s[:1].isalpha():
                recs.append([i, s.decode("latin1"), data])
                i += 4 + ln
                continue
        i += 1
    teams = []
    for k, (off, name, _) in enumerate(recs):
        start = off + 4 + len(name)
        end = recs[k + 1][0] if k + 1 < len(recs) else len(data)
        blk = data[start:end]
        gnum = divpos = None
        j = blk.find(0x3a)
        if j != -1 and j + 2 < len(blk):
            gnum, divpos = blk[j + 1], blk[j + 2]
        teams.append({"name": name, "global": gnum or (k + 1), "divPos": divpos or 0})
    return teams


def assign_divisions(teams):
    """Division resets when divPos drops back to 1 (after team 0)."""
    tier = 0
    for idx, t in enumerate(teams):
        if idx > 0 and t["divPos"] == 1:
            tier += 1
        t["division"] = min(tier, 3)
    return teams


def parse_ui():
    data = (EXT / "RESOURCE.006").read_bytes()
    SLOT = 71
    n = len(data) // SLOT
    # filler tail to strip (stale buffer content)
    fillers = ["DE KORT", "RINGENE", ", Asle Rokstad", "SoftWare", "*e,",
               "SCORERLISTE", "MOLDE", "  ***", "TIDLIGERE", " UE "]

    def clean(slot):
        s = slot.decode("cp865", "replace")
        cut = len(s)
        for f in fillers:
            p = s.find(f)
            if p != -1:
                cut = min(cut, p)
        return s[:cut].rstrip("\x00 ").strip()
    slots = [clean(data[i * SLOT:(i + 1) * SLOT]) for i in range(n)]
    # NO at even index, EN at odd (greeting/version pattern observed)
    pairs = []
    for i in range(0, n - 1, 2):
        pairs.append({"no": slots[i], "en": slots[i + 1]})
    return pairs


def parse_name_pool(fname):
    data = (EXT / fname).read_bytes()
    return [m.group().decode("cp865", "replace").strip()
            for m in re.finditer(rb"[A-Za-z][\x20-\x7e]{1,9}", data)
            if 2 <= len(m.group()) <= 10][:200]


def main():
    p8 = parse_players("RESOURCE.008")
    p17 = parse_players("RESOURCE.017")
    teams = assign_divisions(parse_teams())
    # attach 20 players per team (R008 order == team order)
    for ti, t in enumerate(teams):
        squad = p8[ti * 20:ti * 20 + 20]
        t["players"] = [pl for pl in squad if pl["name"]]
        t["id"] = ti
    divisions = []
    for tier in range(4):
        dteams = [t for t in teams if t["division"] == tier]
        divisions.append({"tier": tier, "name": DIV_NAMES[tier],
                          "teamIds": [t["id"] for t in dteams]})
    dataset = {
        "meta": {"title": "PcLeague 2.1", "author": "Asle Rokstad",
                 "year": 1994, "source": "PcLeague.zip RESOURCE.* originals"},
        "divisions": divisions,
        "teams": teams,
        "reservePool": p17,
        "uiStrings": parse_ui(),
        "namePools": {"first": parse_name_pool("RESOURCE.011"),
                      "last": parse_name_pool("RESOURCE.012")},
    }
    (OUT / "pcleague.json").write_text(json.dumps(dataset, ensure_ascii=False))
    # report
    print("teams:", len(teams), "players:", sum(len(t['players']) for t in teams))
    for d in divisions:
        print(f"  tier {d['tier']} {d['name']:15} {len(d['teamIds'])} teams")
    print("reservePool:", len(p17), "uiStrings:", len(dataset['uiStrings']))
    print("namePools first/last:", len(dataset['namePools']['first']),
          len(dataset['namePools']['last']))
    print("out:", (OUT / 'pcleague.json').stat().st_size, "bytes")


if __name__ == "__main__":
    main()
