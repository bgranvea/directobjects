package com.granveaud.directobjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DirectObjectPointer {

    public static class Builder {
        private DirectObject bean;
        private FileChannel fileChannel;
        private MappedByteBuffer mappedByteBuffer;

        private DirectObjectContext directObjectContext;
        private boolean autoRelease;

        public Builder fromBean(DirectObject bean) {
            this.bean = bean;
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

        public DirectObjectPointer build() {
            if (bean == null && fileChannel == null && mappedByteBuffer == null) {
                throw new IllegalArgumentException("One of fromBean, fromFileChannel or fromMappedByteBuffer is mandatory");
            }

            DirectObjectPointer pointer = null;
            if (bean != null) {
                pointer = DirectObjectPointer.createFromBean(bean, directObjectContext != null ? directObjectContext : new DirectObjectContext());
            } else if (fileChannel != null) {
                try {
                    // read objSize
                    ByteBuffer tempBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
                    fileChannel.read(tempBuffer);

                    tempBuffer.flip();
                    tempBuffer.position(0);
                    int objSize = tempBuffer.getInt();

                    pointer = DirectObjectPointer.createFromFileChannel(fileChannel, objSize);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot read from file channel", e);
                }
            } else if (mappedByteBuffer != null) {
                // read objSize
                int objSize = mappedByteBuffer.getInt();

                pointer = DirectObjectPointer.createFromMappedByteBuffer(mappedByteBuffer, objSize);
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

    private static DirectObjectPointer createFromFileChannel(FileChannel fc, int objSize) throws IOException {
        DirectObjectPointer p = new DirectObjectPointer(objSize);
        fc.position(fc.position() - 4); // rewind objectSize
        fc.read(p.getAsByteBuffer());

        return p;
    }

    private static DirectObjectPointer createFromMappedByteBuffer(MappedByteBuffer map, int objSize) {
        DirectObjectPointer p = new DirectObjectPointer(objSize);

        try {
            // copy memory from MappedByteBuffer to native memory
            long mapAddress = Utils.UNSAFE.getLong(map, Utils.BUFFER_ADDRESS_OFFSET);
            Utils.UNSAFE.copyMemory(null, mapAddress, null, p.address, objSize + 4);

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

    private ByteBuffer getAsByteBuffer() {
        if (address == 0) return null;

        try {
            // instanciate a DirectByteBuffer which wraps the memory block including objectSize
            return (ByteBuffer) Utils.DIRECT_BYTE_BUFFER_CONSTRUCTOR.newInstance(address, getObjectSize() + 4);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instanciate DirectByteBuffer");
        }
    }

    public void write(FileChannel fc) throws IOException {
        ByteBuffer buffer = getAsByteBuffer();
        fc.write(buffer);
    }

    public void write(MappedByteBuffer mappedByteBuffer) throws IOException {
        ByteBuffer buffer = getAsByteBuffer();
        mappedByteBuffer.put(buffer);
    }
}
