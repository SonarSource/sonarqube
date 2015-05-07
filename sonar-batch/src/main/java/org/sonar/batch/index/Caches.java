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

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.persistit.Exchange;
import com.persistit.Persistit;
import com.persistit.Value;
import com.persistit.Volume;
import com.persistit.encoding.CoderManager;
import com.persistit.encoding.ValueCoder;
import com.persistit.exception.PersistitException;
import com.persistit.logging.Slf4jAdapter;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.utils.TempFolder;

import java.io.File;
import java.util.Properties;
import java.util.Set;

/**
 * Factory of caches
 *
 * @since 3.6
 */
@BatchSide
public class Caches implements Startable {

  private final Set<String> cacheNames = Sets.newHashSet();
  private File tempDir;
  private Persistit persistit;
  private Volume volume;
  private final TempFolder tempFolder;

  public Caches(TempFolder tempFolder) {
    this.tempFolder = tempFolder;
    initPersistit();
  }

  private void initPersistit() {
    try {
      tempDir = tempFolder.newDir("caches");
      persistit = new Persistit();
      persistit.setPersistitLogger(new Slf4jAdapter(LoggerFactory.getLogger("PERSISTIT")));
      Properties props = new Properties();
      props.setProperty("datapath", tempDir.getAbsolutePath());
      props.setProperty("logpath", "${datapath}/log");
      props.setProperty("logfile", "${logpath}/persistit_${timestamp}.log");
      props.setProperty("buffer.count.8192", "10");
      props.setProperty("journalpath", "${datapath}/journal");
      props.setProperty("tmpvoldir", "${datapath}");
      props.setProperty("volume.1", "${datapath}/persistit,create,pageSize:8192,initialPages:10,extensionPages:100,maximumPages:25000");
      persistit.setProperties(props);
      persistit.initialize();
      volume = persistit.createTemporaryVolume();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start caches", e);
    }
  }

  public void registerValueCoder(Class<?> clazz, ValueCoder coder) {
    CoderManager cm = persistit.getCoderManager();
    cm.registerValueCoder(clazz, coder);
  }

  public <V> Cache<V> createCache(String cacheName) {
    Preconditions.checkState(volume != null && volume.isOpened(), "Caches are not initialized");
    Preconditions.checkState(!cacheNames.contains(cacheName), "Cache is already created: " + cacheName);
    try {
      Exchange exchange = persistit.getExchange(volume, cacheName, true);
      exchange.setMaximumValueSize(Value.MAXIMUM_SIZE);
      Cache<V> cache = new Cache<V>(cacheName, exchange);
      cacheNames.add(cacheName);
      return cache;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create cache: " + cacheName, e);
    }
  }

  @Override
  public void start() {
    // already started in constructor
  }

  @Override
  public void stop() {
    if (persistit != null) {
      try {
        persistit.close(false);
        persistit = null;
        volume = null;
      } catch (PersistitException e) {
        throw new IllegalStateException("Fail to close caches", e);
      }
    }
    FileUtils.deleteQuietly(tempDir);
    tempDir = null;
    cacheNames.clear();
  }

  File tempDir() {
    return tempDir;
  }

  Persistit persistit() {
    return persistit;
  }
}
