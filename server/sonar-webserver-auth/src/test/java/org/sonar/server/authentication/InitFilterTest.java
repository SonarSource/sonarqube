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
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.web.FilterChain;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class InitFilterTest {

  private static final String OAUTH2_PROVIDER_KEY = "github";
  private static final String BASIC_PROVIDER_KEY = "openid";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  private BaseContextFactory baseContextFactory = mock(BaseContextFactory.class);
  private OAuth2ContextFactory oAuth2ContextFactory = mock(OAuth2ContextFactory.class);

  private HttpRequest request = mock(HttpRequest.class);
  private HttpResponse response = mock(HttpResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private FakeOAuth2IdentityProvider oAuth2IdentityProvider = new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true);
  private OAuth2IdentityProvider.InitContext oauth2Context = mock(OAuth2IdentityProvider.InitContext.class);

  private FakeBasicIdentityProvider baseIdentityProvider = new FakeBasicIdentityProvider(BASIC_PROVIDER_KEY, true);
  private BaseIdentityProvider.Context baseContext = mock(BaseIdentityProvider.Context.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private OAuth2AuthenticationParameters auth2AuthenticationParameters = mock(OAuth2AuthenticationParameters.class);

  private ArgumentCaptor<AuthenticationException> authenticationExceptionCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private InitFilter underTest = new InitFilter(identityProviderRepository, baseContextFactory, oAuth2ContextFactory, authenticationEvent, auth2AuthenticationParameters);

  @Before
  public void setUp() throws Exception {
    when(oAuth2ContextFactory.newContext(request, response, oAuth2IdentityProvider)).thenReturn(oauth2Context);
    when(baseContextFactory.newContext(request, response, baseIdentityProvider)).thenReturn(baseContext);
    when(request.getContextPath()).thenReturn("");
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern()).isNotNull();
  }

  @Test
  public void do_filter_with_context() {
    when(request.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/sessions/init/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertOAuth2InitCalled();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void do_filter_on_auth2_identity_provider() {
    when(request.getRequestURI()).thenReturn("/sessions/init/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertOAuth2InitCalled();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void do_filter_on_basic_identity_provider() {
    when(request.getRequestURI()).thenReturn("/sessions/init/" + BASIC_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(baseIdentityProvider);

    underTest.doFilter(request, response, chain);

    assertBasicInitCalled();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void init_authentication_parameter_on_auth2_identity_provider() {
    when(request.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/sessions/init/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    verify(auth2AuthenticationParameters).init(request, response);
  }

  @Test
  public void does_not_init_authentication_parameter_on_basic_authentication() {
    when(request.getRequestURI()).thenReturn("/sessions/init/" + BASIC_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(baseIdentityProvider);

    underTest.doFilter(request, response, chain);

    verify(auth2AuthenticationParameters, never()).init(request, response);
  }

  @Test
  public void fail_if_identity_provider_key_is_empty() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init/");

    underTest.doFilter(request, response, chain);

    assertError("No provider key found in URI");
    verifyNoInteractions(authenticationEvent);
    verifyNoInteractions(auth2AuthenticationParameters);
  }

  @Test
  public void fail_if_uri_does_not_contains_callback() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init");

    underTest.doFilter(request, response, chain);

    assertError("No provider key found in URI");
    verifyNoInteractions(authenticationEvent);
    verifyNoInteractions(auth2AuthenticationParameters);
  }

  @Test
  public void fail_if_identity_provider_class_is_unsupported() throws Exception {
    String unsupportedKey = "unsupported";
    when(request.getRequestURI()).thenReturn("/sessions/init/" + unsupportedKey);
    IdentityProvider identityProvider = new UnsupportedIdentityProvider(unsupportedKey);
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    assertError("Unsupported IdentityProvider class: class org.sonar.server.authentication.InitFilterTest$UnsupportedIdentityProvider");
    verifyNoInteractions(authenticationEvent);
    verifyNoInteractions(auth2AuthenticationParameters);
  }

  @Test
  public void redirect_contains_cookie_with_error_message_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    IdentityProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider("failing");
    when(request.getRequestURI()).thenReturn("/sessions/init/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized");
    verify(authenticationEvent).loginFailure(eq(request), authenticationExceptionCaptor.capture());
    AuthenticationException authenticationException = authenticationExceptionCaptor.getValue();
    assertThat(authenticationException).hasMessage("Email john@email.com is already used");
    assertThat(authenticationException.getSource()).isEqualTo(AuthenticationEvent.Source.external(identityProvider));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getPublicMessage()).isEqualTo("Email john@email.com is already used");
    verifyDeleteAuthCookie();

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
  public void redirect_with_context_path_when_failing_because_of_UnauthorizedException() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    IdentityProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider("failing");
    when(request.getRequestURI()).thenReturn("/sonarqube/sessions/init/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized");
  }

  @Test
  public void redirect_when_failing_because_of_Exception() throws Exception {
    IdentityProvider identityProvider = new FailWithIllegalStateException("failing");
    when(request.getRequestURI()).thenReturn("/sessions/init/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(logTester.logs(Level.WARN)).containsExactlyInAnyOrder("Fail to initialize authentication with provider 'failing'");
    verifyDeleteAuthCookie();
  }

  @Test
  public void redirect_with_context_when_failing_because_of_Exception() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    IdentityProvider identityProvider = new FailWithIllegalStateException("failing");
    when(request.getRequestURI()).thenReturn("/sessions/init/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized");
  }

  private void assertOAuth2InitCalled() {
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(oAuth2IdentityProvider.isInitCalled()).isTrue();
  }

  private void assertBasicInitCalled() {
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
    assertThat(baseIdentityProvider.isInitCalled()).isTrue();
  }

  private void assertError(String expectedError) throws Exception {
    assertThat(logTester.logs(Level.WARN)).contains(expectedError);
    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(oAuth2IdentityProvider.isInitCalled()).isFalse();
  }

  private void verifyDeleteAuthCookie() {
    verify(auth2AuthenticationParameters).delete(request, response);
  }

  private static class FailWithUnauthorizedExceptionIdProvider extends FakeBasicIdentityProvider {

    public FailWithUnauthorizedExceptionIdProvider(String key) {
      super(key, true);
    }

    @Override
    public void init(Context context) {
      throw new UnauthorizedException("Email john@email.com is already used");
    }
  }

  private static class FailWithIllegalStateException extends FakeBasicIdentityProvider {

    public FailWithIllegalStateException(String key) {
      super(key, true);
    }

    @Override
    public void init(Context context) {
      throw new IllegalStateException("Failure !");
    }
  }

  private static class UnsupportedIdentityProvider implements IdentityProvider {
    private final String unsupportedKey;

    public UnsupportedIdentityProvider(String unsupportedKey) {
      this.unsupportedKey = unsupportedKey;
    }

    @Override
    public String getKey() {
      return unsupportedKey;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public Display getDisplay() {
      return null;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public boolean allowsUsersToSignUp() {
      return false;
    }

  }
}
