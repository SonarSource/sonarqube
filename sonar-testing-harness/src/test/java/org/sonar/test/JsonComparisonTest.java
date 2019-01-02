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
package org.sonar.test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonComparisonTest {

  @Test(expected = IllegalStateException.class)
  public void fail_if_invalid_json() {
    areSimilar("{]", "");
  }

  @Test
  public void syntax_agnostic() {
    assertThat(areSimilar("{}", " {  } ")).isTrue();
    assertThat(areSimilar("{\"foo\":\"bar\"}", "{\"foo\":  \"bar\" \n }")).isTrue();
  }

  @Test
  public void object() {
    assertThat(areSimilar("{}", "{}")).isTrue();

    // exactly the same
    assertThat(areSimilar("{\"foo\":\"bar\"}", "{\"foo\":\"bar\"}")).isTrue();

    // same key but different value
    assertThat(areSimilar("{\"foo\":\"bar\"}", "{\"foo\":\"baz\"}")).isFalse();

    // missing key
    assertThat(areSimilar("{\"foo\":\"bar\"}", "{\"xxx\":\"bar\"}")).isFalse();

    // expected json can be a subset of actual json
    assertThat(areSimilar("{\"foo\":\"bar\"}", "{\"xxx\":\"bar\", \"foo\": \"bar\"}")).isTrue();
  }

  @Test
  public void strict_order_of_array() {
    assertThat(isSimilar_strict_array_order("[]", "[]")).isTrue();
    assertThat(isSimilar_strict_array_order("[1, 2]", "[1, 2]")).isTrue();

    assertThat(isSimilar_strict_array_order("[1, 2]", "[1]")).isFalse();
    assertThat(isSimilar_strict_array_order("[1, 2]", "[2, 1]")).isFalse();
    assertThat(isSimilar_strict_array_order("[1, 2]", "[1 , 2, 3]")).isFalse();
    assertThat(isSimilar_strict_array_order("[1, 2]", "[1 , false]")).isFalse();
    assertThat(isSimilar_strict_array_order("[1, 2]", "[1 , 3.14]")).isFalse();
  }

  @Test
  public void lenient_order_of_array() {
    assertThat(areSimilar("[]", "[]")).isTrue();
    assertThat(areSimilar("[1, 2]", "[1, 2]")).isTrue();
    assertThat(areSimilar("[1, 2]", "[1]")).isFalse();
    assertThat(areSimilar("[1, 2]", "[2, 1]")).isTrue();
    assertThat(areSimilar("[1, 2]", "[1 , 2, 3]")).isFalse();
    assertThat(areSimilar("[1, 2]", "[1 , false]")).isFalse();
    assertThat(areSimilar("[1, 2]", "[1 , 3.14]")).isFalse();
  }

  @Test
  public void lenient_order_of_arrays_by_default() {
    assertThat(new JsonComparison().isStrictArrayOrder()).isFalse();
  }

  @Test
  public void null_value() {
    assertThat(areSimilar("[null]", "[null]")).isTrue();
    assertThat(areSimilar("[null]", "[]")).isFalse();

    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": null}")).isTrue();
    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": \"bar\"}")).isFalse();
    assertThat(areSimilar("{\"foo\": 3}", "{\"foo\": null}")).isFalse();
    assertThat(areSimilar("{\"foo\": 3.14}", "{\"foo\": null}")).isFalse();
    assertThat(areSimilar("{\"foo\": false}", "{\"foo\": null}")).isFalse();
    assertThat(areSimilar("{\"foo\": true}", "{\"foo\": null}")).isFalse();
    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": 3}")).isFalse();
    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": 3.14}")).isFalse();
    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": false}")).isFalse();
    assertThat(areSimilar("{\"foo\": null}", "{\"foo\": true}")).isFalse();
  }

  @Test
  public void maps_and_arrays() {
    assertThat(areSimilar("[]", "{}")).isFalse();
    assertThat(areSimilar("{}", "[]")).isFalse();

    // map of array
    assertThat(areSimilar("{\"foo\": []}", "{\"foo\": []}")).isTrue();
    assertThat(areSimilar("{\"foo\": [1, 3]}", "{\"foo\": [1, 3], \"bar\": [1, 3]}")).isTrue();
    assertThat(areSimilar("{\"foo\": []}", "{\"foo\": []}")).isTrue();
    assertThat(areSimilar("{\"foo\": [1, 2]}", "{\"foo\": [1, 3]}")).isFalse();

    // array of maps
    assertThat(areSimilar("[{}]", "[{}]")).isTrue();
    assertThat(areSimilar("[{}]", "[{\"foo\": 1}]")).isTrue();
    // exactly the sames
    assertThat(areSimilar("[{\"1\": \"3\"}, {\"2\":\"4\"}]", "[{\"1\": \"3\"}, {\"2\":\"4\"}]")).isTrue();
    // different value
    assertThat(areSimilar("[{\"1\": \"3\"}, {\"2\":\"4\"}]", "[{\"1\": \"3\"}, {\"2\":\"3\"}]")).isFalse();
    // missing key
    assertThat(areSimilar("[{\"1\": \"3\"}, {\"2\":\"4\"}]", "[{\"1\": \"3\"}, {\"5\":\"10\"}]")).isFalse();
  }

  @Test
  public void lenient_timezone() {
    // lenient mode by default
    assertThat(new JsonComparison().isStrictTimezone()).isFalse();

    // same instant, same timezone
    assertThat(areSimilar("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T15:50:45+0100\"}")).isTrue();

    // same instant, but different timezone
    assertThat(areSimilar("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T18:50:45+0400\"}")).isTrue();

    // different time
    assertThat(areSimilar("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T15:51:45+0100\"}")).isFalse();
  }

  @Test
  public void strict_timezone() {
    assertThat(new JsonComparison().withTimezone().isStrictTimezone()).isTrue();

    // same instant, same timezone
    assertThat(isSimilar_strict_timezone("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T15:50:45+0100\"}")).isTrue();
    assertThat(isSimilar_strict_timezone("[\"2010-05-18T15:50:45+0100\"]", "[\"2010-05-18T15:50:45+0100\"]")).isTrue();

    // same instant, but different timezone
    assertThat(isSimilar_strict_timezone("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T18:50:45+0400\"}")).isFalse();

    // different time
    assertThat(isSimilar_strict_timezone("{\"foo\": \"2010-05-18T15:50:45+0100\"}", "{\"foo\": \"2010-05-18T15:51:45+0100\"}")).isFalse();
  }

  @Test
  public void compare_doubles() {
    assertThat(areSimilar("{\"foo\": true}", "{\"foo\": false}")).isFalse();
    assertThat(areSimilar("{\"foo\": true}", "{\"foo\": true}")).isTrue();
    assertThat(areSimilar("{\"foo\": true}", "{\"foo\": \"true\"}")).isFalse();
    assertThat(areSimilar("{\"foo\": true}", "{\"foo\": 1}")).isFalse();
  }

  @Test
  public void compare_booleans() {
    assertThat(areSimilar("{\"foo\": 3.14}", "{\"foo\": 3.14000000}")).isTrue();
    assertThat(areSimilar("{\"foo\": 3.14}", "{\"foo\": 3.1400001}")).isTrue();
  }

  private boolean areSimilar(String expected, String actual) {
    return new JsonComparison().areSimilar(expected, actual);
  }

  private boolean isSimilar_strict_timezone(String expected, String actual) {
    return new JsonComparison().withTimezone().areSimilar(expected, actual);
  }

  private boolean isSimilar_strict_array_order(String expected, String actual) {
    return new JsonComparison().withStrictArrayOrder().areSimilar(expected, actual);
  }
}
