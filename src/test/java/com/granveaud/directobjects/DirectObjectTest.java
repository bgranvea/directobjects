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
    @Test
    public void test1() {
        // init bean and save to native memory
        Bean1 b1 = new Bean1();
        b1.setStr1(getStringAllCodes());
        b1.setStr2(getStringAllCodes());
        b1.setStr3(getStringAllASCIICodes());
        b1.save();

        // get a pointer to the native memory
        DirectObjectPointer p = b1.getPointer();

        // reload in another bean
        Bean1 b2 = new Bean1();
        b2.attach(p);
        b2.load();

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
        b1.save();

        // get a pointer to the native memory
        DirectObjectPointer p = b1.getPointer();

        // reload in another bean
        Bean1 b2 = new Bean1();
        b2.attach(p);
        b2.load();

        // change b2 content and save it
        b2.setStr1("123");
        b2.save();

        assertTrue(p == b2.getPointer());

        // reload in another bean
        Bean1 b3 = new Bean1();
        b3.attach(p);
        b3.load();

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
        b1.save();

        DirectObjectPointer p = b1.getPointer();

        // get the byte[]
        byte[] data = p.getAsBytes();

        // free native memory
        p.free();

        // load byte[] in new memory block
        DirectObjectPointer p2 = DirectObjectPointer.fromBytes(data);

        // reload
        Bean1 b2 = new Bean1();
        b2.attach(p2);
        b2.load();

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
        b1.save();

        DirectObjectPointer p = b1.getPointer();

        // save to temp file with NIO
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc1 = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = p.getAsByteBuffer();
        fc1.write(buffer);
        fc1.close();

        // reload from file with NIO
        FileChannel fc2 = new FileInputStream(tempFile).getChannel();
        DirectObjectPointer p2 = DirectObjectPointer.fromFileChannel(fc2, (int) tempFile.length());
        fc2.close();

        // reload
        Bean1 b2 = new Bean1();
        b2.attach(p2);
        b2.load();

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
        b1.save();

        DirectObjectPointer p = b1.getPointer();

        // save to temp file with memory mapped file
        File file = File.createTempFile("directobjecttest", null);
        RandomAccessFile tempFile1 = new RandomAccessFile(file, "rw");
        FileChannel fc1 = tempFile1.getChannel();
        MappedByteBuffer mem1 = fc1.map(FileChannel.MapMode.READ_WRITE, 0, p.getSize());
        mem1.order(ByteOrder.nativeOrder());
        mem1.put(p.getAsByteBuffer());
        fc1.close();
        tempFile1.close();

        // reload from file with memory mapped file
        RandomAccessFile tempFile2 = new RandomAccessFile(file, "r");
        FileChannel fc2 = tempFile2.getChannel();
        MappedByteBuffer mem2 = fc2.map(FileChannel.MapMode.READ_ONLY, 0, tempFile2.length());
        mem2.order(ByteOrder.nativeOrder());
        DirectObjectPointer p2 = DirectObjectPointer.fromMappedByteBuffer(mem2, (int) tempFile2.length());
        fc2.close();
        tempFile2.close();

        // reload
        Bean1 b2 = new Bean1();
        b2.attach(p2);
        b2.load();

        // compare beans
        assertEquals(b1.getStr1(), b2.getStr1());
        assertEquals(b1.getStr2(), b2.getStr2());
        assertEquals(b1.getStr3(), b2.getStr3());
    }

    @Test
    public void bench() throws IOException {
        MetricRegistry metrics = new MetricRegistry();

        Histogram histo1 = metrics.histogram("bench1");
        for (int i = 0; i < 20; i++) {
            bench1(1000, histo1);
        }
        MetricsUtils.displayHistoResults("bench1", histo1, "ms");

        Histogram histo2 = metrics.histogram("bench2");
        for (int i = 0; i < 20; i++) {
            bench2(1000, histo2);
        }
        MetricsUtils.displayHistoResults("bench2", histo2, "ms");

        Histogram histo3 = metrics.histogram("bench3");
        for (int i = 0; i < 5; i++) {
            bench3(1000, histo3);
        }
        MetricsUtils.displayHistoResults("bench3", histo3, "ms");

        Histogram histo4 = metrics.histogram("bench4");
        for (int i = 0; i < 5; i++) {
            bench4(1000, histo4);
        }
        MetricsUtils.displayHistoResults("bench4", histo4, "ms");

        Histogram histo5 = metrics.histogram("bench5");
        for (int i = 0; i < 5; i++) {
            bench5(1000, histo5);
        }
        MetricsUtils.displayHistoResults("bench5", histo5, "ms");
    }

    // bench serializing a bean to native memory
    private void bench1(int count, Histogram histo) {
        String str1 = getStringAllCodes();
        String str3 = getStringAllASCIICodes();

        List<DirectObjectPointer> pointers = new ArrayList<DirectObjectPointer>();
        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();

            // create and save bean
            Bean1 b = new Bean1();
            b.setStr1(str1);
            b.setStr2(str1);
            b.setStr3(str3);
            b.save();

            pointers.add(b.getPointer());

            long dtime = System.nanoTime() - time0;
            histo.update(dtime / 1000);
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
            b.save();

            pointers.add(b.getPointer());
        }

        for (DirectObjectPointer p : pointers) {
            long time0 = System.nanoTime();

            // load bean from pointer
            Bean1 b = new Bean1();
            b.attach(p);
            b.load();

            long dtime = System.nanoTime() - time0;
            histo.update(dtime / 1000);
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
            b.save();

            pointers.add(b.getPointer());
        }

        // save to a file
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(4);

        for (DirectObjectPointer p : pointers) {
            long time0 = System.nanoTime();

            buffer.clear();
            buffer.putInt(p.getSize());
            fc.write(buffer);

            ByteBuffer pbuffer = p.getAsByteBuffer();
            fc.write(pbuffer);

            long dtime = System.nanoTime() - time0;
            histo.update(dtime / 1000);
        }

        fc.close();

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
            b.save();

            pointers.add(b.getPointer());
        }

        // save to a file
        File tempFile = File.createTempFile("directobjecttest", null);
        FileChannel fc1 = new FileOutputStream(tempFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(4);

        for (DirectObjectPointer p : pointers) {
            buffer.clear();
            buffer.putInt(p.getSize());
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

        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();
            fc2.read(buffer);

            buffer.clear();
            int size = buffer.getInt();

            DirectObjectPointer p = DirectObjectPointer.fromFileChannel(fc2, size);
            pointers.add(p);

            long dtime = System.nanoTime() - time0;
            histo.update(dtime / 1000);
        }

        fc2.close();

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
            b.save();

            pointers.add(b.getPointer());
            totalSize += 4 + 4 + b.getPointer().getSize();
        }

        // save to temp file with memory mapped file
        File file = File.createTempFile("directobjecttest", null);

        RandomAccessFile tempFile1 = new RandomAccessFile(file, "rw");
        FileChannel fc1 = tempFile1.getChannel();
        MappedByteBuffer mem1 = fc1.map(FileChannel.MapMode.READ_WRITE, 0, totalSize);
        mem1.order(ByteOrder.nativeOrder());

        for (DirectObjectPointer p : pointers) {
            mem1.putInt(p.getSize());
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

        for (int i = 0; i < count; i++) {
            long time0 = System.nanoTime();

            int size = mem2.getInt();
            DirectObjectPointer p = DirectObjectPointer.fromMappedByteBuffer(mem2, size);
            pointers.add(p);

            long dtime = System.nanoTime() - time0;
            histo.update(dtime / 1000);
        }

        fc2.close();
        tempFile2.close();

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
