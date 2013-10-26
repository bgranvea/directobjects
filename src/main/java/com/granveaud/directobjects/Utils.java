package com.granveaud.directobjects;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

@SuppressWarnings("unchecked")
public class Utils {
    final static protected Unsafe UNSAFE;
    final static protected int BYTES_OFFSET;
    final static protected long STRING_CHAR_ARRAY_OFFSET;
    final static protected Constructor DIRECT_BYTE_BUFFER_CONSTRUCTOR;
    final static protected Field BUFFER_ADDRESS_FIELD;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

            Field f = String.class.getDeclaredField("value");
            STRING_CHAR_ARRAY_OFFSET = UNSAFE.objectFieldOffset(f);

            Class directByteBufferClass = Utils.class.getClassLoader().loadClass("java.nio.DirectByteBuffer");
            DIRECT_BYTE_BUFFER_CONSTRUCTOR = directByteBufferClass.getDeclaredConstructor(long.class, int.class, Object.class);
            DIRECT_BYTE_BUFFER_CONSTRUCTOR.setAccessible(true);

            BUFFER_ADDRESS_FIELD = Buffer.class.getDeclaredField("address");
            BUFFER_ADDRESS_FIELD.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException("Can't initialize Unsafe", e);
        }
    }
}
