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
package org.sonar.server.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.UserRegistration.ExistingEmailStrategy;
import org.sonar.server.authentication.UserRegistration.UpdateLoginStrategy;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSessionFactory;

public class BaseContextFactory {

  private final ThreadLocalUserSession threadLocalUserSession;
  private final UserRegistrar userRegistrar;
  private final Server server;
  private final JwtHttpHandler jwtHttpHandler;
  private final UserSessionFactory userSessionFactory;

  public BaseContextFactory(UserRegistrar userRegistrar, Server server, JwtHttpHandler jwtHttpHandler,
                            ThreadLocalUserSession threadLocalUserSession, UserSessionFactory userSessionFactory) {
    this.userSessionFactory = userSessionFactory;
    this.userRegistrar = userRegistrar;
    this.server = server;
    this.jwtHttpHandler = jwtHttpHandler;
    this.threadLocalUserSession = threadLocalUserSession;
  }

  public BaseIdentityProvider.Context newContext(HttpServletRequest request, HttpServletResponse response, BaseIdentityProvider identityProvider) {
    return new ContextImpl(request, response, identityProvider);
  }

  private class ContextImpl implements BaseIdentityProvider.Context {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final BaseIdentityProvider identityProvider;

    public ContextImpl(HttpServletRequest request, HttpServletResponse response, BaseIdentityProvider identityProvider) {
      this.request = request;
      this.response = response;
      this.identityProvider = identityProvider;
    }

    @Override
    public HttpServletRequest getRequest() {
      return request;
    }

    @Override
    public HttpServletResponse getResponse() {
      return response;
    }

    @Override
    public String getServerBaseURL() {
      return server.getPublicRootUrl();
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      UserDto userDto = userRegistrar.register(
        UserRegistration.builder()
          .setUserIdentity(userIdentity)
          .setProvider(identityProvider)
          .setSource(Source.external(identityProvider))
          .setExistingEmailStrategy(ExistingEmailStrategy.FORBID)
          .setUpdateLoginStrategy(UpdateLoginStrategy.ALLOW)
          .build());
      jwtHttpHandler.generateToken(userDto, request, response);
      threadLocalUserSession.set(userSessionFactory.create(userDto));
    }
  }
}
