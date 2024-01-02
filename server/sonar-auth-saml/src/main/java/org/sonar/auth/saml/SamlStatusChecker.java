/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.onelogin.saml2.Auth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.sonar.auth.saml.SamlSettings.GROUP_NAME_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_EMAIL_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_LOGIN_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_NAME_ATTRIBUTE;

public final class SamlStatusChecker {

  private SamlStatusChecker() {
    throw new IllegalStateException("This Utility class cannot be instantiated");
  }

  public static SamlAuthenticationStatus getSamlAuthenticationStatus(Auth auth, SamlSettings samlSettings) {

    SamlAuthenticationStatus samlAuthenticationStatus = new SamlAuthenticationStatus();

    try {
      auth.processResponse();
    } catch (Exception e) {
      samlAuthenticationStatus.getErrors().add(e.getMessage());
    }

    samlAuthenticationStatus.getErrors().addAll(auth.getErrors().stream().filter(Objects::nonNull).toList());
    if (auth.getLastErrorReason() != null) {
      samlAuthenticationStatus.getErrors().add(auth.getLastErrorReason());
    }
    if (samlAuthenticationStatus.getErrors().isEmpty()) {
      samlAuthenticationStatus.getErrors().addAll(generateMappingErrors(auth, samlSettings));
    }
    samlAuthenticationStatus.setAvailableAttributes(auth.getAttributes());
    samlAuthenticationStatus.setMappedAttributes(getAttributesMapping(auth, samlSettings));

    samlAuthenticationStatus.setWarnings(samlAuthenticationStatus.getErrors().isEmpty() ? generateWarnings(auth, samlSettings) : new ArrayList<>());
    samlAuthenticationStatus.setStatus(samlAuthenticationStatus.getErrors().isEmpty() ? "success" : "error");

    return samlAuthenticationStatus;

  }

  public static SamlAuthenticationStatus getSamlAuthenticationStatus(String errorMessage) {
    SamlAuthenticationStatus samlAuthenticationStatus = new SamlAuthenticationStatus();
    samlAuthenticationStatus.getErrors().add(errorMessage);
    samlAuthenticationStatus.setStatus("error");

    return samlAuthenticationStatus;
  }

  private static Map<String, Collection<String>> getAttributesMapping(Auth auth, SamlSettings samlSettings) {
    Map<String, Collection<String>> attributesMapping = new HashMap<>();

    attributesMapping.put("User login value", auth.getAttribute(samlSettings.getUserLogin()));
    attributesMapping.put("User name value", auth.getAttribute(samlSettings.getUserName()));

    samlSettings.getUserEmail().ifPresent(emailFieldName -> attributesMapping.put("User email value", auth.getAttribute(emailFieldName)));

    samlSettings.getGroupName().ifPresent(groupFieldName -> attributesMapping.put("Groups value", auth.getAttribute(groupFieldName)));

    return attributesMapping;
  }

  private static List<String> generateWarnings(Auth auth, SamlSettings samlSettings) {
    List<String> warnings = new ArrayList<>(generateMappingWarnings(auth, samlSettings));
    generatePrivateKeyWarning(auth, samlSettings).ifPresent(warnings::add);
    return warnings;
  }

  private static List<String> generateMappingWarnings(Auth auth, SamlSettings samlSettings) {
    Map<String, String> mappings = Map.of(
      USER_EMAIL_ATTRIBUTE, samlSettings.getUserEmail().orElse(""),
      GROUP_NAME_ATTRIBUTE, samlSettings.getGroupName().orElse(""));

    return generateMissingMappingMessages(mappings, auth);
  }

  private static Optional<String> generatePrivateKeyWarning(Auth auth, SamlSettings samlSettings) {
    if (samlSettings.getServiceProviderPrivateKey().isPresent() && auth.getSettings().getSPkey() == null) {
      return Optional.of("Error in parsing service provider private key, please make sure that it is in PKCS 8 format.");
    }
    return Optional.empty();
  }

  private static List<String> generateMappingErrors(Auth auth, SamlSettings samlSettings) {
    Map<String, String> mappings = Map.of(
      USER_NAME_ATTRIBUTE, samlSettings.getUserName(),
      USER_LOGIN_ATTRIBUTE, samlSettings.getUserLogin());

    return generateMissingMappingMessages(mappings, auth);
  }

  private static List<String> generateMissingMappingMessages(Map<String, String> mappings, Auth auth) {
    return mappings.entrySet()
      .stream()
      .filter(entry -> !entry.getValue().isEmpty() && (auth.getAttribute(entry.getValue()) == null || auth.getAttribute(entry.getValue()).isEmpty()))
      .map(entry -> String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", entry.getKey(), entry.getValue()))
      .toList();
  }

}
