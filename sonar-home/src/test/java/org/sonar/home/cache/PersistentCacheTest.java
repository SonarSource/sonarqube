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

import org.apache.commons.io.FileUtils;

import org.sonar.home.log.Slf4jLog;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

public class PersistentCacheTest {
  private final static String URI = "key1";
  private final static String VALUE = "cache content";
  private PersistentCache cache = null;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private Slf4jLog log = new Slf4jLog(FileCacheTest.class);

  @Before
  public void setUp() {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, log, false);
  }

  @Test
  public void testCacheMiss() throws Exception {
    assertCacheHit(false);
  }

  @Test
  public void testNullLoader() throws Exception {
    assertThat(cache.get(URI, null)).isNull();
    assertCacheHit(false);
  }

  @Test
  public void testNullValue() throws Exception {
    // mocks have their methods returning null by default
    Callable<byte[]> c = mock(Callable.class);
    assertThat(cache.get(URI, c)).isNull();
    verify(c).call();
    assertCacheHit(false);
  }

  @Test
  public void testClean() throws Exception {
    assertCacheHit(false);
    cache.clear();
    assertCacheHit(false);
  }

  @Test
  public void testCacheHit() throws Exception {
    assertCacheHit(false);
    assertCacheHit(true);
  }

  @Test
  public void testForceUpdate() throws Exception {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, log, true);

    assertCacheHit(false);
    assertCacheHit(false);
    assertCacheHit(false);

    // with forceUpdate, it should still have cached the last call
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, log, false);
    assertCacheHit(true);
  }

  @Test
  public void testReconfigure() throws Exception {
    cache = new PersistentCache(tmp.getRoot().toPath(), Long.MAX_VALUE, log, true);
    assertCacheHit(false);
    assertCacheHit(false);

    File root = tmp.getRoot();
    FileUtils.deleteDirectory(root);

    // should re-create cache directory and start using the cache
    cache.reconfigure(false);
    assertThat(root).exists();

    assertCacheHit(false);
    assertCacheHit(true);
  }

  @Test
  public void testExpiration() throws Exception {
    // negative time to make sure it is expired on the second call
    cache = new PersistentCache(tmp.getRoot().toPath(), -100, log, false);
    assertCacheHit(false);
    assertCacheHit(false);
  }

  private void assertCacheHit(boolean hit) throws Exception {
    CacheFillerString c = new CacheFillerString();
    assertThat(cache.getString(URI, c)).isEqualTo(VALUE);
    assertThat(c.wasCalled).isEqualTo(!hit);
  }

  private class CacheFillerString implements Callable<String> {
    public boolean wasCalled = false;

    @Override
    public String call() throws Exception {
      wasCalled = true;
      return VALUE;
    }
  }

  /**
   * WSCache should be transparent regarding exceptions: if an exception is thrown by the value loader, it should pass through
   * the cache to the original caller using the cache.
   * @throws Exception 
   */
  @Test(expected = ArithmeticException.class)
  public void testExceptions() throws Exception {
    Callable<byte[]> c = mock(Callable.class);
    when(c.call()).thenThrow(ArithmeticException.class);
    cache.get(URI, c);
  }

}
