package com.granveaud.directobjects;

public interface DirectObject {
    void serialize(DirectObjectContext doContext);

    void unserialize(DirectObjectContext doContext);

    int getSerializedSize(DirectObjectContext doContext);
}
