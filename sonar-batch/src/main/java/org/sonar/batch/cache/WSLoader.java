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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;
import static org.sonar.batch.cache.WSLoader.ServerStatus.ACCESSIBLE;
import static org.sonar.batch.cache.WSLoader.ServerStatus.NOT_ACCESSIBLE;
import static org.sonar.batch.cache.WSLoader.ServerStatus.UNKNOWN;

public class WSLoader {
  private static final Logger LOG = Loggers.get(WSLoader.class);
  private static final String FAIL_MSG = "Server is not accessible and data is not cached";
  private static final int CONNECT_TIMEOUT = 5_000;
  private static final int READ_TIMEOUT = 600_000;
  private static final String REQUEST_METHOD = "GET";

  public enum ServerStatus {
    UNKNOWN, ACCESSIBLE, NOT_ACCESSIBLE;
  }

  public enum LoadStrategy {
    SERVER_FIRST, CACHE_FIRST, SERVER_ONLY, CACHE_ONLY;
  }

  private final LoadStrategy defautLoadStrategy;
  private ServerStatus serverStatus;
  private final ServerClient client;
  private final PersistentCache cache;

  public WSLoader(LoadStrategy strategy, PersistentCache cache, ServerClient client) {
    this.defautLoadStrategy = strategy;
    this.serverStatus = UNKNOWN;
    this.cache = cache;
    this.client = client;
  }

  @Nonnull
  public WSLoaderResult<InputStream> loadStream(String id) {
    return load(id, defautLoadStrategy, createStreamLoaderServer(), createStreamLoaderCache());
  }

  @Nonnull
  public WSLoaderResult<String> loadString(String id) {
    return loadString(id, defautLoadStrategy);
  }

  @Nonnull
  public WSLoaderResult<String> loadString(String id, WSLoader.LoadStrategy strategy) {
    return load(id, strategy, createStringLoaderServer(), createStringLoaderCache());
  }

  @Nonnull
  private <T> WSLoaderResult<T> load(String id, WSLoader.LoadStrategy strategy, DataLoader<T> serverLoader, DataLoader<T> cacheLoader) {
    switch (strategy) {
      case CACHE_FIRST:
        return loadFromCacheFirst(id, cacheLoader, serverLoader);
      case CACHE_ONLY:
        return loadFromCacheFirst(id, cacheLoader, null);
      case SERVER_FIRST:
        return loadFromServerFirst(id, serverLoader, cacheLoader);
      case SERVER_ONLY:
      default:
        return loadFromServerFirst(id, serverLoader, null);
    }
  }

  public LoadStrategy getDefaultStrategy() {
    return this.defautLoadStrategy;
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

  @Nonnull
  private <T> WSLoaderResult<T> loadFromCacheFirst(String id, DataLoader<T> cacheLoader, @Nullable DataLoader<T> serverLoader) {
    try {
      return loadFromCache(id, cacheLoader);
    } catch (NotAvailableException cacheNotAvailable) {
      if (serverLoader != null) {
        try {
          return loadFromServer(id, serverLoader);
        } catch (NotAvailableException serverNotAvailable) {
          throw new IllegalStateException(FAIL_MSG, serverNotAvailable.getCause());
        }
      }
      throw new IllegalStateException("Data is not cached", cacheNotAvailable.getCause());
    }
  }

  @Nonnull
  private <T> WSLoaderResult<T> loadFromServerFirst(String id, DataLoader<T> serverLoader, @Nullable DataLoader<T> cacheLoader) {
    try {
      return loadFromServer(id, serverLoader);
    } catch (NotAvailableException serverNotAvailable) {
      if (cacheLoader != null) {
        try {
          return loadFromCache(id, cacheLoader);
        } catch (NotAvailableException cacheNotAvailable) {
          throw new IllegalStateException(FAIL_MSG, serverNotAvailable.getCause());
        }
      }
      throw new IllegalStateException("Server is not available", serverNotAvailable.getCause());
    }
  }

  interface DataLoader<T> {
    T load(String id) throws IOException;
  }

  @Nonnull
  private <T> WSLoaderResult<T> loadFromCache(String id, DataLoader<T> loader) throws NotAvailableException {
    T result = null;
    
    try {
      result = loader.load(id);
    } catch (IOException e) {
      // any exception on the cache should fail fast
      throw new IllegalStateException(e);
    }
    if (result == null) {
      throw new NotAvailableException("resource not cached");
    }
    return new WSLoaderResult<T>(result, true);
  }

  @Nonnull
  private <T> WSLoaderResult<T> loadFromServer(String id, DataLoader<T> loader) throws NotAvailableException {
    if (isOffline()) {
      throw new NotAvailableException("Server not available");
    }
    try {
      T t = loader.load(id);
      switchToOnline();
      return new WSLoaderResult<T>(t, false);
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

  private DataLoader<String> createStringLoaderServer() {
    return new DataLoader<String>() {
      @Override
      public String load(String id) throws IOException {
        InputStream is = client.load(id, REQUEST_METHOD, true, CONNECT_TIMEOUT, READ_TIMEOUT);
        String str = IOUtils.toString(is, StandardCharsets.UTF_8);
        try {
          cache.put(id, str.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new IllegalStateException("Error saving to WS cache", e);
        }
        return str;
      }
    };
  }

  private DataLoader<String> createStringLoaderCache() {
    return new DataLoader<String>() {
      @Override
      public String load(String id) throws IOException {
        return cache.getString(id);
      }
    };
  }

  private DataLoader<InputStream> createStreamLoaderServer() {
    return new DataLoader<InputStream>() {
      @Override
      public InputStream load(String id) throws IOException {
        InputStream is = client.load(id, REQUEST_METHOD, true, CONNECT_TIMEOUT, READ_TIMEOUT);
        try {
          cache.put(id, is);
        } catch (IOException e) {
          throw new IllegalStateException("Error saving to WS cache", e);
        }
        is.close();
        return cache.getStream(id);
      }
    };
  }

  private DataLoader<InputStream> createStreamLoaderCache() {
    return new DataLoader<InputStream>() {
      @Override
      public InputStream load(String id) throws IOException {
        return cache.getStream(id);
      }
    };
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
