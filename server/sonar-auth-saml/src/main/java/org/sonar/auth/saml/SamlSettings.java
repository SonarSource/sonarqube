/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import static org.sonar.api.PropertyType.PASSWORD;

@ServerSide
public class SamlSettings {
  public static final String ENABLED = "sonar.auth.saml.enabled";
  public static final String PROVIDER_ID = "sonar.auth.saml.providerId";
  public static final String PROVIDER_NAME = "sonar.auth.saml.providerName";

  public static final String APPLICATION_ID = "sonar.auth.saml.applicationId";
  public static final String LOGIN_URL = "sonar.auth.saml.loginUrl";
  public static final String CERTIFICATE = "sonar.auth.saml.certificate.secured";

  public static final String USER_LOGIN_ATTRIBUTE = "sonar.auth.saml.user.login";
  public static final String USER_NAME_ATTRIBUTE = "sonar.auth.saml.user.name";
  public static final String USER_EMAIL_ATTRIBUTE = "sonar.auth.saml.user.email";
  public static final String GROUP_NAME_ATTRIBUTE = "sonar.auth.saml.group.name";

  public static final String SIGN_REQUESTS_ENABLED = "sonar.auth.saml.signature.enabled";
  public static final String SERVICE_PROVIDER_CERTIFICATE = "sonar.auth.saml.sp.certificate.secured";
  public static final String SERVICE_PROVIDER_PRIVATE_KEY = "sonar.auth.saml.sp.privateKey.secured";

  public static final String CATEGORY = "authentication";
  public static final String SUBCATEGORY = "saml";

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
    return configuration.get(CERTIFICATE).orElseThrow(() -> new IllegalArgumentException("Identity provider certificate is missing"));
  }

  String getUserLogin() {
    return configuration.get(USER_LOGIN_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User login attribute is missing"));
  }

  String getUserName() {
    return configuration.get(USER_NAME_ATTRIBUTE).orElseThrow(() -> new IllegalArgumentException("User name attribute is missing"));
  }

  boolean isSignRequestsEnabled() {
    return configuration.getBoolean(SIGN_REQUESTS_ENABLED).orElse(false);
  }

  Optional<String> getServiceProviderPrivateKey() {
    return configuration.get(SERVICE_PROVIDER_PRIVATE_KEY);
  }

  String getServiceProviderCertificate() {
    return configuration.get(SERVICE_PROVIDER_CERTIFICATE).orElseThrow(() -> new IllegalArgumentException("Service provider certificate is missing"));
  }

  Optional<String> getUserEmail() {
    return configuration.get(USER_EMAIL_ATTRIBUTE);
  }

  Optional<String> getGroupName() {
    return configuration.get(GROUP_NAME_ATTRIBUTE);
  }

  public boolean isEnabled() {
    return configuration.getBoolean(ENABLED).orElse(false) &&
      configuration.get(PROVIDER_ID).isPresent() &&
      configuration.get(APPLICATION_ID).isPresent() &&
      configuration.get(LOGIN_URL).isPresent() &&
      configuration.get(CERTIFICATE).isPresent() &&
      configuration.get(USER_LOGIN_ATTRIBUTE).isPresent() &&
      configuration.get(USER_NAME_ATTRIBUTE).isPresent();
  }

  public static List<PropertyDefinition> definitions() {
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
        .description("The identifier used on the Identity Provider for registering SonarQube.")
        .defaultValue("sonarqube")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(2)
        .build(),
      PropertyDefinition.builder(PROVIDER_NAME)
        .name("Provider Name")
        .description("Name of the Identity Provider displayed in the login page when SAML authentication is active.")
        .defaultValue("SAML")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(3)
        .build(),
      PropertyDefinition.builder(PROVIDER_ID)
        .name("Provider ID")
        .description("Identifier of the Identity Provider, the entity that provides SAML authentication.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(4)
        .build(),
      PropertyDefinition.builder(LOGIN_URL)
        .name("SAML login url")
        .description("The URL where the Identity Provider expects to receive SAML requests.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(5)
        .build(),
      PropertyDefinition.builder(CERTIFICATE)
        .name("Identity provider certificate")
        .description("The public X.509 certificate used by the Identity Provider to authenticate SAML messages.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(6)
        .build(),
      PropertyDefinition.builder(USER_LOGIN_ATTRIBUTE)
        .name("SAML user login attribute")
        .description("The name of the attribute where the SAML Identity Provider will put the login of the authenticated user.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(7)
        .build(),
      PropertyDefinition.builder(USER_NAME_ATTRIBUTE)
        .name("SAML user name attribute")
        .description("The name of the attribute where the SAML Identity Provider will put the name of the authenticated user.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(8)
        .build(),
      PropertyDefinition.builder(USER_EMAIL_ATTRIBUTE)
        .name("SAML user email attribute")
        .description("The name of the attribute where the SAML Identity Provider will put the email of the authenticated user.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(9)
        .build(),
      PropertyDefinition.builder(GROUP_NAME_ATTRIBUTE)
        .name("SAML group attribute")
        .description("Attribute defining the user groups in SAML, used to synchronize group memberships. If you leave this field empty, " +
          "group memberships will not be synced when users log in.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(10)
        .build(),
      PropertyDefinition.builder(SIGN_REQUESTS_ENABLED)
        .name("Sign requests")
        .description("Enables signature of SAML requests. It requires both service provider private key and certificate to be set.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(BOOLEAN)
        .defaultValue(valueOf(false))
        .index(11)
        .build(),
      PropertyDefinition.builder(SERVICE_PROVIDER_PRIVATE_KEY)
        .name("Service provider private key")
        .description("PKCS8 stored private key used for signing the requests and decrypting responses from the identity provider. ")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(12)
        .build(),
      PropertyDefinition.builder(SERVICE_PROVIDER_CERTIFICATE)
        .name("Service provider certificate")
        .description("X.509 certificate for the service provider, used for signing the requests and decrypting responses from the identity provider.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PASSWORD)
        .index(13)
        .build());
  }
}
