package com.granveaud.directobjects;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.Buffer;

@SuppressWarnings("unchecked")
public class Utils {
    final static protected Unsafe UNSAFE;
    final static protected int BYTES_OFFSET;
    final static protected long STRING_VALUE_OFFSET;
    final static protected long STRING_COUNT_OFFSET; // can be 0 for some Java versions
    final static protected Constructor DIRECT_BYTE_BUFFER_CONSTRUCTOR;
    final static protected long BUFFER_ADDRESS_OFFSET;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

            STRING_VALUE_OFFSET = getFieldOffset(String.class, "value");
            if (STRING_VALUE_OFFSET == 0) {
                throw new RuntimeException("Can't get String.value offset");
            }
            STRING_COUNT_OFFSET = getFieldOffset(String.class, "count");

            Class directByteBufferClass = Utils.class.getClassLoader().loadClass("java.nio.DirectByteBuffer");
            DIRECT_BYTE_BUFFER_CONSTRUCTOR = directByteBufferClass.getDeclaredConstructor(long.class, int.class);
            DIRECT_BYTE_BUFFER_CONSTRUCTOR.setAccessible(true);

            Field f3 = Buffer.class.getDeclaredField("address");
            BUFFER_ADDRESS_OFFSET = UNSAFE.objectFieldOffset(f3);

        } catch (Exception e) {
            throw new RuntimeException("Can't initialize Unsafe", e);
        }
    }

    private static long getFieldOffset(Class clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            return UNSAFE.objectFieldOffset(f);
        } catch (Exception e) {
            return 0;
        }
    }
}
