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

import java.io.Serializable;

import com.persistit.exception.PersistitException;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CachesTest extends AbstractCachesTest {
  @Test
  public void should_create_cache() {
    Cache<Element> cache = caches.createCache("foo");
    assertThat(cache).isNotNull();
  }

  @Test
  public void should_not_create_cache_twice() {
    caches.<Element>createCache("foo");
    try {
      caches.<Element>createCache("foo");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void should_clean_resources() {
    Cache<String> c = caches.<String>createCache("test1");
    for (int i = 0; i < 1_000_000; i++) {
      c.put("a" + i, "a" + i);
    }

    caches.stop();

    // manager continues up
    assertThat(cachesManager.persistit().isInitialized()).isTrue();

    caches = new Caches(cachesManager);
    caches.start();
    caches.createCache("test1");
  }

  @Test
  public void leak_test() throws PersistitException {
    caches.stop();

    int len = 1 * 1024 * 1024;
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append("a");
    }

    for (int i = 0; i < 3; i++) {
      caches = new Caches(cachesManager);
      caches.start();
      Cache<String> c = caches.<String>createCache("test" + i);
      c.put("key" + i, sb.toString());
      cachesManager.persistit().flush();

      caches.stop();
    }
  }

  private static class Element implements Serializable {
    private static final long serialVersionUID = 1L;

  }
}
