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
package org.sonar.core.config;

import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static java.util.Collections.singletonList;

class SecurityProperties {

  private SecurityProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return singletonList(
      PropertyDefinition.builder(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)
        .defaultValue(Boolean.toString(CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE))
        .name("Force user authentication")
        .description(
          "Forcing user authentication prevents anonymous users from accessing the SonarQube UI, or project data via the Web API. "
            + "Some specific read-only Web APIs, including those required to prompt authentication, are still available anonymously.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build());

  }
}
