/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import com.google.common.collect.Sets;
import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.exception.PersistitException;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * This cache is not thread-safe, due to direct usage of {@link com.persistit.Exchange}
 * </p>
 */
public class Cache<V extends Serializable> {

  private final String name;
  private final Exchange exchange;

  Cache(String name, Exchange exchange) {
    this.name = name;
    this.exchange = exchange;
  }

  public Cache put(Object key, V value) {
    resetKey(key);
    return doPut(value);
  }

  public Cache put(Object firstKey, Object secondKey, V value) {
    resetKey(firstKey, secondKey);
    return doPut(value);
  }

  public Cache put(Object firstKey, Object secondKey, Object thirdKey, V value) {
    resetKey(firstKey, secondKey, thirdKey);
    return doPut(value);
  }

  public Cache put(Object[] key, V value) {
    resetKey(key);
    return doPut(value);
  }

  private Cache doPut(V value) {
    try {
      exchange.getValue().put(value);
      exchange.store();
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to put element in the cache " + name, e);
    }
  }

  /**
   * Returns the value object associated with keys, or null if not found.
   */
  public V get(Object key) {
    resetKey(key);
    return doGet();
  }

  /**
   * Returns the value object associated with keys, or null if not found.
   */
  @CheckForNull
  public V get(Object firstKey, Object secondKey) {
    resetKey(firstKey, secondKey);
    return doGet();
  }

  /**
   * Returns the value object associated with keys, or null if not found.
   */
  @CheckForNull
  public V get(Object firstKey, Object secondKey, Object thirdKey) {
    resetKey(firstKey, secondKey, thirdKey);
    return doGet();
  }

  /**
   * Returns the value object associated with keys, or null if not found.
   */
  @CheckForNull
  public V get(Object[] key) {
    resetKey(key);
    return doGet();
  }

  @CheckForNull
  private V doGet() {
    try {
      exchange.fetch();
      if (!exchange.getValue().isDefined()) {
        return null;
      }
      return (V) exchange.getValue().get();
    } catch (Exception e) {
      // TODO add parameters to message
      throw new IllegalStateException("Fail to get element from cache " + name, e);
    }
  }

  public boolean containsKey(Object key) {
    resetKey(key);
    return doContainsKey();
  }

  public boolean containsKey(Object firstKey, Object secondKey) {
    resetKey(firstKey, secondKey);
    return doContainsKey();
  }

  public boolean containsKey(Object firstKey, Object secondKey, Object thirdKey) {
    resetKey(firstKey, secondKey, thirdKey);
    return doContainsKey();
  }

  public boolean containsKey(Object[] key) {
    resetKey(key);
    return doContainsKey();
  }

  private boolean doContainsKey() {
    try {
      exchange.fetch();
      return exchange.isValueDefined();
    } catch (Exception e) {
      // TODO add parameters to message
      throw new IllegalStateException("Fail to check if element is in cache " + name, e);
    }
  }

  public boolean remove(Object key) {
    resetKey(key);
    return doRemove();
  }

  public boolean remove(Object firstKey, Object secondKey) {
    resetKey(firstKey, secondKey);
    return doRemove();
  }

  public boolean remove(Object firstKey, Object secondKey, Object thirdKey) {
    resetKey(firstKey, secondKey, thirdKey);
    return doRemove();
  }

  public boolean remove(Object[] key) {
    resetKey(key);
    return doRemove();
  }

  private boolean doRemove() {
    try {
      return exchange.remove();
    } catch (Exception e) {
      // TODO add parameters to message
      throw new IllegalStateException("Fail to get element from cache " + name, e);
    }
  }

  /**
   * Removes everything in the specified group.
   *
   * @param group The group name.
   */
  public Cache clear(Object key) {
    resetKey(key);
    return doClear();
  }

  public Cache clear(Object firstKey, Object secondKey) {
    resetKey(firstKey, secondKey);
    return doClear();
  }

  public Cache clear(Object firstKey, Object secondKey, Object thirdKey) {
    resetKey(firstKey, secondKey, thirdKey);
    return doClear();
  }

  public Cache clear(Object[] key) {
    resetKey(key);
    return doClear();
  }

  private Cache doClear() {
    try {
      Key to = new Key(exchange.getKey());
      to.append(Key.AFTER);
      exchange.removeKeyRange(exchange.getKey(), to);
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to clear values from cache " + name, e);
    }
  }


  /**
   * Clears the default as well as all group caches.
   */
  public void clear() {
    try {
      exchange.clear();
      exchange.removeAll();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to clear cache", e);
    }
  }

