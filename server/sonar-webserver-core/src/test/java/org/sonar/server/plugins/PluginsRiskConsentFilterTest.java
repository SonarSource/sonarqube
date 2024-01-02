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
package org.sonar.server.plugins;

import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.web.ServletFilter;
import org.sonar.core.extension.PluginRiskConsent;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.PLUGINS_RISK_CONSENT;

public class PluginsRiskConsentFilterTest {

  private Configuration configuration;
  private ThreadLocalUserSession userSession;

  private ServletRequest servletRequest;
  private HttpServletResponse servletResponse;
  private FilterChain chain;

  @Before
  public void before() {
    configuration = mock(Configuration.class);
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.REQUIRED.name()));
    userSession = mock(ThreadLocalUserSession.class);

    servletRequest = mock(HttpServletRequest.class);
    servletResponse = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  public void doFilter_givenNoUserSession_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenNotLoggedIn_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(false);

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenNotLoggedInAndRequired_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(false);
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.REQUIRED.name()));

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenNotLoggedInAndConsentAccepted_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(false);
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.ACCEPTED.name()));

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenLoggedInNotAdmin_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(false);

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenLoggedInNotAdminAndRequiredConsent_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(false);
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.REQUIRED.name()));

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenLoggedInAdminAndConsentRequired_redirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(true);

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(1)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doFilter_givenLoggedInAdminAndConsentNotRequired_dontRedirect() throws Exception {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(true);
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.ACCEPTED.name()));

    consentFilter.doFilter(servletRequest, servletResponse, chain);

    verify(servletResponse, times(0)).sendRedirect(Mockito.anyString());
  }

  @Test
  public void doGetPattern_excludesNotEmpty() {
    PluginsRiskConsentFilter consentFilter = new PluginsRiskConsentFilter(configuration, userSession);

    ServletFilter.UrlPattern urlPattern = consentFilter.doGetPattern();

    assertThat(urlPattern.getExclusions()).isNotEmpty();

  }
}
