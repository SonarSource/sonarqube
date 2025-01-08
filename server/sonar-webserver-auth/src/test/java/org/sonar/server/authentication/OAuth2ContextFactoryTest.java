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

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
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

public class OAuth2ContextFactoryTest {

  private static final String PROVIDER_KEY = "github";
  private static final String SECURED_PUBLIC_ROOT_URL = "https://mydomain.com";
  private static final String PROVIDER_NAME = "provider name";
  private static final UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderId("ABCD")
    .setProviderLogin("johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  private final ThreadLocalUserSession threadLocalUserSession = mock(ThreadLocalUserSession.class);
  private final TestUserRegistrar userIdentityAuthenticator = new TestUserRegistrar();
  private final Server server = mock(Server.class);
  private final OAuthCsrfVerifier csrfVerifier = mock(OAuthCsrfVerifier.class);
  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();
  private final OAuth2AuthenticationParameters oAuthParameters = mock(OAuth2AuthenticationParameters.class);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);

  private final HttpRequest httpRequest = new JakartaHttpRequest(request);
  private final HttpResponse httpResponse = new JakartaHttpResponse(response);

  private final OAuth2IdentityProvider identityProvider = mock(OAuth2IdentityProvider.class);

  private final OAuth2ContextFactory underTest = new OAuth2ContextFactory(threadLocalUserSession, userIdentityAuthenticator, server, csrfVerifier, jwtHttpHandler,
    userSessionFactory,
    oAuthParameters);

  @Before
  public void setUp() {
    when(identityProvider.getKey()).thenReturn(PROVIDER_KEY);
    when(identityProvider.getName()).thenReturn(PROVIDER_NAME);
  }

  @Test
  public void create_context() {
    when(server.getPublicRootUrl()).thenReturn(SECURED_PUBLIC_ROOT_URL);

    OAuth2IdentityProvider.InitContext context = newInitContext();

    assertThat(context.getHttpRequest()).isEqualTo(httpRequest);
    assertThat(context.getHttpResponse()).isEqualTo(httpResponse);

    assertThat(context.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void generate_csrf_state() {
    OAuth2IdentityProvider.InitContext context = newInitContext();

    context.generateCsrfState();

    verify(csrfVerifier).generateState(httpRequest, httpResponse);
  }

  @Test
  public void redirect_to() throws Exception {
    OAuth2IdentityProvider.InitContext context = newInitContext();

    context.redirectTo("/test");

    verify(response).sendRedirect("/test");
  }

  @Test
  public void create_callback() {
    when(server.getPublicRootUrl()).thenReturn(SECURED_PUBLIC_ROOT_URL);

    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    assertThat(callback.getHttpRequest()).isEqualTo(httpRequest);
    assertThat(callback.getHttpResponse()).isEqualTo(httpResponse);
    assertThat(callback.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void authenticate() {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.authenticate(USER_IDENTITY);

    assertThat(userIdentityAuthenticator.isAuthenticated()).isTrue();
    verify(threadLocalUserSession).set(any(UserSession.class));
    ArgumentCaptor<UserDto> userArgumentCaptor = ArgumentCaptor.forClass(UserDto.class);
    verify(jwtHttpHandler).generateToken(userArgumentCaptor.capture(), eq(httpRequest), eq(httpResponse));
    assertThat(userArgumentCaptor.getValue().getExternalId()).isEqualTo(USER_IDENTITY.getProviderId());
    assertThat(userArgumentCaptor.getValue().getExternalLogin()).isEqualTo(USER_IDENTITY.getProviderLogin());
    assertThat(userArgumentCaptor.getValue().getExternalIdentityProvider()).isEqualTo(PROVIDER_KEY);
  }

  @Test
  public void redirect_to_home() throws Exception {
    when(server.getContextPath()).thenReturn("");
    when(oAuthParameters.getReturnTo(httpRequest)).thenReturn(Optional.empty());
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/");
  }

  @Test
  public void redirect_to_home_with_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    when(oAuthParameters.getReturnTo(httpRequest)).thenReturn(Optional.empty());
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/sonarqube/");
  }

  @Test
  public void redirect_to_requested_page() throws Exception {
    when(oAuthParameters.getReturnTo(httpRequest)).thenReturn(Optional.of("/admin/settings"));
    when(server.getContextPath()).thenReturn("");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/admin/settings");
  }

  @Test
  public void redirect_to_requested_page_does_not_need_context() throws Exception {
    when(oAuthParameters.getReturnTo(httpRequest)).thenReturn(Optional.of("/admin/settings"));
    when(server.getContextPath()).thenReturn("/other");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/admin/settings");
  }

  @Test
  public void verify_csrf_state() {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.verifyCsrfState();

    verify(csrfVerifier).verifyState(httpRequest, httpResponse, identityProvider);
  }

  @Test
  public void delete_oauth2_parameters_during_redirection() {
    when(oAuthParameters.getReturnTo(httpRequest)).thenReturn(Optional.of("/admin/settings"));
    when(server.getContextPath()).thenReturn("");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(oAuthParameters).delete(httpRequest, httpResponse);
  }

  private OAuth2IdentityProvider.InitContext newInitContext() {
    return underTest.newContext(httpRequest, httpResponse, identityProvider);
  }

  private OAuth2IdentityProvider.CallbackContext newCallbackContext() {
    return underTest.newCallback(httpRequest, httpResponse, identityProvider);
  }

}
