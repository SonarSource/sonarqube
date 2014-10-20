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
package org.sonar.api.utils;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.apache.commons.collections.bag.TreeBag;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DeprecatedKeyValueFormatTest {

  @Test
  public void formatMap() {
    Map<String, String> map = new TreeMap<String, String>();
    map.put("hello", "world");
    map.put("key1", "val1");
    map.put("key2", "");
    map.put("key3", "val3");
    assertThat(KeyValueFormat.format(map), is("hello=world;key1=val1;key2=;key3=val3"));
  }

  @Test
  public void formatBag() {
    TreeBag bag = new TreeBag();
    bag.add("hello", 1);
    bag.add("key", 3);
    assertThat(KeyValueFormat.format(bag, 0), is("hello=1;key=3"));
  }

  @Test
  public void formatBagWithVariationHack() {
    TreeBag bag = new TreeBag();
    bag.add("hello", 1);
    bag.add("key", 3);
    assertThat(KeyValueFormat.format(bag, -1), is("hello=0;key=2"));
  }

  @Test
  public void formatMultiset() {
    Multiset<String> set = TreeMultiset.create();
    set.add("hello", 1);
    set.add("key", 3);
    assertThat(KeyValueFormat.format(set), is("hello=1;key=3"));
  }

  @Test
  public void parse() {
    Map<String, String> map = KeyValueFormat.parse("hello=world;key1=val1;key2=;key3=val3");
    assertThat(map.size(), is(4));
    assertEquals("world", map.get("hello"));
    assertEquals("val1", map.get("key1"));
    assertEquals("", map.get("key2"));
    assertEquals("val3", map.get("key3"));
  }
}
