#!/usr/bin/env python3
"""Definitive PcLeague 2.1 resource parser.

Player record = 76 bytes, fixed layout:
  [0x00]      name length byte
  [0x01..]    name chars (read `len` chars; buffer ~15 wide, long names spill)
  [0x14..16]  nationality (3-char code)
  [0x17]      position byte: K=keeper F=defence(forsvar) M=midfield A=attack(angrep)
  [0x18..4B]  52-byte stat block (decoded separately, see decode_stats)

Team record (RESOURCE.001 / .016) = [4-byte BE name length][name][team stat block],
whole record padded to 4-byte boundary.
"""
import struct, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXT = ROOT / "_extracted"
REC = 76
POS_MAP = {"K": "GK", "F": "DEF", "M": "MID", "A": "ATT"}  # Norwegian->role


# ---------------------------------------------------------------- players
def read_name(buf):
    n = buf[0]
    if n == 0 or n > 19:
        return ""
    raw = buf[1:1 + n]
    return raw.decode("cp865", "replace").rstrip("\x00 ")


def parse_players(fname):
    data = (EXT / fname).read_bytes()
    out = []
    for k in range(len(data) // REC):
        b = data[k * REC:(k + 1) * REC]
        out.append({
            "idx": k,
            "name": read_name(b),
            "nat": b[0x14:0x17].decode("latin1", "replace"),
            "pos": chr(b[0x17]) if chr(b[0x17]) in POS_MAP else "?",
            "stats": b[0x18:0x4c].hex(),
        })
    return out


# ---------------------------------------------------------------- teams
def parse_teams(fname):
    data = (EXT / fname).read_bytes()
    out = []
    i = 0
    while i < len(data) - 4:
        ln = struct.unpack(">I", data[i:i + 4])[0]
        if 3 <= ln <= 22 and i + 4 + ln <= len(data):
            s = data[i + 4:i + 4 + ln]
            if all(32 <= c < 127 for c in s) and s[:1].isalpha():
                out.append({"off": i, "name": s.decode("latin1")})
                # advance past name; team stat block length unknown yet -> resync
                i += 4 + ln
                continue
        i += 1
    return out


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "summary"
    if cmd == "summary":
        p8 = parse_players("RESOURCE.008")
        p17 = parse_players("RESOURCE.017")
        t1 = parse_teams("RESOURCE.001")
        t16 = parse_teams("RESOURCE.016")
        print(f"RESOURCE.008 players: {len(p8)}  (= {len(p8)/20:.2f} teams x20)")
        print(f"RESOURCE.017 players: {len(p17)} (= {len(p17)/20:.2f} teams x20)")
        print(f"RESOURCE.001 teams  : {len(t1)}")
        print(f"RESOURCE.016 teams  : {len(t16)}")
        from collections import Counter
        print("R008 positions:", Counter(x["pos"] for x in p8).most_common())
        print("R008 nats:", Counter(x["nat"] for x in p8).most_common(20))
        print("\nTeams (RESOURCE.001):")
        for j, t in enumerate(t1):
            print(f"  {j:2} {t['name']}")
        print("\nArsenal squad (players 0-19):")
        for x in p8[:20]:
            print(f"  {x['idx']:2} {x['name']:18} {x['nat']} {POS_MAP.get(x['pos'],'?')}")
        print("\nTeam 1 squad (players 20-39):")
        for x in p8[20:40]:
            print(f"  {x['idx']:2} {x['name']:18} {x['nat']} {POS_MAP.get(x['pos'],'?')}")
    elif cmd == "dump":
        json.dump({"r008": parse_players("RESOURCE.008"),
                   "r017": parse_players("RESOURCE.017"),
                   "teams1": parse_teams("RESOURCE.001"),
                   "teams16": parse_teams("RESOURCE.016")},
                  open(ROOT / "data" / "raw_parse.json", "w"), indent=1)
        print("wrote data/raw_parse.json")


if __name__ == "__main__":
    main()
