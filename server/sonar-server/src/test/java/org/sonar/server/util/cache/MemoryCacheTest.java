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
package org.sonar.server.util.cache;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MemoryCacheTest {

  CacheLoader<String, String> loader = mock(CacheLoader.class);
  MemoryCache<String, String> cache = new MemoryCache<>(loader);

  @Test
  public void getNullable() throws Exception {
    when(loader.load("foo")).thenReturn("bar");
    assertThat(cache.getNullable("foo")).isEqualTo("bar");
    assertThat(cache.getNullable("foo")).isEqualTo("bar");
    verify(loader, times(1)).load("foo");

    // return null if key not found
    assertThat(cache.getNullable("not_exists")).isNull();

    // clear cache -> new calls to CacheLoader
    cache.clear();
    assertThat(cache.getNullable("foo")).isEqualTo("bar");
    verify(loader, times(2)).load("foo");
  }

  @Test
  public void get_throws_exception_if_not_exists() throws Exception {
    when(loader.load("foo")).thenReturn("bar");
    assertThat(cache.get("foo")).isEqualTo("bar");
    assertThat(cache.get("foo")).isEqualTo("bar");
    verify(loader, times(1)).load("foo");

    try {
      cache.get("not_exists");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not found: not_exists");
    }
  }

  @Test
  public void getAllNullable() throws Exception {
    List<String> keys = Arrays.asList("one", "two", "three");
    Map<String, String> values = new HashMap<>();
    values.put("one", "un");
    values.put("two", "deux");
    values.put("three", null);
    when(loader.loadAll(keys)).thenReturn(values);
    assertThat(cache.getAll(keys))
      .hasSize(3)
      .containsEntry("one", "un")
      .containsEntry("two", "deux")
      .containsEntry("three", null);

    when(loader.loadAll(Arrays.asList("four"))).thenReturn(ImmutableMap.of("four", "quatre"));
    assertThat(cache.getAll(Arrays.asList("one", "two", "four")))
      .hasSize(3)
      .containsEntry("one", "un")
      .containsEntry("two", "deux")
      .containsEntry("four", "quatre");
    verify(loader, times(2)).loadAll(anyCollection());
  }
}
