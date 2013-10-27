package com.granveaud.directobjects.map;

import com.granveaud.directobjects.DirectObject;
import com.granveaud.directobjects.DirectObjectContext;
import com.granveaud.directobjects.DirectObjectPointer;

import java.util.HashMap;
import java.util.Map;

public class DirectMap<K, V extends DirectObject> {

    private Map<K, DirectObjectPointer> pointerMap;

    public DirectMap() {
        pointerMap = new HashMap<K, DirectObjectPointer>();
    }

    public void clear() {
        for (DirectObjectPointer p : pointerMap.values()) {
            p.free();
        }
        pointerMap.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        // free all native pointers
        clear();
    }

    public int size() {
        return pointerMap.size();
    }

    public void put(K key, V value) {
        // check existing pointer
        DirectObjectPointer existingPointer = pointerMap.get(key);
        if (existingPointer != null) {
            // update native memory
            existingPointer.updateFromBean(value);
        } else {
            // alloc new memory block
            DirectObjectPointer newPointer = new DirectObjectPointer.Builder().fromBean(value).build();

            // put pointer in map
            pointerMap.put(key, newPointer);
        }
    }

    public boolean get(K key, V value) {
        DirectObjectPointer pointer = pointerMap.get(key);
        if (pointer == null) return false;

        // load from native memory
        pointer.populateBean(value);

        return true;
    }

    public boolean remove(Object key) {
        // free existing pointer and remove from map
        DirectObjectPointer existingPointer = pointerMap.get(key);
        if (existingPointer != null) {
            existingPointer.free();
            pointerMap.remove(key);

            return true;
        }

        return false;
    }
}
