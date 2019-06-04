/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.utils;

import com.google.common.collect.ImmutableMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class KeyValueFormatTest {

  @Test
  public void test_parser() {
    KeyValueFormat.FieldParser reader = new KeyValueFormat.FieldParser("abc=def;ghi=jkl");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("def");
    assertThat(reader.nextKey()).isEqualTo("ghi");
    assertThat(reader.nextVal()).isEqualTo("jkl");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=1;ghi=2");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("1");
    assertThat(reader.nextKey()).isEqualTo("ghi");
    assertThat(reader.nextVal()).isEqualTo("2");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=;ghi=jkl");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("");
    assertThat(reader.nextKey()).isEqualTo("ghi");
    assertThat(reader.nextVal()).isEqualTo("jkl");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=def");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("def");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=\"def\";ghi=\"jkl\"");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("def");
    assertThat(reader.nextKey()).isEqualTo("ghi");
    assertThat(reader.nextVal()).isEqualTo("jkl");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("\"abc\"=\"def\";\"ghi\"=\"jkl\"");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("def");
    assertThat(reader.nextKey()).isEqualTo("ghi");
    assertThat(reader.nextVal()).isEqualTo("jkl");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=\"def\\\"ghi\"");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("def\"ghi");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("");
    assertThat(reader.nextKey()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=;def=");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("");
    assertThat(reader.nextKey()).isEqualTo("def");
    assertThat(reader.nextVal()).isNull();

    reader = new KeyValueFormat.FieldParser("abc=\"1=2;3\";def=\"4;5=6\"");
    assertThat(reader.nextKey()).isEqualTo("abc");
    assertThat(reader.nextVal()).isEqualTo("1=2;3");
    assertThat(reader.nextKey()).isEqualTo("def");
    assertThat(reader.nextVal()).isEqualTo("4;5=6");
    assertThat(reader.nextKey()).isNull();
  }

  @Test
  public void keep_order_of_linked_map() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("lucky", "luke");
    map.put("aste", "rix");
    String s = KeyValueFormat.format(map);
    assertThat(s).isEqualTo("lucky=luke;aste=rix");
  }

  @Test
  public void shouldFormatMapOfIntegerString() {
    Map<Integer, String> map = new LinkedHashMap<>();
    map.put(3, "three");
    map.put(5, "five");
    String s = KeyValueFormat.formatIntString(map);
    assertThat(s).isEqualTo("3=three;5=five");
  }

  @Test
  public void shouldFormatMapOfIntDouble() {
    Map<Integer, Double> map = new LinkedHashMap<>();
    map.put(13, 2.0);
    map.put(5, 5.75);
    String s = KeyValueFormat.formatIntDouble(map);
    assertThat(s).isEqualTo("13=2.0;5=5.75");
  }

  @Test
  public void shouldSetEmptyFieldWhenNullValue() {
    Map<Integer, Double> map = new LinkedHashMap<>();
    map.put(13, null);
    map.put(5, 5.75);
    String s = KeyValueFormat.formatIntDouble(map);
    assertThat(s).isEqualTo("13=;5=5.75");
  }

  @Test
  public void shouldFormatBlank() {
    Map<Integer, String> map = new TreeMap<>();
    String s = KeyValueFormat.formatIntString(map);
    assertThat(s).isEqualTo("");
  }

  @Test
  public void shouldFormatDate() throws ParseException {
    Map<Integer, Date> map = new LinkedHashMap<>();
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
  public void helper_parse_methods() {
    assertThat(KeyValueFormat.parseIntDate("1=2014-01-15")).hasSize(1);
    assertThat(KeyValueFormat.parseIntDateTime("1=2014-01-15T15:50:45+0100")).hasSize(1);
    assertThat(KeyValueFormat.parseIntDouble("1=3.14")).hasSize(1);
    assertThat(KeyValueFormat.parseIntInt("1=10")).containsOnly(entry(1, 10));
    assertThat(KeyValueFormat.parseIntString("1=one")).containsOnly(entry(1, "one"));
    assertThat(KeyValueFormat.parseIntString("1=\"escaped\"")).containsOnly(entry(1, "escaped"));
    assertThat(KeyValueFormat.parseStringInt("one=1")).containsOnly(entry("one", 1));
    assertThat(KeyValueFormat.parseStringDouble("pi=3.14")).containsOnly(entry("pi", 3.14));
  }

  @Test
  public void helper_format_methods() {
    assertThat(KeyValueFormat.formatIntDateTime(ImmutableMap.of(1, new Date()))).startsWith("1=");
    assertThat(KeyValueFormat.formatIntDate(ImmutableMap.of(1, new Date()))).startsWith("1=");
    assertThat(KeyValueFormat.formatIntDouble(ImmutableMap.of(1, 3.14))).startsWith("1=");
    assertThat(KeyValueFormat.formatIntString(ImmutableMap.of(1, "one"))).isEqualTo("1=one");
    assertThat(KeyValueFormat.formatStringInt(ImmutableMap.of("one", 1))).isEqualTo("one=1");
  }

  @Test
  public void parse_blank() {
    Map<String, String> map = KeyValueFormat.parse("");
    assertThat(map).isEmpty();
  }

  @Test
  public void parse_null() {
    Map<String, String> map = KeyValueFormat.parse(null);
    assertThat(map).isEmpty();
  }

  @Test
  public void parse_empty_values() {
    Map<Integer, Double> map = KeyValueFormat.parseIntDouble("4=4.2;2=;6=6.68");
    assertThat(map.size()).isEqualTo(3);
    assertThat(map.get(4)).isEqualTo(4.2);
    // key is present but value is null
    assertThat(map.containsKey(2)).isTrue();
    assertThat(map.get(2)).isNull();
    assertThat(map.get(6)).isEqualTo(6.68);
  }

  @Test
  public void convert_deprecated_priority() {
    assertThat(KeyValueFormat.newPriorityConverter().format(RulePriority.BLOCKER)).isEqualTo("BLOCKER");
    assertThat(KeyValueFormat.newPriorityConverter().format(null)).isEqualTo("");

    assertThat(KeyValueFormat.newPriorityConverter().parse("MAJOR")).isEqualTo(RulePriority.MAJOR);
    assertThat(KeyValueFormat.newPriorityConverter().parse("")).isNull();
  }

  @Test
  public void escape_strings() {
    Map<String, String> input = new LinkedHashMap<>();
    input.put("foo", "a=b=c");
    input.put("bar", "a;b;c");
    input.put("baz", "double\"quote");
    String csv = KeyValueFormat.format(input);
    assertThat(csv).isEqualTo("foo=\"a=b=c\";bar=\"a;b;c\";baz=double\"quote");

    Map<String, String> output = KeyValueFormat.parse(csv);
    assertThat(output.get("foo")).isEqualTo("a=b=c");
    assertThat(output.get("bar")).isEqualTo("a;b;c");
    assertThat(output.get("baz")).isEqualTo("double\"quote");
  }

  @Test
  public void not_instantiable() {
    // only static methods. Bad pattern, should be improved.
    TestUtils.hasOnlyPrivateConstructors(KeyValueFormat.class);
  }

}
