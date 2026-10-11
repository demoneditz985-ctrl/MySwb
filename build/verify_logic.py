#!/usr/bin/env python3
"""
Simulates Sketchware Pro's block -> Java generator (a.a.a.Fx) on the generated
data/logic file, so we can eyeball the code that will be compiled on the device.
Only the opCodes used by this project are implemented.
"""
import json
import re
import sys
from Crypto.Cipher import AES

KEY = b"sketchwaresecure"
OPS = {"repeat", "+", "-", "*", "/", "%", ">", "=", "<", "&&", "||", "not"}


def dec(b):
    c = AES.new(KEY, AES.MODE_CBC, KEY)
    d = c.decrypt(b)
    return d[:-d[-1]].decode("utf-8")


def parse(text):
    sections = {}
    cur = None
    for line in text.split("\n"):
        if line.startswith("@"):
            cur = line[1:].strip()
            sections.setdefault(cur, [])
        elif cur is not None and line.strip():
            sections[cur].append(line.strip())
    return sections


def param_types(spec):
    return [m.group().lower()
            for m in re.finditer(r"%\w+(?:\.\w+)?|%\w", spec)]


def escape(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


class Gen:
    def __init__(self, blocks):
        self.map = {b["id"]: b for b in blocks}

    # mirrors Fx.a(String param, int type, String opcode)
    def value(self, param, ptype):
        if param.startswith("@"):
            return self.block(param[1:])
        if ptype in ("%s",):
            return '"%s"' % escape(param)
        return param

    def params_of(self, bean):
        types = param_types(bean["spec"])
        if len(types) < len(bean["parameters"]):
            raise AssertionError(
                "spec %r has %d placeholders but %d parameters (id %s)"
                % (bean["spec"], len(types), len(bean["parameters"]), bean["id"]))
        out = []
        for i, p in enumerate(bean["parameters"]):
            out.append(self.value(p, types[i]))
        return out

    def block(self, bid):
        b = self.map[bid]
        p = self.params_of(b)
        code = self.code(b, p)
        if b["nextBlock"] >= 0:
            nxt = self.block(str(b["nextBlock"]))
            code = (code + ("\r\n" if code else "") + nxt)
        return code

    def stack(self, sid):
        if sid < 0:
            return ""
        return self.block(str(sid))

    def code(self, b, p):
        op = b["opCode"]
        if op == "addSourceDirectly":
            return p[0]
        if op == "doToast":
            return "SketchwareUtil.showMessage(getApplicationContext(), %s);" % p[0]
        if op == "setText":
            return "%s.setText(%s);" % (p[0], p[1])
        if op == "if":
            return "if (%s) {\r\n%s\r\n}" % (p[0], self.stack(b["subStack1"]))
        if op == "ifElse":
            return "if (%s) {\r\n%s\r\n} else {\r\n%s\r\n}" % (
                p[0], self.stack(b["subStack1"]), self.stack(b["subStack2"]))
        if op == "stringEquals":
            return "%s.equals(%s)" % (p[0], p[1])
        if op == "getText":
            return "%s.getText().toString()" % p[0]
        if op == "intentSetScreen":
            return "%s.setClass(getApplicationContext(), %s.class);" % (p[0], p[1])
        if op == "intentPutExtra":
            return '%s.putExtra(%s, %s);' % (p[0], p[1], p[2])
        if op == "startActivity":
            return "startActivity(%s);" % p[0]
        if op == "finishActivity":
            return "finish();"
        raise AssertionError("unknown opCode " + op)


def main(path):
    raw = dec(open(path, "rb").read())
    sections = parse(raw)
    ok = True
    for name in sorted(sections):
        if name.endswith("_var") or name.endswith("_func") or \
           name.endswith("_components") or name.endswith("_events"):
            continue
        blocks = [json.loads(l) for l in sections[name] if l.startswith("{")]
        if not blocks:
            continue
        print("=" * 72)
        print(name)
        print("=" * 72)
        g = Gen(blocks)
        try:
            print(g.block(blocks[0]["id"]))
        except Exception as e:
            ok = False
            print("!! ERROR:", e)
    print()
    print("RESULT:", "OK" if ok else "FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
