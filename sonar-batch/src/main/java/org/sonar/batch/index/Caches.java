/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.persistit.Exchange;
import com.persistit.Value;
import com.persistit.encoding.CoderManager;
import com.persistit.Persistit;
import com.persistit.encoding.ValueCoder;
import com.persistit.exception.PersistitException;
import com.persistit.Volume;
import org.picocontainer.Startable;
import org.sonar.api.batch.BatchSide;

@BatchSide
public class Caches implements Startable {
  private final Map<String, Exchange> cacheMap = Maps.newHashMap();
  private Persistit persistit;
  private Volume volume;

  public Caches(CachesManager caches) {
    persistit = caches.persistit();
    doStart();
  }

  @Override
  public void start() {
    // done in constructor
  }

  private void doStart() {
    try {
      persistit.flush();
      volume = persistit.createTemporaryVolume();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create a cache volume", e);
    }
  }

  public void registerValueCoder(Class<?> clazz, ValueCoder coder) {
    CoderManager cm = persistit.getCoderManager();
    cm.registerValueCoder(clazz, coder);
  }

  public <V> Cache<V> createCache(String cacheName) {
    Preconditions.checkState(volume != null && volume.isOpened(), "Caches are not initialized");
    Preconditions.checkState(!cacheMap.containsKey(cacheName), "Cache is already created: " + cacheName);
    try {
      Exchange exchange = persistit.getExchange(volume, cacheName, true);
      exchange.setMaximumValueSize(Value.MAXIMUM_SIZE);
      Cache<V> cache = new Cache<>(cacheName, exchange);
      cacheMap.put(cacheName, exchange);
      return cache;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create cache: " + cacheName, e);
    }
  }

  @Override
  public void stop() {
    for (Entry<String, Exchange> e : cacheMap.entrySet()) {
      persistit.releaseExchange(e.getValue());
    }

    cacheMap.clear();

    if (volume != null) {
      try {
        volume.close();
        volume.delete();
      } catch (PersistitException e) {
        throw new IllegalStateException("Fail to close caches", e);
      }
      volume = null;
    }
  }
}
