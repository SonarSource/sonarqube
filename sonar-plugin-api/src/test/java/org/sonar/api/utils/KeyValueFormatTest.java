/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.utils;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class KeyValueFormatTest {

  @Test
  public void shouldFormatMapOfStrings() {
    Map<String,String> map = Maps.newLinkedHashMap();
    map.put("lucky", "luke");
    map.put("aste", "rix");
    String s = KeyValueFormat.createStringString().toString(map);
    assertThat(s, is("lucky=luke;aste=rix"));// same order
  }

  @Test
  public void shouldFormatMapOfIntegerString() {
    Map<Integer,String> map = Maps.newLinkedHashMap();
    map.put(3, "three");
    map.put(5, "five");
    String s = KeyValueFormat.createIntString().toString(map);
    assertThat(s, is("3=three;5=five"));// same order
  }

  @Test
  public void shouldFormatMapOfIntDouble() {
    Map<Integer,Double> map = Maps.newLinkedHashMap();
    map.put(13, 2.0);
    map.put(5, 5.75);
    String s = KeyValueFormat.createIntDouble().toString(map);
    assertThat(s, is("13=2.0;5=5.75"));// same order
  }

  @Test
  public void shouldSetEmptyFieldWhenNullValue() {
    Map<Integer,Double> map = Maps.newLinkedHashMap();
    map.put(13, null);
    map.put(5, 5.75);
    String s = KeyValueFormat.createIntDouble().toString(map);
    assertThat(s, is("13=;5=5.75"));// same order
  }

  @Test
  public void shouldFormatBlank() {
    Map<Integer,String> map = Maps.newTreeMap();
    String s = KeyValueFormat.createIntString().toString(map);
    assertThat(s, is(""));
  }

  @Test
  public void shouldFormatDate() throws ParseException {
    Map<Integer,Date> map = Maps.newLinkedHashMap();
    map.put(4, new SimpleDateFormat("yyyy-MM-dd").parse("2010-12-25"));
    map.put(20, new SimpleDateFormat("yyyy-MM-dd").parse("2009-05-28"));
    map.put(12, null);
    String s = KeyValueFormat.createIntDate().toString(map);
    assertThat(s, is("4=2010-12-25;20=2009-05-28;12="));
  }

  @Test
  public void shouldParseStrings() throws ParseException {
    SortedMap<String,String> map = KeyValueFormat.createStringString().toSortedMap("one=un;two=deux");
    assertThat(map.size(), is(2));
    assertThat(map.get("one"), is("un"));
    assertThat(map.get("two"), is("deux"));
    assertThat(map.keySet().iterator().next(), is("one"));//same order as in string
  }

  @Test
  public void shouldParseBlank() throws ParseException {
    SortedMap<String,String> map = KeyValueFormat.createStringString().toSortedMap("");
    assertThat(map.size(), is(0));
  }

  @Test
  public void shouldParseNull() throws ParseException {
    SortedMap<String,String> map = KeyValueFormat.createStringString().toSortedMap(null);
    assertThat(map.size(), is(0));
  }

  @Test
  public void shouldParseEmptyFields() throws ParseException {
    SortedMap<Integer,Double> map = KeyValueFormat.createIntDouble().toSortedMap("4=4.2;2=;6=6.68");
    assertThat(map.size(), is(3));
    assertThat(map.get(4), is(4.2));
    assertThat(map.get(2), nullValue());
    assertThat(map.get(6), is(6.68));
  }

  @Test
  public void shouldConvertSeverity() {
    assertThat(KeyValueFormat.SeverityConverter.INSTANCE.toString(RulePriority.BLOCKER), is("BLOCKER"));
    assertThat(KeyValueFormat.SeverityConverter.INSTANCE.toString(null), is(""));

    assertThat(KeyValueFormat.SeverityConverter.INSTANCE.fromString("MAJOR"), is(RulePriority.MAJOR));
    assertThat(KeyValueFormat.SeverityConverter.INSTANCE.fromString(""), nullValue());
  }
}
