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
package org.sonar.server.authentication.exception;

import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationRedirection.encodeMessage;

/**
 * This exception is used to redirect the user to a page explaining him that his login will be updated.
 */
public class UpdateLoginRedirectionException extends RedirectionException {

  private static final String PATH = "/sessions/update_login?login=%s&providerKey=%s&providerName=%s&oldLogin=%s&oldOrganizationKey=%s";

  private final UserIdentity userIdentity;
  private final IdentityProvider provider;
  private final UserDto user;
  private final OrganizationDto organization;

  public UpdateLoginRedirectionException(UserIdentity userIdentity, IdentityProvider provider, UserDto user, OrganizationDto organization) {
    this.userIdentity = userIdentity;
    this.provider = provider;
    this.user = user;
    this.organization = organization;
  }

  @Override
  public String getPath(String contextPath) {
    return contextPath + format(PATH,
      encodeMessage(userIdentity.getProviderLogin()),
      encodeMessage(provider.getKey()),
      encodeMessage(provider.getName()),
      encodeMessage(user.getLogin()),
      encodeMessage(organization.getKey()));
  }
}
