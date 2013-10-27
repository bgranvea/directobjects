package com.granveaud.directobjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DirectObjectPointer {

    public static class Builder {
        private DirectObject bean;
        private byte[] bytes;
        private FileChannel fileChannel;
        private MappedByteBuffer mappedByteBuffer;
        private int objectSize = -1;

        private DirectObjectContext directObjectContext;
        private boolean autoRelease;
        private int bytesOffset = -1;
        private int bytesLength = -1;

        public Builder fromBean(DirectObject bean) {
            this.bean = bean;
            return this;
        }

        public Builder fromBytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public Builder fromFileChannel(FileChannel fileChannel) {
            this.fileChannel = fileChannel;
            return this;
        }

        public Builder fromMappedByteBuffer(MappedByteBuffer mappedByteBuffer) {
            this.mappedByteBuffer = mappedByteBuffer;
            return this;
        }

        public Builder withContext(DirectObjectContext directObjectContext) {
            this.directObjectContext = directObjectContext;
            return this;
        }

        public Builder withAutoRelease(boolean autoRelease) {
            this.autoRelease = autoRelease;
            return this;
        }

        public Builder withObjectSize(int objectSize) {
            this.objectSize = objectSize;
            return this;
        }

        public Builder withBytesOffset(int bytesOffset) {
            this.bytesOffset = bytesOffset;
            return this;
        }

        public Builder withBytesLength(int bytesLength) {
            this.bytesLength = bytesLength;
            return this;
        }

        public DirectObjectPointer build() {
            if (bean == null && bytes == null && fileChannel == null && mappedByteBuffer == null) {
                throw new IllegalArgumentException("One of fromBean, fromBytes, fromFileChannel or fromMappedByteBuffer is mandatory");
            }

            if ((fileChannel != null || mappedByteBuffer != null) && objectSize == -1) {
                throw new IllegalArgumentException("withObjectSize is mandatory with fromFileChannel or fromMappedByteBuffer");
            }

            DirectObjectPointer pointer = null;
            if (bean != null) {
                pointer = DirectObjectPointer.createFromBean(bean, directObjectContext != null ? directObjectContext : new DirectObjectContext());
            } else if (bytes != null) {
                pointer = DirectObjectPointer.createFromBytes(bytes, bytesOffset != -1 ? bytesOffset : 0, bytesLength != -1 ? bytesLength : bytes.length);
            } else if (fileChannel != null) {
                try {
                    pointer = DirectObjectPointer.createFromFileChannel(fileChannel, objectSize);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot read from file channel", e);
                }
            } else if (mappedByteBuffer != null) {
                pointer = DirectObjectPointer.createFromMappedByteBuffer(mappedByteBuffer, objectSize);
            }

            // autoclose option
            if (autoRelease) {
                pointer = new DirectObjectPointerAutoRelease(pointer.address);
            }

            return pointer;
        }
    }

    private static class DirectObjectPointerAutoRelease extends DirectObjectPointer {
        DirectObjectPointerAutoRelease(long address) {
            super(address);
        }

        @Override
        protected void finalize() throws Throwable {
            free();
        }
    }

    protected long address;

    private DirectObjectPointer(long address) {
        this.address = address;
    }

    private DirectObjectPointer(int objSize) {
        address = Utils.UNSAFE.allocateMemory(objSize + 4);
        Utils.UNSAFE.putInt(address, objSize);
    }

    private void realloc(int newObjSize) {
        address = Utils.UNSAFE.reallocateMemory(address, newObjSize + 4);
        Utils.UNSAFE.putInt(address, newObjSize);
    }

    public void free() {
        if (address != 0) {
            Utils.UNSAFE.freeMemory(address);
            address = 0;
        }
    }

    private static DirectObjectPointer createFromBean(DirectObject bean, DirectObjectContext doContext) {
        int objSize = bean.getSerializedSize(doContext);

        // allocate native memory
        DirectObjectPointer pointer = new DirectObjectPointer(objSize);
        doContext.reset(pointer);

        // serialize
        doContext.startReadWrite();
        bean.serialize(doContext);
        doContext.finishReadWrite();

        return pointer;
    }

    private static DirectObjectPointer createFromBytes(byte[] bytes, int offset, int objSize) {
        DirectObjectPointer p = new DirectObjectPointer(objSize);
        Utils.UNSAFE.copyMemory(bytes, Utils.BYTES_OFFSET + offset, null, p.address, objSize);

        return p;
    }

    private static DirectObjectPointer createFromFileChannel(FileChannel fileChannel, int objSize) throws IOException {
        DirectObjectPointer p = new DirectObjectPointer(objSize);
        fileChannel.read(p.getAsByteBuffer());

        return p;
    }

    private static DirectObjectPointer createFromMappedByteBuffer(MappedByteBuffer map, int objSize) {
        DirectObjectPointer p = new DirectObjectPointer(objSize);

        try {
            // copy memory from MappedByteBuffer to native memory
            long mapAddress = (Long) Utils.BUFFER_ADDRESS_FIELD.get(map);
            Utils.UNSAFE.copyMemory(null, mapAddress, null, p.address + 4, objSize);

            // change position in MappedByteBuffer
            map.position(map.position() + objSize);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get mapped byte buffer info", e);
        }

        return p;
    }

    public void updateFromBean(DirectObject bean) {
        updateFromBean(bean, new DirectObjectContext());
    }

    public void updateFromBean(DirectObject bean, DirectObjectContext doContext) {
        int objSize = bean.getSerializedSize(doContext);

        doContext.reset(this);

        // check size and realloc if necessary
        int oldObjSize = getObjectSize();
        if (oldObjSize != objSize) {
            realloc(objSize);
        }

        // serialize
        doContext.startReadWrite();
        bean.serialize(doContext);
        doContext.finishReadWrite();
    }

    public void populateBean(DirectObject bean) {
        populateBean(bean, new DirectObjectContext());
    }

    public void populateBean(DirectObject bean, DirectObjectContext doContext) {
        doContext.reset(this);

        // unserialize
        doContext.startReadWrite();
        bean.unserialize(doContext);
        doContext.finishReadWrite();
    }

    public long getAddress() {
        return address;
    }

    public int getObjectSize() {
        return (address != 0 ? Utils.UNSAFE.getInt(address) : 0);
    }

    public byte[] getAsBytes() {
        if (address == 0) return null;

        byte[] result = new byte[getObjectSize()];
        Utils.UNSAFE.copyMemory(null, address, result, Utils.BYTES_OFFSET, result.length);

        return result;
    }

    public ByteBuffer getAsByteBuffer() {
        if (address == 0) return null;

        try {
            // instanciate a DirectByteBuffer which wraps the memory block
            return (ByteBuffer) Utils.DIRECT_BYTE_BUFFER_CONSTRUCTOR.newInstance(address + 4, getObjectSize(), null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instanciate DirectByteBuffer");
        }
    }
}
