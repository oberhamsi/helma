// LruHashtable - a Hashtable that expires least-recently-used objects
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

// Moved to helma.util to use java.util.HashMap instead of java.util.Hashtable
package helma.util;

import java.util.*;

/// A Hashtable that expires least-recently-used objects.
// <P>
// Use just like java.util.Hashtable, except that the initial-capacity
// parameter is required.  Instead of growing bigger than that size,
// it will throw out objects that haven't been looked at in a while.
// <P>
// <A HREF="/resources/classes/Acme/LruHashtable.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see java.util.Hashtable

public class CacheMap  {

    // Number of buckets.
    private static final int nBuckets = 2;

    // Load factor.
    private float loadFactor;

    // When count exceeds this threshold, expires the old table.
    private int threshold;

    // Capacity of each bucket.
    private int eachCapacity;

    // The tables.
    private HashMap oldTable;
    private HashMap newTable;

    /// Constructs a new, empty hashtable with the specified initial 
    // capacity and the specified load factor.
    // Unlike a plain Hashtable, an LruHashtable will never grow or
    // shrink from this initial capacity.
    // @param initialCapacity the initial number of buckets
    // @param loadFactor a number between 0.0 and 1.0, it defines
    //		the threshold for expiring old entries
    // @exception IllegalArgumentException If the initial capacity
    // is less than or equal to zero.
    // @exception IllegalArgumentException If the load factor is
    // less than or equal to zero.
    public CacheMap (int initialCapacity, float loadFactor) {
	// We have to call a superclass constructor, but we're not actually
	// going to use it at all.  The only reason we want to extend Hashtable
	// is for type conformance.  So, make a parent hash table of minimum
	// size and then ignore it.
	if ( initialCapacity <= 0 || loadFactor <= 0.0 )
	    throw new IllegalArgumentException();
	this.loadFactor = loadFactor;
	threshold = (int) (initialCapacity * loadFactor) - 1;
	eachCapacity = initialCapacity / nBuckets + 1;
	oldTable = new HashMap (eachCapacity, loadFactor);
	newTable = new HashMap (eachCapacity, loadFactor);
	// System.out.println ("CREATED NEW CACHEMAP: CAP="+initialCapacity+" EACH="+eachCapacity+"  THRES="+threshold);
    }

    /// Constructs a new, empty hashtable with the specified initial 
    // capacity.
    // Unlike a plain Hashtable, an LruHashtable will never grow or
    // shrink from this initial capacity.
    // @param initialCapacity the initial number of buckets
    public CacheMap (int initialCapacity) {
	this (initialCapacity, 0.75F);
    }

    /// Returns the number of elements contained in the hashtable. 
    public int size() {
	return newTable.size() + oldTable.size();
    }

    /// Returns true if the hashtable contains no elements.
    public boolean isEmpty() {
	return size() == 0;
    }


    /// Returns true if the specified object is an element of the hashtable.
    // This operation is more expensive than the containsKey() method.
    // @param value the value that we are looking for
    // @exception NullPointerException If the value being searched 
    // for is equal to null.
    // @see LruHashtable#containsKey
    public synchronized boolean containsValue (Object value) {
	if (newTable.containsValue (value))
	    return true;
	if (oldTable.containsValue (value)) {
	    // We would like to move the object from the old table to the
	    // new table.  However, we need keys to re-add the objects, and
	    // there's no good way to find all the keys for the given object.
	    // We'd have to enumerate through all the keys and check each
	    // one.  Yuck.  For now we just punt.  Anyway, contains() is
	    // probably not a commonly-used operation.
	    return true;
	}
	return false;
    }

    /// Returns true if the collection contains an element for the key.
    // @param key the key that we are looking for
    // @see LruHashtable#contains
    public synchronized boolean containsKey (Object key) {
	if (newTable.containsKey(key))
	    return true;
	if (oldTable.containsKey (key)) {
	    // Move object from old table to new table.
	    Object value = oldTable.get (key);
	    newTable.put (key, value);
	    oldTable.remove (key);
	    return true;
	}
	return false;
    }

    /// Gets the object associated with the specified key in the 
    // hashtable.
    // @param key the specified key
    // @returns the element for the key or null if the key
    // 		is not defined in the hash table.
    // @see LruHashtable#put
    public synchronized Object get (Object key) {
	Object value;
	value = newTable.get (key);
	if (value != null)
	    return value;
	value = oldTable.get (key);
	if (value != null) {
	    // Move object from old table to new table.
	    newTable.put (key, value);
	    oldTable.remove (key);
	    return value;
	}
	return null;
    }

    /// Puts the specified element into the hashtable, using the specified
    // key.  The element may be retrieved by doing a get() with the same key.
    // The key and the element cannot be null. 
    // @param key the specified key in the hashtable
    // @param value the specified element
    // @exception NullPointerException If the value of the element 
    // is equal to null.
    // @see LruHashtable#get
    // @return the old value of the key, or null if it did not have one.
    public synchronized Object put (Object key, Object value) {

	if (key instanceof helma.objectmodel.Key && value instanceof helma.objectmodel.db.Node) {
	    helma.objectmodel.Key k = (helma.objectmodel.Key) key;
	    helma.objectmodel.db.Node n = (helma.objectmodel.db.Node) value;
	    helma.objectmodel.DbMapping dbm = n.getDbMapping ();
	    String t1 = k.getType ();
	    String t2 = dbm == null ? null: dbm.getTypeName();
	    if (t1 != t2 && (t1 == null || !t1.equals (t2))) {
	        helma.objectmodel.IServer.getLogger().log ("WARNING: "+t1+" != "+t2);
	        System.err.println ("WARNING: "+t1+" != "+t2);
	        Thread.dumpStack ();
	    }
	}

	Object oldValue = newTable.put (key, value);
	if (oldValue != null)
	    return oldValue;
	oldValue = oldTable.get (key);
	if (oldValue != null)
	    oldTable.remove( key );
	else {
	    if (size() >= threshold) {
		// Rotate the tables.
		oldTable = newTable;
		newTable = new HashMap (eachCapacity, loadFactor);
	    }
	}
	return oldValue;
    }

    /// Removes the element corresponding to the key. Does nothing if the
    // key is not present.
    // @param key the key that needs to be removed
    // @return the value of key, or null if the key was not found.
    public synchronized Object remove (Object key) {
	Object oldValue = newTable.remove (key);
	if (oldValue == null)
	    oldValue = oldTable.remove (key);
	return oldValue;
    }

    /// Clears the hash table so that it has no more elements in it.
    public synchronized void clear() {
	newTable.clear ();
	oldTable.clear ();
    }


}


