package com.granveaud.directobjects;

public class DirectObjectContext {
    private DirectObjectPointer pointer;

    // current position for serialization/unserialization operations
    private long currentAddress;

    public DirectObjectContext() {
    }

    public DirectObjectContext(DirectObjectPointer pointer) {
        this.pointer = pointer;
        reset();
    }

    public void reset() {
        currentAddress = pointer.address + 4;
    }

    public void reset(DirectObjectPointer newPointer) {
        this.pointer = newPointer;
        reset();
    }

    public DirectObjectPointer getPointer() {
        return pointer;
    }

    protected void startReadWrite() {
        currentAddress = pointer.address + 4;
    }

    protected void finishReadWrite() {
        // check position
        if (currentAddress != 0 && getRelativePosition() > pointer.getObjectSize()) {
            throw new RuntimeException("Read or write exceeded object size. Risk of memory corruption!");
        }

        if (currentAddress != 0 && getRelativePosition() < pointer.getObjectSize()) {
            System.err.println("warning: unused space " + (currentAddress - pointer.address));
        }
    }

    private int getRelativePosition() {
        return (int) (currentAddress - pointer.address - 4);
    }

    public void alignInt() {
        if ((currentAddress & 3) != 0) {
            currentAddress = ((currentAddress >> 2) + 1) << 2;
        }
    }

    public void alignLong() {
        if ((currentAddress & 7) != 0) {
            currentAddress = ((currentAddress >> 3) + 1) << 3;
        }
    }

    public byte getByte() {
        return Utils.UNSAFE.getByte(currentAddress++);
    }

    public void putByte(byte value) {
        Utils.UNSAFE.putByte(currentAddress++, value);
    }

    public int getUnsignedByte() {
        return Utils.UNSAFE.getByte(currentAddress++) & 0xff;
    }

    public void putUnsignedByte(int value) {
        Utils.UNSAFE.putInt(currentAddress++, (byte) value);
    }

    public char getChar() {
        char res = Utils.UNSAFE.getChar(currentAddress);
        currentAddress += 2;
        return res;
    }

    public void putChar(char value) {
        Utils.UNSAFE.putChar(currentAddress, value);
        currentAddress += 2;
    }

    public short getShort() {
        short res = Utils.UNSAFE.getShort(currentAddress);
        currentAddress += 2;
        return res;
    }

    public void putShort(short value) {
        Utils.UNSAFE.putShort(currentAddress, value);
        currentAddress += 2;
    }

    public int getInt() {
        int res = Utils.UNSAFE.getInt(currentAddress);
        currentAddress += 4;
        return res;
    }

    public void putInt(int value) {
        Utils.UNSAFE.putInt(currentAddress, value);
        currentAddress += 4;
    }

    public long getLong() {
        long res = Utils.UNSAFE.getLong(currentAddress);
        currentAddress += 8;
        return res;
    }

    public void putLong(long value) {
        Utils.UNSAFE.putLong(currentAddress, value);
        currentAddress += 8;
    }

    public void putBytes(byte[] bytes) {
        int len = bytes.length;
        Utils.UNSAFE.copyMemory(bytes, Utils.BYTES_OFFSET, null, currentAddress, len);
        currentAddress += len;
    }

    public void putBytes(byte[] bytes, int off, int len) {
        Utils.UNSAFE.copyMemory(bytes, Utils.BYTES_OFFSET + off, null, currentAddress, len);
        currentAddress += len;
    }

    public void getBytes(byte[] bytes) {
        int len = bytes.length;
        Utils.UNSAFE.copyMemory(null, currentAddress, bytes, Utils.BYTES_OFFSET, len);
        currentAddress += len;
    }

    public void getBytes(byte[] bytes, int off, int len) {
        Utils.UNSAFE.copyMemory(null, currentAddress, bytes, Utils.BYTES_OFFSET + off, len);
        currentAddress += len;
    }

