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
package org.sonar.server.ui;

import com.google.common.base.Splitter;
import java.util.List;

public class VersionFormatter {
  private static final String VERSION_SEQUENCE_SEPARATION = ".";

  private VersionFormatter() {
    // prevent instantiation
  }

  public static String format(String technicalVersion) {
    List<String> elements = Splitter.on(VERSION_SEQUENCE_SEPARATION).splitToList(technicalVersion);
    if (elements.size() != 4) {
      return technicalVersion;
    }

    // version has the form 6.3.1.4563
    StringBuilder builder = new StringBuilder();
    builder
      .append(elements.get(0))
      .append(VERSION_SEQUENCE_SEPARATION)
      .append(elements.get(1));
    if (!"0".equals(elements.get(2))) {
      builder.append(VERSION_SEQUENCE_SEPARATION)
        .append(elements.get(2));
    }

    builder.append(" (build ").append(elements.get(3)).append(")");

    return builder.toString();
  }
}
