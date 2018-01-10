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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class AuthenticatorsImplTest {

  private UserDto user = newUserDto();
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private BasicAuthenticator basicAuthenticator = mock(BasicAuthenticator.class);
  private SsoAuthenticator ssoAuthenticator = mock(SsoAuthenticator.class);
  private Authenticators underTest = new AuthenticatorsImpl(jwtHttpHandler, basicAuthenticator, ssoAuthenticator);

  @Test
  public void authenticate_from_jwt_token() {
    when(ssoAuthenticator.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(user));

    assertThat(underTest.authenticate(request, response)).hasValue(user);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_basic_header() {
    when(basicAuthenticator.authenticate(request)).thenReturn(Optional.of(user));
    when(ssoAuthenticator.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    assertThat(underTest.authenticate(request, response)).hasValue(user);

    verify(jwtHttpHandler).validateToken(request, response);
    verify(basicAuthenticator).authenticate(request);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_sso() {
    when(ssoAuthenticator.authenticate(request, response)).thenReturn(Optional.of(user));
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    assertThat(underTest.authenticate(request, response)).hasValue(user);

    verify(ssoAuthenticator).authenticate(request, response);
    verify(jwtHttpHandler, never()).validateToken(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void return_empty_if_not_authenticated() {
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());
    when(ssoAuthenticator.authenticate(request, response)).thenReturn(Optional.empty());
    when(basicAuthenticator.authenticate(request)).thenReturn(Optional.empty());

    assertThat(underTest.authenticate(request, response)).isEmpty();
    verify(response, never()).setStatus(anyInt());
  }
}
