/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.authentication;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.authentication.event.AuthenticationEvent;

import static org.sonar.server.user.GithubWebhookUserSession.GITHUB_WEBHOOK_USER_NAME;

public class GithubWebhookAuthentication {
  private final AuthenticationEvent authenticationEvent;

  public GithubWebhookAuthentication(AuthenticationEvent authenticationEvent) {
    this.authenticationEvent = authenticationEvent;
  }

  public Optional<UserAuthResult> authenticate(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("x-hub-signature-256");
    if (StringUtils.isEmpty(authorizationHeader)) {
      return Optional.empty();
    }
    //TODO SONAR-17269 implement authentication algorithm
    UserAuthResult userAuthResult = UserAuthResult.withGithubWebhook();
    authenticationEvent.loginSuccess(request, GITHUB_WEBHOOK_USER_NAME, AuthenticationEvent.Source.githubWebhook());
    return Optional.of(userAuthResult);
  }
}
