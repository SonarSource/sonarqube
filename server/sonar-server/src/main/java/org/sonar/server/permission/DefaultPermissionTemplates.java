/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission;

import static java.lang.String.format;

public class DefaultPermissionTemplates {
  public static final String DEFAULT_TEMPLATE_PROPERTY = "sonar.permission.template.default";
  public static final String DEFAULT_TEMPLATE_KEY = "default_template";
  private static final String DEFAULT_ROOT_QUALIFIER_TEMPLATE_PATTERN = "sonar.permission.template.%s.default";

  private DefaultPermissionTemplates() {
    // utility class
  }

  public static String defaultRootQualifierTemplateProperty(String qualifier) {
    return format(DEFAULT_ROOT_QUALIFIER_TEMPLATE_PATTERN, qualifier);
  }
}
