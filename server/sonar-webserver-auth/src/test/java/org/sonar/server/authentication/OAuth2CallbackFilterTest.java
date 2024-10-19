/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.web.FilterChain;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class OAuth2CallbackFilterTest {

  private static final String OAUTH2_PROVIDER_KEY = "github";
  private static final String LOGIN = "foo";

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private OAuth2ContextFactory oAuth2ContextFactory = mock(OAuth2ContextFactory.class);

  private HttpRequest request = mock(HttpRequest.class);
  private HttpResponse response = mock(HttpResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private FakeOAuth2IdentityProvider oAuth2IdentityProvider = new WellbehaveFakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true, LOGIN);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private OAuth2AuthenticationParameters oAuthRedirection = mock(OAuth2AuthenticationParameters.class);
  private ThreadLocalUserSession threadLocalUserSession = mock(ThreadLocalUserSession.class);

  private ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private OAuth2CallbackFilter underTest = new OAuth2CallbackFilter(identityProviderRepository, oAuth2ContextFactory, authenticationEvent, oAuthRedirection,
    threadLocalUserSession);

  @Before
  public void setUp() {
    when(oAuth2ContextFactory.newCallback(request, response, oAuth2IdentityProvider)).thenReturn(mock(OAuth2IdentityProvider.CallbackContext.class));
    when(request.getContextPath()).thenReturn("");
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern()).isNotNull();
  }

  @Test
  public void do_filter_with_context() {
    when(request.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);
    when(threadLocalUserSession.hasSession()).thenReturn(true);
    when(threadLocalUserSession.getLogin()).thenReturn(LOGIN);

    underTest.doFilter(request, response, chain);

    assertCallbackCalled(oAuth2IdentityProvider);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.oauth2(oAuth2IdentityProvider));
  }

  @Test
  public void do_filter_with_context_no_log_if_provider_did_not_call_authenticate_on_context() {
    when(request.getContextPath()).thenReturn("/sonarqube");
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
  public void do_filter_on_auth2_identity_provider() {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);
    when(threadLocalUserSession.hasSession()).thenReturn(true);
    when(threadLocalUserSession.getLogin()).thenReturn(LOGIN);

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
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_on_disabled_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, false));

    underTest.doFilter(request, response, chain);

    assertError("Failed to retrieve IdentityProvider for key 'github'");
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void redirect_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    FailWithUnauthorizedExceptionIdProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider();
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized");
    verify(authenticationEvent).loginFailure(eq(request), authenticationExceptionCaptor.capture());
    AuthenticationException authenticationException = authenticationExceptionCaptor.getValue();
    assertThat(authenticationException).hasMessage("Email john@email.com is already used");
    assertThat(authenticationException.getSource()).isEqualTo(Source.oauth2(identityProvider));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getPublicMessage()).isEqualTo("Email john@email.com is already used");
    verify(oAuthRedirection).delete(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie cookie = cookieArgumentCaptor.getValue();
    assertThat(cookie.getName()).isEqualTo("AUTHENTICATION-ERROR");
    assertThat(cookie.getValue()).isEqualTo("Email%20john%40email.com%20is%20already%20used");
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.getMaxAge()).isEqualTo(300);
    assertThat(cookie.isSecure()).isFalse();
  }

  @Test
  public void redirect_with_context_path_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    FailWithUnauthorizedExceptionIdProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider();
    when(request.getRequestURI()).thenReturn("/sonarqube/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized");
    verify(oAuthRedirection).delete(request, response);
  }

  @Test
  public void redirect_when_failing_because_of_Exception() throws Exception {
    FailWithIllegalStateException identityProvider = new FailWithIllegalStateException();
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(logTester.logs(Level.WARN)).containsExactlyInAnyOrder("Fail to callback authentication with 'failing'");
    verify(oAuthRedirection).delete(request, response);
  }

  @Test
  public void redirect_with_context_when_failing_because_of_Exception() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    FailWithIllegalStateException identityProvider = new FailWithIllegalStateException();
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized");
  }

  @Test
  public void fail_when_no_oauth2_provider_provided() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback");

    underTest.doFilter(request, response, chain);

    assertError("No provider key found in URI");
    verifyNoInteractions(authenticationEvent);
  }

  private void assertCallbackCalled(FakeOAuth2IdentityProvider oAuth2IdentityProvider) {
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(oAuth2IdentityProvider.isCallbackCalled()).isTrue();
  }

  private void assertError(String expectedError) throws Exception {
    assertThat(logTester.logs(Level.WARN)).contains(expectedError);
    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(oAuth2IdentityProvider.isInitCalled()).isFalse();
  }

  private static class FailWithUnauthorizedExceptionIdProvider extends FailingIdentityProvider {
    @Override
    public void callback(CallbackContext context) {
      throw new UnauthorizedException("Email john@email.com is already used");
    }
  }

  private static class FailWithIllegalStateException extends FailingIdentityProvider {
    @Override
    public void callback(CallbackContext context) {
      throw new IllegalStateException("Failure !");
    }
  }

  private static abstract class FailingIdentityProvider extends TestIdentityProvider implements OAuth2IdentityProvider {
    FailingIdentityProvider() {
      this.setKey("failing");
      this.setName("Failing");
      this.setEnabled(true);
    }

    @Override
    public void init(InitContext context) {
      // Nothing to do
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
        .setProviderLogin(login)
        .setEmail(login + "@toto.com")
        .setName("name of " + login)
        .build());
    }
  }
}
