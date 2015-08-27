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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class PersistentCache {

  private static final Charset ENCODING = StandardCharsets.UTF_8;
  private static final String DIGEST_ALGO = "MD5";
  private static final String LOCK_FNAME = ".lock";

  // eviction strategy is to expire entries after modification once a time duration has elapsed
  private final long defaultDurationToExpireMs;
  private final Logger logger;
  private final String version;
  private final Path baseDir;

  public PersistentCache(Path baseDir, long defaultDurationToExpireMs, Logger logger, String version) {
    this.baseDir = baseDir;
    this.defaultDurationToExpireMs = defaultDurationToExpireMs;
    this.logger = logger;
    this.version = version;

    reconfigure();
    logger.debug("cache: " + baseDir + ", default expiration time (ms): " + defaultDurationToExpireMs);
  }

  public synchronized void reconfigure() {
    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new IllegalStateException("failed to create cache dir", e);
    }
  }

  public Path getBaseDirectory() {
    return baseDir;
  }

  @CheckForNull
  public synchronized String getString(@Nonnull String obj, @Nullable final PersistentCacheLoader<String> valueLoader) throws IOException {
    ValueLoaderDecoder decoder = valueLoader != null ? new ValueLoaderDecoder(valueLoader) : null;
    
    byte[] cached = get(obj, decoder);

    if (cached == null) {
      return null;
    }

    return new String(cached, ENCODING);
  }

  @CheckForNull
  public synchronized byte[] get(@Nonnull String obj, @Nullable PersistentCacheLoader<byte[]> valueLoader) throws IOException {
    String key = getKey(obj);

    try {
      lock();

      byte[] cached = getCache(key);

      if (cached != null) {
        logger.debug("cache hit for " + obj + " -> " + key);
        return cached;
      }

      logger.debug("cache miss for " + obj + " -> " + key);

      if (valueLoader != null) {
        byte[] value = valueLoader.get();
        if (value != null) {
          putCache(key, value);
        }
        return value;
      }
    } finally {
      unlock();
    }

    return null;
  }

  public synchronized void put(@Nonnull String obj, @Nonnull byte[] value) throws IOException {
    String key = getKey(obj);
    try {
      lock();
      putCache(key, value);
    } finally {
      unlock();
    }
  }

  /**
   * Deletes all cache entries
   */
  public synchronized void clear() {
    logger.info("cache: clearing");
    try {
      lock();
      deleteCacheEntries(new DirectoryClearFilter());
    } catch (IOException e) {
      logger.error("Error clearing cache", e);
    } finally {
      unlock();
    }
  }

  /**
   * Deletes cache entries that are no longer valid according to the default expiration time period.
   */
  public synchronized void clean() {
    logger.info("cache: cleaning");
    try {
      lock();
      deleteCacheEntries(new DirectoryCleanFilter(defaultDurationToExpireMs));
    } catch (IOException e) {
      logger.error("Error cleaning cache", e);
    } finally {
      unlock();
    }
  }

  private void lock() throws IOException {
    lockRandomAccessFile = new RandomAccessFile(getLockPath().toFile(), "rw");
    lockChannel = lockRandomAccessFile.getChannel();
    lockFile = lockChannel.lock();
  }

  private RandomAccessFile lockRandomAccessFile;
  private FileChannel lockChannel;
  private FileLock lockFile;

  private void unlock() {
    if (lockFile != null) {
      try {
        lockFile.release();
      } catch (IOException e) {
        logger.error("Error releasing lock", e);
      }
    }
    if (lockChannel != null) {
      try {
        lockChannel.close();
      } catch (IOException e) {
        logger.error("Error closing file channel", e);
      }
    }
    if (lockRandomAccessFile != null) {
      try {
        lockRandomAccessFile.close();
      } catch (IOException e) {
        logger.error("Error closing file", e);
      }
    }

    lockFile = null;
    lockRandomAccessFile = null;
    lockChannel = null;
  }

  private String getKey(String uri) {
    try {
      String key = uri;
      if (version != null) {
        key += version;
      }
      MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGO);
      digest.update(key.getBytes(StandardCharsets.UTF_8));
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
          logger.error("Error deleting " + p, e);
        }
      }
    }
  }

  private static class ValueLoaderDecoder implements PersistentCacheLoader<byte[]> {
    PersistentCacheLoader<String> valueLoader;

    ValueLoaderDecoder(PersistentCacheLoader<String> valueLoader) {
      this.valueLoader = valueLoader;
    }

    @Override
    public byte[] get() throws IOException {
      String s = valueLoader.get();
      if (s != null) {
        return s.getBytes(ENCODING);
      }
      return null;
    }
  }

  private static class DirectoryClearFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !LOCK_FNAME.equals(entry.getFileName().toString());
    }
  }

  private static class DirectoryCleanFilter implements DirectoryStream.Filter<Path> {
    private long defaultDurationToExpireMs;

    DirectoryCleanFilter(long defaultDurationToExpireMs) {
      this.defaultDurationToExpireMs = defaultDurationToExpireMs;
    }

    @Override
    public boolean accept(Path entry) throws IOException {
      if (LOCK_FNAME.equals(entry.getFileName().toString())) {
        return false;
      }

      return isCacheEntryExpired(entry, defaultDurationToExpireMs);
    }
  }

  private void putCache(String key, byte[] value) throws IOException {
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
      logger.debug("cache: expiring entry");
      Files.delete(cacheEntryPath);
      return false;
    }

    return true;
  }

  private static boolean isCacheEntryExpired(Path cacheEntryPath, long durationToExpireMs) throws IOException {
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
