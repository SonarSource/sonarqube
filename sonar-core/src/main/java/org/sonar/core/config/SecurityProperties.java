/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static java.util.Arrays.asList;
import static org.sonar.api.CoreProperties.CATEGORY_SECURITY;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;
import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY;

class SecurityProperties {

  private SecurityProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return asList(
      PropertyDefinition.builder(CORE_FORCE_AUTHENTICATION_PROPERTY)
        .defaultValue(Boolean.toString(CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE))
        .name("Force user authentication")
        .description(
          "Forcing user authentication prevents anonymous users from accessing the SonarQube UI, or project data via the Web API. "
            + "Some specific read-only Web APIs, including those required to prompt authentication, are still available anonymously."
            + "<br><strong>Disabling this setting can expose the instance to security risks.</strong>")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY_SECURITY)
        .build(),
      PropertyDefinition.builder(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)
        .defaultValue(Boolean.toString(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE))
        .name("Enable permission management for project administrators")
        .description(
          "Set if users with 'Administer' role in a project should be allowed to change project permissions. By default users with 'Administer' " +
            "role are allowed to change both project configuration and project permissions.")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY_SECURITY)
        .build(),
      PropertyDefinition.builder(SONAR_VALIDATE_WEBHOOKS_PROPERTY)
        .defaultValue(Boolean.toString(SONAR_VALIDATE_WEBHOOKS_DEFAULT_VALUE))
        .name("Enable local webhooks validation")
        .description(
          "Forcing local webhooks validation prevents the creation and triggering of local webhooks"
            + "<br><strong>Disabling this setting can expose the instance to security risks.</strong>")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY_SECURITY)
        .build()
    );

  }
}
