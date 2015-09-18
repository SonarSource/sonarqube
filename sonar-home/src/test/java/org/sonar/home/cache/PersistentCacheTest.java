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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PersistentCacheTest {
  private final static String URI = "key1";
  private final static String VALUE = "cache content";
  private PersistentCache cache = null;
  private DirectoryLock lock = null;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Before
  public void setUp() {
    lock = mock(DirectoryLock.class);
    when(lock.getFileLockName()).thenReturn("lock");
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, mock(Logger.class), lock);
  }

  @Test
  public void testCacheMiss() throws Exception {
    assertCacheHit(false);
  }

  @Test
  public void testClean() throws Exception {
    Path lockFile = cache.getDirectory().resolve("lock");
    // puts entry
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    Files.write(lockFile, "test".getBytes(StandardCharsets.UTF_8));
    assertCacheHit(true);
    // negative time to make sure it is expired
    cache = new PersistentCache(tmp.getRoot().toPath(), -100, mock(Logger.class), lock);
    cache.clean();
    assertCacheHit(false);
    // lock file should not get deleted
    assertThat(new String(Files.readAllBytes(lockFile), StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testClear() throws Exception {
    Path lockFile = cache.getDirectory().resolve("lock");
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    Files.write(lockFile, "test".getBytes(StandardCharsets.UTF_8));
    assertCacheHit(true);
    cache.clear();
    assertCacheHit(false);
    // lock file should not get deleted
    assertThat(new String(Files.readAllBytes(lockFile), StandardCharsets.UTF_8)).isEqualTo("test");
  }

  @Test
  public void testCacheHit() throws Exception {
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    assertCacheHit(true);
  }

  @Test
  public void testReconfigure() throws Exception {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, mock(Logger.class), lock);
    assertCacheHit(false);
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    assertCacheHit(true);

    File root = tmp.getRoot();
    FileUtils.deleteQuietly(root);

    // should re-create cache directory and start using the cache
    cache.reconfigure();
    assertThat(root).exists();

    assertCacheHit(false);
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    assertCacheHit(true);
  }

  @Test
  public void testExpiration() throws Exception {
    // negative time to make sure it is expired
    cache = new PersistentCache(tmp.getRoot().toPath(), -100, mock(Logger.class), lock);
    cache.put(URI, VALUE.getBytes(StandardCharsets.UTF_8));
    assertCacheHit(false);
  }

  private void assertCacheHit(boolean hit) throws Exception {
    assertCacheHit(cache, hit);
  }

  private void assertCacheHit(PersistentCache pCache, boolean hit) throws Exception {
    String expected = hit ? VALUE : null;
    assertThat(pCache.getString(URI)).isEqualTo(expected);
    verify(lock, atLeast(1)).unlock();
  }

}
