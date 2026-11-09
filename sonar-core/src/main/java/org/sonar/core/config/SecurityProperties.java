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

public final class SecurityProperties {

  public static final String SECRET_SOURCE_REDACTION_ENABLED = "sonar.security.secretSourceRedaction.enabled";
  public static final String WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY = "sonar.validateWebhooks.allowedAddresses";
  public static final String SUBCATEGORY_WEBHOOKS = "webhooks";

  private SecurityProperties() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
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
          "Local webhooks validation blocks webhooks that send requests to local network addresses. "
            + "Keep it enabled and add the private network addresses that your webhooks must reach to Allowed addresses. "
            + "Disable it only if that is not enough.")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY_SECURITY)
        .subCategory(SUBCATEGORY_WEBHOOKS)
        .build(),
      PropertyDefinition.builder(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY)
        .name("Allowed addresses")
        .description("When local webhooks validation is enabled, webhooks can reach the private network addresses listed here. "
          + "Enter one IP address or CIDR range per entry, for example 10.0.0.12 or 10.20.0.0/16. "
          + "Hostnames are not supported, and an entry that is not a valid IP address or CIDR range is ignored. "
          + "Loopback, wildcard, link-local, multicast, cloud metadata addresses and addresses of the SonarQube server itself are always blocked.")
        .type(PropertyType.STRING)
        .multiValues(true)
        .index(1000)
        .category(CATEGORY_SECURITY)
        .subCategory(SUBCATEGORY_WEBHOOKS)
        .build(),
      PropertyDefinition.builder(SECRET_SOURCE_REDACTION_ENABLED)
        .defaultValue(Boolean.toString(false))
        .name("Mask detected secret values in source code")
        .description("When enabled, detected secret values are masked in source-code views and Web API responses that return source code. "
          + "For findings where a safe location cannot be determined, the entire source of the affected file is masked.")
        .type(PropertyType.BOOLEAN)
        .category(CATEGORY_SECURITY)
        .build()
    );

  }
}
