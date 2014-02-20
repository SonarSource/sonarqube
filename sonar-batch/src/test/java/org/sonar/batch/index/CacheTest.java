/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.fest.assertions.Assertions.assertThat;

public class CacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Caches caches;

  @Before
  public void start() throws Exception {
    caches = CachesTest.createCacheOnTemp(temp);
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void one_part_key() throws Exception {
    Cache<String> cache = caches.createCache("capitals");

    assertThat(cache.get("france")).isNull();

    cache.put("france", "paris");
    cache.put("italy", "rome");
    assertThat(cache.get("france")).isEqualTo("paris");
    assertThat(cache.keySet()).containsOnly("france", "italy");
    assertThat(cache.keySet("france")).isEmpty();
    assertThat(cache.values()).containsOnly("paris", "rome");
    assertThat(cache.containsKey("france")).isTrue();

    Cache.Entry[] entries = Iterables.toArray(cache.entries(), Cache.Entry.class);
    assertThat(entries).hasSize(2);
    assertThat(entries[0].key()[0]).isEqualTo("france");
    assertThat(entries[0].value()).isEqualTo("paris");
    assertThat(entries[1].key()[0]).isEqualTo("italy");
    assertThat(entries[1].value()).isEqualTo("rome");

    cache.remove("france");
    assertThat(cache.get("france")).isNull();
    assertThat(cache.get("italy")).isEqualTo("rome");
    assertThat(cache.keySet()).containsOnly("italy");
    assertThat(cache.keySet("france")).isEmpty();
    assertThat(cache.containsKey("france")).isFalse();
    assertThat(cache.containsKey("italy")).isTrue();
    assertThat(cache.values()).containsOnly("rome");

    cache.clear();
    assertThat(cache.values()).isEmpty();
  }

  @Test
  public void test_key_being_prefix_of_another_key() throws Exception {
    Cache<String> cache = caches.createCache("components");

    cache.put("struts-el:org.apache.strutsel.taglib.html.ELButtonTag", "the Tag");
    cache.put("struts-el:org.apache.strutsel.taglib.html.ELButtonTagBeanInfo", "the BeanInfo");

    assertThat(cache.get("struts-el:org.apache.strutsel.taglib.html.ELButtonTag")).isEqualTo("the Tag");
    assertThat(cache.get("struts-el:org.apache.strutsel.taglib.html.ELButtonTagBeanInfo")).isEqualTo("the BeanInfo");
  }

  @Test
  public void two_parts_key() throws Exception {
    Cache<String> cache = caches.createCache("capitals");

    assertThat(cache.get("europe", "france")).isNull();

    cache.put("europe", "france", "paris");
    cache.put("europe", "italy", "rome");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.get("europe", "france")).isEqualTo("paris");
    assertThat(cache.get("europe", "italy")).isEqualTo("rome");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.keySet("europe")).containsOnly("france", "italy");
    assertThat(cache.keySet()).containsOnly("europe");
    assertThat(cache.containsKey("europe")).isFalse();
    assertThat(cache.containsKey("europe", "france")).isTrue();
    assertThat(cache.containsKey("europe", "spain")).isFalse();
    assertThat(cache.values()).containsOnly("paris", "rome");
    assertThat(cache.values("america")).isEmpty();
    assertThat(cache.values("europe")).containsOnly("paris", "rome");
    assertThat(cache.values("oceania")).isEmpty();

    Cache.Entry[] allEntries = Iterables.toArray(cache.entries(), Cache.Entry.class);
    assertThat(allEntries).hasSize(2);
    assertThat(allEntries[0].key()).isEqualTo(new String[]{"europe", "france"});
    assertThat(allEntries[0].value()).isEqualTo("paris");
    assertThat(allEntries[1].key()).isEqualTo(new String[]{"europe", "italy"});
    assertThat(allEntries[1].value()).isEqualTo("rome");

    Cache.SubEntry[] subEntries = Iterables.toArray(cache.subEntries("europe"), Cache.SubEntry.class);
    assertThat(subEntries).hasSize(2);
    assertThat(subEntries[0].keyAsString()).isEqualTo("france");
    assertThat(subEntries[0].value()).isEqualTo("paris");
    assertThat(subEntries[1].keyAsString()).isEqualTo("italy");
    assertThat(subEntries[1].value()).isEqualTo("rome");

    cache.remove("europe", "france");
    assertThat(cache.values()).containsOnly("rome");
    assertThat(cache.get("europe", "france")).isNull();
    assertThat(cache.get("europe", "italy")).isEqualTo("rome");
    assertThat(cache.containsKey("europe", "france")).isFalse();
    assertThat(cache.keySet("europe")).containsOnly("italy");

    cache.clear("america");
    assertThat(cache.keySet()).containsOnly("europe");
    cache.clear("europe");
    assertThat(cache.keySet()).isEmpty();
  }

  @Test
  public void three_parts_key() throws Exception {
    Cache<String> cache = caches.createCache("places");
    assertThat(cache.get("europe", "france", "paris")).isNull();

    cache.put("europe", "france", "paris", "eiffel tower");
    cache.put("europe", "france", "annecy", "lake");
    cache.put("europe", "italy", "rome", "colosseum");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.get("europe", "france")).isNull();
    assertThat(cache.get("europe", "france", "paris")).isEqualTo("eiffel tower");
    assertThat(cache.get("europe", "france", "annecy")).isEqualTo("lake");
    assertThat(cache.get("europe", "italy", "rome")).isEqualTo("colosseum");
    assertThat(cache.keySet()).containsOnly("europe");
    assertThat(cache.keySet("europe")).containsOnly("france", "italy");
    assertThat(cache.keySet("europe", "france")).containsOnly("annecy", "paris");
    assertThat(cache.containsKey("europe")).isFalse();
    assertThat(cache.containsKey("europe", "france")).isFalse();
    assertThat(cache.containsKey("europe", "france", "annecy")).isTrue();
    assertThat(cache.containsKey("europe", "france", "biarritz")).isFalse();
    assertThat(cache.values()).containsOnly("eiffel tower", "lake", "colosseum");

    Cache.Entry[] allEntries = Iterables.toArray(cache.entries(), Cache.Entry.class);
    assertThat(allEntries).hasSize(3);
    assertThat(allEntries[0].key()).isEqualTo(new String[]{"europe", "france", "annecy"});
    assertThat(allEntries[0].value()).isEqualTo("lake");
    assertThat(allEntries[1].key()).isEqualTo(new String[]{"europe", "france", "paris"});
    assertThat(allEntries[1].value()).isEqualTo("eiffel tower");
    assertThat(allEntries[2].key()).isEqualTo(new String[]{"europe", "italy", "rome"});
    assertThat(allEntries[2].value()).isEqualTo("colosseum");

    Cache.SubEntry[] subEntries = Iterables.toArray(cache.subEntries("europe"), Cache.SubEntry.class);
    assertThat(subEntries).hasSize(2);
    assertThat(subEntries[0].keyAsString()).isEqualTo("france");
    assertThat(subEntries[0].value()).isNull();
    assertThat(subEntries[1].keyAsString()).isEqualTo("italy");
    assertThat(subEntries[1].value()).isNull();

    cache.remove("europe", "france", "annecy");
    assertThat(cache.values()).containsOnly("eiffel tower", "colosseum");
    assertThat(cache.get("europe", "france", "annecy")).isNull();
    assertThat(cache.get("europe", "italy", "rome")).isEqualTo("colosseum");
    assertThat(cache.containsKey("europe", "france")).isFalse();

    cache.clear("europe", "italy");
    assertThat(cache.values()).containsOnly("eiffel tower");

    cache.clear("europe");
    assertThat(cache.values()).isEmpty();
  }

  @Test
  public void remove_versus_clear() throws Exception {
    Cache<String> cache = caches.createCache("capitals");
    cache.put("europe", "france", "paris");
    cache.put("europe", "italy", "rome");

    // remove("europe") does not remove sub-keys
    cache.remove("europe");
    assertThat(cache.values()).containsOnly("paris", "rome");

    // clear("europe") removes sub-keys
    cache.clear("europe");
    assertThat(cache.values()).isEmpty();
  }

  @Test
  public void empty_cache() throws Exception {
    Cache<String> cache = caches.createCache("empty");

    assertThat(cache.get("foo")).isNull();
    assertThat(cache.get("foo", "bar")).isNull();
    assertThat(cache.get("foo", "bar", "baz")).isNull();
    assertThat(cache.keySet()).isEmpty();
    assertThat(cache.keySet("foo")).isEmpty();
    assertThat(cache.containsKey("foo")).isFalse();
    assertThat(cache.containsKey("foo", "bar")).isFalse();
    assertThat(cache.containsKey("foo", "bar", "baz")).isFalse();
    assertThat(cache.values()).isEmpty();
    assertThat(cache.values("foo")).isEmpty();

    // do not fail
    cache.remove("foo");
    cache.remove("foo", "bar");
    cache.remove("foo", "bar", "baz");
    cache.clear("foo");
    cache.clear("foo", "bar");
    cache.clear("foo", "bar", "baz");
    cache.clear();
  }
}
