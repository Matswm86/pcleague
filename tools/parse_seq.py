#!/usr/bin/env python3
"""Sequential, self-correcting parser for PcLeague player records.

Model (validated empirically below):
  record = name_field + nat(8) + stats(S)
    name_field = [1 byte len][name bytes], total padded UP to a word boundary
    nat(8)     = [4 byte BE len == 3][3 byte code][1 byte position char]
    stats(S)   = fixed-size block (to be determined by stride)

The parser does NOT assume the padding rule: it reads name_len, then scans a
small window for the nat signature, then advances by a fixed stat size that we
solve for by requiring the NEXT record's name to validate too.
"""
import struct, sys
from pathlib import Path
from collections import Counter

EXT = Path(__file__).resolve().parent.parent / "_extracted"
POS = "KDMF"


def nat_at(data, p):
    """If a nat signature starts at p, return (code, poschar) else None."""
    if p + 8 > len(data):
        return None
    if struct.unpack(">I", data[p:p + 4])[0] != 3:
        return None
    code = data[p + 4:p + 7]
    pc = data[p + 7]
    if all(65 <= b <= 90 for b in code) and chr(pc) in POS:
        return code.decode(), chr(pc)
    return None


def find_nat_after_name(data, cur):
    nl = data[cur]
    if not (0 <= nl <= 18):
        return None
    name_end = cur + 1 + nl
    # name field padded to word boundary -> nat starts at name_end rounded up to even,
    # but be tolerant: scan a few offsets.
    for pad in range(0, 6):
        p = name_end + pad
        sig = nat_at(data, p)
        if sig:
            return nl, name_end, p, sig
    return None


def parse(fname, stat_size):
    data = (EXT / fname).read_bytes()
    recs = []
    cur = 0
    while cur < len(data) - 8:
        got = find_nat_after_name(data, cur)
        if not got:
            break
        nl, name_end, natp, (code, pc) = got
        name = data[cur + 1:cur + 1 + nl].decode("latin1", "replace")
        stats = data[natp + 8:natp + 8 + stat_size]
        recs.append({"off": cur, "name": name, "nat": code, "pos": pc,
                     "natp": natp, "stats": stats})
        cur = natp + 8 + stat_size
    return data, recs


def main(fname):
    data = None
    # find stat_size that consumes the whole file with valid records
    best = None
    for S in range(40, 70):
        d, recs = parse(fname, S)
        consumed = recs[-1]["off"] + 0 if recs else 0
        end = (recs[-1]["natp"] + 8 + S) if recs else 0
        leftover = len(d) - end
        if recs and 0 <= leftover < 8 and all(r["pos"] in POS for r in recs):
            best = (S, recs, leftover)
            data = d
            break
    if not best:
        # fall back: just use the modal stride
        d, recs = parse(fname, 54)
        print(f"{fname}: no clean stat_size; parsed {len(recs)} with S=54")
        for r in recs[:20]:
            print(f"  {r['off']:#07x} {r['name']:18} {r['nat']} {r['pos']}")
        return recs
    S, recs, leftover = best
    print(f"{fname}: stat_size={S}, {len(recs)} records, {leftover} trailing bytes")
    print("positions:", Counter(r["pos"] for r in recs).most_common())
    print("nats:", Counter(r["nat"] for r in recs).most_common(20))
    print("name length range:", min(len(r['name']) for r in recs), "-",
          max(len(r['name']) for r in recs))
    print("\nfirst 22 (Arsenal squad):")
    for r in recs[:22]:
        print(f"  {r['off']:#07x} {r['name']:18} {r['nat']} {r['pos']}  "
              f"stats={r['stats'][:24].hex()}")
    return recs


if __name__ == "__main__":
    for f in (sys.argv[1:] or ["RESOURCE.008", "RESOURCE.017"]):
        main(f)
        print("=" * 78)
