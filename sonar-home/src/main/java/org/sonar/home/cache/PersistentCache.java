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
package org.sonar.home.cache;

import org.sonar.home.log.Log;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.*;

public class PersistentCache {
  private static final Charset ENCODING = StandardCharsets.UTF_8;
  private static final String DIGEST_ALGO = "MD5";
  private static final String LOCK_FNAME = ".lock";

  private Path baseDir;

  // eviction strategy is to expire entries after modification once a time duration has elapsed
  private final long defaultDurationToExpireMs;
  private final Log log;
  private boolean forceUpdate;

  public PersistentCache(Path baseDir, long defaultDurationToExpireMs, Log log, boolean forceUpdate) {
    this.baseDir = baseDir;
    this.defaultDurationToExpireMs = defaultDurationToExpireMs;
    this.log = log;

    reconfigure(forceUpdate);
    log.info("cache: " + baseDir + ", default expiration time (ms): " + defaultDurationToExpireMs);
  }

  public void reconfigure(boolean forceUpdate) {
    this.forceUpdate = forceUpdate;

    if (forceUpdate) {
      log.debug("cache: forcing update");
    }

    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new IllegalStateException("failed to create cache dir", e);
    }
  }

  public Path getBaseDirectory() {
    return baseDir;
  }

  public boolean isForceUpdate() {
    return forceUpdate;
  }

  @CheckForNull
  public synchronized String getString(@Nonnull String obj, @Nullable final Callable<String> valueLoader) throws Exception {
    byte[] cached = get(obj, new Callable<byte[]>() {
      @Override
      public byte[] call() throws Exception {
        String s = valueLoader.call();
        if (s != null) {
          return s.getBytes(ENCODING);
        }
        return null;
      }
    });

    if (cached == null) {
      return null;
    }

    return new String(cached, ENCODING);
  }

  @CheckForNull
  public synchronized byte[] get(@Nonnull String obj, @Nullable Callable<byte[]> valueLoader) throws Exception {
    String key = getKey(obj);

    try (FileLock l = lock()) {
      if (!forceUpdate) {
        byte[] cached = getCache(key);

        if (cached != null) {
          log.debug("cache hit for " + obj + " -> " + key);
          return cached;
        }

        log.debug("cache miss for " + obj + " -> " + key);
      } else {
        log.debug("cache force update for " + obj + " -> " + key);
      }

      if (valueLoader != null) {
        byte[] value = valueLoader.call();
        if (value != null) {
          putCache(key, value);
        }
        return value;
      }
    }

    return null;
  }

  /**
   * Deletes all cache entries
   */
  public synchronized void clear() {
    log.info("cache: clearing");
    try (FileLock l = lock()) {
      deleteCacheEntries(createClearFilter());
    } catch (IOException e) {
      log.error("Error clearing cache", e);
    }
  }

  /**
   * Deletes cache entries that are no longer valid according to the default expiration time period.
   */
  public synchronized void clean() {
    log.info("cache: cleaning");
    try (FileLock l = lock()) {
      deleteCacheEntries(createCleanFilter());
    } catch (IOException e) {
      log.error("Error cleaning cache", e);
    }
  }

  private FileLock lock() throws IOException {
    FileChannel ch = FileChannel.open(getLockPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    return ch.lock();
  }

  private String getKey(String uri) {
    try {
      MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGO);
      digest.update(uri.getBytes(StandardCharsets.UTF_8));
      return byteArrayToHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Couldn't create hash", e);
    }
  }

  private void deleteCacheEntries(DirectoryStream.Filter<Path> filter) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, filter)) {
      for (Path p : stream) {
        try {
          Files.delete(p);
        } catch (Exception e) {
          log.error("Error deleting " + p, e);
        }
      }
    }
  }

  private DirectoryStream.Filter<Path> createClearFilter() throws IOException {
    return new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        return !LOCK_FNAME.equals(entry.getFileName().toString());
      }
    };
  }

  private DirectoryStream.Filter<Path> createCleanFilter() throws IOException {
    return new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        if (LOCK_FNAME.equals(entry.getFileName().toString())) {
          return false;
        }

        return isCacheEntryExpired(entry, PersistentCache.this.defaultDurationToExpireMs);
      }
    };
  }

  private void putCache(String key, byte[] value) throws UnsupportedEncodingException, IOException {
    Path cachePath = getCacheEntryPath(key);
    Files.write(cachePath, value, CREATE, WRITE, TRUNCATE_EXISTING);
  }

  private byte[] getCache(String key) throws IOException {
    Path cachePath = getCacheEntryPath(key);

    if (!validateCacheEntry(cachePath, this.defaultDurationToExpireMs)) {
      return null;
    }

    return Files.readAllBytes(cachePath);
  }

  private boolean validateCacheEntry(Path cacheEntryPath, long durationToExpireMs) throws IOException {
    if (!Files.exists(cacheEntryPath)) {
      return false;
    }

    if (isCacheEntryExpired(cacheEntryPath, durationToExpireMs)) {
      log.debug("cache: expiring entry");
      Files.delete(cacheEntryPath);
      return false;
    }

    return true;
  }

  private boolean isCacheEntryExpired(Path cacheEntryPath, long durationToExpireMs) throws IOException {
    BasicFileAttributes attr = Files.readAttributes(cacheEntryPath, BasicFileAttributes.class);
    long modTime = attr.lastModifiedTime().toMillis();

    long age = System.currentTimeMillis() - modTime;

    if (age > durationToExpireMs) {
      return true;
    }

    return false;
  }

  private Path getLockPath() {
    return baseDir.resolve(LOCK_FNAME);
  }

  private Path getCacheEntryPath(String key) {
    return baseDir.resolve(key);
  }

  private static String byteArrayToHex(byte[] a) {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }
}
