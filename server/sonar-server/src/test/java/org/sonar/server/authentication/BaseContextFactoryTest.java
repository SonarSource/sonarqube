/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.user.TestUserSessionFactory;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.UserIdentityAuthenticator.ExistingEmailStrategy.FORBID;

public class BaseContextFactoryTest {

  private static final String PUBLIC_ROOT_URL = "https://mydomain.com";

  private static final UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin("id:johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ThreadLocalUserSession threadLocalUserSession = mock(ThreadLocalUserSession.class);

  private UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);
  private Server server = mock(Server.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private BaseIdentityProvider identityProvider = mock(BaseIdentityProvider.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();

  private BaseContextFactory underTest = new BaseContextFactory(userIdentityAuthenticator, server, jwtHttpHandler, threadLocalUserSession, userSessionFactory);

  @Before
  public void setUp() throws Exception {
    when(server.getPublicRootUrl()).thenReturn(PUBLIC_ROOT_URL);
    when(identityProvider.getName()).thenReturn("provIdeur Nameuh");
    when(request.getSession()).thenReturn(mock(HttpSession.class));
  }

  @Test
  public void create_context() {
    BaseIdentityProvider.Context context = underTest.newContext(request, response, identityProvider);

    assertThat(context.getRequest()).isEqualTo(request);
    assertThat(context.getResponse()).isEqualTo(response);
    assertThat(context.getServerBaseURL()).isEqualTo(PUBLIC_ROOT_URL);
  }

  @Test
  public void authenticate() {
    UserDto userDto = dbTester.users().insertUser();
    when(userIdentityAuthenticator.authenticate(USER_IDENTITY, identityProvider, Source.external(identityProvider), FORBID)).thenReturn(userDto);
    BaseIdentityProvider.Context context = underTest.newContext(request, response, identityProvider);

    context.authenticate(USER_IDENTITY);

    verify(userIdentityAuthenticator).authenticate(USER_IDENTITY, identityProvider, Source.external(identityProvider), FORBID);
    verify(jwtHttpHandler).generateToken(any(UserDto.class), eq(request), eq(response));
    verify(threadLocalUserSession).set(any(UserSession.class));
  }

}
