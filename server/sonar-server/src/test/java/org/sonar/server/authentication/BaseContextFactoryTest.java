/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;

public class BaseContextFactoryTest {

  static String PUBLIC_ROOT_URL = "https://mydomain.com";

  static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin("id:johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);
  Server server = mock(Server.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  BaseIdentityProvider identityProvider = mock(BaseIdentityProvider.class);
  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  BaseContextFactory underTest = new BaseContextFactory(userIdentityAuthenticator, server, jwtHttpHandler);

  @Before
  public void setUp() throws Exception {
    when(server.getPublicRootUrl()).thenReturn(PUBLIC_ROOT_URL);
  }

  @Test
  public void create_context() throws Exception {
    BaseIdentityProvider.Context context = underTest.newContext(request, response, identityProvider);

    assertThat(context.getRequest()).isEqualTo(request);
    assertThat(context.getResponse()).isEqualTo(response);
    assertThat(context.getServerBaseURL()).isEqualTo(PUBLIC_ROOT_URL);
  }

  @Test
  public void authenticate() throws Exception {
    BaseIdentityProvider.Context context = underTest.newContext(request, response, identityProvider);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession()).thenReturn(session);

    context.authenticate(USER_IDENTITY);
    verify(userIdentityAuthenticator).authenticate(USER_IDENTITY, identityProvider);
    verify(jwtHttpHandler).generateToken(any(UserDto.class), eq(response));
  }
}
