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
package org.sonar.batch.bootstrap;

import javax.annotation.Nonnull;

import org.sonar.api.utils.HttpDownloader;
import com.google.common.io.ByteSource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.sonar.batch.bootstrap.WSLoader.ServerStatus.*;
import static org.sonar.batch.bootstrap.WSLoader.LoadStrategy.*;
import org.sonar.home.cache.PersistentCache;

public class WSLoader {
  private static final String FAIL_MSG = "Server is not accessible and data is not cached";
  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 10000;
  private static final String REQUEST_METHOD = "GET";

  public enum ServerStatus {
    UNKNOWN, ACCESSIBLE, NOT_ACCESSIBLE;
  }

  public enum LoadStrategy {
    SERVER_FIRST, CACHE_FIRST;
  }

  private LoadStrategy loadStrategy;
  private boolean cacheEnabled;
  private ServerStatus serverStatus;
  private ServerClient client;
  private PersistentCache cache;

  public WSLoader(boolean cacheEnabled, PersistentCache cache, ServerClient client) {
    this.cacheEnabled = cacheEnabled;
    this.loadStrategy = CACHE_FIRST;
    this.serverStatus = UNKNOWN;
    this.cache = cache;
    this.client = client;
  }

  public WSLoader(PersistentCache cache, ServerClient client) {
    this(false, cache, client);
  }

  public ByteSource loadSource(String id) {
    return ByteSource.wrap(load(id));
  }

  public String loadString(String id) {
    return new String(load(id), StandardCharsets.UTF_8);
  }

  @Nonnull
  public byte[] load(String id) {
    if (loadStrategy == CACHE_FIRST) {
      return loadFromCacheFirst(id);
    } else {
      return loadFromServerFirst(id);
    }
  }

  public void setStrategy(LoadStrategy strategy) {
    this.loadStrategy = strategy;
  }

  public LoadStrategy getStrategy() {
    return this.loadStrategy;
  }

  public void setCacheEnabled(boolean enabled) {
    this.cacheEnabled = enabled;
  }

  public boolean isCacheEnabled() {
    return this.cacheEnabled;
  }

  private void switchToOffline() {
    serverStatus = NOT_ACCESSIBLE;
  }

  private void switchToOnline() {
    serverStatus = ACCESSIBLE;
  }

  private boolean isOffline() {
    return serverStatus == NOT_ACCESSIBLE;
  }

  @Nonnull
  private byte[] loadFromCacheFirst(String id) {
    byte[] cached = loadFromCache(id);
    if (cached != null) {
      return cached;
    }

    try {
      return loadFromServer(id);
    } catch (Exception e) {
      if (e.getCause() instanceof HttpDownloader.HttpException) {
        throw e;
      }
      throw new IllegalStateException(FAIL_MSG, e);
    }
  }

  @Nonnull
  private byte[] loadFromServerFirst(String id) {
    try {
      return loadFromServer(id);
    } catch (Exception serverException) {
      if (serverException.getCause() instanceof HttpDownloader.HttpException) {
        // http exceptions should always be thrown (no fallback)
        throw serverException;
      }
      byte[] cached = loadFromCache(id);
      if (cached != null) {
        return cached;
      }
      throw new IllegalStateException(FAIL_MSG, serverException);
    }
  }

  private byte[] loadFromCache(String id) {
    if (!cacheEnabled) {
      return null;
    }

    try {
      return cache.get(client.getURI(id).toString(), null);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] loadFromServer(String id) {
    if (isOffline()) {
      throw new IllegalStateException("Server is not accessible");
    }

    try {
      InputStream is = client.load(id, REQUEST_METHOD, true, CONNECT_TIMEOUT, READ_TIMEOUT);
      switchToOnline();
      byte[] value = IOUtils.toByteArray(is);
      if (cacheEnabled) {
        cache.put(client.getURI(id).toString(), value);
      }
      return value;
    } catch (IllegalStateException e) {
      switchToOffline();
      throw e;
    } catch (Exception e) {
      switchToOffline();
      throw new IllegalStateException(e);
    }
  }
}
