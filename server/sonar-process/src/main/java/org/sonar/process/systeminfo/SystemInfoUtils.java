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
package org.sonar.process.systeminfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;

import static java.util.Arrays.stream;

public class SystemInfoUtils {

  private SystemInfoUtils() {
    // prevent instantiation
  }

  public static void setAttribute(Section.Builder section, String key, @Nullable String value) {
    if (value != null) {
      section.addAttributesBuilder()
        .setKey(key)
        .setStringValue(value)
        .build();
    }
  }

  public static void setAttribute(Section.Builder section, String key, @Nullable Collection<String> values) {
    if (values != null) {
      section.addAttributesBuilder()
        .setKey(key)
        .addAllStringValues(values)
        .build();
    }
  }

  public static void setAttribute(Section.Builder section, String key, @Nullable Boolean value) {
    if (value != null) {
      section.addAttributesBuilder()
        .setKey(key)
        .setBooleanValue(value)
        .build();
    }
  }

  public static void setAttribute(Section.Builder section, String key, long value) {
    section.addAttributesBuilder()
      .setKey(key)
      .setLongValue(value)
      .build();
  }

  @CheckForNull
  public static ProtobufSystemInfo.Attribute attribute(Section section, String key) {
    for (ProtobufSystemInfo.Attribute attribute : section.getAttributesList()) {
      if (attribute.getKey().equals(key)) {
        return attribute;
      }
    }
    return null;
  }

  public static List<Section> order(Collection<Section> sections, String... orderedNames) {
    Map<String, Section> alphabeticalOrderedMap = new TreeMap<>();
    sections.forEach(section -> alphabeticalOrderedMap.put(section.getName(), section));

    List<Section> result = new ArrayList<>(sections.size());
    stream(orderedNames).forEach(name -> {
      Section section = alphabeticalOrderedMap.remove(name);
      if (section != null) {
        result.add(section);
      }
    });
    result.addAll(alphabeticalOrderedMap.values());
    return result;
  }
}
