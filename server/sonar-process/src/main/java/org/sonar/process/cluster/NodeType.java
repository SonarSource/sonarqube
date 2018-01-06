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
package org.sonar.process.cluster;

import static java.util.Arrays.stream;

public enum NodeType {
  APPLICATION("application"), SEARCH("search");

  private final String value;

  NodeType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static NodeType parse(String nodeType) {
    return stream(values())
      .filter(t -> nodeType.equals(t.value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + nodeType));
  }

  public static boolean isValid(String nodeType) {
    return stream(values())
      .anyMatch(t -> nodeType.equals(t.value));
  }
}
