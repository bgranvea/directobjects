package com.granveaud.directobjects;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.granveaud.directobjects.beans.Bean1;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirectObjectTest {
    final static private int BENCH_WARMUP = 5;
    final static private int BENCH_LOOPS = 30;
    final static private int BENCH_LOOPS2 = 5;

    @Test
    public void test1() {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());

        DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

        // reload in another bean
        Bean1 b2 = new Bean1();
        p.populateBean(b2);

        // free native memory
        p.free();

        // compare beans
        assertEquals(b1.getStr1(), b2.getStr1());
        assertEquals(b1.getStr2(), b2.getStr2());
        assertEquals(b1.getStr3(), b2.getStr3());
    }

    @Test
    public void test2() {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());

        DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

        // reload in another bean
        Bean1 b2 = new Bean1();
        p.populateBean(b2);

        // change b2 content and update native memory
        b2.setStr1("123");
        p.updateFromBean(b2);

        // reload in another bean
        Bean1 b3 = new Bean1();
        p.populateBean(b3);

        // free native memory
        p.free();

        // compare beans
        assertEquals(b3.getStr1(), b2.getStr1());
        assertEquals(b3.getStr2(), b2.getStr2());
        assertEquals(b3.getStr3(), b2.getStr3());
    }

    @Test
    public void test3() {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());

        DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

        // get the byte[]
        byte[] data = p.getAsBytes();

        // free native memory
        p.free();

        // load byte[] in new memory block
        DirectObjectPointer p2 = new DirectObjectPointer.Builder().fromBytes(data).build();

        // reload
        Bean1 b2 = new Bean1();
        p2.populateBean(b2);

        // compare beans
        assertEquals(b1.getStr1(), b2.getStr1());
        assertEquals(b1.getStr2(), b2.getStr2());
        assertEquals(b1.getStr3(), b2.getStr3());
    }

    @Test
    public void test4() throws IOException {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());

        DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

        // save to temp file with NIO
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc1 = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = p.getAsByteBuffer();
        fc1.write(buffer);
        fc1.close();

        // reload from file with NIO
        FileChannel fc2 = new FileInputStream(tempFile).getChannel();
        DirectObjectPointer p2 = new DirectObjectPointer.Builder().fromFileChannel(fc2).withObjectSize((int) tempFile.length()).build();
        fc2.close();

        // reload
        Bean1 b2 = new Bean1();
        p2.populateBean(b2);

        tempFile.delete();

        // compare beans
        assertEquals(b1.getStr1(), b2.getStr1());
        assertEquals(b1.getStr2(), b2.getStr2());
        assertEquals(b1.getStr3(), b2.getStr3());
    }


    @Test
    public void test5() throws IOException {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());

        DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

        // save to temp file with memory mapped file
        File file = File.createTempFile("directobjecttest", null);
        RandomAccessFile tempFile1 = new RandomAccessFile(file, "rw");
        FileChannel fc1 = tempFile1.getChannel();
        MappedByteBuffer mem1 = fc1.map(FileChannel.MapMode.READ_WRITE, 0, p.getObjectSize());
        mem1.order(ByteOrder.nativeOrder());
        mem1.put(p.getAsByteBuffer());
        fc1.close();
        tempFile1.close();

        // reload from file with memory mapped file
        RandomAccessFile tempFile2 = new RandomAccessFile(file, "r");
        FileChannel fc2 = tempFile2.getChannel();
        MappedByteBuffer mem2 = fc2.map(FileChannel.MapMode.READ_ONLY, 0, tempFile2.length());
        mem2.order(ByteOrder.nativeOrder());
        DirectObjectPointer p2 = new DirectObjectPointer.Builder().fromMappedByteBuffer(mem2).withObjectSize((int) tempFile2.length()).build();
        fc2.close();
        tempFile2.close();

        file.delete();

        // reload
        Bean1 b2 = new Bean1();
        p2.populateBean(b2);

        // compare beans
        assertEquals(b1.getStr1(), b2.getStr1());
        assertEquals(b1.getStr2(), b2.getStr2());
        assertEquals(b1.getStr3(), b2.getStr3());
    }

    @Test
    public void bench() throws IOException {
        MetricRegistry metrics = new MetricRegistry();

        Histogram histo1 = metrics.histogram("bench1");
        for (int i = -BENCH_WARMUP; i < BENCH_LOOPS; i++) {
            bench1(1000, i >= 0 ? histo1 : null);
        }
        MetricsUtils.displayHistoResults("bench1", histo1, "ms");

        Histogram histo2 = metrics.histogram("bench2");
        for (int i = -BENCH_WARMUP; i < BENCH_LOOPS; i++) {
            bench2(1000, i >= 0 ? histo2 : null);
        }
        MetricsUtils.displayHistoResults("bench2", histo2, "ms");

        Histogram histo3 = metrics.histogram("bench3");
        for (int i = -BENCH_WARMUP; i < BENCH_LOOPS2; i++) {
            bench3(1000, i >= 0 ? histo3 : null);
        }
        MetricsUtils.displayHistoResults("bench3", histo3, "ms");

        Histogram histo4 = metrics.histogram("bench4");
        for (int i = -BENCH_WARMUP; i < BENCH_LOOPS2; i++) {
            bench4(1000, i >= 0 ? histo4 : null);
        }
        MetricsUtils.displayHistoResults("bench4", histo4, "ms");

        Histogram histo5 = metrics.histogram("bench5");
        for (int i = -BENCH_WARMUP; i < BENCH_LOOPS2; i++) {
            bench5(1000, i >= 0 ? histo5 : null);
        }
        MetricsUtils.displayHistoResults("bench5", histo5, "ms");
    }

    // bench serializing a bean to native memory
    private void bench1(int count, Histogram histo) {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        // create and save bean. Reuse bean and DirectObjectContext for performance
        Bean1 b = new Bean1();
        DirectObjectContext doContext = new DirectObjectContext();

        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();

            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);

            DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b).withContext(doContext).build();
            pointers.add(p);

            long dtime = System.nanoTime() - time0;
            if (histo != null) histo.update(dtime / 1000);
        }

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
    }

    // bench unserializing a bean from native memory
    private void bench2(int count, Histogram histo) {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            // create and save bean
            Bean1 b = new Bean1();
            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);

            DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b).build();
            pointers.add(p);
        }

        for (DirectObjectPointer p : pointers) {
            long time0 = System.nanoTime();

            // load bean from pointer
            Bean1 b = new Bean1();
            p.populateBean(b);

            long dtime = System.nanoTime() - time0;
            if (histo != null) histo.update(dtime / 1000);
        }

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
    }

    // bench saving a serialized bean to a file
    private void bench3(int count, Histogram histo) throws IOException {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            // create and save bean to memory
            Bean1 b = new Bean1();
            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);

            DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b).build();
            pointers.add(p);
        }

        // save to a file
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(4);

        for (DirectObjectPointer p : pointers) {
            long time0 = System.nanoTime();

            buffer.clear();
            buffer.putInt(p.getObjectSize());
            fc.write(buffer);

            ByteBuffer pbuffer = p.getAsByteBuffer();
            fc.write(pbuffer);

            long dtime = System.nanoTime() - time0;
            if (histo != null) histo.update(dtime / 1000);
        }

        fc.close();
        tempFile.delete();

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
    }

    // bench loading a serialized bean from a file
    private void bench4(int count, Histogram histo) throws IOException {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            // create and save bean to memory
            Bean1 b = new Bean1();
            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);

            DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b).build();
            pointers.add(p);
        }

        // save to a file
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc1 = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(4);

        for (DirectObjectPointer p : pointers) {
            buffer.clear();
            buffer.putInt(p.getObjectSize());
            fc1.write(buffer);

            ByteBuffer pbuffer = p.getAsByteBuffer();
            fc1.write(pbuffer);
        }
        fc1.close();

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
        pointers.clear();

        // reload from file
        FileChannel fc2 = new FileInputStream(tempFile).getChannel();

        // reuse builder and context to avoid GC
        DirectObjectPointer.Builder builder = new DirectObjectPointer.Builder().fromFileChannel(fc2).withContext(new DirectObjectContext());
        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();
            fc2.read(buffer);

            buffer.clear();
            int size = buffer.getInt();

            DirectObjectPointer p = builder.withObjectSize(size).build();
            pointers.add(p);

            long dtime = System.nanoTime() - time0;
            if (histo != null) histo.update(dtime / 1000);
        }

        fc2.close();
        tempFile.delete();

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
    }

    // bench loading a serialized bean from a memory mapped file
    private void bench5(int count, Histogram histo) throws IOException {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        int totalSize = 0;
        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            // create and save bean to memory
            Bean1 b = new Bean1();
            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);

            DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b).build();
            pointers.add(p);

            totalSize += 4 + 4 + p.getObjectSize();
        }

        // save to temp file with memory mapped file
        File file = File.createTempFile("directobjecttest", null);

        RandomAccessFile tempFile1 = new RandomAccessFile(file, "rw");
        FileChannel fc1 = tempFile1.getChannel();
        MappedByteBuffer mem1 = fc1.map(FileChannel.MapMode.READ_WRITE, 0, totalSize);
        mem1.order(ByteOrder.nativeOrder());

        for (DirectObjectPointer p : pointers) {
            mem1.putInt(p.getObjectSize());
            mem1.put(p.getAsByteBuffer());
        }

        fc1.close();
        tempFile1.close();

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
        pointers.clear();

        // reload from file with memory mapped file
        RandomAccessFile tempFile2 = new RandomAccessFile(file, "r");
        FileChannel fc2 = tempFile2.getChannel();
        MappedByteBuffer mem2 = fc2.map(FileChannel.MapMode.READ_ONLY, 0, tempFile2.length());
        mem2.order(ByteOrder.nativeOrder());

        // reuse builder and context to avoid GC
        DirectObjectPointer.Builder builder = new DirectObjectPointer.Builder().fromMappedByteBuffer(mem2).withContext(new DirectObjectContext());
        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();

            int size = mem2.getInt();
            DirectObjectPointer p = builder.withObjectSize(size).build();
            pointers.add(p);

            long dtime = System.nanoTime() - time0;
            if (histo != null) histo.update(dtime / 1000);
        }

        fc2.close();
        tempFile2.close();

        file.delete();

        // free memory
        for (DirectObjectPointer p : pointers) {
            p.free();
        }
    }

    private String getStringAllCodes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 0xffff; i++) {
            sb.append((char) i);
        }

        return sb.toString();
    }

    private String getStringAllASCIICodes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 0x7f; i++) {
            sb.append((char) i);
        }

        return sb.toString();
    }
}
