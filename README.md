DirectObjects
=============

A simple library to manipulate off-heap objects. Just define a bean which extends DirectObject
and implements methods to serialize it to native memory and unserialize it.

You can then do the following:

    // init bean and save to native memory
    MyBean b = new MyBean();
    b.setXXX(...);
    b.save();

    // get a pointer to the native memory allocated by save
    DirectObjectPointer p = b.getPointer();

    // reload these values in another bean
    MyBean b2 = new MyBean();
    b2.attach(p);
    b2.load();

    // b and b2 fields have same values

    // free native memory
    p.free();

This way, you can reduce use of heap memory by storing your beans in native memory. You just
have to keep a reference to DirectObjectPointer, which is a lightweight object, to be able
to reload the bean values.

Don't forget to free native memory when not needed anymore to avoid memory leaks!

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

TODO:
=====
- support old version of OpenJDK 6 and Sun JDK 6 which don't have Unsafe.copyMemory
- beans should implement an interface DirectObject instead of having to extend an abstract class
- implement a DirectList
- allow partial loading of objets from native memory when only some values are needed
- support auto-freeing of native memory