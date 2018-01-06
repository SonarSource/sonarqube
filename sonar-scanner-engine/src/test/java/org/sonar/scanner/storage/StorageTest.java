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

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sonar.scanner.index.AbstractCachesTest;
import org.sonar.scanner.storage.Storage.Entry;

import static org.assertj.core.api.Assertions.assertThat;

public class StorageTest extends AbstractCachesTest {

  @Test
  public void one_part_key() {
    Storage<String> cache = caches.createCache("capitals");

    assertThat(cache.get("france")).isNull();

    cache.put("france", "paris");
    cache.put("italy", "rome");
    assertThat(cache.get("france")).isEqualTo("paris");
    assertThat(cache.keySet()).containsOnly("france", "italy");
    assertThat(cache.keySet("france")).isEmpty();
    Iterable<String> values = cache.values();
    assertThat(values).containsOnly("paris", "rome");
    assertThat(values).containsOnly("paris", "rome");
    assertThat(cache.containsKey("france")).isTrue();

    Iterable<Entry<String>> iterable = cache.entries();
    Storage.Entry[] entries = Iterables.toArray(iterable, Storage.Entry.class);
    assertThat(entries).hasSize(2);
    assertThat(iterable).hasSize(2);
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
    assertThat(values).containsOnly("rome");

    cache.clear();
    assertThat(values).isEmpty();
  }

  @Test
  public void test_key_being_prefix_of_another_key() throws Exception {
    Storage<String> cache = caches.createCache("components");

    cache.put("struts-el:org.apache.strutsel.taglib.html.ELButtonTag", "the Tag");
    cache.put("struts-el:org.apache.strutsel.taglib.html.ELButtonTagBeanInfo", "the BeanInfo");

    assertThat(cache.get("struts-el:org.apache.strutsel.taglib.html.ELButtonTag")).isEqualTo("the Tag");
    assertThat(cache.get("struts-el:org.apache.strutsel.taglib.html.ELButtonTagBeanInfo")).isEqualTo("the BeanInfo");
  }

