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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class PersistentCache {
  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
  private static final Charset ENCODING = StandardCharsets.UTF_8;
  private static final String DIGEST_ALGO = "MD5";

  private final PersistentCacheInvalidation invalidation;
  private final Logger logger;
  private final Path dir;
  private DirectoryLock lock;

  public PersistentCache(Path dir, PersistentCacheInvalidation invalidation, Logger logger, DirectoryLock lock) {
    this.dir = dir;
    this.invalidation = invalidation;
    this.logger = logger;
    this.lock = lock;

    reconfigure();
    logger.debug("cache: " + dir);
  }

  public synchronized void reconfigure() {
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new IllegalStateException("failed to create cache dir", e);
    }
  }

  public Path getDirectory() {
    return dir;
  }

  @CheckForNull
  public synchronized String getString(@Nonnull String obj) throws IOException {
    byte[] cached = get(obj);

    if (cached == null) {
      return null;
    }

    return new String(cached, ENCODING);
  }

  @CheckForNull
  public synchronized InputStream getStream(@Nonnull String obj) throws IOException {
    String key = getKey(obj);

    try {
      lock();
      Path path = getCacheCopy(key);
      if (path == null) {
        return null;
      }
      return new DeleteFileOnCloseInputStream(new FileInputStream(path.toFile()), path);

    } finally {
      unlock();
    }
  }

  @CheckForNull
  public synchronized byte[] get(@Nonnull String obj) throws IOException {
    String key = getKey(obj);

    try {
      lock();

      byte[] cached = getCache(key);

      if (cached != null) {
        logger.debug("cache hit for " + obj + " -> " + key);
        return cached;
      }

      logger.debug("cache miss for " + obj + " -> " + key);
    } finally {
      unlock();
    }

    return null;
  }

  public synchronized void put(@Nonnull String obj, @Nonnull InputStream stream) throws IOException {
    String key = getKey(obj);
    try {
      lock();
      putCache(key, stream);
    } finally {
      unlock();
    }
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
      deleteCacheEntries(new DirectoryCleanFilter());
    } catch (IOException e) {
      logger.error("Error cleaning cache", e);
    } finally {
      unlock();
    }
  }

  private void lock() throws IOException {
    lock.lock();
  }

  private void unlock() {
    lock.unlock();
  }

  private static String getKey(String uri) {
    try {
      String key = uri;
      MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGO);
      digest.update(key.getBytes(StandardCharsets.UTF_8));
      return byteArrayToHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Couldn't create hash", e);
    }
  }

  private void deleteCacheEntries(DirectoryStream.Filter<Path> filter) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
      for (Path p : stream) {
        try {
          Files.delete(p);
        } catch (Exception e) {
          logger.error("Error deleting " + p, e);
        }
      }
    }
  }

  private class DirectoryClearFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      return !lock.getFileLockName().equals(entry.getFileName().toString());
    }
  }

  private class DirectoryCleanFilter implements DirectoryStream.Filter<Path> {
    @Override
    public boolean accept(Path entry) throws IOException {
      if (lock.getFileLockName().equals(entry.getFileName().toString())) {
        return false;
      }

      return invalidation.test(entry);
    }
  }

  private void putCache(String key, byte[] value) throws IOException {
    Path cachePath = getCacheEntryPath(key);
    Files.write(cachePath, value, CREATE, WRITE, TRUNCATE_EXISTING);
  }

  private void putCache(String key, InputStream stream) throws IOException {
    Path cachePath = getCacheEntryPath(key);
    Files.copy(stream, cachePath, StandardCopyOption.REPLACE_EXISTING);
  }

  private byte[] getCache(String key) throws IOException {
    Path cachePath = getCacheEntryPath(key);

    if (!validateCacheEntry(cachePath)) {
      return null;
    }

    return Files.readAllBytes(cachePath);
  }

  private Path getCacheCopy(String key) throws IOException {
    Path cachePath = getCacheEntryPath(key);

    if (!validateCacheEntry(cachePath)) {
      return null;
    }

    Path temp = Files.createTempFile("sonar_cache", null);
    Files.copy(cachePath, temp, StandardCopyOption.REPLACE_EXISTING);
    return temp;
  }

  private boolean validateCacheEntry(Path cacheEntryPath) throws IOException {
    if (!Files.exists(cacheEntryPath)) {
      return false;
    }

    if (invalidation.test(cacheEntryPath)) {
      logger.debug("cache: evicting entry");
      Files.delete(cacheEntryPath);
      return false;
    }

    return true;
  }

  private Path getCacheEntryPath(String key) {
    return dir.resolve(key);
  }

  public static String byteArrayToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}
