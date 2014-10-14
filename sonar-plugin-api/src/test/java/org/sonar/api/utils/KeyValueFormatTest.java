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

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class KeyValueFormatTest {

  @Test
  public void shouldFormatMapOfStrings() {
    Map<String, String> map = Maps.newLinkedHashMap();
    map.put("lucky", "luke");
    map.put("aste", "rix");
    String s = KeyValueFormat.format(map);
    // same order
    assertThat(s).isEqualTo("lucky=luke;aste=rix");
  }

  @Test
  public void shouldFormatMapOfIntegerString() {
    Map<Integer, String> map = Maps.newLinkedHashMap();
    map.put(3, "three");
    map.put(5, "five");
    String s = KeyValueFormat.formatIntString(map);
    assertThat(s).isEqualTo("3=three;5=five");
  }

  @Test
  public void shouldFormatMapOfIntDouble() {
    Map<Integer, Double> map = Maps.newLinkedHashMap();
    map.put(13, 2.0);
    map.put(5, 5.75);
    String s = KeyValueFormat.formatIntDouble(map);
    assertThat(s).isEqualTo("13=2.0;5=5.75");
  }

  @Test
  public void shouldSetEmptyFieldWhenNullValue() {
    Map<Integer, Double> map = Maps.newLinkedHashMap();
    map.put(13, null);
    map.put(5, 5.75);
    String s = KeyValueFormat.formatIntDouble(map);
    assertThat(s).isEqualTo("13=;5=5.75");
  }

  @Test
  public void shouldFormatBlank() {
    Map<Integer, String> map = Maps.newTreeMap();
    String s = KeyValueFormat.formatIntString(map);
    assertThat(s).isEqualTo("");
  }

  @Test
  public void shouldFormatDate() throws ParseException {
    Map<Integer, Date> map = Maps.newLinkedHashMap();
    map.put(4, new SimpleDateFormat("yyyy-MM-dd").parse("2010-12-25"));
    map.put(20, new SimpleDateFormat("yyyy-MM-dd").parse("2009-05-28"));
    map.put(12, null);
    String s = KeyValueFormat.formatIntDate(map);
    assertThat(s).isEqualTo("4=2010-12-25;20=2009-05-28;12=");
  }

  @Test
  public void shouldParseStrings() {
    Map<String, String> map = KeyValueFormat.parse("one=un;two=deux");
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.get("one")).isEqualTo("un");
    assertThat(map.get("two")).isEqualTo("deux");
    assertThat(map.keySet().iterator().next()).isEqualTo("one");// same order as in string
  }

  @Test
  public void shouldParseBlank() {
    Map<String, String> map = KeyValueFormat.parse("");
    assertThat(map.size()).isEqualTo(0);
  }

  @Test
  public void shouldParseNull() {
    Map<String, String> map = KeyValueFormat.parse(null);
    assertThat(map.size()).isEqualTo(0);
  }

  @Test
  public void shouldParseEmptyFields() {
    Map<Integer, Double> map = KeyValueFormat.parseIntDouble("4=4.2;2=;6=6.68");
    assertThat(map.size()).isEqualTo(3);
    assertThat(map.get(4)).isEqualTo(4.2);
    assertThat(map.get(2)).isNull();
    assertThat(map.get(6)).isEqualTo(6.68);
  }

  @Test
  public void shouldConvertPriority() {
    assertThat(KeyValueFormat.newPriorityConverter().format(RulePriority.BLOCKER)).isEqualTo("BLOCKER");
    assertThat(KeyValueFormat.newPriorityConverter().format(null)).isEqualTo("");

    assertThat(KeyValueFormat.newPriorityConverter().parse("MAJOR")).isEqualTo(RulePriority.MAJOR);
    assertThat(KeyValueFormat.newPriorityConverter().parse("")).isNull();
  }

  @Test
  public void shouldFormatMultiset() {
    Multiset<String> set = LinkedHashMultiset.create();
    set.add("foo");
    set.add("foo");
    set.add("bar");
    assertThat(KeyValueFormat.format(set)).isEqualTo("foo=2;bar=1");
  }

  @Test
  public void shouldParseMultiset() {
    Multiset<String> multiset = KeyValueFormat.parseMultiset("foo=2;bar=1;none=");
    assertThat(multiset.count("foo")).isEqualTo(2);
    assertThat(multiset.count("bar")).isEqualTo(1);
    assertThat(multiset.count("none")).isEqualTo(0);
    assertThat(multiset.contains("none")).isFalse();
  }

  @Test
  public void shouldKeepOrderWhenParsingMultiset() {
    Multiset<String> multiset = KeyValueFormat.parseMultiset("foo=2;bar=1");

    // first one is foo
    assertThat(multiset.iterator().next()).isEqualTo("foo");
  }
}
