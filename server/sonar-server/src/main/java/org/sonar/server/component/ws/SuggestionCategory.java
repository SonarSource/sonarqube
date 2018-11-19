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
package org.sonar.server.component.ws;

import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.stream;

public enum SuggestionCategory {
  VIEW(Qualifiers.VIEW),
  SUBVIEW(Qualifiers.SUBVIEW),
  APP(Qualifiers.APP),
  PROJECT(Qualifiers.PROJECT),
  MODULE(Qualifiers.MODULE),
  FILE(Qualifiers.FILE),
  UNIT_TEST_FILE(Qualifiers.UNIT_TEST_FILE),;

  private final String qualifier;

  SuggestionCategory(String qualifier) {
    this.qualifier = qualifier;
  }

  public String getName() {
    return qualifier;
  }

  public String getQualifier() {
    return qualifier;
  }

  public static SuggestionCategory getByName(String name) {
    return stream(values()).filter(c -> c.getName().equals(name)).findAny()
      .orElseThrow(() -> new IllegalStateException(String.format("Cannot find category for name '%s'.", name)));
  }
}
