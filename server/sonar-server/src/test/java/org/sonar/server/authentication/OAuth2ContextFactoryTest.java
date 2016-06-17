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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.MessageException;
import org.sonar.db.user.UserDto;

public class OAuth2ContextFactoryTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static String PROVIDER_KEY = "github";

  static String SECURED_PUBLIC_ROOT_URL = "https://mydomain.com";
  static String NOT_SECURED_PUBLIC_URL = "http://mydomain.com";

  static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin("id:johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  UserIdentityAuthenticator userIdentityAuthenticator = mock(UserIdentityAuthenticator.class);
  Server server = mock(Server.class);
  OAuthCsrfVerifier csrfVerifier = mock(OAuthCsrfVerifier.class);
  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  HttpSession session = mock(HttpSession.class);
  OAuth2IdentityProvider identityProvider = mock(OAuth2IdentityProvider.class);

  OAuth2ContextFactory underTest = new OAuth2ContextFactory(userIdentityAuthenticator, server, csrfVerifier, jwtHttpHandler);

  @Before
  public void setUp() throws Exception {
    when(request.getSession()).thenReturn(session);
    when(identityProvider.getKey()).thenReturn(PROVIDER_KEY);
  }

  @Test
  public void create_context() throws Exception {
    when(server.getPublicRootUrl()).thenReturn(SECURED_PUBLIC_ROOT_URL);

    OAuth2IdentityProvider.InitContext context = newInitContext();

    assertThat(context.getRequest()).isEqualTo(request);
    assertThat(context.getResponse()).isEqualTo(response);
    assertThat(context.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void generate_csrf_state() throws Exception {
    OAuth2IdentityProvider.InitContext context = newInitContext();

    context.generateCsrfState();

    verify(csrfVerifier).generateState(response);
  }

  @Test
  public void redirect_to() throws Exception {
    OAuth2IdentityProvider.InitContext context = newInitContext();

    context.redirectTo("/test");

    verify(response).sendRedirect("/test");
  }

  @Test
  public void fail_to_get_callback_url_on_not_secured_server() throws Exception {
    when(server.getPublicRootUrl()).thenReturn(NOT_SECURED_PUBLIC_URL);

    OAuth2IdentityProvider.InitContext context = newInitContext();

    thrown.expect(MessageException.class);
    thrown.expectMessage("The server url should be configured in https, please update the property 'sonar.core.serverBaseURL'");
    context.getCallbackUrl();
  }

  @Test
  public void create_callback() throws Exception {
    when(server.getPublicRootUrl()).thenReturn(SECURED_PUBLIC_ROOT_URL);

    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    assertThat(callback.getRequest()).isEqualTo(request);
    assertThat(callback.getResponse()).isEqualTo(response);
    assertThat(callback.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void authenticate() throws Exception {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.authenticate(USER_IDENTITY);

    verify(userIdentityAuthenticator).authenticate(USER_IDENTITY, identityProvider);
    verify(jwtHttpHandler).generateToken(any(UserDto.class), eq(response));
  }

  @Test
  public void redirect_to_requested_page() throws Exception {
    when(server.getContextPath()).thenReturn("");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/");
  }

  @Test
  public void redirect_to_requested_page_with_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/sonarqube/");
  }

  @Test
  public void verify_csrf_state() throws Exception {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.verifyCsrfState();

    verify(csrfVerifier).verifyState(request, response);
  }

  private OAuth2IdentityProvider.InitContext newInitContext() {
    return underTest.newContext(request, response, identityProvider);
  }

  private OAuth2IdentityProvider.CallbackContext newCallbackContext() {
    return underTest.newCallback(request, response, identityProvider);
  }

}
