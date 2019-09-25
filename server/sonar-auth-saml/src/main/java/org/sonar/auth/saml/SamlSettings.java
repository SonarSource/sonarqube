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
package org.sonar.auth.saml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.server.ServerSide;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;

@ServerSide
public class SamlSettings {

  private static final String ENABLED = "sonar.auth.saml.enabled";
  private static final String PROVIDER_ID = "sonar.auth.saml.providerId";
  private static final String PROVIDER_NAME = "sonar.auth.saml.providerName";

  private static final String APPLICATION_ID = "sonar.auth.saml.applicationId";
  private static final String LOGIN_URL = "sonar.auth.saml.loginUrl";
  private static final String CERTIFICATE = "sonar.auth.saml.certificate.secured";

  private static final String USER_LOGIN_ATTRIBUTE = "sonar.auth.saml.user.login";
  private static final String USER_NAME_ATTRIBUTE = "sonar.auth.saml.user.name";
  private static final String USER_EMAIL_ATTRIBUTE = "sonar.auth.saml.user.email";
  private static final String GROUP_NAME_ATTRIBUTE = "sonar.auth.saml.group.name";

  private static final String CATEGORY = "security";
  private static final String SUBCATEGORY = "saml";

  private final Configuration configuration;

  public SamlSettings(Configuration configuration) {
    this.configuration = configuration;
  }

  String getProviderId() {
    return configuration.get(PROVIDER_ID).orElseThrow(() -> new IllegalArgumentException("Provider ID is missing"));
  }

  String getProviderName() {
    return configuration.get(PROVIDER_NAME).orElseThrow(() -> new IllegalArgumentException("Provider Name is missing"));
  }

  String getApplicationId() {
    return configuration.get(APPLICATION_ID).orElseThrow(() -> new IllegalArgumentException("Application ID is missing"));
  }

  String getLoginUrl() {
    return configuration.get(LOGIN_URL).orElseThrow(() -> new IllegalArgumentException("Login URL is missing"));
  }

  String getCertificate() {
    return configuration.get(CERTIFICATE).orElseThrow(() -> new IllegalArgumentException("Certificate is missing"));
  }

  String getUserLogin() {
    return configuration.get(USER_LOGIN_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User login attribute is missing"));
  }

  String getUserName() {
    return configuration.get(USER_NAME_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User name attribute is missing"));
  }

  Optional<String> getUserEmail() {
    return configuration.get(USER_EMAIL_ATTRIBUTE);
  }

  Optional<String> getGroupName() {
    return configuration.get(GROUP_NAME_ATTRIBUTE);
  }

  boolean isEnabled() {
    return configuration.getBoolean(ENABLED).orElse(false) &&
      configuration.get(PROVIDER_ID).isPresent() &&
      configuration.get(APPLICATION_ID).isPresent() &&
      configuration.get(LOGIN_URL).isPresent() &&
      configuration.get(CERTIFICATE).isPresent() &&
      configuration.get(USER_LOGIN_ATTRIBUTE).isPresent() &&
      configuration.get(USER_NAME_ATTRIBUTE).isPresent();
  }

  static List<PropertyDefinition> definitions() {
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable SAML users to login. Value is ignored if provider ID, login url, certificate, login, name attributes are not defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(1)
        .build(),
      PropertyDefinition.builder(APPLICATION_ID)
        .name("Application ID")
        .description("Identifier of the application.")
        .defaultValue("sonarqube")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(2)
        .build(),
      PropertyDefinition.builder(PROVIDER_NAME)
        .name("Provider Name")
        .description("Name displayed for the provider in the login page.")
        .defaultValue("SAML")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(3)
        .build(),
      PropertyDefinition.builder(PROVIDER_ID)
        .name("Provider ID")
        .description("Identifier of the identity provider, the entity that provides SAML authentication.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(4)
        .build(),
      PropertyDefinition.builder(LOGIN_URL)
        .name("SAML login url")
        .description("SAML login URL for the identity provider.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(5)
        .build(),
      PropertyDefinition.builder(CERTIFICATE)
        .name("Provider certificate")
        .description("X.509 certificate for the identity provider.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(6)
        .build(),
      PropertyDefinition.builder(USER_LOGIN_ATTRIBUTE)
        .name("SAML user login attribute")
        .description("Attribute defining the user login in SAML.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(7)
        .build(),
      PropertyDefinition.builder(USER_NAME_ATTRIBUTE)
        .name("SAML user name attribute")
        .description("Attribute defining the user name in SAML.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(8)
        .build(),
      PropertyDefinition.builder(USER_EMAIL_ATTRIBUTE)
        .name("SAML user email attribute")
        .description("Attribute defining the user email in SAML.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(9)
        .build(),
      PropertyDefinition.builder(GROUP_NAME_ATTRIBUTE)
        .name("SAML group attribute")
        .description("Attribute defining the user groups in SAML. " +
          "Users are associated to the default group only if no attribute is defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(10)
        .build());
  }
}