    public void putString(CharSequence str) {
        if (str == null) {
            putInt(-1);
            return;
        }

        int strlen = str.length();
        putInt(strlen);

        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (c < 0x80) {
                putByte((byte) c);
            } else if (c < 0x4000) {
                putByte((byte) (0x80 | (c & 0x7f)));
                putByte((byte) ((c >> 7) & 0x7f));
            } else {
                putByte((byte) (0x80 | (c & 0x7f)));
                putByte((byte) (0x80 | ((c >> 7) & 0x7f)));
                putByte((byte) ((c >> 14) & 0x7f));
            }
        }
    }

    public String getString() {
        int strlen = getInt();
        if (strlen == -1) return null;

        char[] chars = new char[strlen];

        for (int i = 0; i < strlen; i++) {
            int c1 = getByte();
            if ((c1 & 0x80) == 0) {
                chars[i] = (char) c1;
            } else {
                int c2 = getByte();
                if ((c2 & 0x80) == 0) {
                    chars[i] = (char) ((c2 << 7) | (c1 & 0x7f));
                } else {
                    int c3 = getByte();
                    chars[i] = (char) ((c3 << 14) | (c2 & 0x7f) << 7 | (c1 & 0x7f));
                }
            }
        }

        // create String without copying char[]
        // UnsafeUtils.UNSAFE.allocateInstance(String.class) crashes??
        String res = new String();
        Utils.UNSAFE.putObject(res, Utils.STRING_VALUE_OFFSET, chars);
        if (Utils.STRING_COUNT_OFFSET != 0) {
            Utils.UNSAFE.putInt(res, Utils.STRING_COUNT_OFFSET, chars.length);
        }

        return res;
    }

    public int getStringLength(CharSequence str) {
        if (str == null) return 4;

        int res = 4;
        int strlen = str.length();
        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (c < 0x80) {
                res++;
            } else if (c < 0x4000) {
                res += 2;
            } else {
                res += 3;
            }
        }

        return res;
    }

    public void putStringFast(CharSequence str) {
        if (str == null) {
            putInt(-1);
            return;
        }

        int strlen = str.length();
        putInt(strlen);

        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            putChar(c);
        }
    }

    public String getStringFast() {
        int strlen = getInt();
        if (strlen == -1) return null;

        char[] chars = new char[strlen];
        for (int i = 0; i < strlen; i++) {
            chars[i] = getChar();
        }

        // create String without copying char[]
        // UnsafeUtils.UNSAFE.allocateInstance(String.class) crashes??
        String res = new String();
        Utils.UNSAFE.putObject(res, Utils.STRING_VALUE_OFFSET, chars);
        if (Utils.STRING_COUNT_OFFSET != 0) {
            Utils.UNSAFE.putInt(res, Utils.STRING_COUNT_OFFSET, chars.length);
        }

        return res;
    }

    public int getStringFastLength(CharSequence str) {
        if (str == null) return 4;

        return 4 + str.length() * 2;
    }

    public void putStringASCII(CharSequence str) {
        if (str == null) {
            putInt(-1);
            return;
        }

        int strlen = str.length();
        putInt(strlen);

        for (int i = 0; i < strlen; i++) {
            putByte((byte) str.charAt(i));
        }
    }

    public String getStringASCII() {
        int strlen = getInt();
        if (strlen == -1) return null;

        char[] chars = new char[strlen];
        for (int i = 0; i < strlen; i++) {
            chars[i] = (char) getByte();
        }

        // create String without copying char[]
        // UnsafeUtils.UNSAFE.allocateInstance(String.class) crashes??
        String res = new String();
        Utils.UNSAFE.putObject(res, Utils.STRING_VALUE_OFFSET, chars);
        if (Utils.STRING_COUNT_OFFSET != 0) {
            Utils.UNSAFE.putInt(res, Utils.STRING_COUNT_OFFSET, chars.length);
        }

        return res;
    }

    public int getStringASCIILength(CharSequence str) {
        if (str == null) return 4;

        return 4 + str.length();
    }

    public int alignPositionInt(int pos) {
        if ((pos & 3) != 0) {
            return ((pos >> 2) + 1) << 2;
        }
        return pos;
    }

    public int alignPositionLong(int pos) {
        if ((pos & 7) != 0) {
            return ((pos >> 3) + 1) << 3;
        }
        return pos;
    }
}
