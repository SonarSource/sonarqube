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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class LogOAuthWarningTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();


  private Server server = mock(Server.class);

  @Test
  public void log_warning_at_startup_if_non_secured_base_url_and_oauth_is_installed() {
    when(server.getPublicRootUrl()).thenReturn("http://mydomain.com");

    LogOAuthWarning underTest = new LogOAuthWarning(server, new OAuth2IdentityProvider[1]);

    underTest.start();

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("For security reasons, OAuth authentication should use HTTPS. You should set the property 'Administration > Configuration > Server base URL' to a HTTPS URL.");

    underTest.stop();
  }

  @Test
  public void do_not_log_warning_at_startup_if_secured_base_url_and_oauth_is_installed() {
    when(server.getPublicRootUrl()).thenReturn("https://mydomain.com");

    LogOAuthWarning underTest = new LogOAuthWarning(server, new OAuth2IdentityProvider[1]);

    underTest.start();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();

    underTest.stop();
  }

  @Test
  public void do_not_log_warning_at_startup_if_non_secured_base_url_but_oauth_is_not_installed() {
    when(server.getPublicRootUrl()).thenReturn("http://mydomain.com");

    LogOAuthWarning underTest = new LogOAuthWarning(server);

    underTest.start();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();

    underTest.stop();
  }
}
