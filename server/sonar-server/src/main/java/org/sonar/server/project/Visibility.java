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
package org.sonar.server.project;

import java.util.List;

import static java.util.Arrays.stream;
import static org.sonar.core.util.stream.MoreCollectors.toList;

public enum Visibility {

  PRIVATE(true, "private"),
  PUBLIC(false, "public");

  private static final List<String> LABELS = stream(values()).map(Visibility::getLabel).collect(toList(values().length));

  private final boolean isPrivate;
  private final String label;

  Visibility(boolean isPrivate, String label) {
    this.isPrivate = isPrivate;
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  boolean isPrivate() {
    return isPrivate;
  }

  public static String getLabel(boolean isPrivate) {
    return stream(values())
      .filter(v -> v.isPrivate == isPrivate)
      .map(Visibility::getLabel)
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Invalid visibility boolean '" + isPrivate + "'"));
  }

  public static boolean isPrivate(String label) {
    return parseVisibility(label).isPrivate();
  }

  public static Visibility parseVisibility(String label) {
    return stream(values())
      .filter(v -> v.label.equals(label))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Invalid visibility label '" + label + "'"));
  }

  public static List<String> getLabels() {
    return LABELS;
  }
}
