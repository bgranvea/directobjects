package com.granveaud.directobjects.map;

import com.granveaud.directobjects.beans.Bean1;
import com.granveaud.directobjects.map.DirectMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirectMapTest {
    @Test
    public void test1() {
        DirectMap<Integer, Bean1> map = new DirectMap<Integer, Bean1>();

        // insert elements (reuse same bean to avoid GC)
        Bean1 b = new Bean1();
        for (int i = 0; i < 1000; i++) {
            b.setStr1(Integer.toString(i));
            b.setStr2("123456");

            map.put(i, b);
        }

        // replace elements
        for (int i = 0; i < 1000; i++) {
            b.setStr1(Integer.toString(i));
            b.setStr2("12345678");

            map.put(i, b);
        }

        // get
        for (int i = 0; i < 1000; i++) {
            // reset bean to be sure values are loaded from native memory
            b.setStr1(null);
            b.setStr2(null);

            boolean result = map.get(i, b);
            assertTrue(result);
            assertEquals(b.getStr1(), Integer.toString(i));
            assertEquals(b.getStr2(), "12345678");
        }

        // remove and free
        for (int i = 0; i < 1000; i++) {
            boolean result = map.remove(i);
            assertTrue(result);
        }

        assertTrue(map.size() == 0);
    }
}
