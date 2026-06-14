#!/usr/bin/env python3
"""Decode the 52-byte player stat block. Hypothesis: Turbo Pascal 6-byte Reals
plus integer fields. Validate against known anchors (age, position role)."""
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from pcl import parse_players, POS_MAP


def tp_real(b):
    """Borland Turbo Pascal 48-bit Real -> float. b is 6 bytes."""
    if b[0] == 0:
        return 0.0
    exp = b[0] - 129
    # mantissa: bytes 1..5, byte5 high bit = sign
    sign = -1.0 if (b[5] & 0x80) else 1.0
    mant = ((b[5] & 0x7f) << 32) | (b[4] << 24) | (b[3] << 16) | (b[2] << 8) | b[1]
    frac = 1.0 + mant / (1 << 40)
    return sign * frac * (2.0 ** exp)


def show_squad(players, label):
    print(f"\n===== {label} =====")
    for p in players:
        b = bytes.fromhex(p["stats"])
        # try reals at every offset where exp byte looks plausible (0x70..0x90)
        reals = []
        for o in range(0, len(b) - 5):
            if 0x70 <= b[o] <= 0x90:
                v = tp_real(b[o:o + 6])
                if 0 < abs(v) < 1000:
                    reals.append((o, round(v, 3)))
        age = b[2]
        print(f"{p['name']:18} {POS_MAP.get(p['pos'],'?'):3} age={age:2} "
              f"ints[0,1,6,9]={b[0]},{b[1]},{b[6]},{b[9]}")
        print(f"   reals: " + " ".join(f"@{o}={v}" for o, v in reals[:10]))


if __name__ == "__main__":
    p8 = parse_players("RESOURCE.008")
    show_squad(p8[:20], "ARSENAL")
    # raw grid for first 3 players
    print("\n=== raw 52-byte stat grid (first 6 players) ===")
    print("     " + " ".join(f"{i:2}" for i in range(52)))
    for p in p8[:6]:
        b = bytes.fromhex(p["stats"])
        print(f"{p['name'][:10]:10} " + " ".join(f"{x:02x}" for x in b))
