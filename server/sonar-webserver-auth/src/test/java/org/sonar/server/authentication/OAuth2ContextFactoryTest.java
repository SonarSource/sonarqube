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

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.OAuth2ContextFactory.OAuthContextImpl;
import org.sonar.server.authentication.UserRegistration.ExistingEmailStrategy;
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
    .setLogin("id:johndoo")
    .setName("John")
    .setEmail("john@email.com")
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ThreadLocalUserSession threadLocalUserSession = mock(ThreadLocalUserSession.class);
  private TestUserRegistrar userIdentityAuthenticator = new TestUserRegistrar();
  private Server server = mock(Server.class);
  private OAuthCsrfVerifier csrfVerifier = mock(OAuthCsrfVerifier.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();
  private OAuth2AuthenticationParameters oAuthParameters = mock(OAuth2AuthenticationParameters.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private HttpSession session = mock(HttpSession.class);
  private OAuth2IdentityProvider identityProvider = mock(OAuth2IdentityProvider.class);

  private OAuth2ContextFactory underTest = new OAuth2ContextFactory(threadLocalUserSession, userIdentityAuthenticator, server, csrfVerifier, jwtHttpHandler, userSessionFactory,
    oAuthParameters);

  @Before
  public void setUp() throws Exception {
    when(request.getSession()).thenReturn(session);
    when(identityProvider.getKey()).thenReturn(PROVIDER_KEY);
    when(identityProvider.getName()).thenReturn(PROVIDER_NAME);
  }

  @Test
  public void create_context() {
    when(server.getPublicRootUrl()).thenReturn(SECURED_PUBLIC_ROOT_URL);

    OAuth2IdentityProvider.InitContext context = newInitContext();

    assertThat(context.getRequest()).isEqualTo(request);
    assertThat(context.getResponse()).isEqualTo(response);
    assertThat(context.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void generate_csrf_state() {
    OAuth2IdentityProvider.InitContext context = newInitContext();

    context.generateCsrfState();

    verify(csrfVerifier).generateState(request, response);
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

    assertThat(callback.getRequest()).isEqualTo(request);
    assertThat(callback.getResponse()).isEqualTo(response);
    assertThat(callback.getCallbackUrl()).isEqualTo("https://mydomain.com/oauth2/callback/github");
  }

  @Test
  public void authenticate() {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.authenticate(USER_IDENTITY);

    assertThat(userIdentityAuthenticator.isAuthenticated()).isTrue();
    verify(threadLocalUserSession).set(any(UserSession.class));
    ArgumentCaptor<UserDto> userArgumentCaptor = ArgumentCaptor.forClass(UserDto.class);
    verify(jwtHttpHandler).generateToken(userArgumentCaptor.capture(), eq(request), eq(response));
    assertThat(userArgumentCaptor.getValue().getLogin()).isEqualTo(USER_IDENTITY.getLogin());
    assertThat(userArgumentCaptor.getValue().getExternalId()).isEqualTo(USER_IDENTITY.getProviderId());
    assertThat(userArgumentCaptor.getValue().getExternalLogin()).isEqualTo(USER_IDENTITY.getProviderLogin());
    assertThat(userArgumentCaptor.getValue().getExternalIdentityProvider()).isEqualTo(PROVIDER_KEY);
  }

  @Test
  public void authenticate_with_allow_email_shift() {
    when(oAuthParameters.getAllowEmailShift(request)).thenReturn(Optional.of(true));
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.authenticate(USER_IDENTITY);

    assertThat(userIdentityAuthenticator.isAuthenticated()).isTrue();
    assertThat(userIdentityAuthenticator.getAuthenticatorParameters().getExistingEmailStrategy()).isEqualTo(ExistingEmailStrategy.ALLOW);
  }

  @Test
  public void authenticate_without_email_shift() {
    when(oAuthParameters.getAllowEmailShift(request)).thenReturn(Optional.of(false));
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.authenticate(USER_IDENTITY);

    assertThat(userIdentityAuthenticator.isAuthenticated()).isTrue();
    assertThat(userIdentityAuthenticator.getAuthenticatorParameters().getExistingEmailStrategy()).isEqualTo(ExistingEmailStrategy.WARN);
  }

  @Test
  public void authenticate_with_organization_alm_ids() {
    OAuthContextImpl callback = (OAuthContextImpl) newCallbackContext();
    Set<String> organizationAlmIds = ImmutableSet.of("ABCD", "EFGH");

    callback.authenticate(USER_IDENTITY, organizationAlmIds);

    assertThat(userIdentityAuthenticator.getAuthenticatorParameters().getOrganizationAlmIds()).containsAll(organizationAlmIds);
  }

  @Test
  public void redirect_to_home() throws Exception {
    when(server.getContextPath()).thenReturn("");
    when(oAuthParameters.getReturnTo(request)).thenReturn(Optional.empty());
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/");
  }

  @Test
  public void redirect_to_home_with_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    when(oAuthParameters.getReturnTo(request)).thenReturn(Optional.empty());
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/sonarqube/");
  }

  @Test
  public void redirect_to_requested_page() throws Exception {
    when(oAuthParameters.getReturnTo(request)).thenReturn(Optional.of("/settings"));
    when(server.getContextPath()).thenReturn("");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/settings");
  }

  @Test
  public void redirect_to_requested_page_does_not_need_context() throws Exception {
    when(oAuthParameters.getReturnTo(request)).thenReturn(Optional.of("/sonarqube/settings"));
    when(server.getContextPath()).thenReturn("/other");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(response).sendRedirect("/sonarqube/settings");
  }

  @Test
  public void verify_csrf_state() {
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.verifyCsrfState();

    verify(csrfVerifier).verifyState(request, response, identityProvider);
  }

  @Test
  public void delete_oauth2_parameters_during_redirection() {
    when(oAuthParameters.getReturnTo(request)).thenReturn(Optional.of("/settings"));
    when(server.getContextPath()).thenReturn("");
    OAuth2IdentityProvider.CallbackContext callback = newCallbackContext();

    callback.redirectToRequestedPage();

    verify(oAuthParameters).delete(eq(request), eq(response));
  }

  private OAuth2IdentityProvider.InitContext newInitContext() {
    return underTest.newContext(request, response, identityProvider);
  }

  private OAuth2IdentityProvider.CallbackContext newCallbackContext() {
    return underTest.newCallback(request, response, identityProvider);
  }

}