  @Test
  public void two_parts_key() {
    Storage<String> cache = caches.createCache("capitals");

    assertThat(cache.get("europe", "france")).isNull();

    cache.put("europe", "france", "paris");
    cache.put("europe", "italy", "rome");
    cache.put("asia", "china", "pekin");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.get("europe", "france")).isEqualTo("paris");
    assertThat(cache.get("europe", "italy")).isEqualTo("rome");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.keySet("europe")).containsOnly("france", "italy");
    assertThat(cache.keySet()).containsOnly("europe", "asia");
    assertThat(cache.containsKey("europe")).isFalse();
    assertThat(cache.containsKey("europe", "france")).isTrue();
    assertThat(cache.containsKey("europe", "spain")).isFalse();
    assertThat(cache.values()).containsOnly("paris", "rome", "pekin");
    assertThat(cache.values("america")).isEmpty();
    assertThat(cache.values("europe")).containsOnly("paris", "rome");
    assertThat(cache.values("oceania")).isEmpty();

    Iterable<Entry<String>> iterable = cache.entries();
    Storage.Entry[] allEntries = Iterables.toArray(iterable, Storage.Entry.class);
    assertThat(allEntries).hasSize(3);
    assertThat(iterable).hasSize(3);
    assertThat(allEntries[0].key()).isEqualTo(new String[] {"asia", "china"});
    assertThat(allEntries[0].value()).isEqualTo("pekin");
    assertThat(allEntries[1].key()).isEqualTo(new String[] {"europe", "france"});
    assertThat(allEntries[1].value()).isEqualTo("paris");
    assertThat(allEntries[2].key()).isEqualTo(new String[] {"europe", "italy"});
    assertThat(allEntries[2].value()).isEqualTo("rome");

    Iterable<Entry<String>> iterable2 = cache.entries("europe");
    Storage.Entry[] subEntries = Iterables.toArray(iterable2, Storage.Entry.class);
    assertThat(subEntries).hasSize(2);
    assertThat(iterable2).hasSize(2);
    assertThat(subEntries[0].key()).isEqualTo(new String[] {"europe", "france"});
    assertThat(subEntries[0].value()).isEqualTo("paris");
    assertThat(subEntries[1].key()).isEqualTo(new String[] {"europe", "italy"});
    assertThat(subEntries[1].value()).isEqualTo("rome");

    cache.remove("europe", "france");
    assertThat(cache.values()).containsOnly("rome", "pekin");
    assertThat(cache.get("europe", "france")).isNull();
    assertThat(cache.get("europe", "italy")).isEqualTo("rome");
    assertThat(cache.containsKey("europe", "france")).isFalse();
    assertThat(cache.keySet("europe")).containsOnly("italy");

    cache.clear("america");
    assertThat(cache.keySet()).containsOnly("europe", "asia");
    cache.clear();
    assertThat(cache.keySet()).isEmpty();
  }

  @Test
  public void three_parts_key() {
    Storage<String> cache = caches.createCache("places");
    assertThat(cache.get("europe", "france", "paris")).isNull();

    cache.put("europe", "france", "paris", "eiffel tower");
    cache.put("europe", "france", "annecy", "lake");
    cache.put("europe", "france", "poitiers", "notre dame");
    cache.put("europe", "italy", "rome", "colosseum");
    cache.put("europe2", "ukrania", "kiev", "dunno");
    cache.put("asia", "china", "pekin", "great wall");
    cache.put("america", "us", "new york", "empire state building");
    assertThat(cache.get("europe")).isNull();
    assertThat(cache.get("europe", "france")).isNull();
    assertThat(cache.get("europe", "france", "paris")).isEqualTo("eiffel tower");
    assertThat(cache.get("europe", "france", "annecy")).isEqualTo("lake");
    assertThat(cache.get("europe", "italy", "rome")).isEqualTo("colosseum");
    assertThat(cache.keySet()).containsOnly("europe", "asia", "america", "europe2");
    assertThat(cache.keySet("europe")).containsOnly("france", "italy");
    assertThat(cache.keySet("europe", "france")).containsOnly("annecy", "paris", "poitiers");
    assertThat(cache.containsKey("europe")).isFalse();
    assertThat(cache.containsKey("europe", "france")).isFalse();
    assertThat(cache.containsKey("europe", "france", "annecy")).isTrue();
    assertThat(cache.containsKey("europe", "france", "biarritz")).isFalse();
    assertThat(cache.values()).containsOnly("eiffel tower", "lake", "colosseum", "notre dame", "great wall", "empire state building", "dunno");
    assertThat(cache.values("europe")).containsOnly("eiffel tower", "lake", "colosseum", "notre dame");
    assertThat(cache.values("europe", "france")).containsOnly("eiffel tower", "lake", "notre dame");

    Iterable<Entry<String>> iterable = cache.entries();
    Storage.Entry[] allEntries = Iterables.toArray(iterable, Storage.Entry.class);
    assertThat(allEntries).hasSize(7);
    assertThat(iterable).hasSize(7);
    assertThat(allEntries[0].key()).isEqualTo(new String[] {"america", "us", "new york"});
    assertThat(allEntries[0].value()).isEqualTo("empire state building");
    assertThat(allEntries[1].key()).isEqualTo(new String[] {"asia", "china", "pekin"});
    assertThat(allEntries[1].value()).isEqualTo("great wall");
    assertThat(allEntries[2].key()).isEqualTo(new String[] {"europe", "france", "annecy"});
    assertThat(allEntries[2].value()).isEqualTo("lake");
    assertThat(allEntries[3].key()).isEqualTo(new String[] {"europe", "france", "paris"});
    assertThat(allEntries[3].value()).isEqualTo("eiffel tower");
    assertThat(allEntries[4].key()).isEqualTo(new String[] {"europe", "france", "poitiers"});
    assertThat(allEntries[4].value()).isEqualTo("notre dame");
    assertThat(allEntries[5].key()).isEqualTo(new String[] {"europe", "italy", "rome"});
    assertThat(allEntries[5].value()).isEqualTo("colosseum");

    Iterable<Entry<String>> iterable2 = cache.entries("europe");
    Storage.Entry[] subEntries = Iterables.toArray(iterable2, Storage.Entry.class);
    assertThat(subEntries).hasSize(4);
    assertThat(iterable2).hasSize(4);
    assertThat(subEntries[0].key()).isEqualTo(new String[] {"europe", "france", "annecy"});
    assertThat(subEntries[0].value()).isEqualTo("lake");
    assertThat(subEntries[1].key()).isEqualTo(new String[] {"europe", "france", "paris"});
    assertThat(subEntries[1].value()).isEqualTo("eiffel tower");
    assertThat(subEntries[2].key()).isEqualTo(new String[] {"europe", "france", "poitiers"});
    assertThat(subEntries[2].value()).isEqualTo("notre dame");
    assertThat(subEntries[3].key()).isEqualTo(new String[] {"europe", "italy", "rome"});
    assertThat(subEntries[3].value()).isEqualTo("colosseum");

    cache.remove("europe", "france", "annecy");
    assertThat(cache.values()).containsOnly("eiffel tower", "colosseum", "notre dame", "great wall", "empire state building", "dunno");
    assertThat(cache.values("europe")).containsOnly("eiffel tower", "colosseum", "notre dame");
    assertThat(cache.values("europe", "france")).containsOnly("eiffel tower", "notre dame");
    assertThat(cache.get("europe", "france", "annecy")).isNull();
    assertThat(cache.get("europe", "italy", "rome")).isEqualTo("colosseum");
    assertThat(cache.containsKey("europe", "france")).isFalse();

    cache.clear("europe", "italy");
    assertThat(cache.values()).containsOnly("eiffel tower", "notre dame", "great wall", "empire state building", "dunno");

    cache.clear("europe");
    assertThat(cache.values()).containsOnly("great wall", "empire state building", "dunno");

    cache.clear();
    assertThat(cache.values()).isEmpty();
  }

  @Test
  public void remove_versus_clear() {
    Storage<String> cache = caches.createCache("capitals");
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
  public void empty_cache() {
    Storage<String> cache = caches.createCache("empty");

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
