/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.storage;

import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.KeyFilter;
import com.persistit.exception.PersistitException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * <p>
 * This storage is not thread-safe, due to direct usage of {@link com.persistit.Exchange}
 * </p>
 */
public class Storage<V> {

  private final String name;
  private final Exchange exchange;

  Storage(String name, Exchange exchange) {
    this.name = name;
    this.exchange = exchange;
  }

  public Storage<V> put(Object key, V value) {
    resetKey(key);
    return doPut(value);
  }

  public Storage<V> put(Object firstKey, Object secondKey, V value) {
    resetKey(firstKey, secondKey);
    return doPut(value);
  }

  public Storage<V> put(Object firstKey, Object secondKey, Object thirdKey, V value) {
    resetKey(firstKey, secondKey, thirdKey);
    return doPut(value);
  }

  public Storage<V> put(Object[] key, V value) {
    resetKey(key);
    return doPut(value);
  }

  private Storage<V> doPut(V value) {
    try {
      exchange.getValue().put(value);
      exchange.store();
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to put element in the storage '" + name + "'", e);
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

  @SuppressWarnings("unchecked")
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
  public Storage<V> clear(Object key) {
    resetKey(key);
    return doClear();
  }

  public Storage<V> clear(Object firstKey, Object secondKey) {
    resetKey(firstKey, secondKey);
    return doClear();
  }

  public Storage<V> clear(Object firstKey, Object secondKey, Object thirdKey) {
    resetKey(firstKey, secondKey, thirdKey);
    return doClear();
  }

  public Storage<V> clear(Object[] key) {
    resetKey(key);
    return doClear();
  }

  private Storage<V> doClear() {
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
  @SuppressWarnings("rawtypes")
  public Set keySet(Object key) {
    try {
      Set<Object> keys = new LinkedHashSet<>();
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

  @SuppressWarnings("rawtypes")
  public Set keySet(Object firstKey, Object secondKey) {
    try {
      Set<Object> keys = new LinkedHashSet<>();
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
      Set<Object> keys = new LinkedHashSet<>();
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
   * Lazy-loading values for given keys
   */
  public Iterable<V> values(Object firstKey, Object secondKey) {
    return new ValueIterable<>(exchange, firstKey, secondKey);
  }

  /**
   * Lazy-loading values for a given key
   */
  public Iterable<V> values(Object firstKey) {
    return new ValueIterable<>(exchange, firstKey);
  }

  /**
   * Lazy-loading values
   */
  public Iterable<V> values() {
    return new ValueIterable<>(exchange);
  }

  public Iterable<Entry<V>> entries() {
    return new EntryIterable<>(exchange);
  }

  public Iterable<Entry<V>> entries(Object firstKey) {
    return new EntryIterable<>(exchange, firstKey);
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

  private static class ValueIterable<T> implements Iterable<T> {
    private final Exchange originExchange;
    private final Object[] keys;

    private ValueIterable(Exchange originExchange, Object... keys) {
      this.originExchange = originExchange;
      this.keys = keys;
    }

    @Override
    public Iterator<T> iterator() {
      originExchange.clear();
      KeyFilter filter = new KeyFilter();
      for (Object key : keys) {
        originExchange.append(key);
        filter = filter.append(KeyFilter.simpleTerm(key));
      }
      originExchange.append(Key.BEFORE);
      Exchange iteratorExchange = new Exchange(originExchange);
      return new ValueIterator<>(iteratorExchange, filter);
    }
  }

  private static class ValueIterator<T> implements Iterator<T> {
    private final Exchange exchange;
    private final KeyFilter keyFilter;

    private ValueIterator(Exchange exchange, KeyFilter keyFilter) {
      this.exchange = exchange;
      this.keyFilter = keyFilter;
    }

    @Override
    public boolean hasNext() {
      try {
        return exchange.hasNext(keyFilter);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T next() {
      try {
        exchange.next(keyFilter);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
      if (exchange.getValue().isDefined()) {
        return (T) exchange.getValue().get();
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing an item is not supported");
    }
  }

  private static class EntryIterable<T> implements Iterable<Entry<T>> {
    private final Exchange originExchange;
    private final Object[] keys;

    private EntryIterable(Exchange originExchange, Object... keys) {
      this.originExchange = originExchange;
      this.keys = keys;
    }

    @Override
    public Iterator<Entry<T>> iterator() {
      originExchange.clear();
      KeyFilter filter = new KeyFilter();
      for (Object key : keys) {
        originExchange.append(key);
        filter = filter.append(KeyFilter.simpleTerm(key));
      }
      originExchange.append(Key.BEFORE);
      Exchange iteratorExchange = new Exchange(originExchange);
      return new EntryIterator<>(iteratorExchange, filter);
    }
  }

  private static class EntryIterator<T> implements Iterator<Entry<T>> {
    private final Exchange exchange;
    private final KeyFilter keyFilter;

    private EntryIterator(Exchange exchange, KeyFilter keyFilter) {
      this.exchange = exchange;
      this.keyFilter = keyFilter;
    }

    @Override
    public boolean hasNext() {
      try {
        return exchange.hasNext(keyFilter);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<T> next() {
      try {
        exchange.next(keyFilter);
      } catch (PersistitException e) {
        throw new IllegalStateException(e);
      }
      if (exchange.getValue().isDefined()) {
        T value = (T) exchange.getValue().get();
        Key key = exchange.getKey();
        Object[] array = new Object[key.getDepth()];
        for (int i = 0; i < key.getDepth(); i++) {
          array[i] = key.indexTo(i - key.getDepth()).decode();
        }
        return new Entry<>(array, value);
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing an item is not supported");
    }
  }

  public static class Entry<V> {
    private final Object[] key;
    private final V value;

    Entry(Object[] key, V value) {
      this.key = key;
      this.value = value;
    }

    public Object[] key() {
      return key;
    }

    public V value() {
      return value;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }

}
