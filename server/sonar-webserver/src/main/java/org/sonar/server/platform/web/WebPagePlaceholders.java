/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.web;

import java.util.Set;

final class WebPagePlaceholders {
  static final String WEB_CONTEXT = "WEB_CONTEXT";
  static final String SERVER_STATUS = "%SERVER_STATUS%";
  static final String INSTANCE = "%INSTANCE%";
  static final String OFFICIAL = "%OFFICIAL%";

  private static final Set<String> SERVING_TIME_PLACEHOLDERS = Set.of(SERVER_STATUS, INSTANCE, OFFICIAL);

  private WebPagePlaceholders() {
    // Utility class
  }

  static boolean containsServingTimePlaceholder(String content) {
    return SERVING_TIME_PLACEHOLDERS.stream().anyMatch(content::contains);
  }
}
