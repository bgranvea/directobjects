package com.granveaud.directobjects.beans;

import com.granveaud.directobjects.DirectObject;

public class Bean1 extends DirectObject {
    private String str1;
    private String str2;
    private String str3;

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }

    public String getStr3() {
        return str3;
    }

    public void setStr3(String str3) {
        this.str3 = str3;
    }

    @Override
    protected void serialize() {
        putString(str1);
        alignInt();
        putStringFast(str2);
        alignInt();
        putStringASCII(str3);
    }

    /**
     * DirectObject methods
     */
    @Override
    protected void unserialize() {
        str1 = getString();
        alignInt();
        str2 = getStringFast();
        alignInt();
        str3 = getStringASCII();
    }

    @Override
    protected int getSerializedSize() {
        int pos = 0;
        pos += getStringLength(str1);
        pos = alignPositionInt(pos);
        pos += getStringFastLength(str2);
        pos = alignPositionInt(pos);
        pos += getStringASCIILength(str3);

        return pos;
    }
}
