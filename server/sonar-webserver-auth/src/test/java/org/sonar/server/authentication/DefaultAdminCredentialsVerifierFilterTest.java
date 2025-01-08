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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class DefaultAdminCredentialsVerifierFilterTest {

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final Configuration config = mock(Configuration.class);
  private final DefaultAdminCredentialsVerifier defaultAdminCredentialsVerifier = mock(DefaultAdminCredentialsVerifier.class);
  private final ThreadLocalUserSession session = mock(ThreadLocalUserSession.class);

  private final DefaultAdminCredentialsVerifierFilter underTest = new DefaultAdminCredentialsVerifierFilter(config, defaultAdminCredentialsVerifier, session);

  @Before
  public void before() {
    when(request.getRequestURI()).thenReturn("/");
    when(request.getContextPath()).thenReturn("");

    when(config.getBoolean("sonar.forceRedirectOnDefaultAdminCredentials")).thenReturn(Optional.of(true));
    when(defaultAdminCredentialsVerifier.hasDefaultCredentialUser()).thenReturn(true);
    when(session.hasSession()).thenReturn(true);
    when(session.isLoggedIn()).thenReturn(true);
    when(session.isSystemAdministrator()).thenReturn(true);
  }

  @Test
  public void verify_other_methods() {
    underTest.init();
    underTest.destroy();

    verifyNoInteractions(request, response, chain, session);
  }

  @Test
  public void redirect_if_instance_uses_default_admin_credentials() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/admin/change_admin_password");
  }

  @Test
  public void redirect_if_instance_uses_default_admin_credentials_and_web_context_configured() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/admin/change_admin_password");
  }

  @Test
  public void redirect_if_request_uri_ends_with_slash() throws Exception {
    when(request.getRequestURI()).thenReturn("/projects/");
    when(request.getContextPath()).thenReturn("/sonarqube");

    underTest.doFilter(request, response, chain);

    verify(response).sendRedirect("/sonarqube/admin/change_admin_password");
  }

  @Test
  public void do_not_redirect_if_not_a_system_administrator() throws Exception {
    when(session.isSystemAdministrator()).thenReturn(false);

    underTest.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void do_not_redirect_if_not_logged_in() throws Exception {
    when(session.isLoggedIn()).thenReturn(false);

    underTest.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void do_not_redirect_if_user_is_admin() throws Exception {
    when(session.getLogin()).thenReturn("admin");

    underTest.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void do_not_redirect_if_instance_does_not_use_default_admin_credentials() throws Exception {
    when(defaultAdminCredentialsVerifier.hasDefaultCredentialUser()).thenReturn(false);

    underTest.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void do_not_redirect_if_config_says_so() throws Exception {
    when(config.getBoolean("sonar.forceRedirectOnDefaultAdminCredentials")).thenReturn(Optional.of(false));

    underTest.doFilter(request, response, chain);

    verify(response, never()).sendRedirect(any());
  }

  @Test
  @UseDataProvider("skipped_urls")
  public void doGetPattern_verify(String urltoSkip) throws Exception {
    when(request.getRequestURI()).thenReturn(urltoSkip);
    when(request.getContextPath()).thenReturn("");
    underTest.doGetPattern().matches(urltoSkip);

    verify(response, never()).sendRedirect(any());
  }

  @DataProvider
  public static Object[][] skipped_urls() {
    return new Object[][] {
      {"/batch/index"},
      {"/batch/file"},
      {"/api/issues"},
      {"/api/issues/"},
      {"/api/*"},
      {"/admin/change_admin_password"},
      {"/account/reset_password"},
    };
  }

}
