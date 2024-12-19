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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import static org.sonar.auth.saml.SamlSettings.GROUP_NAME_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_EMAIL_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_LOGIN_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_NAME_ATTRIBUTE;

@ServerSide
final class SamlStatusChecker {
  private static final Pattern ENCRYPTED_ASSERTION_PATTERN = Pattern.compile("<saml:EncryptedAssertion|<EncryptedAssertion|<saml2:EncryptedAssertion");

  private final SamlSettings samlSettings;

  SamlStatusChecker(SamlSettings samlSettings) {
    this.samlSettings = samlSettings;
  }

  public SamlAuthenticationStatus getSamlAuthenticationStatus(String samlResponse, Saml2AuthenticatedPrincipal principal) {

    SamlAuthenticationStatus samlAuthenticationStatus = new SamlAuthenticationStatus();

    Map<String, List<String>> attributes = principal.getAttributes().entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map(Objects::toString).toList()));

    if (samlAuthenticationStatus.getErrors().isEmpty()) {
      samlAuthenticationStatus.getErrors().addAll(generateMappingErrors(principal, samlSettings));
    }
    samlAuthenticationStatus.setAvailableAttributes(attributes);

    samlAuthenticationStatus.setMappedAttributes(getAttributesMapping(principal, samlSettings));

    samlAuthenticationStatus.setSignatureEnabled(isSignatureEnabled(samlSettings));
    samlAuthenticationStatus.setEncryptionEnabled(isEncryptionEnabled(samlResponse));

    samlAuthenticationStatus.setWarnings(samlAuthenticationStatus.getErrors().isEmpty() ? generateMappingWarnings(principal, samlSettings) : new ArrayList<>());
    samlAuthenticationStatus.setStatus(samlAuthenticationStatus.getErrors().isEmpty() ? "success" : "error");

    return samlAuthenticationStatus;

  }

  public SamlAuthenticationStatus getSamlAuthenticationStatus(String errorMessage) {
    SamlAuthenticationStatus samlAuthenticationStatus = new SamlAuthenticationStatus();
    samlAuthenticationStatus.getErrors().add(errorMessage);
    samlAuthenticationStatus.setStatus("error");
    return samlAuthenticationStatus;
  }

  private static Map<String, Collection<String>> getAttributesMapping(Saml2AuthenticatedPrincipal saml2AuthenticatedPrincipal, SamlSettings samlSettings) {
    Map<String, Collection<String>> attributesMapping = new HashMap<>();

    attributesMapping.put("User login value", saml2AuthenticatedPrincipal.getAttribute(samlSettings.getUserLogin()));
    attributesMapping.put("User name value", saml2AuthenticatedPrincipal.getAttribute(samlSettings.getUserName()));

    samlSettings.getUserEmail().ifPresent(emailFieldName -> attributesMapping.put("User email value", saml2AuthenticatedPrincipal.getAttribute(emailFieldName)));

    samlSettings.getGroupName().ifPresent(groupFieldName -> attributesMapping.put("Groups value", saml2AuthenticatedPrincipal.getAttribute(groupFieldName)));

    return attributesMapping;
  }

  private static List<String> generateMappingWarnings(Saml2AuthenticatedPrincipal principal, SamlSettings samlSettings) {
    Map<String, String> mappings = Map.of(
      USER_EMAIL_ATTRIBUTE, samlSettings.getUserEmail().orElse(""),
      GROUP_NAME_ATTRIBUTE, samlSettings.getGroupName().orElse(""));

    return generateMissingMappingMessages(mappings, principal);
  }

  private static List<String> generateMappingErrors(Saml2AuthenticatedPrincipal principal, SamlSettings samlSettings) {
    Map<String, String> mappings = Map.of(
      USER_NAME_ATTRIBUTE, samlSettings.getUserName(),
      USER_LOGIN_ATTRIBUTE, samlSettings.getUserLogin());

    List<String> mappingErrors = generateMissingMappingMessages(mappings, principal);
    if (mappingErrors.isEmpty()) {
      mappingErrors = generateEmptyMappingsMessages(mappings, principal);
    }

    return mappingErrors;
  }

  private static List<String> generateMissingMappingMessages(Map<String, String> mappings, Saml2AuthenticatedPrincipal principal) {
    return mappings.entrySet()
      .stream()
      .filter(entry -> !entry.getValue().isEmpty() && (principal.getAttribute(entry.getValue()) == null || principal.getAttribute(entry.getValue()).isEmpty()))
      .map(entry -> String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", entry.getKey(), entry.getValue()))
      .toList();
  }

  private static List<String> generateEmptyMappingsMessages(Map<String, String> mappings, Saml2AuthenticatedPrincipal principal) {
    return mappings.entrySet()
      .stream()
      .filter(entry -> (principal.getAttribute(entry.getValue()).size() == 1 && principal.getAttribute(entry.getValue()).contains("")))
      .map(entry -> String.format("Mapping found for the property %s, but the field %s is empty in the SAML response.", entry.getKey(), entry.getValue()))
      .toList();
  }

  private static boolean isSignatureEnabled(SamlSettings samlSettings) {
    return samlSettings.isSignRequestsEnabled();
  }

  private static boolean isEncryptionEnabled(@Nullable String samlResponse) {
    if (samlResponse != null) {
      byte[] decoded = Base64.getDecoder().decode(samlResponse);
      String decodedResponse = new String(decoded, StandardCharsets.UTF_8);
      Matcher matcher = ENCRYPTED_ASSERTION_PATTERN.matcher(decodedResponse);
      //We assume that the presence of an encrypted assertion means that the response is encrypted
      return matcher.find();
    }
    return false;
  }

}
