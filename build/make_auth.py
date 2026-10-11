#!/usr/bin/env python3
"""
Regenerate src/data/files/java/AuthCore.java with a new username/password.

    python3 build/make_auth.py admin 1234

The password is never written to the file - only a salted, iterated
SHA-256 digest, XOR-scrambled so that `strings` on the DEX finds nothing.
"""
import hashlib
import os
import sys

SALT = bytes([0x5f, 0xa3, 0x1c, 0xe7, 0x2b, 0x94, 0xd8, 0x06, 0x71, 0x3e,
              0xaa, 0x4d, 0xc2, 0x0f, 0x9b, 0x65, 0x18, 0xd1, 0x37, 0x8c,
              0x2e, 0xf0, 0x53, 0xbe, 0x84, 0x11, 0x76, 0xcd, 0x29, 0xe2,
              0x4a, 0x90])
ROUNDS = 4096
K_SALT = 0x3c
K_HASH = 0xa7

TEMPLATE = '''package com.shadow.injector;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Credential verification for Shadow Injector.
 *
 * The password is NOT stored anywhere in this app - not in the layouts, not in
 * the generated block code and not in this file. What is kept here is a
 * salted, iterated SHA-256 digest that is assembled at runtime from XOR
 * scrambled byte tables, so a plain strings/DEX scan turns up nothing usable.
 *
 * To change the credentials you must regenerate the tables - see
 * build/make_auth.py.
 */
public final class AuthCore {{

    private AuthCore() {{
    }}

    /**
     * @return true when the supplied username/password pair is valid.
     */
    public static boolean verify(String username, String password) {{
        return Zq7.k(username, password);
    }}

    /** Kept only so a decompiler wastes time on it. Always false. */
    static boolean debugBypass(String a, String b) {{
        return "shadow".equals(a) && Zq7.k(b, a);
    }}

    /**
     * Everything below is deliberately written to be unpleasant to read.
     * Renaming/cleaning it up is fine, but keep the maths identical or every
     * existing password stops working.
     */
    static final class Zq7 {{

{salt}

{expected}

{decoy}

        private static final int K_S = 0x{ks:02x};

        private static final int K_D = 0x{kd:02x};

        private static final int ROUNDS = {rounds};

        private Zq7() {{
        }}

        static boolean k(String u, String p) {{
            if (u == null || p == null) {{
                return false;
            }}
            if (u.length() > 64 || p.length() > 128) {{
                return false;
            }}
            byte[] expected;
            byte[] got;
            try {{
                expected = u_(E_D, K_D);
                got = h_(u_(S_A, K_S), u, p);
            }} catch (Throwable t) {{
                return false;
            }}
            if (expected.length != got.length) {{
                return false;
            }}
            int diff = 0;
            for (int i = 0; i < expected.length; i++) {{
                diff |= (expected[i] ^ got[i]);
            }}
            for (int i = 0; i < expected.length; i++) {{
                expected[i] = 0;
            }}
            return diff == 0;
        }}

        private static byte[] u_(byte[] in, int key) {{
            byte[] out = new byte[in.length];
            for (int i = 0; i < in.length; i++) {{
                out[i] = (byte) (((in[i] ^ key) ^ (i * 31)) & 0xff);
            }}
            return out;
        }}

        private static byte[] h_(byte[] salt, String u, String p) throws Exception {{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            Charset cs = Charset.forName("UTF-8");
            md.update(salt);
            md.update(u.getBytes(cs));
            md.update(new byte[]{{0x3a}});
            md.update(p.getBytes(cs));
            byte[] h = md.digest();
            for (int r = 0; r < ROUNDS; r++) {{
                md.reset();
                md.update(h);
                md.update(salt);
                h = md.digest();
            }}
            return h;
        }}
    }}
}}
'''


def digest(user, password):
    h = hashlib.sha256(SALT + (user + ":" + password).encode("utf-8")).digest()
    for _ in range(ROUNDS):
        h = hashlib.sha256(h + SALT).digest()
    return h


def table(name, data, key):
    scrambled = bytes(((data[i] ^ (i * 31)) & 0xff) ^ key for i in range(len(data)))
    vals = ["(byte)0x%02x" % c for c in scrambled]
    lines, line = [], ""
    for v in vals:
        if len(line) + len(v) + 2 > 92:
            lines.append(line)
            line = ""
        line += v + ", "
    lines.append(line.rstrip(", "))
    body = "\n".join("            " + l for l in lines)
    return "    private static final byte[] %s = new byte[]{\n%s\n    };" % (name, body)


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 1
    user, password = sys.argv[1], sys.argv[2]
    real = digest(user, password)
    decoy = digest(password, user)

    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, os.pardir, "src", "data", "files", "java", "AuthCore.java")
    out = os.path.normpath(out)

    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(TEMPLATE.format(salt=table("S_A", SALT, K_SALT),
                                expected=table("E_D", real, K_HASH),
                                decoy=table("D_Y", decoy, K_HASH),
                                ks=K_SALT, kd=K_HASH, rounds=ROUNDS))
    print("wrote %s" % out)
    print("digest: %s" % real.hex())
    return 0


if __name__ == "__main__":
    sys.exit(main())
