package com.granveaud.directobjects;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnsafeTest {
    enum InstructionType {
        BYTE(1), SHORT(2), CHAR(2), INT(4), LONG(8);

        final public int length;

        InstructionType(int length) {
            this.length = length;
        }
    }

    @Test
    public void bench() {
        int bufferLength = 10 * 1024 * 1024;

        ByteBuffer bb1 = ByteBuffer.allocateDirect(bufferLength).order(ByteOrder.nativeOrder());
        ByteBuffer bb2 = ByteBuffer.allocateDirect(bufferLength).order(ByteOrder.nativeOrder());

        MetricRegistry metrics = new MetricRegistry();

        for (InstructionType type : InstructionType.values()) {
            for (int shift = 0; shift < 8; shift++) {
                String name = "bench1-" + type + "-shift" + shift;
                Histogram histo = metrics.histogram(name);
                for (int i = 0; i < 50; i++) {
                    runTest1(bb1, bb2, type, histo, bufferLength / type.length - shift, shift);
                }
                MetricsUtils.displayHistoResults(name, histo, "ps");
            }
        }

        for (InstructionType type : InstructionType.values()) {
            for (int shift = 0; shift < 8; shift++) {
                String name = "bench2-" + type + "-shift" + shift;
                Histogram histo = metrics.histogram(name);
                for (int i = 0; i < 50; i++) {
                    runTest2(bb1, bb2, type, histo, bufferLength / type.length - shift, shift);
                }
                MetricsUtils.displayHistoResults(name, histo, "ps");
            }
        }
    }

    private void runTest1(ByteBuffer bb1, ByteBuffer bb2, InstructionType type, Histogram histo, int count, int shift) {
        bb1.clear();
        bb2.clear();

        bb1.position(shift);
        bb2.position(shift);

        long time0 = System.nanoTime();
        int n = count;
        switch (type) {
            case BYTE:
                while (n > 0) {
                    bb2.put(bb1.get());
                    n--;
                }
                break;

            case SHORT:
                while (n > 0) {
                    bb2.putShort(bb1.getShort());
                    n--;
                }
                break;

            case CHAR:
                while (n > 0) {
                    bb2.putChar(bb1.getChar());
                    n--;
                }
                break;

            case INT:
                while (n > 0) {
                    bb2.putInt(bb1.getInt());
                    n--;
                }
                break;

            case LONG:
                while (n > 0) {
                    bb2.putLong(bb1.getLong());
                    n--;
                }
                break;
        }

        long dtime = System.nanoTime() - time0;

        histo.update((1000 * dtime) / (count * 2));
    }

    private void runTest2(ByteBuffer bb1, ByteBuffer bb2, InstructionType type, Histogram histo, int count, int shift) {
        bb1.clear();
        bb2.clear();

        long src = ((DirectBuffer) bb1).address() + shift;
        long dest = ((DirectBuffer) bb2).address() + shift;

        long time0 = System.nanoTime();
        int n = count;
        switch (type) {
            case BYTE:
                while (n > 0) {
                    Utils.UNSAFE.putByte(dest, Utils.UNSAFE.getByte(src));
                    dest++;
                    src++;
                    n--;
                }
                break;

            case SHORT:
                while (n > 0) {
                    Utils.UNSAFE.putShort(dest, Utils.UNSAFE.getShort(src));
                    dest += 2;
                    src += 2;
                    n--;
                }
                break;

            case CHAR:
                while (n > 0) {
                    Utils.UNSAFE.putChar(dest, Utils.UNSAFE.getChar(src));
                    dest += 2;
                    src += 2;
                    n--;
                }
                break;

            case INT:
                while (n > 0) {
                    Utils.UNSAFE.putInt(dest, Utils.UNSAFE.getInt(src));
                    dest += 4;
                    src += 4;
                    n--;
                }
                break;

            case LONG:
                while (n > 0) {
                    Utils.UNSAFE.putLong(dest, Utils.UNSAFE.getLong(src));
                    dest += 8;
                    src += 8;
                    n--;
                }
                break;
        }

        long dtime = System.nanoTime() - time0;

        histo.update((1000 * dtime) / (count * 2));
    }
}
