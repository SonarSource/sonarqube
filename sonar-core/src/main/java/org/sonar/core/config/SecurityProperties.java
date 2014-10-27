/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

class SecurityProperties {

  private SecurityProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return ImmutableList.of(

      PropertyDefinition.builder(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY)
        .defaultValue("" + CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE)
        .name("Allow users to sign up online")
        .description("Users can sign up online.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_DEFAULT_GROUP)
        .defaultValue(CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE)
        .name("Default user group")
        .description("Any new users will automatically join this group.")
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)
        .defaultValue("" + CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE)
        .name("Force user authentication")
        .description("Forcing user authentication stops un-logged users to access SonarQube.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build(),

      PropertyDefinition.builder(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION)
        .defaultValue(Boolean.toString(false))
        .name("Prevent automatic project creation")
        .description("Set to true to prevent automatic project creation at first analysis and force project provisioning.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build()
      );

  }
}
