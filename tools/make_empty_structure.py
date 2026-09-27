"""Generate an empty structure template (.nbt) for GameTests.

Why this exists: the datapack route of StructureTemplateManager only reads
BINARY .nbt files (FileToIdConverter("structure", ".nbt")); .snbt files are
only honored by the IDE-only `gameteststructures/` filesystem path, which
drops the namespace. So GameTest templates shipped in the mod must be .nbt.

Usage:
    python3 tools/make_empty_structure.py <width> <height> <depth> <output>
    python3 tools/make_empty_structure.py smoke          # regenerates the defaults
"""

import gzip
import pathlib
import struct
import sys

DATA_VERSION = 3955  # MC 1.21.1

ROOT = pathlib.Path(__file__).resolve().parent.parent
STRUCTURES = ROOT / "src/main/resources/data/creativecontainer/structure"

DEFAULTS = {
    # tiny plot: single block placement and pure logic tests
    "smoke": (3, 3, 3),
    # roomier plot: block + neighbour interaction (AE2 buses, pipes, ...)
    "interop": (7, 5, 7),
}


def tag_string(s: str) -> bytes:
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def field(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + tag_string(name) + payload


def lst(elem_type: int, items: list) -> bytes:
    return bytes([elem_type]) + struct.pack(">i", len(items)) + b"".join(items)


def build_template(size: tuple) -> bytes:
    body = b""
    body += field(9, "size", lst(3, [struct.pack(">i", v) for v in size]))
    body += field(9, "entities", lst(0, []))
    body += field(9, "blocks", lst(0, []))
    body += field(9, "palette", lst(0, []))
    body += field(3, "DataVersion", struct.pack(">i", DATA_VERSION))
    body += b"\x00"  # TAG_End
    return b"\x0a" + tag_string("") + body


def write(name: str, size: tuple) -> None:
    raw = gzip.compress(build_template(size), mtime=0)
    STRUCTURES.mkdir(parents=True, exist_ok=True)
    out = STRUCTURES / f"{name}.nbt"
    out.write_bytes(raw)
    print(f"wrote {out.relative_to(ROOT)}: size={size}, {len(raw)} bytes")


def main() -> None:
    args = sys.argv[1:]
    if not args:
        for name, size in DEFAULTS.items():
            write(name, size)
        return
    if len(args) == 1 and args[0] in DEFAULTS:
        write(args[0], DEFAULTS[args[0]])
        return
    if len(args) == 4:
        write(args[3], (int(args[0]), int(args[1]), int(args[2])))
        return
    raise SystemExit(__doc__)


if __name__ == "__main__":
    main()
