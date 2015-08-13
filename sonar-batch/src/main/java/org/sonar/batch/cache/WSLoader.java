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
package org.sonar.batch.cache;

import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nonnull;

import org.sonar.api.utils.HttpDownloader;
import com.google.common.io.ByteSource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.sonar.batch.cache.WSLoader.ServerStatus.*;
import org.sonar.home.cache.PersistentCache;

public class WSLoader {
  private static final Logger LOG = Loggers.get(WSLoader.class);
  private static final String FAIL_MSG = "Server is not accessible and data is not cached";
  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 10000;
  private static final String REQUEST_METHOD = "GET";

  public enum ServerStatus {
    UNKNOWN, ACCESSIBLE, NOT_ACCESSIBLE;
  }

  public enum LoadStrategy {
    SERVER_FIRST, CACHE_FIRST, SERVER_ONLY, CACHE_ONLY;
  }

  private final LoadStrategy loadStrategy;
  private ServerStatus serverStatus;
  private final ServerClient client;
  private final PersistentCache cache;

  public WSLoader(LoadStrategy strategy, PersistentCache cache, ServerClient client) {
    this.loadStrategy = strategy;
    this.serverStatus = UNKNOWN;
    this.cache = cache;
    this.client = client;
  }

  @Nonnull
  public WSLoaderResult<ByteSource> loadSource(String id) {
    WSLoaderResult<byte[]> byteResult = load(id);
    return new WSLoaderResult<ByteSource>(ByteSource.wrap(byteResult.get()), byteResult.isFromCache());
  }

  @Nonnull
  public WSLoaderResult<String> loadString(String id) {
    WSLoaderResult<byte[]> byteResult = load(id);
    return new WSLoaderResult<String>(new String(byteResult.get(), StandardCharsets.UTF_8), byteResult.isFromCache());
  }

  @Nonnull
  public WSLoaderResult<byte[]> load(String id) {
    switch (loadStrategy) {
      case CACHE_FIRST:
        return loadFromCacheFirst(id, true);
      case CACHE_ONLY:
        return loadFromCacheFirst(id, false);
      case SERVER_FIRST:
        return loadFromServerFirst(id, true);
      case SERVER_ONLY:
      default:
        return loadFromServerFirst(id, false);
    }
  }

  public LoadStrategy getStrategy() {
    return this.loadStrategy;
  }

  private void switchToOffline() {
    LOG.debug("server not available - switching to offline mode");
    serverStatus = NOT_ACCESSIBLE;
  }

  private void switchToOnline() {
    serverStatus = ACCESSIBLE;
  }

  private boolean isOffline() {
    return serverStatus == NOT_ACCESSIBLE;
  }

  private void updateCache(String id, byte[] value) {
    try {
      cache.put(client.getURI(id).toString(), value);
    } catch (IOException e) {
      LOG.warn("Error saving to WS cache", e);
    }
  }

  @Nonnull
  private WSLoaderResult<byte[]> loadFromCacheFirst(String id, boolean fallback) {
    try {
      return loadFromCache(id);
    } catch (NotAvailableException cacheNotAvailable) {
      if (fallback) {
        try {
          return loadFromServer(id);
        } catch (NotAvailableException serverNotAvailable) {
          throw new IllegalStateException(FAIL_MSG, serverNotAvailable.getCause());
        }
      }
      throw new IllegalStateException("Data is not cached", cacheNotAvailable.getCause());
    }
  }

  @Nonnull
  private WSLoaderResult<byte[]> loadFromServerFirst(String id, boolean fallback) {
    try {
      return loadFromServer(id);
    } catch (NotAvailableException serverNotAvailable) {
      if (fallback) {
        try {
          return loadFromCache(id);
        } catch (NotAvailableException cacheNotAvailable) {
          throw new IllegalStateException(FAIL_MSG, serverNotAvailable.getCause());
        }
      }
      throw new IllegalStateException("Server is not available", serverNotAvailable.getCause());
    }
  }

  @Nonnull
  private WSLoaderResult<byte[]> loadFromCache(String id) throws NotAvailableException {
    try {
      byte[] result = cache.get(client.getURI(id).toString(), null);
      if (result == null) {
        throw new NotAvailableException("resource not cached");
      }
      return new WSLoaderResult<byte[]>(result, true);
    } catch (IOException e) {
      // any exception on the cache should fail fast
      throw new IllegalStateException(e);
    }
  }

  @Nonnull
  private WSLoaderResult<byte[]> loadFromServer(String id) throws NotAvailableException {
    if (isOffline()) {
      throw new NotAvailableException("Server not available");
    }
    try {
      InputStream is = client.load(id, REQUEST_METHOD, true, CONNECT_TIMEOUT, READ_TIMEOUT);
      switchToOnline();
      byte[] value = IOUtils.toByteArray(is);
      updateCache(id, value);
      return new WSLoaderResult<byte[]>(value, false);
    } catch (IllegalStateException e) {
      if (e.getCause() instanceof HttpDownloader.HttpException) {
        // fail fast if it could connect but there was a application-level error
        throw e;
      }
      switchToOffline();
      throw new NotAvailableException(e);
    } catch (Exception e) {
      // fail fast
      throw new IllegalStateException(e);
    }
  }

  private class NotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotAvailableException(String message) {
      super(message);
    }

    public NotAvailableException(Throwable cause) {
      super(cause);
    }
  }
}
