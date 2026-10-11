#!/usr/bin/env python3
"""Normalize custom-header Lua 5.1-style bytecode (32-bit size_t, endian byte,
LUAC_DATA block) into vanilla Lua 5.1 bytecode with host (64-bit) size_t,
so stock 64-bit tools (luadec/luac) can read it accurately.

Custom header layout observed:
  0..3   signature  \x1bLua
  4      version    (0x52 here)
  5      format     (0)
  6      endianness (1 = little)
  7      sizeof(int)          = 4
  8      sizeof(size_t)       = 4   <-- 32-bit compile
  9      sizeof(Instruction)  = 4
  10     sizeof(lua_Number)   = 8
  11     integral number flag = 0
  12..17 LUAC_DATA  \x19\x93\x0d\x0a\x1a\x0a
  18..   main Proto (Lua 5.1 layout, size_t = 4)
"""
import struct
import sys

LUAC_DATA = bytes([0x19, 0x93, 0x0D, 0x0A, 0x1A, 0x0A])


class Reader:
    def __init__(self, buf, off=0, sz_size=4, int_size=4, instr_size=4, num_size=8):
        self.b = buf
        self.o = off
        self.sz = sz_size
        self.int = int_size
        self.instr = instr_size
        self.num = num_size

    def _chk(self, n):
        if self.o + n > len(self.b) or self.o < 0:
            raise EOFError(f'read of {n} bytes at offset {self.o} beyond {len(self.b)}')

    def u8(self):
        self._chk(1)
        v = self.b[self.o]
        self.o += 1
        return v

    def uint(self, n):
        self._chk(n)
        v = int.from_bytes(self.b[self.o:self.o + n], 'little')
        self.o += n
        return v

    def i(self):
        return self.uint(self.int)

    def size(self):
        return self.uint(self.sz)

    def raw(self, n):
        v = self.b[self.o:self.o + n]
        self.o += n
        return v


class Writer:
    def __init__(self, sz_size=8, int_size=4, instr_size=4, num_size=8):
        self.out = bytearray()
        self.sz = sz_size
        self.int = int_size
        self.instr = instr_size
        self.num = num_size

    def u8(self, v):
        self.out.append(v & 0xFF)

    def uint(self, v, n):
        self.out += v.to_bytes(n, 'little')

    def i(self, v):
        self.uint(v, self.int)

    def size(self, v):
        self.uint(v, self.sz)

    def raw(self, b):
        self.out += b


def conv_string(r, w):
    n = r.size()
    if n == 0:
        w.size(0)
        return b''
    data = r.raw(n)
    w.size(n)
    w.raw(data)
    return data


