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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

public class InitFilterTest {

  static String OAUTH2_PROVIDER_KEY = "github";
  static String BASIC_PROVIDER_KEY = "openid";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  BaseContextFactory baseContextFactory = mock(BaseContextFactory.class);
  OAuth2ContextFactory oAuth2ContextFactory = mock(OAuth2ContextFactory.class);
  Server server = mock(Server.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  FakeOAuth2IdentityProvider oAuth2IdentityProvider = new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true);
  OAuth2IdentityProvider.InitContext oauth2Context = mock(OAuth2IdentityProvider.InitContext.class);

  FakeBasicIdentityProvider baseIdentityProvider = new FakeBasicIdentityProvider(BASIC_PROVIDER_KEY, true);
  BaseIdentityProvider.Context baseContext = mock(BaseIdentityProvider.Context.class);

  InitFilter underTest = new InitFilter(identityProviderRepository, baseContextFactory, oAuth2ContextFactory, server);

  @Before
  public void setUp() throws Exception {
    when(oAuth2ContextFactory.newContext(request, response, oAuth2IdentityProvider)).thenReturn(oauth2Context);
    when(baseContextFactory.newContext(request, response, baseIdentityProvider)).thenReturn(baseContext);
    when(server.getContextPath()).thenReturn("");
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern()).isNotNull();
  }

  @Test
  public void do_filter_with_context() throws Exception {
    when(server.getContextPath()).thenReturn("/sonarqube");
    when(request.getRequestURI()).thenReturn("/sonarqube/sessions/init/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertOAuth2InitCalled();
  }

  @Test
  public void do_filter_on_auth2_identity_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertOAuth2InitCalled();
  }

  @Test
  public void do_filter_on_basic_identity_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init/" + BASIC_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(baseIdentityProvider);

    underTest.doFilter(request, response, chain);

    assertBasicInitCalled();
  }

  @Test
  public void fail_if_identity_provider_key_is_empty() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init/");

    underTest.doFilter(request, response, chain);

    assertError("Fail to initialize authentication with provider ''");
  }

  @Test
  public void fail_if_uri_doesnt_contains_callback() throws Exception {
    when(request.getRequestURI()).thenReturn("/sessions/init");

    underTest.doFilter(request, response, chain);

    assertError("Fail to initialize authentication with provider ''");
  }

  @Test
  public void fail_if_identity_provider_class_is_unsupported() throws Exception {
    final String unsupportedKey = "unsupported";
    when(request.getRequestURI()).thenReturn("/sessions/init/" + unsupportedKey);
    identityProviderRepository.addIdentityProvider(new IdentityProvider() {
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
    });

    underTest.doFilter(request, response, chain);

    assertError("Fail to initialize authentication with provider 'unsupported'");
  }

  @Test
  public void redirect_when_failing_because_of_UnauthorizedExceptionException() throws Exception {
    IdentityProvider identityProvider = new FailWithUnauthorizedExceptionIdProvider("failing");
    when(request.getRequestURI()).thenReturn("/sessions/init/" + identityProvider.getKey());
    identityProviderRepository.addIdentityProvider(identityProvider);

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sessions/unauthorized?message=Email+john%40email.com+is+already+used");
  }

  private void assertOAuth2InitCalled() {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(oAuth2IdentityProvider.isInitCalled()).isTrue();
  }

  private void assertBasicInitCalled() {
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(baseIdentityProvider.isInitCalled()).isTrue();
  }

  private void assertError(String expectedError) throws Exception {
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains(expectedError);
    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(oAuth2IdentityProvider.isInitCalled()).isFalse();
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
}
