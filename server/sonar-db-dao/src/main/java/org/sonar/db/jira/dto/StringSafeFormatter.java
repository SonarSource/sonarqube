/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.jira.dto;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;

public final class StringSafeFormatter {

  private static final Set<String> SECRET_MEMBER_NAMES = Set.of(
    "JIRAACCESSTOKEN",
    "JIRAACCESSTOKENEXPIRESAT",
    "JIRAREFRESHTOKEN",
    "JIRAREFRESHTOKENCREATEDAT",
    "JIRAREFRESHTOKENUPDATEDAT",
    "SECRET"
  );

  private StringSafeFormatter() {
    // static class
  }

  public static String toString(Object instance) {
    var fieldStrings = getSafeToPrintFieldValues(instance)
      .stream()
      .map(x -> format("%s='%s'", x.getKey(), x.getValue()))
      .distinct()
      .sorted()
      .toList();

    return format("%s[%s]", instance.getClass().getName(), String.join(", ", fieldStrings));
  }

  private static List<Map.Entry<String, String>> getSafeToPrintFieldValues(Object instance) {
    var clazz = instance.getClass();
    var allMembers = Stream.concat(
      Arrays.stream(clazz.getDeclaredFields()),
      Arrays.stream(clazz.isRecord() ? clazz.getRecordComponents() : new RecordComponent[0])
    );

    return allMembers
      .map(member -> getEntrySafe(instance, member))
      .filter(Objects::nonNull)
      .toList();
  }

  private static Map.Entry<String, String> getEntrySafe(Object instance, Object member) {
    try {
      return getEntry(instance, member);
    } catch (Exception e) {
      return null;
    }
  }

  private static Map.Entry<String, String> getEntry(Object instance, Object member) throws InvocationTargetException, IllegalAccessException {
    String name;
    Object value;

    if (member instanceof Field field) {
      name = field.getName();
      var accessible = field.canAccess(instance);
      field.setAccessible(true);
      value = field.get(instance);
      field.setAccessible(accessible);
    } else {
      var recordComponent = ((RecordComponent) member);
      name = recordComponent.getName();
      value = recordComponent.getAccessor().invoke(instance);
    }

    if (SECRET_MEMBER_NAMES.contains(name.toUpperCase(Locale.getDefault()))) {
      value = "***";
    }
    return Map.entry(name, value == null ? "" : value.toString());
  }
}
