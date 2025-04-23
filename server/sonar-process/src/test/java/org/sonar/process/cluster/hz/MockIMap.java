/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.process.cluster.hz;

import com.hazelcast.aggregation.Aggregator;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.core.EntryView;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.map.QueryCache;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.map.listener.MapPartitionLostListener;
import com.hazelcast.projection.Projection;
import com.hazelcast.query.Predicate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockIMap<K, V> implements IMap<K, V> {
  private final Map<K, V> map = new HashMap<>();
  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public boolean remove(@NotNull Object o, @NotNull Object o1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeAll(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean tryLock(K key) {
    return lock.tryLock();
  }

  @Override
  public boolean tryLock(@NotNull K k, long l, @Nullable TimeUnit timeUnit) throws InterruptedException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean tryLock(@NotNull K k, long l, @Nullable TimeUnit timeUnit, long l1, @Nullable TimeUnit timeUnit1) throws InterruptedException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void unlock(K key) {
    lock.unlock();
  }

  @Override
  public void forceUnlock(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addLocalEntryListener(@NotNull MapListener mapListener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addLocalEntryListener(@NotNull MapListener mapListener, @NotNull Predicate<K, V> predicate, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addLocalEntryListener(@NotNull MapListener mapListener, @NotNull Predicate<K, V> predicate, @Nullable K k, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String addInterceptor(@NotNull MapInterceptor mapInterceptor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean removeInterceptor(@NotNull String s) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addEntryListener(@NotNull MapListener mapListener, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean removeEntryListener(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addPartitionLostListener(@NotNull MapPartitionLostListener mapPartitionLostListener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean removePartitionLostListener(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addEntryListener(@NotNull MapListener mapListener, @NotNull K k, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addEntryListener(@NotNull MapListener mapListener, @NotNull Predicate<K, V> predicate, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UUID addEntryListener(@NotNull MapListener mapListener, @NotNull Predicate<K, V> predicate, @Nullable K k, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public EntryView<K, V> getEntryView(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean evict(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void evictAll() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return map.values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public Set<K> keySet(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Set<Entry<K, V>> entrySet(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<V> values(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<V> localValues() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<V> localValues(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Set<K> localKeySet() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Set<K> localKeySet(@NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addIndex(IndexConfig indexConfig) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public LocalMapStats getLocalMapStats() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> R executeOnKey(@NotNull K k, @NotNull EntryProcessor<K, V, R> entryProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> Map<K, R> executeOnKeys(@NotNull Set<K> set, @NotNull EntryProcessor<K, V, R> entryProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> CompletionStage<Map<K, R>> submitToKeys(@NotNull Set<K> set, @NotNull EntryProcessor<K, V, R> entryProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> CompletionStage<R> submitToKey(@NotNull K k, @NotNull EntryProcessor<K, V, R> entryProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> Map<K, R> executeOnEntries(@NotNull EntryProcessor<K, V, R> entryProcessor) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> Map<K, R> executeOnEntries(@NotNull EntryProcessor<K, V, R> entryProcessor, @NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> R aggregate(@NotNull Aggregator<? super Entry<K, V>, R> aggregator) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> R aggregate(@NotNull Aggregator<? super Entry<K, V>, R> aggregator, @NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> Collection<R> project(@NotNull Projection<? super Entry<K, V>, R> projection) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <R> Collection<R> project(@NotNull Projection<? super Entry<K, V>, R> projection, @NotNull Predicate<K, V> predicate) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public QueryCache<K, V> getQueryCache(@NotNull String s) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public QueryCache<K, V> getQueryCache(@NotNull String s, @NotNull Predicate<K, V> predicate, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public QueryCache<K, V> getQueryCache(@NotNull String s, @NotNull MapListener mapListener, @NotNull Predicate<K, V> predicate, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean setTtl(@NotNull K k, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V computeIfPresent(@NotNull K k, @NotNull BiFunction<? super K, ? super V, ? extends V> biFunction) {
    if (map.containsKey(k)) {
      V value = map.get(k);
      V newValue = biFunction.apply(k, value);
      if (null != newValue) {
        map.put(k, newValue);
      } else {
        map.remove(k);
      }
      return newValue;
    }
    return null;
  }

  @Override
  public V computeIfAbsent(@NotNull K k, @NotNull Function<? super K, ? extends V> function) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V compute(@NotNull K k, @NotNull BiFunction<? super K, ? super V, ? extends V> biFunction) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V merge(@NotNull K k, @NotNull V v, @NotNull BiFunction<? super V, ? super V, ? extends V> biFunction) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @NotNull
  @Override
  public Iterator<Entry<K, V>> iterator() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @NotNull
  @Override
  public Iterator<Entry<K, V>> iterator(int i) {
    throw new UnsupportedOperationException("Not implemented");
  }

  // Add other methods as needed, throwing UnsupportedOperationException for unimplemented ones
  @Override
  public void delete(Object key) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void flush() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Map<K, V> getAll(@Nullable Set<K> set) {
    return Map.of();
  }

  @Override
  public void loadAll(boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void loadAll(@NotNull Set<K> set, boolean b) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public CompletionStage<V> getAsync(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<V> putAsync(@NotNull K k, @NotNull V v) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<V> putAsync(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<V> putAsync(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Void> putAllAsync(@NotNull Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Void> setAsync(@NotNull K k, @NotNull V v) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Void> setAsync(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Void> setAsync(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<V> removeAsync(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Boolean> deleteAsync(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean tryRemove(@NotNull K k, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean tryPut(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V put(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V put(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void putTransient(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void putTransient(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V putIfAbsent(@NotNull K k, @NotNull V v) {
    if (!map.containsKey(k)) {
      map.put(k, v);
      return null;
    }
    return map.get(k);
  }

  @Override
  public V putIfAbsent(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V putIfAbsent(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean replace(@NotNull K k, @NotNull V v, @NotNull V v1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public V replace(@NotNull K k, @NotNull V v) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void set(@NotNull K k, @NotNull V v) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void set(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void set(@NotNull K k, @NotNull V v, long l, @NotNull TimeUnit timeUnit, long l1, @NotNull TimeUnit timeUnit1) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setAll(@NotNull Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public CompletionStage<Void> setAllAsync(@NotNull Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void lock(@NotNull K k) {
    lock.lock();
  }

  @Override
  public void lock(@NotNull K k, long l, @Nullable TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isLocked(@NotNull K k) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(@NotNull Object o) {
    return map.containsKey(o);
  }

  @Override
  public String getPartitionKey() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getServiceName() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void destroy() {
    throw new UnsupportedOperationException("Not implemented");
  }

}
