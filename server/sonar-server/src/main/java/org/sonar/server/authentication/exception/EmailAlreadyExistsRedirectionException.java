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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;

import static org.sonar.server.authentication.AuthenticationError.addErrorCookie;

/**
 * This exception is used to redirect the user to a page explaining him that his email is already used by another account,
 * and where he has the ability to authenticate by "steeling" this email.
 */
public class EmailAlreadyExistsRedirectionException extends RedirectionException {

  private static final String PATH = "/sessions/email_already_exists";
  private static final String EMAIL_FIELD = "email";
  private static final String LOGIN_FIELD = "login";
  private static final String PROVIDER_FIELD = "provider";
  private static final String EXISTING_LOGIN_FIELD = "existingLogin";
  private static final String EXISTING_PROVIDER_FIELD = "existingProvider";

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

  public void addCookie(HttpServletRequest request, HttpServletResponse response) {
    Gson gson = new GsonBuilder().create();
    String message = gson.toJson(ImmutableMap.of(
      EMAIL_FIELD, email,
      LOGIN_FIELD, userIdentity.getProviderLogin(),
      PROVIDER_FIELD, provider.getKey(),
      EXISTING_LOGIN_FIELD, existingUser.getExternalLogin(),
      EXISTING_PROVIDER_FIELD, existingUser.getExternalIdentityProvider()));
    addErrorCookie(request, response, message);
  }

  @Override
  public String getPath(String contextPath) {
    return contextPath + PATH;
  }
}