def conv_proto(r, w, depth=0, report=None):
    tr = report is not None

    def T(*a):
        if tr:
            print('  ' * depth + f'[@{r.o:#x}]', *a)

    # NOTE: this custom format has NO source-name string at proto head;
    # vanilla 5.1 expects one, so emit an empty source string.
    w.size(0)
    # linedefined, lastlinedefined
    w.i(r.i())
    w.i(r.i())
    # numparams, is_vararg, maxstacksize
    w.u8(r.u8())
    w.u8(r.u8())
    w.u8(r.u8())
    # code
    n = r.i()
    n_code = n
    T('code n =', n)
    if n > 1000000:
        raise EOFError(f'absurd code count {n} at depth {depth}')
    w.i(n)
    for _ in range(n):
        w.uint(r.uint(r.instr), w.instr)
    # constants (Bynze under-reports the count; walk trailing extras)
    n = r.i()
    T('k n =', n)
    if n > 1000000:
        raise EOFError(f'absurd const count {n} at depth {depth}')

    def one_const():
        s0 = r.o
        t = r.u8()
        if t == 3:
            r.raw(r.num)
        elif t == 4:
            ln = r.size()
            if ln > len(r.b):
                raise EOFError('string too big')
            r.raw(ln)
        elif t not in (0, 1, 2):
            raise EOFError(f'bad const tag {t} at offset {s0:#x}')
        return t, r.b[s0:r.o]

    def p_sane(code_n):
        """peek: does a sane p-count + child-proto head sit at r.o now?"""
        save = r.o
        try:
            p = r.i()
            if not (0 <= p <= 4096):
                return False
            if p == 0:
                ln = r.i()
                return ln in (0, code_n)
            if r.o + 15 > len(r.b):
                return False
            lined = r.i()
            lastl = r.i()
            r.u8(); r.u8(); r.u8()
            cn = r.i()
            return (0 <= lined <= 1000000 and 0 <= lastl <= 1000000
                    and 0 < cn <= 3000000 and cn * r.instr <= len(r.b))
        except EOFError:
            return False
        finally:
            r.o = save

    w.i(n)
    parsed = 0
    while parsed < n:
        t, chunk = one_const()
        w.raw(chunk)
        if t == 4 and report is not None and len(chunk) < 200:
            report.append(chunk[5:])
        parsed += 1
    extras = 0
    if not p_sane(n_code):
        while extras < 32:
            save = r.o
            try:
                t, chunk = one_const()
            except EOFError:
                r.o = save
                break
            w.raw(chunk)
            if t == 4 and report is not None and len(chunk) < 200:
                report.append(chunk[5:])
            extras += 1
            if p_sane(n_code):
                break
    if extras:
        T(f'k: field said {n}, actually {parsed + extras}')
    # child protos
    n = r.i()
    T('p n =', n)
    if n > 100000:
        raise EOFError(f'absurd proto count {n} at depth {depth}')
    w.i(n)
    for _ in range(n):
        conv_proto(r, w, depth + 1, report)
    # debug: lineinfo
    n = r.i()
    T('lineinfo n =', n)
    if n > 10000000:
        raise EOFError(f'absurd lineinfo count {n} at depth {depth}')
    w.i(n)
    for _ in range(n):
        w.i(r.i())
    # locvars
    n = r.i()
    T('locvars n =', n)
    if n > 1000000:
        raise EOFError(f'absurd locvars count {n} at depth {depth}')
    w.i(n)
    for _ in range(n):
        conv_string(r, w)
        w.i(r.i())
        w.i(r.i())
    # upvalue names
    n = r.i()
    T('upvals n =', n)
    if n > 100000:
        raise EOFError(f'absurd upvals count {n} at depth {depth}')
    w.i(n)
    for _ in range(n):
        conv_string(r, w)
    T('proto end')


def main():
    src, dst = sys.argv[1], sys.argv[2]
    verbose = len(sys.argv) > 3 and sys.argv[3] == '-v'
    b = open(src, 'rb').read()
    assert b[:4] == b'\x1bLua', 'not lua bytecode'
    ver, fmt, endian = b[4], b[5], b[6]
    sz_int, sz_size, sz_instr, sz_num, integral = b[7], b[8], b[9], b[10], b[11]
    luac = b[12:18]
    print(f'version=0x{ver:02x} format={fmt} endian={endian} '
          f'int={sz_int} size_t={sz_size} instr={sz_instr} num={sz_num} '
          f'integral={integral} luac_data_ok={luac == LUAC_DATA}')
    assert luac == LUAC_DATA, 'unexpected LUAC_DATA'
    r = Reader(b, 18, sz_size=sz_size, int_size=sz_int,
               instr_size=sz_instr, num_size=sz_num)
    w = Writer(sz_size=8, int_size=sz_int, instr_size=sz_instr, num_size=sz_num)
    report = []
    try:
        conv_proto(r, w, 0, report)
    except EOFError as e:
        print('PARSE STOPPED:', e)
    hdr = b'\x1bLua' + bytes([0x51, 0x00, sz_int, 8, sz_instr, sz_num, integral])
    out = hdr + bytes(w.out)
    open(dst, 'wb').write(out)
    print(f'consumed {r.o} of {len(b)} bytes; wrote {len(out)} bytes to {dst}')
    if r.o != len(b):
        print(f'WARNING: trailing {len(b) - r.o} bytes')
    if verbose:
        seen = set()
        for s in report:
            t = s.rstrip(b'\0').decode('utf-8', 'replace')
            if t not in seen:
                seen.add(t)
                print('  STR:', repr(t[:120]))


if __name__ == '__main__':
    main()
