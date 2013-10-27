package com.granveaud.directobjects.beans;

import com.granveaud.directobjects.DirectObject;
import com.granveaud.directobjects.DirectObjectContext;

public class Bean1 implements DirectObject {
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
    public void serialize(DirectObjectContext doContext) {
        doContext.putString(str1);
        doContext.alignInt();
        doContext.putStringFast(str2);
        doContext.alignInt();
        doContext.putStringASCII(str3);
    }

    @Override
    public void unserialize(DirectObjectContext doContext) {
        str1 = doContext.getString();
        doContext.alignInt();
        str2 = doContext.getStringFast();
        doContext.alignInt();
        str3 = doContext.getStringASCII();
    }

    @Override
    public int getSerializedSize(DirectObjectContext doContext) {
        int pos = 0;
        pos += doContext.getStringLength(str1);
        pos = doContext.alignPositionInt(pos);
        pos += doContext.getStringFastLength(str2);
        pos = doContext.alignPositionInt(pos);
        pos += doContext.getStringASCIILength(str3);

        return pos;
    }
}
