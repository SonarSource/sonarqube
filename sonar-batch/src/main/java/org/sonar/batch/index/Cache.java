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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This cache is not thread-safe, due to direct usage of {@link com.persistit.Exchange}
 */
public class Cache<K, V extends Serializable> {

  private static final String DEFAULT_GROUP = "_";
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

      iteratorExchange.append(group);
      iteratorExchange.append(Key.BEFORE);
      while (iteratorExchange.next(false)) {
        values.add((V) iteratorExchange.getValue().get());
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
}
