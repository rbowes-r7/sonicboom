// Mostly decompiled from SonicWall GMS's codebase
//
// (Replaced main(), the rest is original)

import java.math.BigInteger;

public class Decryptor {
    private int[] _key;
    private byte[] _keyBytes;
    private int _padding;
    protected static final char[] hex = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static void main(String[] args) {
        if(args.length == 0) {
            System.err.println("Decrypts TEAV-encrypted strings in SonicWall GMS");
            System.err.println();
            System.err.println("Usage: java Decryptor <hex string>");
            System.err.println();
            System.err.println("Eg: java Decryptor 2D2624C80F73C1B77C4A091581F3AD25");
            System.exit(1);
        }
        System.out.println("input = [" + args[0] + "]");
        System.out.println("Decrypted: " + Decryptor.decryptText(args[0]));
    }

    public static String encryptText(String input) {
        String keyString = "6751289138240981ac56d88f43abb87";
        byte[] key = new BigInteger(keyString, 16).toByteArray();
        Decryptor t = new Decryptor(key);
        String src = "M" + input + "M";
        src = t.padPlaintext(src);
        byte[] plainSource = src.getBytes();
        int[] enc = t.encode(plainSource, plainSource.length);
        return t.binToHex(enc);
    }

    public static String decryptText(String input) {
        String keyString = "6751289138240981ac56d88f43abb87";
        byte[] key = new BigInteger(keyString, 16).toByteArray();
        Decryptor t = new Decryptor(key);
        int[] enc2 = t.hexToBin(input);
        byte[] dec = t.decode(enc2);
        String decoded = new String(dec).trim();
        return decoded.substring(1, decoded.length() - 1);
    }

    public Decryptor(byte[] key) {
        int klen = key.length;
        this._key = new int[4];
        if (klen != 16) {
            throw new ArrayIndexOutOfBoundsException(this.getClass().getName() + ": Key is not 16 bytes");
        }
        int i = 0;
        int j = 0;
        while (j < klen) {
            this._key[i] = key[j] << 24 | (key[j + 1] & 0xFF) << 16 | (key[j + 2] & 0xFF) << 8 | key[j + 3] & 0xFF;
            j += 4;
            ++i;
        }
        this._keyBytes = key;
    }

    public Decryptor(int[] key) {
        this._key = key;
    }

    public String toString() {
        String tea = this.getClass().getName();
        tea = tea + ": Tiny Encryption Algorithm (TEA)  key: " + this.getHex(this._keyBytes);
        return tea;
    }

    public int[] encipher(int[] v) {
        int y = v[0];
        int z = v[1];
        int sum = 0;
        int delta = -1640531527;
        int n = 32;
        while (n-- > 0) {
            z += ((y += (z << 4 ^ z >>> 5) + (z ^ sum) + this._key[sum & 3]) << 4 ^ y >>> 5) + (y ^ (sum += delta)) + this._key[sum >>> 11 & 3];
        }
        int[] w = new int[]{y, z};
        return w;
    }

    public int[] decipher(int[] v) {
        int y = v[0];
        int z = v[1];
        int sum = -957401312;
        int delta = -1640531527;
        int n = 32;
        while (n-- > 0) {
            y -= ((z -= (y << 4 ^ y >>> 5) + (y ^ sum) + this._key[sum >>> 11 & 3]) << 4 ^ z >>> 5) + (z ^ (sum -= delta)) + this._key[sum & 3];
        }
        int[] w = new int[]{y, z};
        return w;
    }

    public int[] encode(byte[] b, int count) {
        int bLen = count;
        byte[] bp = b;
        this._padding = bLen % 8;
        if (this._padding != 0) {
            this._padding = 8 - bLen % 8;
            bp = new byte[bLen + this._padding];
            System.arraycopy(b, 0, bp, 0, bLen);
            bLen = bp.length;
        }
        int intCount = bLen / 4;
        int[] r = new int[2];
        int[] out = new int[intCount];
        int i = 0;
        int j = 0;
        while (j < bLen) {
            r[0] = bp[j] << 24 | (bp[j + 1] & 0xFF) << 16 | (bp[j + 2] & 0xFF) << 8 | bp[j + 3] & 0xFF;
            r[1] = bp[j + 4] << 24 | (bp[j + 5] & 0xFF) << 16 | (bp[j + 6] & 0xFF) << 8 | bp[j + 7] & 0xFF;
            r = this.encipher(r);
            out[i] = r[0];
            out[i + 1] = r[1];
            j += 8;
            i += 2;
        }
        return out;
    }

    public int padding() {
        return this._padding;
    }

    public byte[] decode(byte[] b, int count) {
        int intCount = count / 4;
        int[] ini = new int[intCount];
        int i = 0;
        int j = 0;
        while (i < intCount) {
            ini[i] = b[j] << 24 | (b[j + 1] & 0xFF) << 16 | (b[j + 2] & 0xFF) << 8 | b[j + 3] & 0xFF;
            ini[i + 1] = b[j + 4] << 24 | (b[j + 5] & 0xFF) << 16 | (b[j + 6] & 0xFF) << 8 | b[j + 7] & 0xFF;
            i += 2;
            j += 8;
        }
        return this.decode(ini);
    }

