#!/usr/bin/env python3
"""Parse RESOURCE.008/.017 as a fixed 76-byte player grid and validate."""
import struct, sys
from pathlib import Path
from collections import Counter

EXT = Path(__file__).resolve().parent.parent / "_extracted"
REC = 76


def parse(fname):
    data = (EXT / fname).read_bytes()
    assert len(data) % REC == 0, f"{fname} not a multiple of {REC}: {len(data)}"
    n = len(data) // REC
    recs = []
    bad = 0
    for k in range(n):
        b = data[k * REC:(k + 1) * REC]
        nl = b[0]
        name = b[1:1 + nl].decode("latin1", "replace") if 1 <= nl <= 15 else ""
        natlen = struct.unpack(">I", b[0x10:0x14])[0]
        nat = b[0x14:0x17].decode("latin1", "replace")
        pos = chr(b[0x17]) if b[0x17] else "?"
        ok = (natlen == 3 and pos in "KDMF" and 1 <= nl <= 15
              and all(32 <= c < 127 for c in b[1:1 + nl]))
        if not ok:
            bad += 1
        recs.append((k, name, nat, pos, ok, b))
    return recs, n, bad


def main(fname):
    recs, n, bad = parse(fname)
    print(f"{fname}: {n} records, {bad} fail validation")
    print("positions:", Counter(r[3] for r in recs).most_common())
    print("nats:", Counter(r[2] for r in recs).most_common(15))
    print("\nfirst 25 names:")
    for r in recs[:25]:
        flag = "" if r[4] else "  <<BAD>>"
        print(f"  {r[0]:4} {r[1]:18} {r[2]} {r[3]}{flag}")
    # show any bad records' raw start
    bads = [r for r in recs if not r[4]]
    if bads:
        print(f"\nfirst bad records ({len(bads)}):")
        for r in bads[:8]:
            print(f"  idx {r[0]} namelen={r[5][0]} raw={r[5][:24].hex()}")
    return recs


if __name__ == "__main__":
    for f in (sys.argv[1:] or ["RESOURCE.008", "RESOURCE.017"]):
        main(f)
        print("=" * 72)
