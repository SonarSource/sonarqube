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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static java.util.Collections.synchronizedSet;

@ThreadSafe
class JsonComparison {

  private boolean strictTimezone = false;
  private boolean strictArrayOrder = false;
  private Set<String> ignoredFields = synchronizedSet(new HashSet<String>());

  boolean isStrictTimezone() {
    return strictTimezone;
  }

  JsonComparison withTimezone() {
    this.strictTimezone = true;
    return this;
  }

  boolean isStrictArrayOrder() {
    return strictArrayOrder;
  }

  JsonComparison withStrictArrayOrder() {
    this.strictArrayOrder = true;
    return this;
  }

  JsonComparison setIgnoredFields(String... ignoredFields) {
    Collections.addAll(this.ignoredFields, ignoredFields);
    return this;
  }

  boolean areSimilar(String expected, String actual) {
    Object expectedJson = parse(expected);
    Object actualJson = parse(actual);
    return compare(expectedJson, actualJson);
  }

  private Object parse(String s) {
    try {
      JSONParser parser = new JSONParser();
      return parser.parse(s);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid JSON: " + s, e);
    }
  }

  private boolean compare(@Nullable Object expectedObject, @Nullable Object actualObject) {
    if (expectedObject == null) {
      return actualObject == null;
    }
    if (actualObject == null) {
      // expected non-null, got null
      return false;
    }
    if (expectedObject.getClass() != actualObject.getClass()) {
      return false;
    }
    if (expectedObject instanceof JSONArray) {
      return compareArrays((JSONArray) expectedObject, (JSONArray) actualObject);
    }
    if (expectedObject instanceof JSONObject) {
      return compareObjects((JSONObject) expectedObject, (JSONObject) actualObject);
    }
    if (expectedObject instanceof String) {
      return compareStrings((String) expectedObject, (String) actualObject);
    }
    if (expectedObject instanceof Number) {
      return compareNumbers((Number) expectedObject, (Number) actualObject);
    }
    return compareBooleans((Boolean) expectedObject, (Boolean) actualObject);
  }

  private boolean compareBooleans(Boolean expected, Boolean actual) {
    return expected.equals(actual);
  }

  private boolean compareNumbers(Number expected, Number actual) {
    double d1 = expected.doubleValue();
    double d2 = actual.doubleValue();
    if (Double.compare(d1, d2) == 0) {
      return true;
    }
    return Math.abs(d1 - d2) <= 0.0000001;
  }

  private boolean compareStrings(String expected, String actual) {
    if (!strictTimezone) {
      // two instants with different timezones are considered as identical (2015-01-01T13:00:00+0100 and 2015-01-01T12:00:00+0000)
      Date expectedDate = tryParseDate(expected);
      Date actualDate = tryParseDate(actual);
      if (expectedDate != null && actualDate != null) {
        return expectedDate.getTime() == actualDate.getTime();
      }
    }
    return expected.equals(actual);
  }

  private boolean compareArrays(JSONArray expected, JSONArray actual) {
    if (strictArrayOrder) {
      return compareArraysByStrictOrder(expected, actual);
    }
    return compareArraysByLenientOrder(expected, actual);
  }

  private boolean compareArraysByStrictOrder(JSONArray expected, JSONArray actual) {
    if (expected.size() != actual.size()) {
      return false;
    }

    for (int index = 0; index < expected.size(); index++) {
      Object expectedElt = expected.get(index);
      Object actualElt = actual.get(index);
      if (!compare(expectedElt, actualElt)) {
        return false;
      }
    }
    return true;
  }

  private boolean compareArraysByLenientOrder(JSONArray expected, JSONArray actual) {
    if (expected.size() > actual.size()) {
      return false;
    }

    List remainingActual = new ArrayList(actual);
    for (Object expectedElement : expected) {
      // element can be null
      boolean found = false;
      for (Object actualElement : remainingActual) {
        if (compare(expectedElement, actualElement)) {
          found = true;
          remainingActual.remove(actualElement);
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return remainingActual.isEmpty();
  }

  private boolean compareObjects(JSONObject expectedMap, JSONObject actualMap) {
    // each key-value of expected map must exist in actual map
    for (Map.Entry<Object, Object> expectedEntry : (Set<Map.Entry<Object, Object>>) expectedMap.entrySet()) {
      Object key = expectedEntry.getKey();
      if (shouldIgnoreField(key)) {
        continue;
      }
      if (!actualMap.containsKey(key)) {
        return false;
      }
      if (!compare(expectedEntry.getValue(), actualMap.get(key))) {
        return false;
      }
    }
    return true;
  }

  private boolean shouldIgnoreField(Object key) {
    return key instanceof String && ignoredFields.contains((String) key);
  }

  @CheckForNull
  private static Date tryParseDate(String s) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
    } catch (ParseException ignored) {
      // not a datetime
      return null;
    }
  }
}
