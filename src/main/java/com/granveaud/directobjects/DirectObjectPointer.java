package com.granveaud.directobjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DirectObjectPointer {
           /*
    static class DirectObjectPointerAutoRelease extends DirectObjectPointer {
        DirectObjectPointerAutoRelease(int size) {
            super(size);
        }

        @Override
        protected void finalize() throws Throwable {
            free();
        }
    }
       */
    protected long address;

   /* public static DirectObjectPointer create(int size, boolean autoRelease) {
        if (autoRelease) {
            return new DirectObjectPointerAutoRelease(size);
        }
        return new DirectObjectPointer(size);
    }    */

    DirectObjectPointer(int size) {
        address = Utils.UNSAFE.allocateMemory(size + 4);
        Utils.UNSAFE.putInt(address, size);
    }

    public static DirectObjectPointer fromBytes(byte[] bytes) {
        return fromBytes(bytes, 0, bytes.length);
    }

    public static DirectObjectPointer fromBytes(byte[] bytes, int off, int len) {
        DirectObjectPointer p = new DirectObjectPointer(len);
        Utils.UNSAFE.copyMemory(bytes, Utils.BYTES_OFFSET + off, null, p.address, len);

        return p;
    }

    public static DirectObjectPointer fromFileChannel(FileChannel fileChannel, int len) throws IOException {
        DirectObjectPointer p = new DirectObjectPointer(len);
        fileChannel.read(p.getAsByteBuffer());

        return p;
    }

    public static DirectObjectPointer fromMappedByteBuffer(MappedByteBuffer map, int len) {
        DirectObjectPointer p = new DirectObjectPointer(len);

        try {
            // copy memory from MappedByteBuffer to native memory
            long mapAddress = (Long) Utils.BUFFER_ADDRESS_FIELD.get(map);
            Utils.UNSAFE.copyMemory(null, mapAddress, null, p.address + 4, len);

            // change position in MappedByteBuffer
            map.position(map.position() + len);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get mapped byte buffer info", e);
        }

        return p;
    }

    public void free() {
        if (address != 0) {
            Utils.UNSAFE.freeMemory(address);
            address = 0;
        }
    }

    public void realloc(int newSize) {
        address = Utils.UNSAFE.reallocateMemory(address, newSize + 4);
        Utils.UNSAFE.putInt(address, newSize);
    }

    public long getAddress() {
        return address;
    }

    public int getSize() {
        return (address != 0 ? Utils.UNSAFE.getInt(address) : 0);
    }

    public byte[] getAsBytes() {
        if (address == 0) return null;

        byte[] result = new byte[getSize()];
        Utils.UNSAFE.copyMemory(null, address, result, Utils.BYTES_OFFSET, result.length);

        return result;
    }

    public ByteBuffer getAsByteBuffer() {
        if (address == 0) return null;

        try {
            // instanciate a DirectByteBuffer which wraps the memory block
            return (ByteBuffer) Utils.DIRECT_BYTE_BUFFER_CONSTRUCTOR.newInstance(address + 4, getSize(), null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instanciate DirectByteBuffer");
        }
    }
}
