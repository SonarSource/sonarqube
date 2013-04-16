/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CacheTest {
  Caches caches = new Caches();

  @Before
  public void start() {
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void test_put_get_remove() throws Exception {
    Cache<String, String> cache = caches.createCache("issues");
    assertThat(cache.get("foo")).isNull();
    cache.put("foo", "bar");
    assertThat(cache.get("foo")).isEqualTo("bar");
    assertThat(cache.keySet()).containsOnly("foo");
    cache.remove("foo");
    assertThat(cache.get("foo")).isNull();
    assertThat(cache.keySet()).isEmpty();
  }

  @Test
  public void test_put_get_remove_on_groups() throws Exception {
    Cache<String, Float> cache = caches.createCache("measures");
    String group = "org/apache/struts/Action.java";
    assertThat(cache.get(group, "ncloc")).isNull();
    cache.put(group, "ncloc", 123f);
    assertThat(cache.get(group, "ncloc")).isEqualTo(123f);
    assertThat(cache.keySet(group)).containsOnly("ncloc");
    assertThat(cache.get("ncloc")).isNull();
    assertThat(cache.get(group)).isNull();
    cache.remove(group, "ncloc");
    assertThat(cache.get(group, "ncloc")).isNull();
    assertThat(cache.keySet(group)).isEmpty();
  }

  @Test
  public void test_clear_group() throws Exception {
    Cache<String, Float> cache = caches.createCache("measures");
    String group = "org/apache/struts/Action.java";
    cache.put(group, "ncloc", 123f);
    cache.put(group, "lines", 200f);
    assertThat(cache.get(group, "lines")).isNotNull();

    cache.clear("other group");
    assertThat(cache.get(group, "lines")).isNotNull();

    cache.clear(group);
    assertThat(cache.get(group, "lines")).isNull();
  }

  @Test
  public void test_operations_on_empty_cache() throws Exception {
    Cache<String, String> cache = caches.createCache("issues");
    assertThat(cache.get("foo")).isNull();
    assertThat(cache.get("group", "foo")).isNull();
    assertThat(cache.keySet()).isEmpty();
    assertThat(cache.keySet("group")).isEmpty();
    assertThat(cache.values()).isEmpty();
    assertThat(cache.values("group")).isEmpty();

    // do not fail
    cache.remove("foo");
    cache.remove("group", "foo");
    cache.clear();
    cache.clear("group");
    cache.clearAll();
  }

  @Test
  public void test_get_missing_key() {
    Cache<String, String> cache = caches.createCache("issues");
    assertThat(cache.get("foo")).isNull();
  }

  @Test
  public void test_keyset_of_group() {
    Cache<String, Float> cache = caches.createCache("issues");
    cache.put("org/apache/struts/Action.java", "ncloc", 123f);
    cache.put("org/apache/struts/Action.java", "lines", 200f);
    cache.put("org/apache/struts/Filter.java", "coverage", 500f);
    assertThat(cache.keySet("org/apache/struts/Action.java")).containsOnly("ncloc", "lines");
    assertThat(cache.keySet("org/apache/struts/Filter.java")).containsOnly("coverage");
  }

  @Test
  public void test_values_of_group() {
    Cache<String, Float> cache = caches.createCache("issues");
    cache.put("org/apache/struts/Action.java", "ncloc", 123f);
    cache.put("org/apache/struts/Action.java", "lines", 200f);
    cache.put("org/apache/struts/Filter.java", "lines", 500f);
    assertThat(cache.values("org/apache/struts/Action.java")).containsOnly(123f, 200f);
    assertThat(cache.values("org/apache/struts/Filter.java")).containsOnly(500f);
  }

  @Test
  public void test_values() {
    Cache<String, Float> cache = caches.createCache("issues");
    cache.put("ncloc", 123f);
    cache.put("lines", 200f);
    assertThat(cache.values()).containsOnly(123f, 200f);
  }

  @Test
  public void test_all_values() {
    Cache<String, Float> cache = caches.createCache("issues");
    cache.put("org/apache/struts/Action.java", "ncloc", 123f);
    cache.put("org/apache/struts/Action.java", "lines", 200f);
    cache.put("org/apache/struts/Filter.java", "ncloc", 400f);
    cache.put("org/apache/struts/Filter.java", "lines", 500f);

    assertThat(cache.allValues()).containsOnly(123f, 200f, 400f, 500f);
  }
}
