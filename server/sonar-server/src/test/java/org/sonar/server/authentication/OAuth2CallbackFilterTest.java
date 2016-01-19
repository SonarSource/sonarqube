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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OAuth2CallbackFilterTest {

  static String OAUTH2_PROVIDER_KEY = "github";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public IdentityProviderRepositoryRule identityProviderRepository = new IdentityProviderRepositoryRule();

  OAuth2ContextFactory oAuth2ContextFactory = mock(OAuth2ContextFactory.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  FakeOAuth2IdentityProvider oAuth2IdentityProvider = new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, true);
  OAuth2IdentityProvider.InitContext oauth2Context = mock(OAuth2IdentityProvider.InitContext.class);

  OAuth2CallbackFilter underTest = new OAuth2CallbackFilter(identityProviderRepository, oAuth2ContextFactory);

  @Before
  public void setUp() throws Exception {
    when(oAuth2ContextFactory.newContext(request, response, oAuth2IdentityProvider)).thenReturn(oauth2Context);
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern()).isNotNull();
  }

  @Test
  public void do_filter_on_auth2_identity_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(oAuth2IdentityProvider);

    underTest.doFilter(request, response, chain);

    assertCallbackCalled();
  }

  @Test
  public void fail_on_not_oauth2_provider() throws Exception {
    String providerKey = "openid";
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + providerKey);
    identityProviderRepository.addIdentityProvider(new FakeBasicIdentityProvider(providerKey, true));

    underTest.doFilter(request, response, chain);

    assertError("Not an OAuth2IdentityProvider: class org.sonar.server.authentication.FakeBasicIdentityProvider");
  }

  @Test
  public void fail_on_disabled_provider() throws Exception {
    when(request.getRequestURI()).thenReturn("/oauth2/callback/" + OAUTH2_PROVIDER_KEY);
    identityProviderRepository.addIdentityProvider(new FakeOAuth2IdentityProvider(OAUTH2_PROVIDER_KEY, false));

    underTest.doFilter(request, response, chain);

    assertError("Fail to callback authentication with github");
  }

  private void assertCallbackCalled(){
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(oAuth2IdentityProvider.isCallbackCalled()).isTrue();
  }

  private void assertError(String expectedError) throws Exception {
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains(expectedError);
    verify(response).sendRedirect("/sessions/unauthorized");
    assertThat(oAuth2IdentityProvider.isInitCalled()).isFalse();
  }

}
