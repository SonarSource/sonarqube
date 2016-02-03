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
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

class SecurityProperties {

  private SecurityProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return ImmutableList.of(

      PropertyDefinition.builder(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY)
        .defaultValue(Boolean.toString(CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE))
        .name("Activate sign up for local account")
        .description("Allow users to sign up online for a local account. For that purpose, a \"Sign Up\" link will be available in the \"Login\" page.")
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
        .defaultValue(Boolean.toString(CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE))
        .name("Force user authentication")
        .description("Forcing user authentication stops un-logged users to access SonarQube.")
        .type(PropertyType.BOOLEAN)
        .category(CoreProperties.CATEGORY_SECURITY)
        .build()
      );

  }
}
