/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.sonar.test.JsonAssert.assertJson;

/**
 * Assertion methods to compare server-sent events messages.
 *
 * <h3>Usage</h3>
 * <pre>
 * String actual = "";
 * String expected = "event: E\ndata: D";
 * EventAssert.assertEvent(actual).hasType("E");
 * </pre>
 *
 * @since 9.4
 */
public class EventAssert {

  private static final String EVENT = "event";
  private static final String DATA = "data";
  private static final String ID = "id";
  private static final String RETRY = "retry";

  private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(EVENT, DATA, ID, RETRY));

  private final String eventPayload;

  private EventAssert(String eventPayload) {
    this.eventPayload = eventPayload;
  }

  public static EventAssert assertThatEvent(String eventPayload) {
    return new EventAssert(eventPayload);
  }

  public EventAssert isValid() {
    extractFields();
    return this;
  }

  public EventAssert hasField(String name) {
    isValid();
    if (!extractFields().containsKey(name)) {
      fail("Expected event to contain field '" + name + "'. Actual event was: '" + eventPayload + "'");
    }
    return this;
  }

  public EventAssert hasType(String value) {
    return hasField(EVENT, value);
  }

  public EventAssert hasData(String value) {
    return hasField(DATA, value);
  }

  public EventAssert hasField(String name, String value) {
    isValid();
    hasField(name);
    String actual = extractFields().get(name);
    if (!Objects.equals(actual, value)) {
      fail("Expected field '" + name + "' to contain '" + value + "' but was '" + actual + "'");
    }
    return this;
  }

  public EventAssert hasJsonData(URL url) {
    isValid();
    hasField(DATA);
    assertJson(extractFields().get(DATA))
      .withStrictArrayOrder()
      .isSimilarTo(url);
    return this;
  }

  private Map<String, String> extractFields() {
    Map<String, String> fields = new HashMap<>();
    Arrays.stream(eventPayload.split("\n")).forEach(line -> {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        int fieldDelimiterIndex = line.indexOf(':');
        if (fieldDelimiterIndex != -1) {
          String fieldName = line.substring(0, fieldDelimiterIndex);
          if (!ALLOWED_FIELDS.contains(fieldName)) {
            fail("Unknown field in event: '" + fieldName + "'");
          }
          fields.put(fieldName, line.substring(fieldDelimiterIndex + 1).trim());
        } else {
          fail("Invalid line in event: '" + line + "'");
        }
      }
    });
    return fields;
  }
}
