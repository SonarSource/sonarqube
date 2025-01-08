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
package org.sonar.server.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.sonar.server.user.TestUserSessionFactory;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseContextFactoryTest {

  private static final String PUBLIC_ROOT_URL = "https://mydomain.com";

  private static final UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderId("ABCD")
    .setProviderLogin("johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  private final ThreadLocalUserSession threadLocalUserSession = mock(ThreadLocalUserSession.class);

  private final TestUserRegistrar userIdentityAuthenticator = new TestUserRegistrar();
  private final Server server = mock(Server.class);

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final BaseIdentityProvider identityProvider = mock(BaseIdentityProvider.class);
  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();

  private final BaseContextFactory underTest = new BaseContextFactory(userIdentityAuthenticator, server, jwtHttpHandler, threadLocalUserSession, userSessionFactory);

  @Before
  public void setUp() {
    when(server.getPublicRootUrl()).thenReturn(PUBLIC_ROOT_URL);
    when(identityProvider.getName()).thenReturn("GitHub");
    when(identityProvider.getKey()).thenReturn("github");
  }

  @Test
  public void create_context() {
    JakartaHttpRequest httpRequest = new JakartaHttpRequest(request);
    JakartaHttpResponse httpResponse = new JakartaHttpResponse(response);
    BaseIdentityProvider.Context context = underTest.newContext(httpRequest, httpResponse, identityProvider);

    assertThat(context.getHttpRequest()).isEqualTo(httpRequest);
    assertThat(context.getHttpResponse()).isEqualTo(httpResponse);

    assertThat(context.getServerBaseURL()).isEqualTo(PUBLIC_ROOT_URL);
  }

  @Test
  public void authenticate() {
    JakartaHttpRequest httpRequest = new JakartaHttpRequest(request);
    JakartaHttpResponse httpResponse = new JakartaHttpResponse(response);

    BaseIdentityProvider.Context context = underTest.newContext(httpRequest, httpResponse, identityProvider);
    ArgumentCaptor<UserDto> userArgumentCaptor = ArgumentCaptor.forClass(UserDto.class);

    context.authenticate(USER_IDENTITY);

    assertThat(userIdentityAuthenticator.isAuthenticated()).isTrue();
    verify(threadLocalUserSession).set(any(UserSession.class));
    verify(jwtHttpHandler).generateToken(userArgumentCaptor.capture(), eq(httpRequest), eq(httpResponse));
    assertThat(userArgumentCaptor.getValue().getExternalId()).isEqualTo(USER_IDENTITY.getProviderId());
    assertThat(userArgumentCaptor.getValue().getExternalLogin()).isEqualTo(USER_IDENTITY.getProviderLogin());
    assertThat(userArgumentCaptor.getValue().getExternalIdentityProvider()).isEqualTo("github");
  }

}
