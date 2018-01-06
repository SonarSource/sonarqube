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
package org.sonar.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.ComparisonFailure;

/**
 * Assertion to compare JSON documents. Comparison is not strict:
 * <ul>
 *   <li>formatting differences are ignored</li>
 *   <li>order of elements in objects <code>{}</code> is not verified</li>
 *   <li>objects can contain more elements than expected, for example <code>{"one":1, "two":2}</code>
 *   matches <code>{"one":1}</code></li>
 *   <li>order of elements in arrays <code>[]</code> is not verified by default, for example <code>[1, 2]</code>
 *   matches <code>[2, 1]</code>. This mode can be disabled with {@link #withStrictArrayOrder()}</li>
 *   <li>timezones in datetime values are not strictly verified, for example <code>{"foo": "2015-01-01T13:00:00+2000"}</code>
 *   matches <code>{"foo": "2015-01-01T10:00:00-1000"}</code>. This feature can be disabled with
 *   {@link #withStrictTimezone()}
 *   </li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * String actual = "{}";
 * String expected = "{}";
 * JsonAssert.assertJson(actual).isSimilarTo(expected);
 * </pre>
 *
 * <p>Expected JSON document can be loaded from URLs:</p>
 * <pre>
 * String actual = "{}";
 * JsonAssert.assertJson(actual).isSimilarTo(getClass().getResource("MyTest/expected.json"));
 * </pre>
 *
 * @since 5.2
 */
public class JsonAssert {

  private final String actualJson;
  private final JsonComparison comparison = new JsonComparison();

  private JsonAssert(String actualJson) {
    this.actualJson = actualJson;
  }

  public JsonAssert withStrictTimezone() {
    comparison.withTimezone();
    return this;
  }

  public JsonAssert withStrictArrayOrder() {
    comparison.withStrictArrayOrder();
    return this;
  }

  public JsonAssert ignoreFields(String... ignoredFields) {
    comparison.setIgnoredFields(ignoredFields);
    return this;
  }

  public JsonAssert isSimilarTo(String expected) {
    boolean similar = comparison.areSimilar(expected, actualJson);
    if (!similar) {
      throw new ComparisonFailure("Not a super-set of expected JSON -", pretty(expected), pretty(actualJson));
    }
    return this;
  }

  public JsonAssert isSimilarTo(URL expected) {
    return isSimilarTo(urlToString(expected));
  }

  public static JsonAssert assertJson(String actualJson) {
    return new JsonAssert(actualJson);
  }

  public static JsonAssert assertJson(URL actualJson) {
    return new JsonAssert(urlToString(actualJson));
  }

  private static String urlToString(URL url) {
    try {
      return IOUtils.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to load JSON from " + url, e);
    }
  }

  private static String pretty(String json) {
    JsonElement gson = new JsonParser().parse(json);
    return new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(gson);
  }
}
