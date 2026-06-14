#!/usr/bin/env python3
"""Probe RESOURCE.008 / .017 player record structure by anchoring on the
nationality signature (4-byte BE length + uppercase code + position char) and
measuring strides between consecutive records. No semantics guessed yet."""
import struct, sys, re
from pathlib import Path

EXT = Path(__file__).resolve().parent.parent / "_extracted"
POS = set(b"KDMF")


def find_anchors(data):
    """Return list of (nat_int_off, code, pos_off, pos_char)."""
    out = []
    i = 0
    n = len(data)
    while i < n - 9:
        ln = struct.unpack(">I", data[i:i + 4])[0]
        if ln == 3:
            code = data[i + 4:i + 7]
            pos = data[i + 7:i + 8]
            if all(65 <= b <= 90 for b in code) and pos and pos[0] in POS:
                out.append((i, code.decode(), i + 7, chr(pos[0])))
                i += 8
                continue
        i += 1
    return out


def main(fname):
    data = (EXT / fname).read_bytes()
    anchors = find_anchors(data)
    print(f"{fname}: {len(data)} bytes, {len(anchors)} anchors")
    # stride between nat_int offsets
    offs = [a[0] for a in anchors]
    strides = [b - a for a, b in zip(offs, offs[1:])]
    from collections import Counter
    print("stride histogram (record size incl. name field):", Counter(strides).most_common(12))
    # For the first records, show the name that precedes each anchor.
    print("\nfirst 12 records (name reconstructed from <= anchor):")
    prev_end = 0
    for k, (ni, code, po, pc) in enumerate(anchors[:12]):
        # name field sits between previous record end and this nat_int.
        # name = pascal string somewhere in [prev_end, ni). Find last len-byte
        # whose run of printable chars ends just before padding before ni.
        seg = data[prev_end:ni]
        # heuristic: name len byte is seg[0]
        nl = seg[0] if seg else 0
        name = seg[1:1 + nl].decode("latin1", "replace") if 1 <= nl <= 20 else "?"
        namefield = ni - prev_end
        print(f"  [{k}] off={prev_end:#06x} namelen={nl:2} field={namefield:2} "
              f"nat={code} pos={pc}  name={name!r}")
        prev_end = po + 1  # next record starts after position byte + stats; updated below
    # better: recompute record starts assuming record = [namefield][nat7][pos1][stats]
    print("\nstride detail (nat_int deltas):", strides[:15])
    # nationalities used
    from collections import Counter as C
    print("\nnationalities:", C(a[1] for a in anchors).most_common())
    print("positions:", C(a[3] for a in anchors).most_common())
    return anchors, data


if __name__ == "__main__":
    for f in (sys.argv[1:] or ["RESOURCE.008", "RESOURCE.017"]):
        main(f)
        print("=" * 70)
