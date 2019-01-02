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
import org.sonar.db.user.UserDto;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationRedirection.encodeMessage;

/**
 * This exception is used to redirect the user to a page explaining him that his email is already used by another account,
 * and where he has the ability to authenticate by "steeling" this email.
 */
public class EmailAlreadyExistsRedirectionException extends RedirectionException {

  private static final String PATH = "/sessions/email_already_exists?email=%s&login=%s&provider=%s&existingLogin=%s&existingProvider=%s";

  private final String email;
  private final UserDto existingUser;
  private final UserIdentity userIdentity;
  private final IdentityProvider provider;

  public EmailAlreadyExistsRedirectionException(String email, UserDto existingUser, UserIdentity userIdentity, IdentityProvider provider) {
    this.email = email;
    this.existingUser = existingUser;
    this.userIdentity = userIdentity;
    this.provider = provider;
  }

  @Override
  public String getPath(String contextPath) {
    return contextPath + format(PATH,
      encodeMessage(email),
      encodeMessage(userIdentity.getProviderLogin()),
      encodeMessage(provider.getKey()),
      encodeMessage(existingUser.getExternalLogin()),
      encodeMessage(existingUser.getExternalIdentityProvider()));
  }
}