  /**
   * Returns the set of cache keys associated with this group.
   * TODO implement a lazy-loading equivalent with Iterator/Iterable
   *
   * @param group The group.
   * @return The set of cache keys for this group.
   */
  @SuppressWarnings("unchecked")
  public Set keySet(Object key) {
    try {
      Set<Object> keys = Sets.newLinkedHashSet();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(key);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        keys.add(iteratorExchange.getKey().indexTo(-1).decode());
      }
      return keys;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get keys from cache " + name, e);
    }
  }

  public Set keySet(Object firstKey, Object secondKey) {
    try {
      Set<Object> keys = Sets.newLinkedHashSet();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(firstKey);
      iteratorExchange.append(secondKey);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        keys.add(iteratorExchange.getKey().indexTo(-1).decode());
      }
      return keys;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get keys from cache " + name, e);
    }
  }

  /**
   * Returns the set of keys associated with this cache.
   *
   * @return The set containing the keys for this cache.
   */
  public Set<Object> keySet() {
    try {
      Set<Object> keys = Sets.newLinkedHashSet();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        keys.add(iteratorExchange.getKey().indexTo(-1).decode());
      }
      return keys;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get keys from cache " + name, e);
    }
  }

  /**
   * Lazy-loading values for a given key
   */
  public Iterable<V> values(Object key) {
    try {
      exchange.clear();
      exchange.append(key).append(Key.BEFORE);
      Exchange iteratorExchange = new Exchange(exchange);
      return new ValueIterable<V>(iteratorExchange, false);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get values from cache " + name, e);
    }
  }

  /**
   * Lazy-loading values
   */
  public Iterable<V> values() {
    try {
      exchange.clear().append(Key.BEFORE);
      Exchange iteratorExchange = new Exchange(exchange);
      return new ValueIterable<V>(iteratorExchange, true);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get values from cache " + name, e);
    }
  }

  public Iterable<Entry<V>> entries() {
    exchange.clear().to(Key.BEFORE);
    return new EntryIterable(new Exchange(exchange), true);
  }

  public Iterable<SubEntry<V>> subEntries(Object key) {
    exchange.clear().append(key).append(Key.BEFORE);
    return new SubEntryIterable(new Exchange(exchange), false);
  }

  private void resetKey(Object key) {
    exchange.clear();
    exchange.append(key);
  }

  private void resetKey(Object first, Object second) {
    exchange.clear();
    exchange.append(first).append(second);
  }

  private void resetKey(Object first, Object second, Object third) {
    exchange.clear();
    exchange.append(first).append(second).append(third);
  }

  private void resetKey(Object[] keys) {
    exchange.clear();
    for (Object o : keys) {
      exchange.append(o);
    }
  }


  //
  // LAZY ITERATORS AND ITERABLES
  //

  private static class ValueIterable<T extends Serializable> implements Iterable<T> {
    private final Iterator<T> iterator;

    private ValueIterable(Exchange exchange, boolean deep) {
      this.iterator = new ValueIterator<T>(exchange, deep);
    }

    @Override
    public Iterator<T> iterator() {
      return iterator;
    }
  }

  private static class ValueIterator<T extends Serializable> implements Iterator<T> {
    private final Exchange exchange;
    private final boolean deep;

    private ValueIterator(Exchange exchange, boolean deep) {
      this.exchange = exchange;
      this.deep = deep;
    }

    @Override
    public boolean hasNext() {
      try {
        return exchange.hasNext(deep);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public T next() {
      try {
        exchange.next(deep);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
      T value = null;
      if (exchange.getValue().isDefined()) {
        value = (T) exchange.getValue().get();
      }
      return value;
    }

    @Override
    public void remove() {
    }
  }

  private static class SubEntryIterable<T extends Serializable> implements Iterable<SubEntry<T>> {
    private final SubEntryIterator<T> it;

    private SubEntryIterable(Exchange exchange, boolean deep) {
      it = new SubEntryIterator<T>(exchange, deep);
    }

    @Override
    public Iterator<SubEntry<T>> iterator() {
      return it;
    }
  }

  private static class SubEntryIterator<T extends Serializable> implements Iterator<SubEntry<T>> {
    private final Exchange exchange;
    private final boolean deep;

    private SubEntryIterator(Exchange exchange, boolean deep) {
      this.exchange = exchange;
      this.deep = deep;
    }

    @Override
    public boolean hasNext() {
      try {
        return exchange.next(deep);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public SubEntry next() {
      Serializable value = null;
      if (exchange.getValue().isDefined()) {
        value = (Serializable) exchange.getValue().get();
      }
      Key key = exchange.getKey();
      return new SubEntry(key.indexTo(-1).decode(), value);
    }

    @Override
    public void remove() {
      // nothing to do
    }
  }

  public static class SubEntry<V extends Serializable> {
    private final Object key;
    private final V value;

    SubEntry(Object key, V value) {
      this.key = key;
      this.value = value;
    }

    public Object key() {
      return key;
    }

    public String keyAsString() {
      return (String) key;
    }

    @CheckForNull
    public V value() {
      return value;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }

  private static class EntryIterable<T extends Serializable> implements Iterable<Entry<T>> {
    private final EntryIterator<T> it;

    private EntryIterable(Exchange exchange, boolean deep) {
      it = new EntryIterator<T>(exchange, deep);
    }

    @Override
    public Iterator<Entry<T>> iterator() {
      return it;
    }
  }

  private static class EntryIterator<T extends Serializable> implements Iterator<Entry<T>> {
    private final Exchange exchange;
    private final boolean deep;

    private EntryIterator(Exchange exchange, boolean deep) {
      this.exchange = exchange;
      this.deep = deep;
    }

    @Override
    public boolean hasNext() {
      try {
        return exchange.next(deep);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public Entry next() {
      Serializable value = null;
      if (exchange.getValue().isDefined()) {
        value = (Serializable) exchange.getValue().get();
      }
      Key key = exchange.getKey();
      Object[] array = new Object[key.getDepth()];
      for (int i = 0; i < key.getDepth(); i++) {
        array[i] = key.indexTo(i - key.getDepth()).decode();
      }
      return new Entry(array, value);
    }

    @Override
    public void remove() {
      // nothing to do
    }
  }

  public static class Entry<V extends Serializable> {
    private final Object[] key;
    private final V value;

    Entry(Object[] key, V value) {
      this.key = key;
      this.value = value;
    }

    public Object[] key() {
      return key;
    }

    @CheckForNull
    public V value() {
      return value;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }
}
