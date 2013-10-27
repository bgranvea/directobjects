DirectObjects
=============

A simple library to manipulate off-heap objects. Just define a bean which extends DirectObject
and implements methods to serialize it to native memory and unserialize it.

You can then do the following:

    // init bean
    MyBean b = new MyBean();
    b.setXXX(...);

    // save it to native memory and get a pointer
    DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).build();

    // reload these values in another bean
    MyBean b2 = new MyBean();
    p.populateBean(b2);

    // b and b2 fields have same values

    // free native memory
    p.free();

This way, you can reduce use of heap memory by storing your beans in native memory. You just
have to keep a reference to DirectObjectPointer, which is a lightweight object, to be able
to reload the bean values.

Don't forget to free native memory when not needed anymore to avoid memory leaks!

You can also use autoRelease feature which will automatically free native memory when the DirectObjectPointer object
is finalized by JVM:

    DirectObjectPointer p = new DirectObjectPointer.Builder().fromBean(b1).withAutoRelease(true).build();

DirectMap
---------
A simple map which stores your beans in native memory.

Usage:

    DirectMap<String, MyBean> map = new DirectMap<String, MyBean>();

    // insert element
    MyBean b = new MyBean();
    b.setXXX(...);

    map.put("a", b);

    // get
    MyBean b2 = new MyBean();
    boolean result = map.get("a", b2);

    if (result) {
        // use b2
    }

    // remove and free native memory used by this entry
    boolean result = map.remove("a");

Note that DirectMap manages the native memory, so when a value is removed from the map, the corresponding memory block
is released.

TODO:
=====
- support old version of OpenJDK 6 and Sun JDK 6 which don't have Unsafe.copyMemory
- implement a DirectList
- allow partial loading of objets from native memory when only some values are needed
