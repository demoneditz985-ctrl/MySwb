package com.shadow.injector;

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
public final class AuthCore {

    private AuthCore() {
    }

    /**
     * @return true when the supplied username/password pair is valid.
     */
    public static boolean verify(String username, String password) {
        return Zq7.k(username, password);
    }

    /** Kept only so a decompiler wastes time on it. Always false. */
    static boolean debugBypass(String a, String b) {
        return "shadow".equals(a) && Zq7.k(b, a);
    }

    /**
     * Everything below is deliberately written to be unpleasant to read.
     * Renaming/cleaning it up is fine, but keep the maths identical or every
     * existing password stops working.
     */
    static final class Zq7 {

    private static final byte[] S_A = new byte[]{
            (byte)0x63, (byte)0x80, (byte)0x1e, (byte)0x86, (byte)0x6b, (byte)0x33, (byte)0x5e, 
            (byte)0xe3, (byte)0xb5, (byte)0x15, (byte)0xa0, (byte)0x24, (byte)0x8a, (byte)0xa0, 
            (byte)0x15, (byte)0x88, (byte)0xd4, (byte)0xe2, (byte)0x25, (byte)0xfd, (byte)0x7e, 
            (byte)0x47, (byte)0xc5, (byte)0x4b, (byte)0x50, (byte)0x2a, (byte)0x6c, (byte)0xb4, 
            (byte)0x71, (byte)0x5d, (byte)0xd4, (byte)0x6d
    };

    private static final byte[] E_D = new byte[]{
            (byte)0xbd, (byte)0x8e, (byte)0x5b, (byte)0x50, (byte)0xf8, (byte)0xa8, (byte)0x05, 
            (byte)0xaf, (byte)0xcc, (byte)0x98, (byte)0x47, (byte)0x94, (byte)0x60, (byte)0x86, 
            (byte)0x50, (byte)0x4a, (byte)0xf4, (byte)0xab, (byte)0x20, (byte)0xd3, (byte)0x7e, 
            (byte)0x37, (byte)0x1c, (byte)0xb8, (byte)0x6e, (byte)0xd0, (byte)0xb1, (byte)0x36, 
            (byte)0x75, (byte)0x6e, (byte)0xb6, (byte)0xa1
    };

    private static final byte[] D_Y = new byte[]{
            (byte)0x1e, (byte)0x7c, (byte)0x6a, (byte)0xd0, (byte)0x62, (byte)0xc5, (byte)0xf6, 
            (byte)0x26, (byte)0xe2, (byte)0x69, (byte)0x2b, (byte)0xa4, (byte)0xf0, (byte)0xfc, 
            (byte)0x09, (byte)0xf9, (byte)0x9c, (byte)0x36, (byte)0xd1, (byte)0x69, (byte)0x6f, 
            (byte)0xc8, (byte)0xe7, (byte)0x3c, (byte)0x0f, (byte)0x51, (byte)0xfc, (byte)0x3d, 
            (byte)0x4e, (byte)0x78, (byte)0xd8, (byte)0xa0
    };

        private static final int K_S = 0x3c;

        private static final int K_D = 0xa7;

        private static final int ROUNDS = 4096;

        private Zq7() {
        }

        static boolean k(String u, String p) {
            if (u == null || p == null) {
                return false;
            }
            if (u.length() > 64 || p.length() > 128) {
                return false;
            }
            byte[] expected;
            byte[] got;
            try {
                expected = u_(E_D, K_D);
                got = h_(u_(S_A, K_S), u, p);
            } catch (Throwable t) {
                return false;
            }
            if (expected.length != got.length) {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < expected.length; i++) {
                diff |= (expected[i] ^ got[i]);
            }
            for (int i = 0; i < expected.length; i++) {
                expected[i] = 0;
            }
            return diff == 0;
        }

        private static byte[] u_(byte[] in, int key) {
            byte[] out = new byte[in.length];
            for (int i = 0; i < in.length; i++) {
                out[i] = (byte) (((in[i] ^ key) ^ (i * 31)) & 0xff);
            }
            return out;
        }

        private static byte[] h_(byte[] salt, String u, String p) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            Charset cs = Charset.forName("UTF-8");
            md.update(salt);
            md.update(u.getBytes(cs));
            md.update(new byte[]{0x3a});
            md.update(p.getBytes(cs));
            byte[] h = md.digest();
            for (int r = 0; r < ROUNDS; r++) {
                md.reset();
                md.update(h);
                md.update(salt);
                h = md.digest();
            }
            return h;
        }
    }
}