    public byte[] decode(int[] b) {
        int intCount = b.length;
        byte[] outb = new byte[intCount * 4];
        int[] tmp = new int[2];
        int j = 0;
        int i = 0;
        while (i < intCount) {
            tmp[0] = b[i];
            tmp[1] = b[i + 1];
            tmp = this.decipher(tmp);
            outb[j] = (byte)(tmp[0] >>> 24);
            outb[j + 1] = (byte)(tmp[0] >>> 16);
            outb[j + 2] = (byte)(tmp[0] >>> 8);
            outb[j + 3] = (byte)tmp[0];
            outb[j + 4] = (byte)(tmp[1] >>> 24);
            outb[j + 5] = (byte)(tmp[1] >>> 16);
            outb[j + 6] = (byte)(tmp[1] >>> 8);
            outb[j + 7] = (byte)tmp[1];
            i += 2;
            j += 8;
        }
        return outb;
    }

    public int[] hexToBin(String hexStr) throws ArrayIndexOutOfBoundsException {
        int hexStrLen = hexStr.length();
        if (hexStrLen % 8 != 0) {
            throw new ArrayIndexOutOfBoundsException("Hex string has incorrect length, required to be divisible by eight: " + hexStrLen);
        }
        int outLen = hexStrLen / 8;
        int[] out = new int[outLen];
        byte[] nibble = new byte[2];
        byte[] b = new byte[4];
        int posn = 0;
        for (int i = 0; i < outLen; ++i) {
            for (int j = 0; j < 4; ++j) {
                block26: for (int k = 0; k < 2; ++k) {
                    switch (hexStr.charAt(posn++)) {
                        case '0': {
                            nibble[k] = 0;
                            continue block26;
                        }
                        case '1': {
                            nibble[k] = 1;
                            continue block26;
                        }
                        case '2': {
                            nibble[k] = 2;
                            continue block26;
                        }
                        case '3': {
                            nibble[k] = 3;
                            continue block26;
                        }
                        case '4': {
                            nibble[k] = 4;
                            continue block26;
                        }
                        case '5': {
                            nibble[k] = 5;
                            continue block26;
                        }
                        case '6': {
                            nibble[k] = 6;
                            continue block26;
                        }
                        case '7': {
                            nibble[k] = 7;
                            continue block26;
                        }
                        case '8': {
                            nibble[k] = 8;
                            continue block26;
                        }
                        case '9': {
                            nibble[k] = 9;
                            continue block26;
                        }
                        case 'A': {
                            nibble[k] = 10;
                            continue block26;
                        }
                        case 'B': {
                            nibble[k] = 11;
                            continue block26;
                        }
                        case 'C': {
                            nibble[k] = 12;
                            continue block26;
                        }
                        case 'D': {
                            nibble[k] = 13;
                            continue block26;
                        }
                        case 'E': {
                            nibble[k] = 14;
                            continue block26;
                        }
                        case 'F': {
                            nibble[k] = 15;
                            continue block26;
                        }
                        case 'a': {
                            nibble[k] = 10;
                            continue block26;
                        }
                        case 'b': {
                            nibble[k] = 11;
                            continue block26;
                        }
                        case 'c': {
                            nibble[k] = 12;
                            continue block26;
                        }
                        case 'd': {
                            nibble[k] = 13;
                            continue block26;
                        }
                        case 'e': {
                            nibble[k] = 14;
                            continue block26;
                        }
                        case 'f': {
                            nibble[k] = 15;
                        }
                    }
                }
                b[j] = (byte)(nibble[0] << 4 | nibble[1]);
            }
            out[i] = b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | b[3] & 0xFF;
        }
        return out;
    }

    public String binToHex(int[] enc) throws ArrayIndexOutOfBoundsException {
        if (enc.length % 2 == 1) {
            throw new ArrayIndexOutOfBoundsException("Odd number of ints found: " + enc.length);
        }
        StringBuffer sb = new StringBuffer();
        byte[] outb = new byte[8];
        int[] tmp = new int[2];
        int counter = enc.length / 2;
        for (int i = 0; i < enc.length; i += 2) {
            outb[0] = (byte)(enc[i] >>> 24);
            outb[1] = (byte)(enc[i] >>> 16);
            outb[2] = (byte)(enc[i] >>> 8);
            outb[3] = (byte)enc[i];
            outb[4] = (byte)(enc[i + 1] >>> 24);
            outb[5] = (byte)(enc[i + 1] >>> 16);
            outb[6] = (byte)(enc[i + 1] >>> 8);
            outb[7] = (byte)enc[i + 1];
            sb.append(this.getHex(outb));
        }
        return sb.toString();
    }

    public String getHex(byte[] b) {
        StringBuffer r = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            int c = b[i] >>> 4 & 0xF;
            r.append(hex[c]);
            c = b[i] & 0xF;
            r.append(hex[c]);
        }
        return r.toString();
    }

    public String padPlaintext(String str, char pc) {
        StringBuffer sb = new StringBuffer(str);
        int padding = sb.length() % 8;
        for (int i = 0; i < padding; ++i) {
            sb.append(pc);
        }
        return sb.toString();
    }

    public String padPlaintext(String str) {
        return this.padPlaintext(str, ' ');
    }
}
