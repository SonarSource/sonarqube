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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class OAuth2CallbackFilterTest {

  private static final String OAUTH2_PROVIDER_KEY = "github";
  private static final String LOGIN = "foo";

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private OAuth2ContextFactory oAuth2ContextFactory = mock(OAuth2ContextFactory.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private Server server = mock(Server.class);
  private FilterChain chain = mock(FilterChain.class);

  private FakeOAuth2IdentityProvider oAuth2IdentityProvider = new WellbehaveFakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true, LOGIN);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private OAuth2Redirection oAuthRedirection = mock(OAuth2Redirection.class);

  private ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);

  private OAuth2CallbackFilter underTest = new OAuth2CallbackFilter(identityProviderRepository, oAuth2ContextFactory, server, authenticationEvent, oAuthRedirection);

  @Before
  public void setUp() throws Exception {
    when(oAuth2ContextFactory.newCallback(request, response, oAuth2IdentityProvider)).thenReturn(mock(OAuth2IdentityProvider.CallbackContext.class));
    when(server.getContextPath()).thenReturn("");
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern()).isNotNull();
  }

  @Test
  public void do_filter_with_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertCallbackCalled(oAuth2IdentityProvider);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.oauth2(oAuth2IdentityProvider));
  }

  @Test
  public void do_filter_with_context_no_log_if_provider_did_not_call_authenticate_on_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    FakeOAuth2IdentityProvider identityProvider = new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true);
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    assertCallbackCalled(identityProvider);
    verify(authenticationEvent).loginFailure(eq(request), authenticationExceptionCaptor.capture());
    AuthenticationException authenticationException = authenticationExceptionCaptor.getValue();
    assertThat(authenticationException).hasMessage("Plugin did not call authenticate");
    assertThat(authenticationException.getSource()).isEqualTo(Source.oauth2(identityProvider));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getPublicMessage()).isNull();
  }

  @Test
  public void do_filter_on_auth2_identity_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertCallbackCalled(oAuth2IdentityProvider);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.oauth2(oAuth2IdentityProvider));
  }

  @Test
  public void fail_on_not_oauth2_provider() throws Exception {
    String providerKey = "openid";
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + providerKey);
    identityProviderRepository.addIdentityProvider(new FakeBasicIdentityProvider(providerKey, true));

    underTest.doFilter(request, response, chain);

    assertError("Not an OAuth2IdentityProvider: class org.sonar.server.authentication.FakeBasicIdentityProvider");
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void fail_on_disabled_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, false));

    underTest.doFilter(request, response, chain);

    assertError("Failed to retrieve IdentityProvider for key 'github'");
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void redirect_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    FailWithUnauthorizedExceptionIdProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider();
    identityProvider
      .setKey("failing")
      .setName("name of failing")
      .setEnabled(true);
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized?message=Email+john%40email.com+is+already+used");
    verify(authenticationEvent).loginFailure(eq(request), authenticationExceptionCaptor.capture());
    AuthenticationException authenticationException = authenticationExceptionCaptor.getValue();
    assertThat(authenticationException).hasMessage("Email john@email.com is already used");
    assertThat(authenticationException.getSource()).isEqualTo(Source.oauth2(identityProvider));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getPublicMessage()).isEqualTo("Email john@email.com is already used");
    verify(oAuthRedirection).delete(eq(request), eq(response));
  }

  @Test
  public void redirect_with_context_path_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    FailWithUnauthorizedExceptionIdProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider();
    identityProvider
      .setKey("failing")
      .setName("name of failing")
      .setEnabled(true);
    when(request.getRequestURI()).thenReturn("/sonarqube/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized?message=Email+john%40email.com+is+already+used");
    verify(oAuthRedirection).delete(eq(request), eq(response));
  }

  @Test
  public void redirect_when_failing_because_of_Exception() throws Exception {
    FailWithIllegalStateException identityProvider = new FailWithIllegalStateException();
    identityProvider
      .setKey("failing")
      .setName("name of failing")
      .setEnabled(true);
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactlyInAnyOrder("Fail to callback authentication with 'failing'");
    verify(oAuthRedirection).delete(eq(request), eq(response));
  }

  @Test
  public void fail_when_no_oauth2_provider_provided() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback");

    underTest.doFilter(request, response, chain);

    assertError("No provider key found in URI");
    verifyZeroInteractions(authenticationEvent);
  }

  private void assertCallbackCalled(FakeOAuth2IdentityProvider oAuth2IdentityProvider) {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(oAuth2IdentityProvider.isCallbackCalled()).isTrue();
  }

  private void assertError(String expectedError) throws Exception {
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains(expectedError);
    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(oAuth2IdentityProvider.isInitCalled()).isFalse();
  }

  private static class FailWithUnauthorizedExceptionIdProvider extends TestIdentityProvider implements OAuth2IdentityProvider {

    @Override
    public void init(InitContext context) {

    }

    @Override
    public void callback(CallbackContext context) {
      throw new UnauthorizedException("Email john@email.com is already used");
    }
  }

  private static class FailWithIllegalStateException extends TestIdentityProvider implements OAuth2IdentityProvider {

    @Override
    public void init(InitContext context) {

    }

    @Override
    public void callback(CallbackContext context) {
      throw new IllegalStateException("Failure !");
    }
  }

  /**
   * An extension of {@link FakeOAuth2IdentityProvider} that actually call {@link org.sonar.api.server.authentication.OAuth2IdentityProvider.CallbackContext#authenticate(UserIdentity)}.
   */
  private static class WellbehaveFakeOAuth2IdentityProvider extends FakeOAuth2IdentityProvider {
    private final String login;

    public WellbehaveFakeOAuth2IdentityProvider(String key, boolean enabled, String login) {
      super(key, enabled);
      this.login = login;
    }

    @Override
    public void callback(CallbackContext context) {
      super.callback(context);
      context.authenticate(UserIdentity.builder()
        .setLogin(login)
        .setProviderLogin(login)
        .setEmail(login + "@toto.com")
        .setName("name of " + login)
        .build());
    }
  }
}
