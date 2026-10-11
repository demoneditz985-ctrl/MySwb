# Bynze 3.4.2 "Beta Private" — bytecode analysis report

Target: `input_bytecode.luac` (originally uploaded as `decompile me.lua`, 377,537 bytes)
Date: 2026-10-11 · Tooling: `tools/luanorm.py` (dissector / header normalizer), `viruscamp/luadec` (built, 64-bit)

## 1. What this file is

The banner constant at `0x49` identifies the protector:

```
Bynze 3.4.2 Beta Private ~> @YX048
```

Bynze is a **commercial Lua obfuscator** sold on Telegram by `@YX048` ("Ynx Lua").
This is *not* stock Lua bytecode and no public decompiler supports it
(verified: no Bynze tooling exists on GitHub; the only known leak, "Bynze 3.4.2 Enc
Lite" (`Bynze_Enc.lua`), is posted inside Telegram channels that are not reachable
from this environment).

The protected payload is an **Android GameGuardian mod-menu script**: constants
include `gg`, `getFile`, `MemoryPatcher`, `sorts`, plus a licence/online-check layer
(`BASE_URL`, `INTEGRATION_SECRET`, `current_time`, `time_match`, `if_time_not_matched`,
`checkHack`) and UI helpers (`menu`, `fastToast`).

The file is **recursively re-obfuscated**: inner layers carry the watermarks
`Bynze 3.5`, `Bynze 3.6`, `Bynze 3.7` … `Bynze 3.14` (each layer's decoder is
protected by the next Bynze version), interleaved with Sun-Zi *Art of War* quotes
as watermarks. This matches the vendor's own description of the product:
`[1] Lasm Obfuscate · [2] AntiLog/Hook/Loadfile · [3] Encode Table, Confusion ·
[4] Decoder obfuscated with next Bynze version`.

## 2. Container format (reversed)

```
offset  size  field
0x00    4     signature "\x1bLua"
0x04    1     version byte 0x52 (custom; not Lua 5.2)
0x05    1     format 0
0x06    1     endianness (1 = little)
0x07    1     sizeof(int) = 4
0x08    1     sizeof(size_t) = 4      <-- 32-bit compile
0x09    1     sizeof(Instruction) = 4
0x0A    1     sizeof(lua_Number) = 8 (double)
0x0B    1     integral flag = 0
0x0C    6     LUAC_DATA 19 93 0D 0A 1A 0A
0x12    ...   main Proto
```

Proto layout (Lua-5.1-like but **the source-name string is removed**):

```
linedefined u32, lastlinedefined u32,
numparams u8, is_vararg u8, maxstacksize u8,
code:      u32 n, n × u32 instructions
constants: u32 n  (UNDER-REPORTED, see 3.2)
           tag u8: 0 nil | 1 false | 2 true | 3 double(8 bytes) | 4 string(u32 len incl. trailing \0, bytes)
children:  u32 n, recursive Protos
debug:     lineinfo u32 n (+ n × u32)   — usually 0 (stripped)
           locvars  u32 n (+ n × (string, u32, u32)) — usually 0; some protos keep YNX_L* names
           upvals   u32 n (+ n × string) — usually 0
```

`tools/luanorm.py` parses this and rewrites vanilla 64-bit Lua 5.1 chunks
(expanding every `size_t` field 4 → 8 bytes so 64-bit tools read it correctly —
the "accurate for 64 bits" conversion). It parses the outer ~13 % of the file
(main stub + first proto tree, see `proto_map_prefix.txt`) before hitting
per-proto confusion (section 3.3).

## 3. Protection layers observed

### 3.1 Custom header / 32-bit sizes
Defeats every stock Lua tool (luac/luadec/unluac/ChunkSpy) at load time.

### 3.2 Under-reported constant counts
The `constants n` field is smaller than the real constant list (deltas observed:
+2, +4, +6, +12 …). Real end of the constant section is only detectable
structurally (next field must be a sane child-proto count + proto head).
Any naive parser desynchronises here.

### 3.3 Per-proto "Encode Table / Confusion"
From ~`0x1ff43` onward proto heads no longer follow the layout above:
children of some protos are stored with additional per-proto transformation
(field reorder / encrypted proto body). Entropy analysis shows the region is
*not* ciphertext (5.2–5.7 bits/byte = structured data), i.e. it is a custom
confusion of the serialization, not encryption.

### 3.4 Scrambled opcode table
Decoding instructions with the stock Lua 5.1 opcode map yields semantic
nonsense even in cleanly-parsed protos (e.g. main: `SETGLOBAL A0 B0 C256`,
`OP37`, `FORPREP sBx-131071`; depth-1: endless `LT A0 B257 Cxxx`).
The opcode permutation is per-build ("Encode Table"), so disassembly requires
the encoder's table or the patched VM.

### 3.5 Recursive re-obfuscation
Inner decoders are themselves Bynze 3.5 → 3.14 outputs; each would need the
same treatment again, recursively.

## 4. Feasibility statement

Producing the **original source** from this file requires one of:

1. the Bynze encoder script (`Bynze_Enc.lua` 3.4.2, leaked on Telegram) — it
   contains the exact encode table + serializer, from which an exact decoder
   can be generated; or
2. the patched Lua runtime (`liblua.so` / loader) that executes these files —
   reversing its `lundump` + VM loop gives the same information; or
3. a runtime dump (hook `loadstring`/`load` in the executor and dump the
   decrypted top-level function with `string.dump`).

Without one of these, any "decompiled source" anyone hands you for this file is
guesswork. What *is* fully recoverable statically is delivered here:
complete constant/string intelligence (`extracted_constants.txt`, 167 strings)
and the proto map of the parseable prefix (`proto_map_prefix.txt`).

## 5. Payload intelligence (highlights from extracted_constants.txt)

| offset | constant | meaning |
|---|---|---|
| 0x01b61f | `MemoryPatcher` | GG memory-patching class |
| 0x0326ea | `gg` | GameGuardian API global |
| 0x032704 | `getFile` | GG file helper |
| 0x01b429 | `BASE_URL` | licence server |
| 0x01b5fe | `INTEGRATION_SECRET` | licence HMAC/key |
| 0x01b2a3 | `current_time` / `time_match` / `if_time_not_matched` | time-lock / expiry |
| 0x01b3a2 | `checkHack` | anti-tamper self-check |
| 0x01b5ed | `fastToast` | Android toast UI helper |
| 0x0206e7 | `YNX_L007` | obfuscator-generated local name |
| 0x0258c3…0x031c71 | `Bynze 3.5` … `Bynze 3.14` | nested decoder layers |
