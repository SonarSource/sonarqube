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
package org.sonar.auth.saml;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UserIdentity;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import static java.util.stream.Collectors.toSet;

@ServerSide
public class PrincipalToUserIdentityConverter {

  private final SamlSettings samlSettings;

  public PrincipalToUserIdentityConverter(SamlSettings samlSettings) {
    this.samlSettings = samlSettings;
  }

  UserIdentity convertToUserIdentity(Saml2AuthenticatedPrincipal principal) {
    String login = getAttribute(principal, samlSettings.getUserLogin());
    String name = getAttribute(principal, samlSettings.getUserName());

    UserIdentity.Builder userIdentityBuilder = UserIdentity.builder()
      .setProviderLogin(login)
      .setName(name);
    getEmail(principal).ifPresent(userIdentityBuilder::setEmail);
    getGroups(principal).ifPresent(userIdentityBuilder::setGroups);

    return userIdentityBuilder.build();
  }

  private Optional<String> getEmail(Saml2AuthenticatedPrincipal principal) {
    return samlSettings.getUserEmail()
      .map(principal::getFirstAttribute)
      .map(Object::toString);
  }

  private Optional<Set<String>> getGroups(Saml2AuthenticatedPrincipal principal) {
    return samlSettings.getGroupName()
      .map(principal::getAttribute)
      .map(this::toString)
      .filter(set -> !set.isEmpty());
  }

  private Set<String> toString(List<Object> groups) {
    return groups.stream()
      .filter(Objects::nonNull)
      .map(Object::toString)
      .collect(toSet());
  }

  private String getAttribute(Saml2AuthenticatedPrincipal principal, String attribute) {
    return Optional.ofNullable(principal.getFirstAttribute(attribute))
      .map(Object::toString)
      .orElseThrow(() -> new IllegalStateException("%s is missing".formatted(attribute)));
  }

}
