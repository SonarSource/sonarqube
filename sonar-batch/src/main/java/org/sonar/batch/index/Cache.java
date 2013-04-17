/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.exception.PersistitException;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.CheckForNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * <p>
 * This cache is not thread-safe, due to direct usage of {@link com.persistit.Exchange}
 * </p>
 */
public class Cache<K, V extends Serializable> {

  private static final String DEFAULT_GROUP = "_";
  // TODO improve exception messages by using this cache name
  private final String name;
  private final Exchange exchange;

  Cache(String name, Exchange exchange) {
    this.name = name;
    this.exchange = exchange;
  }

  public Cache put(K key, V value) {
    return put(DEFAULT_GROUP, key, value);
  }

  public Cache put(String group, K key, V value) {
    try {
      exchange.clear();
      exchange.append(group).append(key);
      exchange.getValue().put(value);
      exchange.store();
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to put element in cache", e);
    }
  }

  /**
   * Implements group-based retrieval of cache elements.
   *
   * @param key   The key.
   * @param group The group.
   * @return The element associated with key in the group, or null.
   */
  @SuppressWarnings("unchecked")
  public V get(String group, K key) {
    try {
      exchange.clear();
      exchange.append(group).append(key);
      exchange.fetch();
      if (!exchange.getValue().isDefined()) {
        return null;
      }
      return (V) exchange.getValue().get();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get element from cache", e);
    }
  }


  /**
   * Returns the object associated with key from the cache, or null if not found.
   *
   * @param key The key whose associated value is to be retrieved.
   * @return The value, or null if not found.
   */
  @SuppressWarnings("unchecked")
  public V get(K key) {
    return get(DEFAULT_GROUP, key);
  }

  public Cache remove(String group, K key) {
    try {
      exchange.clear();
      exchange.append(group).append(key);
      exchange.remove();
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get element from cache", e);
    }
  }

  public Cache remove(K key) {
    return remove(DEFAULT_GROUP, key);
  }

  /**
   * Removes everything in the specified group.
   *
   * @param group The group name.
   */
  public Cache clear(String group) {
    try {
      exchange.clear();
      exchange.append(group);
      Key key = new Key(exchange.getKey());
      key.to(Key.AFTER);
      exchange.removeKeyRange(exchange.getKey(), key);
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to clear cache group: " + group, e);
    }
  }


  /**
   * Removes everything in the default cache, but not any of the group caches.
   */
  public Cache clear() {
    return clear(DEFAULT_GROUP);
  }

  /**
   * Clears the default as well as all group caches.
   */
  public void clearAll() {
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
  public Set<K> keySet(String group) {
    try {
      Set<K> keys = Sets.newLinkedHashSet();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);

      iteratorExchange.append(group);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        keys.add((K) iteratorExchange.getKey().indexTo(-1).decode());
      }
      return keys;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get cache keys", e);
    }
  }


  /**
   * Returns the set of keys associated with this cache.
   *
   * @return The set containing the keys for this cache.
   */
  public Set<K> keySet() {
    return keySet(DEFAULT_GROUP);
  }

  // TODO implement a lazy-loading equivalent with Iterator/Iterable
  public Collection<V> values(String group) {
    try {
      List<V> values = Lists.newLinkedList();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(group).append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        if (iteratorExchange.getValue().isDefined()) {
          values.add((V) iteratorExchange.getValue().get());
        }
      }
      return values;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get cache values", e);
    }
  }

  public Iterable<V> values() {
    return values(DEFAULT_GROUP);
  }

  public Collection<V> allValues() {
    try {
      List<V> values = Lists.newLinkedList();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(true)) {
        values.add((V) iteratorExchange.getValue().get());
      }
      return values;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get cache values", e);
    }
  }

  public Set<String> groups() {
    try {
      Set<String> groups = Sets.newLinkedHashSet();
      exchange.clear();
      Exchange iteratorExchange = new Exchange(exchange);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        groups.add(iteratorExchange.getKey().decodeString());
      }
      return groups;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get cache values", e);
    }
  }

  public <T extends Serializable> Iterable<Entry<T>> entries() {
    exchange.clear().to(Key.BEFORE);
    return new EntryIterable(new Exchange(exchange), true);
  }

  public <T extends Serializable> Iterable<Entry<T>> entries(String group) {
    exchange.clear().append(group).append(Key.BEFORE);
    return new EntryIterable(new Exchange(exchange), false);
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
      return new Entry(key.indexTo(-2).decodeString(), key.indexTo(-1).decodeString(), value);
    }

    @Override
    public void remove() {
    }
  }

  public static class Entry<T extends Serializable> {
    private final String group;
    private final String key;
    private final T value;

    Entry(String group, String key, T value) {
      this.group = group;
      this.key = key;
      this.value = value;
    }

    public String group() {
      return group;
    }

    public String key() {
      return key;
    }

    @CheckForNull
    public T value() {
      return value;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }
}
