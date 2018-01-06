/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.storage;

import com.persistit.exception.PersistitException;
import java.io.Serializable;
import org.junit.Test;
import org.sonar.scanner.index.AbstractCachesTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class StoragesTest extends AbstractCachesTest {
  @Test
  public void should_create_cache() {
    Storage<Element> cache = caches.createCache("foo");
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
    Storage<String> c = caches.<String>createCache("test1");
    for (int i = 0; i < 1_000_000; i++) {
      c.put("a" + i, "a" + i);
    }

    caches.stop();

    // manager continues up
    assertThat(cachesManager.persistit().isInitialized()).isTrue();

    caches = new Storages(cachesManager);
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
      caches = new Storages(cachesManager);
      caches.start();
      Storage<String> c = caches.<String>createCache("test" + i);
      c.put("key" + i, sb.toString());
      cachesManager.persistit().flush();

      caches.stop();
    }
  }

  private static class Element implements Serializable {
    private static final long serialVersionUID = 1L;

  }
}
